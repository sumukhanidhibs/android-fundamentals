# UnicodeToNudi for Windows

A small Windows app that runs in the background and converts **Kannada Unicode** to **Nudi ASCII** inside **Microsoft Word** with a keyboard shortcut.

## Download

https://github.com/sumukhanidhibs/android-fundamentals/releases/latest/download/UnicodeToNudi.exe

It's a single file and needs nothing else installed (it uses .NET Framework 4.8, which comes with Windows 10 and 11).

Windows may show **"Windows protected your PC"** because the app isn't code-signed. Click **More info** → **Run anyway**.

## How to use

1. Run `UnicodeToNudi.exe`. A blue **ಕ** icon appears near the clock (it may be under the **^** arrow). On first run it also adds itself to Windows startup, so it's always ready after you sign in. You can switch this off in Settings.
2. In Word, select the text that has Kannada Unicode in it. It can be a word, a paragraph or the whole document (Ctrl+A).
3. Press **Ctrl+Alt+N**.

Only the Kannada text changes:

| Before | After |
|---|---|
| `Report ಸಂಖ್ಯೆ 25: ಕನ್ನಡ (Kannada) Office` | `Report` `¸ÀASÉå` `25:` `PÀ£ÀßqÀ` `(Kannada) Office` |

- Each Kannada word is replaced with its Nudi text **and set to the Nudi font**, so it shows as Kannada. Spell-check is turned off for that text, so there are no red underlines.
- **English words, numbers and punctuation are not touched.** They keep their text, font and formatting.
- **Ctrl+Z** undoes the whole conversion in one step.

A small **"Converting to Nudi, please wait..."** window with a progress bar shows while it works.

**Updating:** just run the new `UnicodeToNudi.exe`. It closes the copy that's already running and takes over. Hover over the tray icon to see the version, e.g. `Unicode to Nudi 1.0.8`; the last number is the build number on the Releases page.

In other programs (Notepad, browsers, ...) the same shortcut copies the selection, converts it and pastes it back. English text is kept, but the font can't be changed there.

## Settings

Right-click the tray icon → **Settings...** (or double-click it):

- **Shortcut:** Ctrl+Alt+N (default), Ctrl+Alt+K, Ctrl+Shift+Alt+N, Ctrl+Alt+F12 or Win+Alt+N
- **Nudi font:** default `Nudi 01 e`. Pick the Nudi font installed on your PC.
- **Start automatically** when you sign in to Windows
- A **Try it** box to test the conversion

Settings are saved in `%APPDATA%\UnicodeToNudi\settings.ini`.

## How it works

| File | Purpose |
|---|---|
| `NudiConverter.cs` | Conversion logic, a C# port of the Android app's `NudiConverter.java` |
| `WordConverter.cs` | Talks to the running Word through COM. It reads the selected text, lists the distinct Kannada words, and runs one Word **Replace All** per word, longest first. Each Replace All also applies the Nudi font in every font slot (Word draws `¹` = ಸಿ and `²` = ಶಿ with the East Asian font) and turns off spell-check. Anything left over is converted word by word. It all happens inside one undo step. Word's wildcard search isn't used, because it treats a letter plus its vowel sign (ಸಾ) as one character |
| `ClipboardConverter.cs` | Ctrl+C → convert → Ctrl+V fallback for other programs |
| `HotkeyWindow.cs` | Global shortcut via `RegisterHotKey` |
| `TrayApp.cs` | Tray icon, menu and notifications. Runs the Word conversion on a background thread |
| `ProgressForm.cs` | The "please wait" window with a progress bar |
| `SettingsForm.cs`, `Settings.cs` | Settings window and storage |

## Tests

`tests/NudiConverter.Tests` checks the C# converter against `vectors.tsv`, which is generated from the Android (Java) converter. That way both apps produce identical Nudi text.

```
dotnet run --project tests/NudiConverter.Tests
```

## Build

```
dotnet build src/UnicodeToNudi/UnicodeToNudi.csproj -c Release
```
