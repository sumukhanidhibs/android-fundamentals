package com.example.doclens;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PdfLibraryTest {

    private static final String DL = "/storage/emulated/0/Download";
    private static final String DOCS = "/storage/emulated/0/Documents";

    private final PdfFile invoice = new PdfFile(DL + "/invoice.pdf", "invoice.pdf", DL, 2_000, 300);
    private final PdfFile alpha = new PdfFile(DL + "/Alpha.pdf", "Alpha.pdf", DL, 9_000, 100);
    private final PdfFile report = new PdfFile(DOCS + "/report.pdf", "report.pdf", DOCS, 5_000, 200);

    private List<PdfFile> all() {
        return new ArrayList<>(Arrays.asList(invoice, alpha, report));
    }

    @Test
    public void filterIgnoresCase() {
        assertEquals(Arrays.asList(alpha), PdfLibrary.filter(all(), "ALP"));
        assertEquals(3, PdfLibrary.filter(all(), "  ").size());
        assertEquals(0, PdfLibrary.filter(all(), "zzz").size());
    }

    @Test
    public void sortByNameDateAndSize() {
        List<PdfFile> files = all();
        PdfLibrary.sort(files, PdfLibrary.Sort.NAME);
        assertEquals(Arrays.asList(alpha, invoice, report), files);

        PdfLibrary.sort(files, PdfLibrary.Sort.DATE);
        assertEquals(Arrays.asList(invoice, report, alpha), files);

        PdfLibrary.sort(files, PdfLibrary.Sort.SIZE);
        assertEquals(Arrays.asList(alpha, report, invoice), files);
    }

    @Test
    public void groupsIntoFolders() {
        List<PdfLibrary.Folder> folders = PdfLibrary.folders(all(), PdfLibrary.Sort.NAME);
        assertEquals(2, folders.size());
        assertEquals("Documents", folders.get(0).name);
        assertEquals("Download", folders.get(1).name);
        assertEquals(Arrays.asList(alpha, invoice), folders.get(1).files);
        assertEquals(11_000, folders.get(1).totalSize());
        assertEquals(300, folders.get(1).latestModified());

        List<PdfLibrary.Folder> byDate = PdfLibrary.folders(all(), PdfLibrary.Sort.DATE);
        assertEquals("Download", byDate.get(0).name);
    }

    @Test
    public void folderNames() {
        assertEquals("Download", PdfLibrary.folderName(DL));
        assertEquals("Download", PdfLibrary.folderName(DL + "/"));
        assertEquals("Internal storage", PdfLibrary.folderName("/storage/emulated/0"));
        assertEquals("SD card", PdfLibrary.folderName("/storage/1A2B-3C4D"));
    }

    @Test
    public void displayPaths() {
        assertEquals("Internal storage/Download", PdfLibrary.displayPath(DL));
        assertEquals("SD card/Books", PdfLibrary.displayPath("/storage/1A2B-3C4D/Books"));
        assertEquals("/data/x", PdfLibrary.displayPath("/data/x"));
    }

    @Test
    public void fromPathSplitsNameAndFolder() {
        PdfFile f = PdfFile.fromPath(DL + "/a b.pdf", 1, 2);
        assertEquals("a b.pdf", f.name);
        assertEquals(DL, f.folderPath);
    }

    @Test
    public void formatsSizes() {
        assertEquals("0 B", PdfLibrary.formatSize(0));
        assertEquals("512 B", PdfLibrary.formatSize(512));
        assertEquals("1 KB", PdfLibrary.formatSize(1024));
        assertEquals("1.5 KB", PdfLibrary.formatSize(1536));
        assertEquals("2.3 MB", PdfLibrary.formatSize(2_400_000));
        assertEquals("150 MB", PdfLibrary.formatSize(150L * 1024 * 1024));
        assertEquals("1.1 GB", PdfLibrary.formatSize(1_200_000_000L));
    }
}
