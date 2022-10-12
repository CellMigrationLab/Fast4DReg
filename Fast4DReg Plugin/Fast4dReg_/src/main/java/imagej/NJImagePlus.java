package imagej;

import ij.ImagePlus;
import io.zip.imageInBlocks.OpenImageFromBlocksInZip;
import io.zip.virtualStacks.FullFramesVirtualStack;
import tools.Log;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 01/04/15
 * Time: 11:54
 */
public class NJImagePlus extends ImagePlus {

    final int RADIALITY = 0;
    final int RENDER = 1;
    final int COMPRESSED_DATASET = 2;

    public int type;
    private boolean _isBlockImage = false;
    private OpenImageFromBlocksInZip openImageFromBlocksInZip = null;
    private Log log = new Log();

    public NJImagePlus(OpenImageFromBlocksInZip openImageFromBlocksInZip) {
        super(openImageFromBlocksInZip.getTitle(), openImageFromBlocksInZip.getFramesFromBlocksVirtualStack());
        this.openImageFromBlocksInZip = openImageFromBlocksInZip;
        if (getTitle().contains("-NanoJRadiality"))
            type = RADIALITY;
        else if (getTitle().contains("-NanoJRender"))
            type = RENDER;
        this._isBlockImage = true;
    }

    public NJImagePlus(FullFramesVirtualStack fullFramesVirtualStack) {
        super(fullFramesVirtualStack.getTitle(), fullFramesVirtualStack);
        type = COMPRESSED_DATASET;
        this.setCalibration(fullFramesVirtualStack.calibration);
        if (fullFramesVirtualStack.properties != null) {
            for (Object key : fullFramesVirtualStack.properties.keySet())
                this.setProperty((String) key, fullFramesVirtualStack.properties.get(key));
        }
        this.setFileInfo(fullFramesVirtualStack.fileInfo);
        this.setOverlay(fullFramesVirtualStack.overlay);
        this.setRoi(fullFramesVirtualStack.roi);
        this.getCalibration().setUnit(fullFramesVirtualStack.unit);
        if (fullFramesVirtualStack.info != null) log.msg(fullFramesVirtualStack.info);
    }

    public int getMagnification() {
        if (isBlockImage())
            return this.openImageFromBlocksInZip.magnification;
        return 1;
    }

    public boolean isBlockImage() {
        return this._isBlockImage;
    }

    public OpenImageFromBlocksInZip getOpenImageFromBlocksInZip() {
        return this.openImageFromBlocksInZip;
    }

    public long getMemorySizeBytes() {
        return ImagePlusTools.getMemorySizeBytes(this.getImageStack());
    }
}
