package io.zip;

import ij.ImagePlus;
import ij.io.Opener;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by paxcalpt on 15/03/15.
 */
public class OpenFileWithinZip {

    private ZipFile zf = null;
    private Map<String, ZipArchiveEntry> zipEntryMap = new LinkedHashMap<String, ZipArchiveEntry>();
    private Opener opener = new Opener();

    public OpenFileWithinZip(String filePath) throws IOException {
        zf = new ZipFile(filePath);

        for (Enumeration<? extends ZipArchiveEntry> e = zf.getEntries(); e.hasMoreElements(); ) {
            ZipArchiveEntry ze = e.nextElement();
            zipEntryMap.put(ze.getName(), ze);
        }
    }

    synchronized public boolean contains(String filenameInZip) {
        return zipEntryMap.keySet().contains(filenameInZip);
    }

    synchronized public String[] listFileNames() {
        ArrayList<String> fileNames = new ArrayList<String>();
        for (String fileName: zipEntryMap.keySet()) {
            fileNames.add(fileName);
        }
        return fileNames.toArray(new String[fileNames.size()]);
    }

    synchronized public String[] listFileNamesWithExtension(String extension) {
        ArrayList<String> fileNames = new ArrayList<String>();
        for (String fileName: zipEntryMap.keySet()) {
            if (fileName.endsWith(extension))
                fileNames.add(fileName);
        }
        return fileNames.toArray(new String[fileNames.size()]);
    }

    synchronized public String[] listFileNamesThatContain(String pattern) {
        ArrayList<String> fileNames = new ArrayList<String>();
        for (String fileName: zipEntryMap.keySet()) {
            if (fileName.contains(pattern))
                fileNames.add(fileName);
        }
        return fileNames.toArray(new String[fileNames.size()]);
    }

    synchronized public ImagePlus loadTiffImage(String filePathInZip) throws IOException {
        InputStream inputStream =
                new BufferedInputStream(zf.getInputStream(zipEntryMap.get(filePathInZip)));
        return opener.openTiff(inputStream, filePathInZip);
    }

    synchronized public String loadText(String filePathInZip) throws IOException {
        InputStream inputStream =
                new BufferedInputStream(zf.getInputStream(zipEntryMap.get(filePathInZip)));
        return new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
    }

    synchronized public Scanner getTextScanner(String filePathInZip) throws IOException {
        InputStream inputStream = new BufferedInputStream(zf.getInputStream(zipEntryMap.get(filePathInZip)));
        return new Scanner(inputStream, "UTF-8");
    }

    synchronized public void close() throws IOException {
        zf.close();
    }
}
