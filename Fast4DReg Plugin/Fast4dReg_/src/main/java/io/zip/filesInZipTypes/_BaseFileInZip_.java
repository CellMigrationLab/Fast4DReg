package io.zip.filesInZipTypes;

import io.zip.OpenFileWithinZip;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 20/03/15
 * Time: 16:25
 */
public class _BaseFileInZip_ {
    protected OpenFileWithinZip openFileWithinZip;
    protected String filePathInZip;

    public _BaseFileInZip_(OpenFileWithinZip openFileWithinZip, String filePathInZip) {
        this.openFileWithinZip = openFileWithinZip;
        this.filePathInZip = filePathInZip;
    }

    public String getFileName() {
        return filePathInZip;
    }
}
