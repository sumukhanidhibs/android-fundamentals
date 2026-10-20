# UnicodeToNudi for Windows

A small Windows app that runs in the background and converts **Kannada Unicode** to **Nudi ASCII** inside **Microsoft Word** with a keyboard shortcut.

## Download

https://github.com/sumukhanidhibs/android-fundamentals/releases/latest/download/UnicodeToNudi.exe

It's a single file and needs nothing else installed (it uses .NET Framework 4.8, which comes with Windows 10 and 11).

Windows may show **"Windows protected your PC"** because the app isn't code-signed. Click **More info** → **Run anyway**.

## How to use

1. Run `UnicodeToNudi.exe`. A blue **ಕ** icon appears near the clock (it may be under the **^** arrow).
2. In Word, select the text that has Kannada Unicode in it. It can be a word, a paragraph or the whole document (Ctrl+A).
3. Press **Ctrl+Alt+N**.

Only the Kannada text changes:

| Before | After |
|---|---|
| `Report ಸಂಖ್ಯೆ 25: ಕನ್ನಡ (Kannada) Office` | `Report` `¸ÀASÉå` `25:` `PÀ£ÀßqÀ` `(Kannada) Office` |

- Each Kannada word is replaced with its Nudi text **and set to the Nudi font**, so it shows as Kannada.
- **English words, numbers and punctuation are not touched.** They keep their text, font and formatting.
- **Ctrl+Z** undoes the whole conversion in one step.

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
| `WordConverter.cs` | Talks to the running Word through COM. It reads the selected text, finds each Kannada word, and replaces that whole word with Nudi in the Nudi font, all inside one undo step. Word's wildcard search isn't used, because it treats a letter plus its vowel sign (ಸಾ) as one character and would convert only bare letters |
| `ClipboardConverter.cs` | Ctrl+C → convert → Ctrl+V fallback for other programs |
| `HotkeyWindow.cs` | Global shortcut via `RegisterHotKey` |
| `TrayApp.cs` | Tray icon, menu and notifications |
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
