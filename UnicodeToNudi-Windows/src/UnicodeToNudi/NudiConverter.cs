using System.Collections.Generic;
using System.Text;

namespace UnicodeToNudi
{
    /// <summary>
    /// Converts Kannada Unicode text into the ASCII (ANSI) encoding used by the
    /// Nudi fonts (Nudi 01 e, Nudi Akshara, ...).
    ///
    /// C# port of UnicodeToNudi/app/src/main/java/com/example/unicodetonudi/NudiConverter.java.
    /// Keep the two in sync; tests/NudiConverter.Tests/vectors.tsv is shared test data.
    ///
    /// Unicode stores Kannada in logical order (ಕ್ರೀ = ಕ + ್ + ರ + ೀ), Nudi in
    /// glyph order (ಕ್ರೀ = "Q" ಕಿ + "æ" ರ ottu + "Ã" deergha = "QæÃ").
    /// Each akshara  [ರ್] consonant [್ consonant]* [vowel sign] [ಂ/ಃ]  is written as
    /// base glyph → ottaksharas → trailing deergha → arkavattu → ಂ/ಃ.
    /// </summary>
    public static class NudiConverter
    {
        private const char Halant = '್';     // ್
        private const char Nukta = '಼';      // ಼
        private const char Zwnj = '‌';
        private const char Zwj = '‍';
        private const char Anusvara = 'ಂ';   // ಂ
        private const char Visarga = 'ಃ';    // ಃ
        private const char Ra = 'ರ';         // ರ

        private const char SignAa = 'ಾ';     // ಾ
        private const char SignI = 'ಿ';      // ಿ
        private const char SignIi = 'ೀ';     // ೀ
        private const char SignU = 'ು';      // ು
        private const char SignUu = 'ೂ';     // ೂ
        private const char SignR = 'ೃ';      // ೃ
        private const char SignE = 'ೆ';      // ೆ
        private const char SignEe = 'ೇ';     // ೇ
        private const char SignAi = 'ೈ';     // ೈ
        private const char SignO = 'ೊ';      // ೊ
        private const char SignOo = 'ೋ';     // ೋ
        private const char SignAu = 'ೌ';     // ೌ

        private const string NudiHalant = "ï";
        private const string NudiDeergha = "Ã";
        private const string NudiAi = "Ê";
        private const string NudiArkavattu = "ð";
        private const string NudiRaOttu = "æ";
        private const string NudiRaOttuLower = "ç";

        /// <summary>How one consonant is drawn in Nudi with each vowel.</summary>
        private sealed class Glyphs
        {
            public readonly string Stem;      // used with ಾ, ೌ and ್
            public readonly string Inherent;  // the letter with its "a" (ಕ)
            public readonly string IForm;     // ಕಿ
            public readonly string EForm;     // ಕೆ
            public readonly string OForm;     // ಕೊ
            public readonly string USign;     // ು part
            public readonly string UuSign;    // ೂ part

            public Glyphs(string stem, string inherent, string iForm, string eForm,
                          string oForm, string uSign, string uuSign)
            {
                Stem = stem;
                Inherent = inherent;
                IForm = iForm;
                EForm = eForm;
                OForm = oForm;
                USign = uSign;
                UuSign = uuSign;
            }
        }

        private static readonly Dictionary<char, Glyphs> Consonants = new Dictionary<char, Glyphs>();
        private static readonly Dictionary<char, string> Ottaksharas = new Dictionary<char, string>();
        private static readonly Dictionary<char, string> IndependentVowels = new Dictionary<char, string>();

