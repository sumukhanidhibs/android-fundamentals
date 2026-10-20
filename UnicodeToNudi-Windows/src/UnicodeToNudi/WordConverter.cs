using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;

namespace UnicodeToNudi
{
    /// <summary>
    /// Converts the selection in Microsoft Word through COM automation.
    ///
    /// Only Kannada text is touched: each run of Kannada characters is replaced by
    /// its Nudi text and given the Nudi font. English words, numbers, punctuation
    /// and their fonts/formatting stay exactly as they were.
    /// </summary>
    internal static class WordConverter
    {
        private const int WdFindStop = 0;

        // Word's Find has a 255-character limit on the search text
        private const int MaxFindLength = 255;

        internal sealed class Result
        {
            public int Converted;
            public int Skipped;
            public bool HadSelection;
        }

        /// <summary>Returns the running Word instance, or null if Word is not running.</summary>
        public static object GetRunningWord()
        {
            try
            {
                return Marshal.GetActiveObject("Word.Application");
            }
            catch (COMException)
            {
                return null;
            }
        }

        public static Result ConvertSelection(object wordApplication, string nudiFont)
        {
            dynamic word = wordApplication;
            dynamic selection = word.Selection;
            var result = new Result();

            int selStart = selection.Start;
            int selEnd = selection.End;
            if (selEnd <= selStart)
            {
                return result;
            }
            result.HadSelection = true;

            dynamic doc = selection.Range.Document;
            dynamic undo = null;
            try
            {
                // Word 2010+: one Ctrl+Z undoes the whole conversion
                undo = word.UndoRecord;
                undo.StartCustomRecord("Unicode to Nudi");
            }
            catch (Exception)
            {
                undo = null;
            }

            bool screenUpdating = word.ScreenUpdating;
            word.ScreenUpdating = false;
            try
            {
                string text = doc.Range(selStart, selEnd).Text;
                if (NudiConverter.ContainsKannada(text))
                {
                    selEnd = ConvertRuns(doc, selStart, selEnd, text, nudiFont, result);
                }

                doc.Range(selStart, selEnd).Select();
            }
            finally
            {
                ResetFind(doc);
                word.ScreenUpdating = screenUpdating;
                if (undo != null)
                {
                    try
                    {
                        undo.EndCustomRecord();
                    }
                    catch (Exception)
                    {
                        // ignore
                    }
                }
            }

            if (result.Converted > 0)
            {
                try
                {
                    word.StatusBar = "Unicode to Nudi: converted " + result.Converted + " Kannada word(s)";
                }
                catch (Exception)
                {
                    // ignore
                }
            }
            return result;
        }

        /// <summary>
        /// Replaces each Kannada word (maximal run of Kannada characters) in the
        /// selection, left to right.
        ///
        /// The run's document position is normally its offset in the selection's
        /// text. Fields, tables and other objects can shift that, so if the text at
        /// the expected position doesn't match, the run is located with a plain
        /// (non-wildcard) search from the current position instead. A run that
        /// can't be located exactly is skipped, never guessed.
        ///
        /// Word's wildcard search is not used: it treats a letter plus its vowel
        /// sign (ಸಾ) as one character, so a pattern like [ಀ-೿]@ only matches bare
        /// letters and words come out half converted.
        /// </summary>
        private static int ConvertRuns(dynamic doc, int selStart, int selEnd, string text, string nudiFont, Result result)
        {
            int shift = 0;       // document position minus text offset, adjusted as we go
            int cursor = selStart;
            int i = 0;
            while (i < text.Length)
            {
                if (!IsRunChar(text[i]))
                {
                    i++;
                    continue;
                }
                int runStart = i;
                while (i < text.Length && IsRunChar(text[i]))
                {
                    i++;
                }
                string run = text.Substring(runStart, i - runStart);

                int start = selStart + runStart + shift;
                if (start < cursor || start + run.Length > selEnd || doc.Range(start, start + run.Length).Text != run)
                {
                    start = Locate(doc, run, cursor, selEnd);
                    if (start < 0)
                    {
                        result.Skipped++;
                        continue;
                    }
                }

                int newLength = Replace(doc, start, start + run.Length, nudiFont);
                result.Converted++;
                selEnd += newLength - run.Length;
                cursor = start + newLength;
                shift = cursor - (selStart + i);
            }
            return selEnd;
        }

        /// <summary>Finds the exact text of <paramref name="run"/> in [from, to); returns its start or -1.</summary>
        private static int Locate(dynamic doc, string run, int from, int to)
        {
            if (run.Length > MaxFindLength || from >= to)
            {
                return -1;
            }
            dynamic range = doc.Range(from, to);
            dynamic find = range.Find;
            find.ClearFormatting();
            find.Text = run;
            find.MatchWildcards = false;
            find.MatchCase = true;
            find.MatchWholeWord = false;
            find.MatchSoundsLike = false;
            find.MatchAllWordForms = false;
            try
            {
                // Don't let Word ignore vowel signs while matching
                find.MatchDiacritics = true;
            }
            catch (Exception)
            {
                // Property missing in some Word versions
            }
            find.Forward = true;
            find.Wrap = WdFindStop;
            find.Format = false;
            if (!(bool)find.Execute())
            {
                return -1;
            }
            int start = range.Start;
            int end = range.End;
            if (end > to || doc.Range(start, end).Text != run)
            {
                return -1;
            }
            return start;
        }

        /// <summary>Replaces document text [start, end) with Nudi; returns the new length.</summary>
        private static int Replace(dynamic doc, int start, int end, string nudiFont)
        {
            dynamic range = doc.Range(start, end);
            string unicode = range.Text;
            string nudi = NudiConverter.Convert(unicode);
            range.Text = nudi;
            dynamic converted = doc.Range(start, start + nudi.Length);
            // Nudi text is plain Latin-1, so set the Latin ("ASCII"/"Other") fonts
            converted.Font.Name = nudiFont;
            converted.Font.NameAscii = nudiFont;
            converted.Font.NameOther = nudiFont;
            return nudi.Length;
        }

        /// <summary>Word remembers the last search; don't leave our wildcard pattern in the user's Find box.</summary>
        private static void ResetFind(dynamic doc)
        {
            try
            {
                dynamic find = doc.Range(0, 0).Find;
                find.ClearFormatting();
                find.Text = "";
                find.MatchWildcards = false;
            }
            catch (Exception)
            {
                // ignore
            }
        }

        private static bool IsRunChar(char c)
        {
            return NudiConverter.IsKannada(c) || c == '‌' || c == '‍';
        }
    }
}
