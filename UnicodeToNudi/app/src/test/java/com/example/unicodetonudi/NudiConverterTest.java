package com.example.unicodetonudi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Local JVM tests for {@link NudiConverter}. Run with: ./gradlew test
 */
public class NudiConverterTest {

    private static void check(String unicode, String nudi) {
        assertEquals(unicode, nudi, NudiConverter.convert(unicode));
    }

    @Test
    public void simpleLetters() {
        check("ಕ", "PÀ");
        check("ಕಾ", "PÁ");
        check("ಕಿ", "Q");
        check("ಕೀ", "QÃ");
        check("ಕು", "PÀÄ");
        check("ಕೂ", "PÀÆ");
        check("ಕೆ", "PÉ");
        check("ಕೇ", "PÉÃ");
        check("ಕೈ", "PÉÊ");
        check("ಕೊ", "PÉÆ");
        check("ಕೋ", "PÉÆÃ");
        check("ಕೌ", "PË");
        check("ಕ್", "Pï");
    }

    @Test
    public void specialConsonants() {
        check("ಮ", "ªÀÄ");
        check("ಮಾ", "ªÀiÁ");
        check("ಮೊ", "ªÉÆ");
        check("ಯ", "AiÀÄ");
        check("ಯಾ", "AiÀiÁ");
        check("ಪು", "¥ÀÅ");
        check("ವೂ", "ªÀÇ");
        check("ವೊ", "ªÉÇ");
    }

    @Test
    public void words() {
        check("ಕನ್ನಡ", "PÀ£ÀßqÀ");
        check("ಅಮ್ಮ", "CªÀÄä");
        check("ಬೆಂಗಳೂರು", "¨ÉAUÀ¼ÀÆgÀÄ");
        check("ಶ್ರೀ", "²æÃ");
        check("ಹುಟ್ಟು", "ºÀÄlÄÖ");
        check("ಮುಖ್ಯ", "ªÀÄÄRå");
    }

    @Test
    public void arkavattu() {
        check("ಕರ್ನಾಟಕ", "PÀ£ÁðlPÀ");
        check("ಕೀರ್ತಿ", "QÃwð");
        check("ಕಾರ್ಯ", "PÁAiÀÄð");
        check("ಧರ್ಮ", "zsÀªÀÄð");
    }

    @Test
    public void conjunctEndingInHalant() {
        check("ಆಗಸ್ಟ್", "DUÀ¸ïÖ");
        check("ಲಾಸ್ಟ್", "¯Á¸ïÖ");
    }

    @Test
    public void keepsSpacesPunctuationAndDigits() {
        check("ನಮಸ್ಕಾರ, ಕನ್ನಡ!", "£ÀªÀÄ¸ÁÌgÀ, PÀ£ÀßqÀ!");
        check("೧೨೩", "123");
        check("ಕನ್ನಡ\nಕನ್ನಡ", "PÀ£ÀßqÀ\nPÀ£ÀßqÀ");
        check("", "");
    }

    @Test
    public void detectsKannada() {
        assertTrue(NudiConverter.containsKannada("hello ಕನ್ನಡ"));
        assertFalse(NudiConverter.containsKannada("hello"));
    }
}
