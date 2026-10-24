package com.example.doclens;

/**
 * One PDF found on the device. Plain Java (no Android classes) so the
 * library logic built on it can be unit tested on the JVM.
 */
public final class PdfFile {

    public final String path;
    public final String name;
    public final String folderPath;
    public final long size;
    /** Last modified time in milliseconds. */
    public final long modified;

    public PdfFile(String path, String name, String folderPath, long size, long modified) {
        this.path = path;
        this.name = name;
        this.folderPath = folderPath;
        this.size = size;
        this.modified = modified;
    }

    /** Builds a PdfFile from an absolute path, using the path for the name and folder. */
    public static PdfFile fromPath(String path, long size, long modified) {
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        String folder = slash > 0 ? path.substring(0, slash) : "/";
        return new PdfFile(path, name, folder, size, modified);
    }
}
