package imagej;


import ij.IJ;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import tools.Prefs;

/**
 * Created by Henriques-lab on 12/07/2016.
 */
public class FilesAndFoldersTools {

    private static Prefs prefs = new Prefs();

    public static String getSavePath(String header, String filenNameGuess, String extension) {
        String filePath;
        SaveDialog sd = new SaveDialog(header, prefs.get("NJ.defaultSavePath", ""), filenNameGuess, extension);
        if (sd.getFileName() == null) {
            return null;
        }
        String dirPath = sd.getDirectory();
        filePath = sd.getDirectory()+sd.getFileName();
        prefs.set("NJ.defaultSavePath", dirPath);
        prefs.set("NJ.filePath", filePath);
        return filePath;
    }

    public static String getOpenPath(String header, String fileNameGuess) {
        String filePath;
        IJ.showStatus(header);
        OpenDialog sd = new OpenDialog(header, prefs.get("NJ.defaultSavePath", ""), fileNameGuess);
        if (sd.getFileName() == null) {
            return null;
        }
        String dirPath = sd.getDirectory();
        filePath = sd.getDirectory()+sd.getFileName();
        prefs.set("NJ.defaultSavePath", dirPath);
        prefs.set("NJ.filePath", filePath);
        return filePath;
    }

    public static String getDirectory(String header) {
        IJ.showStatus(header);
        String dirPath = IJ.getDir(header);
        IJ.showStatus("Chosen directory:"+dirPath);
        return dirPath;
    }

}