        static NudiConverter()
        {
            // Regular consonants: ಕೆ = stem + "É", ಕೊ = stem + "ÉÆ"
            Regular('ಕ', "P", "PÀ", "Q");
            Regular('ಖ', "S", "R", "T");
            Regular('ಗ', "U", "UÀ", "V");
            Regular('ಘ', "W", "WÀ", "X");
            Regular('ಙ', "Y", "Y", "YÂ");
            Regular('ಚ', "Z", "ZÀ", "a");
            Regular('ಛ', "b", "bÀ", "c");
            Regular('ಜ', "e", "d", "f");
            Regular('ಞ', "k", "k", "kÂ");
            Regular('ಟ', "m", "l", "n");
            Regular('ಠ', "o", "oÀ", "p");
            Regular('ಡ', "q", "qÀ", "r");
            Regular('ಢ', "qs", "qsÀ", "rü");
            Regular('ಣ', "u", "t", "tÂ");
            Regular('ತ', "v", "vÀ", "w");
            Regular('ಥ', "x", "xÀ", "y");
            Regular('ದ', "z", "zÀ", "¢");
            Regular('ಧ', "zs", "zsÀ", "¢ü");
            Regular('ನ', "£", "£À", "¤");
            Regular('ಬ', "¨", "§", "©");
            Regular('ಭ', "¨s", "¨sÀ", "©ü");
            Regular('ರ', "g", "gÀ", "j");
            Regular('ಲ', "¯", "®", "°");
            Regular('ಳ', "¼", "¼À", "½");
            Regular('ಶ', "±", "±À", "²");
            Regular('ಷ', "µ", "µÀ", "¶");
            Regular('ಸ', "¸", "¸À", "¹");
            Regular('ಹ', "º", "ºÀ", "»");

            // ಪ, ಫ, ವ use a different ು / ೂ / ೊ hook
            Consonants['ಪ'] = new Glyphs("¥", "¥À", "¦", "¥É", "¥ÉÇ", "Å", "Ç");
            Consonants['ಫ'] = new Glyphs("¥s", "¥sÀ", "¦ü", "¥sÉ", "¥sÉÇ", "Å", "Ç");
            Consonants['ವ'] = new Glyphs("ª", "ªÀ", "«", "ªÉ", "ªÉÇ", "Å", "Ç");

            // ಮ, ಯ, ಝ are built from several glyph pieces
            Consonants['ಮ'] = new Glyphs("ªÀi", "ªÀÄ", "«Ä", "ªÉÄ", "ªÉÆ", "Ä", "Æ");
            Consonants['ಯ'] = new Glyphs("AiÀi", "AiÀÄ", "¬Ä", "AiÉÄ", "AiÉÆ", "Ä", "Æ");
            Consonants['ಝ'] = new Glyphs("gÀhi", "gÀhÄ", "jhÄ", "gÉhÄ", "gÉhÆ", "Ä", "Æ");

            // Ottaksharas (vattu / subscript consonants)
            string[,] ottu =
            {
                {"ಕ", "Ì"}, {"ಖ", "Í"}, {"ಗ", "Î"}, {"ಘ", "Ï"}, {"ಞ", "Õ"},
                {"ಚ", "Ñ"}, {"ಛ", "Ò"}, {"ಜ", "Ó"}, {"ಝ", "Ô"},
                {"ಟ", "Ö"}, {"ಠ", "×"}, {"ಡ", "Ø"}, {"ಢ", "Ù"}, {"ಣ", "Ú"},
                {"ತ", "Û"}, {"ಥ", "Ü"}, {"ದ", "Ý"}, {"ಧ", "Þ"}, {"ನ", "ß"},
                {"ಪ", "à"}, {"ಫ", "á"}, {"ಬ", "â"}, {"ಭ", "ã"}, {"ಮ", "ä"},
                {"ಯ", "å"}, {"ರ", NudiRaOttu}, {"ಲ", "è"}, {"ವ", "é"},
                {"ಶ", "ê"}, {"ಷ", "ë"}, {"ಸ", "ì"}, {"ಹ", "í"}, {"ಳ", "î"},
            };
            for (int k = 0; k < ottu.GetLength(0); k++)
            {
                Ottaksharas[ottu[k, 0][0]] = ottu[k, 1];
            }

            string[,] vowels =
            {
                {"ಅ", "C"}, {"ಆ", "D"}, {"ಇ", "E"}, {"ಈ", "F"}, {"ಉ", "G"},
                {"ಊ", "H"}, {"ಋ", "IÄ"}, {"ಎ", "J"}, {"ಏ", "K"}, {"ಐ", "L"},
                {"ಒ", "M"}, {"ಓ", "N"}, {"ಔ", "O"},
            };
            for (int k = 0; k < vowels.GetLength(0); k++)
            {
                IndependentVowels[vowels[k, 0][0]] = vowels[k, 1];
            }
        }

        private static void Regular(char c, string stem, string inherent, string iForm)
        {
            Consonants[c] = new Glyphs(stem, inherent, iForm, stem + "É", stem + "ÉÆ", "Ä", "Æ");
        }

        /// <summary>Converts Kannada Unicode text to Nudi ASCII. Non-Kannada characters are kept as-is.</summary>
        public static string Convert(string text)
        {
            if (string.IsNullOrEmpty(text))
            {
                return "";
            }
            var output = new StringBuilder(text.Length * 2);
            int i = 0;
            int n = text.Length;
            while (i < n)
            {
                char c = text[i];
                if (Consonants.ContainsKey(c))
                {
                    i = ConvertAkshara(text, i, output);
                }
                else if (IndependentVowels.TryGetValue(c, out string vowel))
                {
                    output.Append(vowel);
                    i++;
                }
                else if (c == Anusvara)
                {
                    output.Append('A');
                    i++;
                }
                else if (c == Visarga)
                {
                    output.Append('B');
                    i++;
                }
                else if (c >= '೦' && c <= '೯')
                {
                    // Kannada digits → ASCII digits
                    output.Append((char)('0' + (c - '೦')));
                    i++;
                }
                else if (c == Zwj || c == Zwnj || c == Nukta)
                {
                    i++;
                }
                else
                {
                    output.Append(c);
                    i++;
                }
            }
            return output.ToString();
        }

