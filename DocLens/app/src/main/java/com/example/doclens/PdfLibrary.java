package com.example.doclens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Searching, sorting and grouping the device's PDFs into folders.
 * No Android code here, see PdfLibraryTest.
 */
public final class PdfLibrary {

    public enum Sort { NAME, DATE, SIZE }

    public static final String INTERNAL_ROOT = "/storage/emulated/0";
    private static final Pattern SD_CARD_ROOT = Pattern.compile("^/storage/([0-9A-Fa-f]{4}-[0-9A-Fa-f]{4})");

    /** A folder that contains at least one PDF. */
    public static final class Folder {
        public final String path;
        public final String name;
        public final List<PdfFile> files = new ArrayList<>();

        Folder(String path) {
            this.path = path;
            this.name = folderName(path);
        }

        public long latestModified() {
            long latest = 0;
            for (PdfFile f : files) latest = Math.max(latest, f.modified);
            return latest;
        }

        public long totalSize() {
            long total = 0;
            for (PdfFile f : files) total += f.size;
            return total;
        }
    }

    private PdfLibrary() {
    }

    /** Files whose name contains {@code query}, ignoring case. A blank query keeps everything. */
    public static List<PdfFile> filter(List<PdfFile> files, String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>(files);
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<PdfFile> result = new ArrayList<>();
        for (PdfFile f : files) {
            if (f.name.toLowerCase(Locale.ROOT).contains(q)) result.add(f);
        }
        return result;
    }

    public static void sort(List<PdfFile> files, Sort sort) {
        Collections.sort(files, comparator(sort));
    }

    static Comparator<PdfFile> comparator(Sort sort) {
        final Comparator<PdfFile> byName = (a, b) -> {
            int c = String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name);
            return c != 0 ? c : a.path.compareTo(b.path);
        };
        switch (sort) {
            case DATE:
                return (a, b) -> {
                    int c = Long.compare(b.modified, a.modified);
                    return c != 0 ? c : byName.compare(a, b);
                };
            case SIZE:
                return (a, b) -> {
                    int c = Long.compare(b.size, a.size);
                    return c != 0 ? c : byName.compare(a, b);
                };
            default:
                return byName;
        }
    }

    /** Groups files by folder. Files inside each folder and the folders themselves follow {@code sort}. */
    public static List<Folder> folders(List<PdfFile> files, Sort sort) {
        Map<String, Folder> byPath = new LinkedHashMap<>();
        for (PdfFile f : files) {
            Folder folder = byPath.get(f.folderPath);
            if (folder == null) {
                folder = new Folder(f.folderPath);
                byPath.put(f.folderPath, folder);
            }
            folder.files.add(f);
        }
        List<Folder> result = new ArrayList<>(byPath.values());
        for (Folder folder : result) sort(folder.files, sort);

        final Comparator<Folder> byName = (a, b) -> {
            int c = String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name);
            return c != 0 ? c : a.path.compareTo(b.path);
        };
        Comparator<Folder> order;
        switch (sort) {
            case DATE:
                order = (a, b) -> {
                    int c = Long.compare(b.latestModified(), a.latestModified());
                    return c != 0 ? c : byName.compare(a, b);
                };
                break;
            case SIZE:
                order = (a, b) -> {
                    int c = Long.compare(b.totalSize(), a.totalSize());
                    return c != 0 ? c : byName.compare(a, b);
                };
                break;
            default:
                order = byName;
        }
        Collections.sort(result, order);
        return result;
    }

    /** "Download" for /storage/emulated/0/Download, "Internal storage" for the root itself. */
    public static String folderName(String folderPath) {
        if (folderPath == null || folderPath.isEmpty() || folderPath.equals("/")) return "/";
        String path = trimSlash(folderPath);
        if (path.equals(INTERNAL_ROOT)) return "Internal storage";
        Matcher sd = SD_CARD_ROOT.matcher(path);
        if (sd.matches()) return "SD card";
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /** Shortens storage roots: /storage/emulated/0/Download → "Internal storage/Download". */
    public static String displayPath(String folderPath) {
        if (folderPath == null) return "";
        String path = trimSlash(folderPath);
        if (path.startsWith(INTERNAL_ROOT)) {
            return "Internal storage" + path.substring(INTERNAL_ROOT.length());
        }
        Matcher sd = SD_CARD_ROOT.matcher(path);
        if (sd.find()) return "SD card" + path.substring(sd.end());
        return path;
    }

    /** Human readable file size: 512 B, 1.5 KB, 2.3 MB, 1.1 GB. */
    public static String formatSize(long bytes) {
        if (bytes < 1024) return Math.max(bytes, 0) + " B";
        String[] units = {"KB", "MB", "GB", "TB"};
        double value = bytes;
        int unit = -1;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        String number = value >= 100 || value == Math.floor(value)
                ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.1f", value);
        return number + " " + units[unit];
    }

    private static String trimSlash(String path) {
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
