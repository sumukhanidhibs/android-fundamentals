# UnicodeToNudi

An Android app that converts **Kannada Unicode** text (what you get from WhatsApp, websites, Google Input Tools, Gboard, ...) into **Nudi ASCII** text, which shows correctly when pasted into documents that use the Nudi fonts (Nudi 01 e, Nudi Akshara, ...). It's useful for government and office documents that still need Nudi.

## Download

Get the latest APK (no GitHub sign-in needed):
https://github.com/sumukhanidhibs/android-fundamentals/releases/latest/download/UnicodeToNudi.apk

Open the link on your phone and tap the downloaded file to install. A new release is published automatically on every build.

## Three ways to convert

| Way | How |
|---|---|
| **Open the app** | Copy Kannada text anywhere, then open the app. It picks up the clipboard, converts it, and (with *auto-copy* on) puts the Nudi text on the clipboard. Just paste. |
| **Selection menu** | In any app, select Kannada text → ⋮ / more → **Copy as Nudi**. The Nudi text is copied to the clipboard without leaving the app you're in. |
| **Share** | Share text from any app → **Unicode to Nudi**. |

You can also type or paste into the top box. The Nudi output updates as you type.

> The Nudi output looks like gibberish (`PÀ£ÀßqÀ`) on the phone. That's expected:
> it only turns into Kannada (ಕನ್ನಡ) once the Nudi font is applied, e.g. in MS Word on a PC.

## How the conversion works

Unicode stores Kannada in **logical** order (the order you'd say it), while Nudi stores the **glyphs** in the order they are drawn:

```
ಕ್ರೀ   Unicode: ಕ + ್ + ರ + ೀ
       Nudi:    Q (ಕಿ glyph) + æ (ರ ottu) + Ã (deergha)   →  "QæÃ"

ಕರ್ನಾಟಕ  Unicode: ಕ  ರ ್ ನ ಾ  ಟ  ಕ
         Nudi:    PÀ £Á ð      l  PÀ   ← arkavattu (ð) is written AFTER ನಾ
```

`NudiConverter` splits the text into aksharas (syllables):

```
[ರ್ repha] consonant [್ consonant]* [vowel sign] [ಂ/ಃ]
```

and writes each one as: base glyph → ottaksharas → trailing deergha (Ã/Ê) → arkavattu (ð) → ಂ/ಃ.

## Files

| File | Purpose |
|---|---|
| `NudiConverter.java` | Pure-Java conversion logic and glyph tables (no Android code, unit tested) |
| `MainActivity.java` | Input/output screen, clipboard auto-import, auto-copy, share |
| `ProcessTextActivity.java` | Invisible activity behind the **Copy as Nudi** selection-menu item (`ACTION_PROCESS_TEXT`) |
| `ClipboardUtils.java` | Read/write clipboard helpers |
| `NudiConverterTest.java` | JVM unit tests with known Nudi spellings |

## Concepts Covered

- `ACTION_PROCESS_TEXT`: adding your app to the system text-selection menu
- `ACTION_SEND` intent filter: receiving shared text
- `ClipboardManager`: reading the clipboard only works when the window has focus (Android 10+), so it is done in `onWindowFocusChanged`
- `TextWatcher` for live conversion, and `Handler.postDelayed` to debounce auto-copy
- `SharedPreferences` to remember the auto-copy setting
- Keeping logic free of Android classes so it can be tested with plain JUnit

## Run the tests

```
./gradlew test
```

## Limitations

- Kannada digits (೦–೯) become ASCII digits 0–9.
- Rare letters with no Nudi glyph (ಱ, ೞ, ೠ, ಌ) are left unchanged.
- A few uncommon combinations can be typed in more than one way in Nudi. The converter picks one of them, and it renders the same.
