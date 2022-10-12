package io.zip;

import io.zip.filesInZipTypes.TextFileInZip;
import io.zip.filesInZipTypes.TiffFileInZip;
import tools.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 18/03/15
 * Time: 21:49
 */
public class ParseFileNamesInZip {

    public Map<String, TextFileInZip> textFilesInZipMap;
    public Map<String, TiffFileInZip> tiffFilesInZipMap;
    public List<OpenFileWithinZip> openFileWithinZipList;
    public String pattern;
    public String extension;
    private Log log = new Log();

    public ParseFileNamesInZip(String fileName) throws IOException {
        textFilesInZipMap = new LinkedHashMap<String, TextFileInZip>();
        tiffFilesInZipMap = new LinkedHashMap<String, TiffFileInZip>();
        openFileWithinZipList = new ArrayList<OpenFileWithinZip>();

        File file = new File(fileName);
        pattern = file.getName().substring(0, file.getName().lastIndexOf("-"));
        extension = fileName.substring(fileName.lastIndexOf('.'));

        for (File zipFile : listZipFiles(file.getParentFile(), pattern)) {
            log.status("Parsing: "+zipFile.getName());

            OpenFileWithinZip openFileWithinZip = new OpenFileWithinZip(zipFile.getPath());
            openFileWithinZipList.add(openFileWithinZip);

            for (String fileNameInZip: openFileWithinZip.listFileNames()) {
                log.progress();
                if (fileNameInZip.endsWith(".tif") || fileNameInZip.endsWith(".tiff"))
                    tiffFilesInZipMap.put(fileNameInZip, new TiffFileInZip(openFileWithinZip, fileNameInZip));
                else if (fileNameInZip.endsWith(".txt"))
                    textFilesInZipMap.put(fileNameInZip, new TextFileInZip(openFileWithinZip, fileNameInZip));
            }
        }
        log.progress(1);
    }

    public void close() throws IOException {
        for (OpenFileWithinZip openFileWithinZip : openFileWithinZipList) {
            openFileWithinZip.close();
        }
    }

    private File[] listZipFiles(File root, final String pattern) {
        if (!root.isDirectory()) {
            throw new IllegalArgumentException(root + " is no directory.");
        }

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                String fileName = file.getName();
                String localPattern = "";
                if (fileName.contains("-"))
                    localPattern = fileName.substring(0, fileName.lastIndexOf("-"));
                if (localPattern.equals(pattern) && fileName.endsWith(extension))
                    return true;
                return false;
            }
        };

        // Makes sure file list is sorted
        TreeMap<String, File> filesTree = new TreeMap<String, File>();
        File[] files = root.listFiles(filter);
        for (File f: files) filesTree.put(f.getName(), f);
        int counter = 0;
        for (String fName: filesTree.keySet()) {
            files[counter] = filesTree.get(fName);
            counter++;
        }
        return files;
    }
}