using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;

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
        private const int WdReplaceAll = 2;
        private const string MarkerName = "UnicodeToNudiSelection";
        private const int WdNoProofing = 1024;

        // Nudi characters that Word may draw with a non-Latin font slot: ¹ = ಸಿ, ² = ಶಿ
        private static readonly char[] FontSensitiveChars = { '\u00B9', '\u00B2' };

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

        /// <param name="progress">Called with 0-100 while converting (from this thread).</param>
        public static Result ConvertSelection(object wordApplication, string nudiFont, Action<int> progress)
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
            // Replace All applies AutoFormat "smart quotes" to the text it inserts,
            // which turns « (ವಿ) and » (ಹಿ) into “ and ”. Switch it off meanwhile.
            dynamic options = word.Options;
            bool smartQuotes = options.AutoFormatAsYouTypeReplaceQuotes;
            options.AutoFormatAsYouTypeReplaceQuotes = false;
            // A bookmark keeps track of the selection while its text changes length
            dynamic marker = doc.Bookmarks.Add(MarkerName, doc.Range(selStart, selEnd));
            try
            {
                string text = marker.Range.Text;
                if (NudiConverter.ContainsKannada(text))
                {
                    ReplaceAllWords(marker, text, nudiFont, result, progress);

                    // Whatever Replace All could not handle: word by word
                    selStart = marker.Range.Start;
                    selEnd = marker.Range.End;
                    string rest = doc.Range(selStart, selEnd).Text;
                    if (NudiConverter.ContainsKannada(rest))
                    {
                        ConvertRuns(doc, selStart, selEnd, rest, nudiFont, result);
                    }

                    VerifyAndRepair(doc, marker, text, nudiFont);
                    progress?.Invoke(100);
                }

                marker.Range.Select();
            }
            finally
            {
                try
                {
                    marker.Delete();
                }
                catch (Exception)
                {
                    // ignore
                }
                ResetFind(doc);
                options.AutoFormatAsYouTypeReplaceQuotes = smartQuotes;
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
                ShowStatus(word, "Unicode to Nudi: converted " + result.Converted + " Kannada word(s)");
            }
            return result;
        }

        /// <summary>
        /// Fast path: one Word "Replace All" per distinct Kannada word, which also
        /// applies the Nudi font and turns off spell-check for the new text. Word
        /// does the work internally, instead of several round trips (each followed
        /// by re-layout and spell-check) for every single word.
        ///
        /// Longest words go first, so a short word can only ever match a whole
        /// remaining word, never part of a longer one that is still in Unicode.
        /// </summary>
        private static void ReplaceAllWords(dynamic marker, string text, string nudiFont, Result result, Action<int> progress)
        {
            var counts = new Dictionary<string, int>();
            foreach (string run in KannadaRuns(text))
            {
                counts.TryGetValue(run, out int count);
                counts[run] = count + 1;
            }
            var words = new List<string>(counts.Keys);
            words.Sort((a, b) => b.Length.CompareTo(a.Length));

            for (int k = 0; k < words.Count; k++)
            {
                string run = words[k];
                string nudi = NudiConverter.Convert(run);
                if (run.Length > MaxFindLength || nudi.Length > MaxFindLength)
                {
                    continue; // left for the word-by-word pass
                }

                dynamic range = marker.Range;
                dynamic find = range.Find;
                PrepareFind(find, run);
                dynamic replacement = find.Replacement;
                replacement.ClearFormatting();
                replacement.Text = nudi;
                SetNudiFont(replacement.Font, nudiFont);
                replacement.NoProofing = true;
                try
                {
                    // No language: no red underlines, and no Kannada font rules for the Nudi text
                    replacement.LanguageID = WdNoProofing;
                }
                catch (Exception)
                {
                    // ignore
                }
                find.Format = true;
                if ((bool)find.Execute(Replace: WdReplaceAll))
                {
                    result.Converted += counts[run];
                }

                if (k % 10 == 0)
                {
                    // The last 10% is for the font fix-up pass
                    progress?.Invoke(k * 90 / words.Count);
                }
            }
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
            PrepareFind(find, run);
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
            SetNudiFont(converted.Font, nudiFont);
            converted.NoProofing = true;
            try
            {
                converted.LanguageID = WdNoProofing;
            }
            catch (Exception)
            {
                // ignore
            }
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
                find.Format = false;
                find.Replacement.ClearFormatting();
                find.Replacement.Text = "";
            }
            catch (Exception)
            {
                // ignore
            }
        }

        /// <summary>
        /// Checks the converted text against what it should be and fixes it.
        ///
        /// The expected text is the original selection with each Kannada word
        /// converted. When Word's text lines up position for position with the
        /// document (no hidden field codes etc.), any character Word changed on its
        /// own (e.g. AutoFormat) is put back, and every ¹ (ಸಿ) / ² (ಶಿ) the
        /// conversion produced gets the Nudi font set directly in all font slots.
        /// Otherwise the ¹/² font fix falls back to searching for them.
        /// </summary>
        private static void VerifyAndRepair(dynamic doc, dynamic marker, string original, string nudiFont)
        {
            var expected = new StringBuilder(original.Length * 2);
            var converted = new List<bool>(original.Length * 2);
            int i = 0;
            while (i < original.Length)
            {
                if (!IsRunChar(original[i]))
                {
                    expected.Append(original[i]);
                    converted.Add(false);
                    i++;
                    continue;
                }
                int start = i;
                while (i < original.Length && IsRunChar(original[i]))
                {
                    i++;
                }
                string nudi = NudiConverter.Convert(original.Substring(start, i - start));
                expected.Append(nudi);
                for (int k = 0; k < nudi.Length; k++)
                {
                    converted.Add(true);
                }
            }

            dynamic range = marker.Range;
            int rangeStart = range.Start;
            int rangeEnd = range.End;
            string actual = range.Text;
            if (actual.Length != expected.Length || rangeEnd - rangeStart != actual.Length)
            {
                FixFontSensitiveChars(doc, marker, nudiFont);
                return;
            }

            for (int k = 0; k < actual.Length; k++)
            {
                if (!converted[k])
                {
                    continue;
                }
                char want = expected[k];
                bool wrongChar = actual[k] != want;
                bool fontSensitive = Array.IndexOf(FontSensitiveChars, want) >= 0;
                if (!wrongChar && !fontSensitive)
                {
                    continue;
                }
                dynamic one = doc.Range(rangeStart + k, rangeStart + k + 1);
                if (wrongChar)
                {
                    one.Text = want.ToString();
                    one = doc.Range(rangeStart + k, rangeStart + k + 1);
                    one.NoProofing = true;
                }
                SetNudiFont(one.Font, nudiFont);
            }
        }

        /// <summary>
        /// Sets the Nudi font directly on every ¹ (ಸಿ) and ² (ಶಿ) produced by the
        /// conversion. Word treats these as digits and may draw them with the complex
        /// script or East Asian font slot, where they appear as superscript 1 and 2.
        /// Formatting set through Replace All does not always reach those slots, so
        /// each occurrence is fixed directly.
        /// </summary>
        private static void FixFontSensitiveChars(dynamic doc, dynamic marker, string nudiFont)
        {
            foreach (char ch in FontSensitiveChars)
            {
                string c = ch.ToString();
                int end = marker.Range.End;
                int from = marker.Range.Start;
                while (from < end)
                {
                    dynamic range = doc.Range(from, end);
                    dynamic find = range.Find;
                    PrepareFind(find, c);
                    if (!(bool)find.Execute())
                    {
                        break;
                    }
                    int start = range.Start;
                    if (start >= end)
                    {
                        break;
                    }
                    dynamic found = doc.Range(start, start + 1);
                    dynamic font = found.Font;
                    // Only characters this conversion produced (already Nudi in the
                    // Latin slot); leave e.g. an English "km²" alone
                    if ((string)font.NameAscii == nudiFont)
                    {
                        SetNudiFont(font, nudiFont);
                    }
                    from = start + 1;
                }
            }
        }

        /// <summary>Plain, exact, case- and mark-sensitive search for <paramref name="text"/>.</summary>
        private static void PrepareFind(dynamic find, string text)
        {
            find.ClearFormatting();
            find.Text = text;
            find.MatchWildcards = false;
            find.MatchCase = true;
            find.MatchWholeWord = false;
            find.MatchSoundsLike = false;
            find.MatchAllWordForms = false;
            try
            {
                // Don't let Word ignore vowel signs or ZWJ/ZWNJ while matching
                find.MatchDiacritics = true;
                find.MatchControl = true;
            }
            catch (Exception)
            {
                // Properties missing in some Word versions
            }
            find.Forward = true;
            find.Wrap = WdFindStop;
            find.Format = false;
        }

        /// <summary>
        /// Sets the Nudi font in every font slot. Word draws some Latin-1 characters
        /// with the East Asian font (e.g. "¹" = ಸಿ and "²" = ಶಿ, which otherwise show
        /// up as superscript 1 and 2), so setting only the Latin font is not enough.
        /// </summary>
        private static void SetNudiFont(dynamic font, string nudiFont)
        {
            font.Name = nudiFont;
            font.NameAscii = nudiFont;
            font.NameOther = nudiFont;
            try
            {
                font.NameFarEast = nudiFont;
            }
            catch (Exception)
            {
                // ignore
            }
            try
            {
                font.NameBi = nudiFont;
            }
            catch (Exception)
            {
                // ignore
            }
        }

        private static void ShowStatus(dynamic word, string text)
        {
            try
            {
                word.StatusBar = text;
            }
            catch (Exception)
            {
                // ignore
            }
        }

        /// <summary>Maximal runs of Kannada characters in <paramref name="text"/>.</summary>
        private static IEnumerable<string> KannadaRuns(string text)
        {
            int i = 0;
            while (i < text.Length)
            {
                if (!IsRunChar(text[i]))
                {
                    i++;
                    continue;
                }
                int start = i;
                while (i < text.Length && IsRunChar(text[i]))
                {
                    i++;
                }
                yield return text.Substring(start, i - start);
            }
        }

        private static bool IsRunChar(char c)
        {
            return NudiConverter.IsKannada(c) || c == '‌' || c == '‍';
        }
    }
}
