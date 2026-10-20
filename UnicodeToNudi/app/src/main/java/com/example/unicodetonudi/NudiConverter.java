package com.example.unicodetonudi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts Kannada Unicode text into the ASCII (ANSI) encoding used by the
 * Nudi fonts (Nudi 01 e, Nudi Akshara, ...).
 *
 * Unicode stores Kannada in LOGICAL order:
 *   ಕ್ರೀ = ಕ + ್ + ರ + ೀ
 * Nudi stores it in VISUAL (glyph) order, as the letter is drawn:
 *   ಕ್ರೀ = "Q" (ಕಿ) + "æ" (ರ ottu) + "Ã" (deergha) → "QæÃ"
 *
 * So the converter works one akshara (syllable) at a time:
 *   [ರ್ (repha)] consonant [್ consonant]* [vowel sign] [ಂ / ಃ]
 * and emits:
 *   base glyph (consonant + vowel) → ottaksharas → trailing deergha → arkavattu → ಂ/ಃ
 *
 * This class has no Android dependencies so it can be unit tested on the JVM.
 */
public final class NudiConverter {

    private static final char HALANT = '್';      // ್
    private static final char NUKTA = '಼';       // ಼
    private static final char ZWNJ = '‌';
    private static final char ZWJ = '‍';
    private static final char ANUSVARA = 'ಂ';    // ಂ
    private static final char VISARGA = 'ಃ';     // ಃ
    private static final char RA = 'ರ';          // ರ

    // Vowel signs (matras)
    private static final char SIGN_AA = 'ಾ';     // ಾ
    private static final char SIGN_I = 'ಿ';      // ಿ
    private static final char SIGN_II = 'ೀ';     // ೀ
    private static final char SIGN_U = 'ು';      // ು
    private static final char SIGN_UU = 'ೂ';     // ೂ
    private static final char SIGN_R = 'ೃ';      // ೃ
    private static final char SIGN_E = 'ೆ';      // ೆ
    private static final char SIGN_EE = 'ೇ';     // ೇ
    private static final char SIGN_AI = 'ೈ';     // ೈ
    private static final char SIGN_O = 'ೊ';      // ೊ
    private static final char SIGN_OO = 'ೋ';     // ೋ
    private static final char SIGN_AU = 'ೌ';     // ೌ

    private static final String NUDI_HALANT = "ï";
    private static final String NUDI_DEERGHA = "Ã";
    private static final String NUDI_AI = "Ê";
    private static final String NUDI_ARKAVATTU = "ð";
    private static final String NUDI_RA_OTTU = "æ";
    private static final String NUDI_RA_OTTU_LOWER = "ç";

    /** How one consonant is drawn in Nudi with each vowel. */
    private static final class Glyphs {
        final String stem;      // used with ಾ, ೌ and ್
        final String inherent;  // the letter with its "a" (ಕ)
        final String iForm;     // ಕಿ
        final String eForm;     // ಕೆ
        final String oForm;     // ಕೊ
        final String uSign;     // ು part
        final String uuSign;    // ೂ part

        Glyphs(String stem, String inherent, String iForm, String eForm,
               String oForm, String uSign, String uuSign) {
            this.stem = stem;
            this.inherent = inherent;
            this.iForm = iForm;
            this.eForm = eForm;
            this.oForm = oForm;
            this.uSign = uSign;
            this.uuSign = uuSign;
        }
    }

    private static final Map<Character, Glyphs> CONSONANTS = new HashMap<>();
    private static final Map<Character, String> OTTAKSHARAS = new HashMap<>();
    private static final Map<Character, String> INDEPENDENT_VOWELS = new HashMap<>();

