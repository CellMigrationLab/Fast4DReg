package imagej;

import ij.IJ;
import ij.ImageStack;
import ij.WindowManager;
import io.OpenNanoJDataset;
import tools.Prefs;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 02/04/15
 * Time: 13:57
 */
public class ImagePlusTools {

    private static Prefs prefs = new Prefs();

    public static long getMemorySizeBytes(ImageStack ims) {
        return ims.getHeight()*ims.getWidth()*ims.getBitDepth()*ims.getSize() / 8;
    }

    public static NJImagePlus getNJB(String endsWithPattern){
        NJImagePlus imp = null;
        try {
            imp = (NJImagePlus) WindowManager.getCurrentImage();
        } catch (ClassCastException e) {
            imp = null;
        }
        if (imp == null) {
            imp = openNJB();
        }
        if (imp == null || !imp.isBlockImage()) {
            IJ.error("No NanoJ (NJB) image open...");
        }
        if (imp != null && !imp.getTitle().endsWith(endsWithPattern)){
            IJ.error("Image title does not end in '"+endsWithPattern+"'");
            return null;
        }
        return imp;
    }

    public static NJImagePlus openNJB(){
        NJImagePlus imp = null;
        String filePath = IJ.getFilePath("Choose NJB file to open...");
        if (filePath==null) return null;
        if (!filePath.endsWith(".njb")) {
            IJ.error("Not an .njb file...");
            return null;
        }
        imp = OpenNanoJDataset.openNanoJDataset(filePath);
        return imp;
    }

}