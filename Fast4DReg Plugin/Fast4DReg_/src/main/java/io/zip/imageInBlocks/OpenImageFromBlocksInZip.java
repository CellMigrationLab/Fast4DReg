package io.zip.imageInBlocks;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import io.zip.filesInZipTypes.TiffFileInZip;
import io.zip.virtualStacks.FramesFromBlocksVirtualStack;

import java.io.IOException;

import static java.lang.Math.min;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 18/03/15
 * Time: 07:59
 */
public class OpenImageFromBlocksInZip {

    public int width, height;
    public int blockWidth = 100;
    public int blockHeight = 100;
    public int blockWidthMargin = 5;
    public int blockHeightMargin = 5;
    public int maxXBlock;
    public int maxYBlock = 0;
    public int maxZBlock = 0;
    public int maxTBlock = 0;
    public int magnification = 0;
    private int nZs = 0;
    private int nTs = 0;

    private final String pattern;
    TiffFileInZip[][][][] blocks;

    io.zip.ParseFileNamesInZip pzf;

    public OpenImageFromBlocksInZip(String filePath) throws IOException {
        pzf = new io.zip.ParseFileNamesInZip(filePath);
        pattern = pzf.pattern;
        parseBlockSize();
        parseDimensions();
    }

    private void parseBlockSize() throws IOException {
        assert pzf.textFilesInZipMap.containsKey("block-size.txt");
        String blockInfo = pzf.textFilesInZipMap.get("block-size.txt").openText();

        for (String line: blockInfo.split("\n")) {
            //System.out.println(line.split("=")[1]);
            if (line.startsWith("blockWidth=")) blockWidth = Integer.parseInt(line.split("=")[1]);
            else if (line.startsWith("blockHeight=")) blockHeight = Integer.parseInt(line.split("=")[1]);
            else if (line.startsWith("blockWidthMargin=")) blockWidthMargin = Integer.parseInt(line.split("=")[1]);
            else if (line.startsWith("blockHeightMargin=")) blockHeightMargin = Integer.parseInt(line.split("=")[1]);
            else if (line.startsWith("maxXBlock=")) maxXBlock = Integer.parseInt(line.split("=")[1]);
            else if (line.startsWith("maxYBlock=")) maxYBlock = Integer.parseInt(line.split("=")[1]);
            else if (line.startsWith("maxZBlock=")) maxZBlock = Integer.parseInt(line.split("=")[1]);
            else if (line.startsWith("maxTBlock=")) maxTBlock = Integer.parseInt(line.split("=")[1]);
            else if (line.startsWith("width=")) width = Integer.parseInt(line.split("=")[1]);
            else if (line.startsWith("height=")) height = Integer.parseInt(line.split("=")[1]);
            else if (line.startsWith("magnification=")) magnification = Integer.parseInt(line.split("=")[1]);
        }

        nTs = maxTBlock + 1;
        nZs = maxZBlock + 1;
        //System.out.println("nTs="+nTs+" "+"nZs="+nZs);
    }

    private void parseDimensions() {
        String imageFileNameInZip = "";
        blocks = new TiffFileInZip[maxZBlock+1][maxTBlock+1][maxXBlock+1][maxYBlock+1];

        for (int t=0;t<=maxTBlock;t++) {
            for (int z=0;z<=maxZBlock;z++) {
                for (int y=0;y<=maxYBlock;y++) {
                    for (int x=0;x<=maxXBlock;x++) {
                        imageFileNameInZip = "img_x"+x+"y"+y+"z"+z+"t"+t+".tif";
                        blocks[z][t][x][y] = pzf.tiffFilesInZipMap.get(imageFileNameInZip);
                        if (blocks[z][t][x][y] == null) {
                            IJ.error("Could not find file in zip: "+imageFileNameInZip);
                            return;
                        }
                    }
                }
            }
        }
        return;
    }

    public String getTitle() {
        return pattern;
    }

    public int getNumberOfFrames() {
        return nZs*nTs;
    }

    public int getSliceZIndex(int slice) {
        slice--;
        return slice % nZs;
    }

    public int getSliceTIndex(int slice) {
        slice--;
        int z = slice % nZs;
        return (slice / nZs) % nTs;
    }

    public String getSliceLabel(int slice) {
        slice--;
        int z = slice % nZs;
        int t = (slice / nZs) % nTs;
        return "Z="+z+" T="+t;
    }

    public ImageProcessor getSlice(int slice) throws IOException {
        slice--;
        int z = slice % nZs;
        int t = (slice / nZs) % nTs;

        ImageProcessor ip = null;
        ImageProcessor ipBlock;

        for (int j=0; j<=maxYBlock; j++) {
            for (int i=0; i<=maxXBlock; i++) {
                ipBlock = blocks[z][t][i][j].openImage().getProcessor();

                if (ip == null) {
                    if (ipBlock.getBitDepth()==16) ip = new ShortProcessor(width, height);
                    else ip = new FloatProcessor(width, height);
                }

                // Crop margins
                ipBlock.setRoi(getRoiToCutMargins(i, j, ipBlock.getWidth(), ipBlock.getHeight()));
                ipBlock = ipBlock.crop();
                ip.copyBits(ipBlock, i*blockWidth, j*blockHeight, Blitter.COPY);
            }
        }
        return ip;
    }

    public FramesFromBlocksVirtualStack getFramesFromBlocksVirtualStack() {
        try {
            return new FramesFromBlocksVirtualStack(pattern, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public io.zip.virtualStacks.BlocksVirtualStack getTBlockVirtualStack(int zBlock, int xBlock, int yBlock, boolean withMargins) {
        TiffFileInZip[] blocks = new TiffFileInZip[maxTBlock+1];

        for (int tBlock = 0; tBlock <= maxTBlock; tBlock++) {
            blocks[tBlock] = this.blocks[zBlock][tBlock][xBlock][yBlock];
        }
        ImagePlus impBlock = null;
        try {
            impBlock = blocks[0].openImage();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Roi r = getRoiToCutMargins(xBlock, yBlock, impBlock.getWidth(), impBlock.getHeight());
        return new io.zip.virtualStacks.BlocksVirtualStack(pattern, blocks, r, withMargins);
    }

    private Roi getRoiToCutMargins(int xb, int yb, int blockWidth, int blockHeight) {
        int xMargin = (xb == 0) ? 0 : blockWidthMargin;
        int yMargin = (yb == 0) ? 0 : blockHeightMargin;
        int interiorWidth = min(this.blockWidth, blockWidth - xMargin);
        int interiorHeight = min(this.blockHeight, blockHeight - yMargin);
        return new Roi(xMargin, yMargin, interiorWidth, interiorHeight);
    }
}