    static {
        // Regular consonants: ಕೆ = stem + "É", ಕೊ = stem + "ÉÆ"
        regular('ಕ', "P", "PÀ", "Q");
        regular('ಖ', "S", "R", "T");
        regular('ಗ', "U", "UÀ", "V");
        regular('ಘ', "W", "WÀ", "X");
        regular('ಙ', "Y", "Y", "YÂ");
        regular('ಚ', "Z", "ZÀ", "a");
        regular('ಛ', "b", "bÀ", "c");
        regular('ಜ', "e", "d", "f");
        regular('ಞ', "k", "k", "kÂ");
        regular('ಟ', "m", "l", "n");
        regular('ಠ', "o", "oÀ", "p");
        regular('ಡ', "q", "qÀ", "r");
        regular('ಢ', "qs", "qsÀ", "rü");
        regular('ಣ', "u", "t", "tÂ");
        regular('ತ', "v", "vÀ", "w");
        regular('ಥ', "x", "xÀ", "y");
        regular('ದ', "z", "zÀ", "¢");
        regular('ಧ', "zs", "zsÀ", "¢ü");
        regular('ನ', "£", "£À", "¤");
        regular('ಬ', "¨", "§", "©");
        regular('ಭ', "¨s", "¨sÀ", "©ü");
        regular('ರ', "g", "gÀ", "j");
        regular('ಲ', "¯", "®", "°");
        regular('ಳ', "¼", "¼À", "½");
        regular('ಶ', "±", "±À", "²");
        regular('ಷ', "µ", "µÀ", "¶");
        regular('ಸ', "¸", "¸À", "¹");
        regular('ಹ', "º", "ºÀ", "»");

        // ಪ, ಫ, ವ use a different ು / ೂ / ೊ hook
        CONSONANTS.put('ಪ', new Glyphs("¥", "¥À", "¦", "¥É", "¥ÉÇ", "Å", "Ç"));
        CONSONANTS.put('ಫ', new Glyphs("¥s", "¥sÀ", "¦ü", "¥sÉ", "¥sÉÇ", "Å", "Ç"));
        CONSONANTS.put('ವ', new Glyphs("ª", "ªÀ", "«", "ªÉ", "ªÉÇ", "Å", "Ç"));

        // ಮ, ಯ, ಝ are built from several glyph pieces
        CONSONANTS.put('ಮ', new Glyphs("ªÀi", "ªÀÄ", "«Ä", "ªÉÄ", "ªÉÆ", "Ä", "Æ"));
        CONSONANTS.put('ಯ', new Glyphs("AiÀi", "AiÀÄ", "¬Ä", "AiÉÄ", "AiÉÆ", "Ä", "Æ"));
        CONSONANTS.put('ಝ', new Glyphs("gÀhi", "gÀhÄ", "jhÄ", "gÉhÄ", "gÉhÆ", "Ä", "Æ"));

        // Ottaksharas (vattu / subscript consonants)
        String[][] ottu = {
                {"ಕ", "Ì"}, {"ಖ", "Í"}, {"ಗ", "Î"}, {"ಘ", "Ï"}, {"ಞ", "Õ"},
                {"ಚ", "Ñ"}, {"ಛ", "Ò"}, {"ಜ", "Ó"}, {"ಝ", "Ô"},
                {"ಟ", "Ö"}, {"ಠ", "×"}, {"ಡ", "Ø"}, {"ಢ", "Ù"}, {"ಣ", "Ú"},
                {"ತ", "Û"}, {"ಥ", "Ü"}, {"ದ", "Ý"}, {"ಧ", "Þ"}, {"ನ", "ß"},
                {"ಪ", "à"}, {"ಫ", "á"}, {"ಬ", "â"}, {"ಭ", "ã"}, {"ಮ", "ä"},
                {"ಯ", "å"}, {"ರ", NUDI_RA_OTTU}, {"ಲ", "è"}, {"ವ", "é"},
                {"ಶ", "ê"}, {"ಷ", "ë"}, {"ಸ", "ì"}, {"ಹ", "í"}, {"ಳ", "î"},
        };
        for (String[] pair : ottu) {
            OTTAKSHARAS.put(pair[0].charAt(0), pair[1]);
        }

        String[][] vowels = {
                {"ಅ", "C"}, {"ಆ", "D"}, {"ಇ", "E"}, {"ಈ", "F"}, {"ಉ", "G"},
                {"ಊ", "H"}, {"ಋ", "IÄ"}, {"ಎ", "J"}, {"ಏ", "K"}, {"ಐ", "L"},
                {"ಒ", "M"}, {"ಓ", "N"}, {"ಔ", "O"},
        };
        for (String[] pair : vowels) {
            INDEPENDENT_VOWELS.put(pair[0].charAt(0), pair[1]);
        }
    }

    private static void regular(char c, String stem, String inherent, String iForm) {
        CONSONANTS.put(c, new Glyphs(stem, inherent, iForm, stem + "É", stem + "ÉÆ", "Ä", "Æ"));
    }

    private NudiConverter() {
    }

