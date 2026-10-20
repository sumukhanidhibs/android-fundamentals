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
        private const int WdCollapseEnd = 0;
        private const int WdFindStop = 0;

        // One run of Kannada letters (plus ZWJ/ZWNJ, which belong inside Kannada words).
        // Word wildcards accept Unicode character ranges.
        private const string KannadaRunPattern = "[ಀ-೿‌‍]@";

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
                try
                {
                    selEnd = ConvertWithFind(doc, selStart, selEnd, nudiFont, result);
                }
                catch (COMException)
                {
                    // Wildcard search not supported here; fall back to text offsets
                    result.Converted = 0;
                }

                if (result.Converted == 0)
                {
                    dynamic range = doc.Range(selStart, selEnd);
                    string text = range.Text;
                    if (NudiConverter.ContainsKannada(text))
                    {
                        selEnd = ConvertByOffsets(doc, selStart, selEnd, text, nudiFont, result);
                    }
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

        /// <summary>Finds each Kannada run with Word's wildcard search and replaces it.</summary>
        private static int ConvertWithFind(dynamic doc, int selStart, int selEnd, string nudiFont, Result result)
        {
            dynamic range = doc.Range(selStart, selEnd);
            while (true)
            {
                dynamic find = range.Find;
                find.ClearFormatting();
                find.Text = KannadaRunPattern;
                find.MatchWildcards = true;
                find.Forward = true;
                find.Wrap = WdFindStop;
                find.Format = false;
                if (!(bool)find.Execute())
                {
                    break;
                }

                int start = range.Start;
                int end = range.End;
                if (start >= selEnd)
                {
                    break;
                }
                if (end > selEnd)
                {
                    // Run continues past the selection: convert only the selected part
                    end = selEnd;
                }

                int newLength = Replace(doc, start, end, nudiFont);
                result.Converted++;
                selEnd += newLength - (end - start);

                range = doc.Range(start + newLength, selEnd);
                if (range.Start >= selEnd)
                {
                    break;
                }
            }
            return selEnd;
        }

        /// <summary>
        /// Fallback: locate Kannada runs in the selection's text and map them to
        /// document positions. Runs whose position can't be confirmed (e.g. next to
        /// fields or table markers) are skipped rather than risk changing other text.
        /// </summary>
        private static int ConvertByOffsets(dynamic doc, int selStart, int selEnd, string text, string nudiFont, Result result)
        {
            var runs = new List<KeyValuePair<int, int>>();
            int i = 0;
            while (i < text.Length)
            {
                if (IsRunChar(text[i]))
                {
                    int start = i;
                    while (i < text.Length && IsRunChar(text[i]))
                    {
                        i++;
                    }
                    runs.Add(new KeyValuePair<int, int>(start, i - start));
                }
                else
                {
                    i++;
                }
            }

            // Work backwards so earlier positions stay valid
            for (int k = runs.Count - 1; k >= 0; k--)
            {
                int start = selStart + runs[k].Key;
                int end = start + runs[k].Value;
                string expected = text.Substring(runs[k].Key, runs[k].Value);
                string actual = doc.Range(start, end).Text;
                if (actual != expected)
                {
                    result.Skipped++;
                    continue;
                }
                int newLength = Replace(doc, start, end, nudiFont);
                result.Converted++;
                selEnd += newLength - (end - start);
            }
            return selEnd;
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
