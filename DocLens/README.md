# DocLens

A fast, modern PDF viewer for Android. It lists every PDF on the phone (by folder too), opens them instantly, and can be set as the phone's default PDF viewer.

## Download

https://github.com/sumukhanidhibs/android-fundamentals/releases/download/doclens-latest/DocLens.apk

Open the link on your phone and tap the downloaded file to install.

## Features

| Feature | How |
|---|---|
| **All PDFs on the device** | Found through MediaStore (the system's file index), so the list appears in milliseconds, with no folder walking |
| **Folders** | The same PDFs grouped by folder (Download, WhatsApp Documents, ...) |
| **Recent** | Files you opened, with the page you stopped on. Reopening resumes there |
| **Search & sort** | Search by name; sort by name, date or size |
| **Fast opening** | The file goes straight to Android's built-in `PdfRenderer`. Only the visible pages render, one screen ahead is pre-rendered, and rendered pages stay in a memory cache |
| **Reader** | Pinch and double-tap zoom (sharper re-render when zoomed), page slider, go to page, night mode, share, tap to hide the bars |
| **Default viewer** | Opens PDFs from any app (*Open with* / *Share*). The **Set as default** card walks you through choosing DocLens → *Always* |

## Making DocLens the default PDF viewer

Android doesn't let an app make itself the default; you choose it once:

1. Tap **Set as default** on the DocLens home screen.
2. If no app is the default yet, Android shows *Open with*: pick **DocLens** → **Always**.
3. If another app is the default, DocLens opens that app's settings: **Open by default → Clear defaults**, then repeat step 1.

## Files

| File | Purpose |
|---|---|
| `MainActivity.java` | Library screen: Recent / All PDFs / Folders tabs, search, sort, open file picker |
| `LibraryAdapter.java` | List rows: PDFs, folders, recent files, the default-viewer card, empty states |
| `PdfScanner.java` | Queries MediaStore for every PDF |
| `PdfLibrary.java` / `PdfFile.java` | Pure-Java search, sort, folder grouping and formatting (unit tested) |
| `RecentStore.java` | Recent files and last page, stored in SharedPreferences |
| `ViewerActivity.java` | The reader. Handles `ACTION_VIEW` / `ACTION_SEND` for `application/pdf` |
| `PdfDoc.java` | Wraps `PdfRenderer`; renders pages on one background thread |
| `PageAdapter.java` / `PageView.java` | Page list with a bitmap cache and quality switching on zoom |
| `ZoomRecyclerView.java` | RecyclerView with pinch zoom, double-tap zoom and panning |
| `DefaultViewer.java` | Detects the current default PDF app and guides the user to pick DocLens |
| `StorageAccess.java` | File access: *All files access* on Android 11+, storage permission before |
| `Files.java` | Sharing through a `FileProvider` |

## Concepts Covered

- `ACTION_VIEW` intent filters with `mimeType="application/pdf"`: how an app becomes a candidate (and default) handler for a file type
- `PdfRenderer` and why it's single-threaded (one page open at a time)
- `MediaStore.Files` queries to find documents quickly
- `MANAGE_EXTERNAL_STORAGE` vs `READ_EXTERNAL_STORAGE` and scoped storage
- `RecyclerView` with view types, `LruCache` for bitmaps, extra layout space for pre-rendering
- Custom touch handling: `ScaleGestureDetector` + `GestureDetector`, drawing a zoomed canvas in `dispatchDraw`
- Storage Access Framework (`OpenDocument`) and persisted URI permissions
- `FileProvider` for sharing files safely
- Material 3 theming with custom light/dark palettes from the app icon

## Run the tests

```
./gradlew test
```

## Icon

`art/doclens-icon.png` is the source artwork. The launcher icons in `res/mipmap-*` are generated from it
(an adaptive icon foreground with the artwork in the safe zone, plus square and round legacy icons).

## Limitations

- Password-protected PDFs can't be opened yet (`PdfRenderer` before Android 15 has no password support).
- No text selection or search inside a PDF. `PdfRenderer` only draws pages.