    /** Converts Kannada Unicode text to Nudi ASCII. Non-Kannada characters are kept as-is. */
    public static String convert(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() * 2);
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (CONSONANTS.containsKey(c)) {
                i = convertAkshara(text, i, out);
            } else if (INDEPENDENT_VOWELS.containsKey(c)) {
                out.append(INDEPENDENT_VOWELS.get(c));
                i++;
            } else if (c == ANUSVARA) {
                out.append('A');
                i++;
            } else if (c == VISARGA) {
                out.append('B');
                i++;
            } else if (c >= '೦' && c <= '೯') {
                // Kannada digits → ASCII digits
                out.append((char) ('0' + (c - '೦')));
                i++;
            } else if (c == ZWJ || c == ZWNJ || c == NUKTA) {
                i++;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** Returns true if the text contains at least one Kannada Unicode character. */
    public static boolean containsKannada(CharSequence text) {
        if (text == null) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'ಀ' && c <= '೿') {
                return true;
            }
        }
        return false;
    }

    /** Converts one akshara starting at {@code start}; returns the index after it. */
    private static int convertAkshara(String text, int start, StringBuilder out) {
        int n = text.length();
        int i = start;

        // 1. Collect the consonant cluster: C (್ C)*
        List<Character> cluster = new ArrayList<>();
        cluster.add(text.charAt(i++));
        i = skipNukta(text, i);
        while (i + 1 < n && text.charAt(i) == HALANT && CONSONANTS.containsKey(text.charAt(i + 1))) {
            cluster.add(text.charAt(i + 1));
            i = skipNukta(text, i + 2);
        }

        // 2. Vowel sign, halant, or the inherent "a"
        char vowel = 0;
        if (i < n && isVowelSign(text.charAt(i))) {
            vowel = text.charAt(i++);
        } else if (i < n && text.charAt(i) == HALANT) {
            vowel = HALANT;
            i++;
            while (i < n && (text.charAt(i) == ZWJ || text.charAt(i) == ZWNJ)) {
                i++;
            }
        }

        // 3. ರ್ at the start of a cluster is written as arkavattu after the letter
        boolean repha = cluster.size() > 1 && cluster.get(0) == RA;
        if (repha) {
            cluster.remove(0);
        }

        // 4. Base glyph = first consonant + vowel; deergha/ai marks go after the ottus
        Glyphs base = CONSONANTS.get(cluster.get(0));
        String trailing = "";
        switch (vowel) {
            case 0:
                out.append(base.inherent);
                break;
            case HALANT:
                if (cluster.size() > 1) {
                    out.append(base.inherent);
                    trailing = NUDI_HALANT;
                } else {
                    out.append(base.stem).append(NUDI_HALANT);
                }
                break;
            case SIGN_AA:
                out.append(base.stem).append('Á');
                break;
            case SIGN_I:
                out.append(base.iForm);
                break;
            case SIGN_II:
                out.append(base.iForm);
                trailing = NUDI_DEERGHA;
                break;
            case SIGN_U:
                out.append(base.inherent).append(base.uSign);
                break;
            case SIGN_UU:
                out.append(base.inherent).append(base.uuSign);
                break;
            case SIGN_R:
                out.append(base.inherent).append('È');
                break;
            case SIGN_E:
                out.append(base.eForm);
                break;
            case SIGN_EE:
                out.append(base.eForm);
                trailing = NUDI_DEERGHA;
                break;
            case SIGN_AI:
                out.append(base.eForm);
                trailing = NUDI_AI;
                break;
            case SIGN_O:
                out.append(base.oForm);
                break;
            case SIGN_OO:
                out.append(base.oForm);
                trailing = NUDI_DEERGHA;
                break;
            case SIGN_AU:
                out.append(base.stem).append('Ë');
                break;
            default:
                out.append(base.inherent);
                break;
        }

        // 5. Ottaksharas for the rest of the cluster
        for (int k = 1; k < cluster.size(); k++) {
            char c = cluster.get(k);
            if (c == RA && k > 1) {
                out.append(NUDI_RA_OTTU_LOWER);
            } else if (OTTAKSHARAS.containsKey(c)) {
                out.append(OTTAKSHARAS.get(c));
            } else {
                // No ottu glyph (e.g. ಙ): fall back to halant + full letter
                out.append(NUDI_HALANT).append(CONSONANTS.get(c).inherent);
            }
        }

        out.append(trailing);
        if (repha) {
            out.append(NUDI_ARKAVATTU);
        }
        return i;
    }

    private static int skipNukta(String text, int i) {
        return (i < text.length() && text.charAt(i) == NUKTA) ? i + 1 : i;
    }

    private static boolean isVowelSign(char c) {
        return (c >= SIGN_AA && c <= SIGN_R) || (c >= SIGN_E && c <= SIGN_AI)
                || (c >= SIGN_O && c <= SIGN_AU);
    }
}