        /// <summary>True for characters of the Kannada Unicode block.</summary>
        public static bool IsKannada(char c)
        {
            return c >= 'ಀ' && c <= '೿';
        }

        /// <summary>Returns true if the text contains at least one Kannada Unicode character.</summary>
        public static bool ContainsKannada(string text)
        {
            if (text == null)
            {
                return false;
            }
            foreach (char c in text)
            {
                if (IsKannada(c))
                {
                    return true;
                }
            }
            return false;
        }

        /// <summary>Converts one akshara starting at <paramref name="start"/>; returns the index after it.</summary>
        private static int ConvertAkshara(string text, int start, StringBuilder output)
        {
            int n = text.Length;
            int i = start;

            // 1. Collect the consonant cluster: C (್ C)*
            var cluster = new List<char> { text[i++] };
            i = SkipNukta(text, i);
            while (i + 1 < n && text[i] == Halant && Consonants.ContainsKey(text[i + 1]))
            {
                cluster.Add(text[i + 1]);
                i = SkipNukta(text, i + 2);
            }

            // 2. Vowel sign, halant, or the inherent "a"
            char vowel = '\0';
            if (i < n && IsVowelSign(text[i]))
            {
                vowel = text[i++];
            }
            else if (i < n && text[i] == Halant)
            {
                vowel = Halant;
                i++;
                while (i < n && (text[i] == Zwj || text[i] == Zwnj))
                {
                    i++;
                }
            }

            // 3. ರ್ at the start of a cluster is written as arkavattu after the letter
            bool repha = cluster.Count > 1 && cluster[0] == Ra;
            if (repha)
            {
                cluster.RemoveAt(0);
            }

            // 4. Base glyph = first consonant + vowel; deergha/ai marks go after the ottus
            Glyphs b = Consonants[cluster[0]];
            string trailing = "";
            switch (vowel)
            {
                case '\0':
                    output.Append(b.Inherent);
                    break;
                case Halant:
                    // ಸ್ಟ್ = ಸ್ glyph + ಟ ottu ("¸ïÖ")
                    output.Append(b.Stem).Append(NudiHalant);
                    break;
                case SignAa:
                    output.Append(b.Stem).Append('Á');
                    break;
                case SignI:
                    output.Append(b.IForm);
                    break;
                case SignIi:
                    output.Append(b.IForm);
                    trailing = NudiDeergha;
                    break;
                case SignU:
                    output.Append(b.Inherent).Append(b.USign);
                    break;
                case SignUu:
                    output.Append(b.Inherent).Append(b.UuSign);
                    break;
                case SignR:
                    output.Append(b.Inherent).Append('È');
                    break;
                case SignE:
                    output.Append(b.EForm);
                    break;
                case SignEe:
                    output.Append(b.EForm);
                    trailing = NudiDeergha;
                    break;
                case SignAi:
                    output.Append(b.EForm);
                    trailing = NudiAi;
                    break;
                case SignO:
                    output.Append(b.OForm);
                    break;
                case SignOo:
                    output.Append(b.OForm);
                    trailing = NudiDeergha;
                    break;
                case SignAu:
                    output.Append(b.Stem).Append('Ë');
                    break;
                default:
                    output.Append(b.Inherent);
                    break;
            }

            // 5. Ottaksharas for the rest of the cluster
            for (int k = 1; k < cluster.Count; k++)
            {
                char c = cluster[k];
                if (c == Ra && k > 1)
                {
                    output.Append(NudiRaOttuLower);
                }
                else if (Ottaksharas.TryGetValue(c, out string ottu))
                {
                    output.Append(ottu);
                }
                else
                {
                    // No ottu glyph (e.g. ಙ): fall back to halant + full letter
                    output.Append(NudiHalant).Append(Consonants[c].Inherent);
                }
            }

            output.Append(trailing);
            if (repha)
            {
                output.Append(NudiArkavattu);
            }
            return i;
        }

        private static int SkipNukta(string text, int i)
        {
            return (i < text.Length && text[i] == Nukta) ? i + 1 : i;
        }

        private static bool IsVowelSign(char c)
        {
            return (c >= SignAa && c <= SignR) || (c >= SignE && c <= SignAi)
                   || (c >= SignO && c <= SignAu);
        }
    }
}
