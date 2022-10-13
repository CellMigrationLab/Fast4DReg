package io;

import ij.IJ;
import imagej.NJImagePlus;
import io.zip.imageInBlocks.OpenImageFromBlocksInZip;
import io.zip.virtualStacks.FullFramesVirtualStack;

import java.io.IOException;
import java.util.zip.ZipException;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 21/03/15
 * Time: 10:27
 */
public class OpenNanoJDataset {

    public static NJImagePlus openNanoJDataset(String filePath) {
        NJImagePlus imp = null;

        try {
            if (filePath.endsWith(".nji")) {
                FullFramesVirtualStack fullFramesVirtualStack = new FullFramesVirtualStack(filePath);
                imp = new NJImagePlus(fullFramesVirtualStack);
            }
            else if (filePath.contains(".zip")) {
                FullFramesVirtualStack fullFramesVirtualStack = new FullFramesVirtualStack(filePath, true);
                imp = new NJImagePlus(fullFramesVirtualStack);
            }
            else if (filePath.endsWith(".njb")) {
                OpenImageFromBlocksInZip openImageFromBlocksInZip = new OpenImageFromBlocksInZip(filePath);
                imp = new NJImagePlus(openImageFromBlocksInZip);
            }
            else {
                IJ.error("Not a NanoJ file...");
                return null;
            }
        }
        catch (ZipException e) {
            IJ.error("Error trying to open NanoJ file:\n"+ filePath);
            //"\nNote: datasets generated with Java 1.6 are not compatible with Java >=1.7");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return imp;
    }
}