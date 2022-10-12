package tools;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static java.lang.Math.round;
import static tools.NativeTools.getLocalFileFromResource;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 02/01/15
 * Time: 16:10
 */
public class Log {

    public boolean useIJLog = true;
    public boolean useLogFile = false;
    public boolean useProgressInLog = false;
    public int level = 1; // 0: silent
    private Prefs prefs = new Prefs();
    private FileWriter fw = null;
    private String[] levelHeaders = new String[]{".", "..", "...", "....", ".....", "......", ".......", "........"};
    private long timeStart = 0;
    private boolean showTime = false;
    private float _progress = 0;
    private float _progressDirection = 1;
    public String currentStatus = "";
    public boolean CATS = prefs.get("NJ.cats", false);
    private ImageStack _CATSStack = null;
    private ThreadedCats _CATSThread = null;
    public String FPSString = "FPS";

    public Log() {
        this.level = (int) prefs.get("NJ.debugLevel", 2);
    }

    public Log(int level){
        this.level = level;
    }

    public Log(int level, FileWriter fw){
        this.level = level;
        this.fw = fw;
    }

    private void logFileWrite(String msg){
        if (useLogFile && fw!=null) try {
            fw.write(msg+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void status(String msg) {
        if (level>0) IJ.showStatus("NanoJ: " + msg);
        currentStatus = msg;
    }

    public void progress(int level, double progress){
        if (level <= this.level) this.progress(progress);
    }

    public void progress(int level, int currentIndex, int finalIndex){
        if (level <= this.level) this.progress(currentIndex, finalIndex);
    }

    public void progress(double p){
        if (p>1) p /= 100;
        this.progress((int) round(p*100), 100);
    }

    public void progress(int currentIndex, int finalIndex){
        if (level>0) {
            IJ.showProgress(currentIndex, finalIndex);
            if (useProgressInLog) {
                String msg = "done " + currentIndex + "/" + finalIndex;
                if (useIJLog) IJ.log(msg);
                if (useLogFile) logFileWrite(msg);
            }
            if (CATS) {
                if (currentIndex == finalIndex) closeCATS();
                else incrementCATS(currentIndex, finalIndex);
            }
        }
    }

    public void progress() {
        if (_progress == 0f) _progressDirection = 0.01f;
        if (_progress == 1f) _progressDirection = -0.01f;
        _progress += _progressDirection;
        progress(_progress);
    }

    public void msg(String msg) {
        String txt = "";
        if (showTime) txt+=String.format("%.3gs: ", getTimerValueSeconds())+msg;
        else          txt+=msg;
        if (useIJLog) IJ.log(txt);
        if (useLogFile) logFileWrite(txt);
    }

    public void msg(int level, String msg) {
        if (level <= this.level) {
            String txt = "";
            if (level>1)
                txt += levelHeaders[level-2]+" ";
            this.msg(txt+msg);
        }
    }

    public void warning(String msg) {
        msg = "WARNING: "+msg;
        if (useIJLog) IJ.log(msg);
        if (useLogFile) logFileWrite(msg);
    }

    public void error(String msg) {
        IJ.error(msg);
    }

    public void abort() {
        String msg = "Aborting NanoJ execution...";
        IJ.resetEscape();
        progress(1);
        warning(msg);
        status(msg);
    }

    public boolean useDebugChoices() {
        return prefs.get("NJ.debugChoices", false);
    }

    public void showTimeInMessages(boolean flag){
        this.showTime = flag;
        this.startTimer();
    }

    public void startTimer(){
        this.timeStart = System.nanoTime();
    }

    public float getTimerValueMilliSeconds(){
        return (System.nanoTime()-this.timeStart)/1000000f;
    }

    public float getTimerValueSeconds(){
        return (System.nanoTime()-this.timeStart)/1000000000f;
    }

    public int getNumberCPUs() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static String getETFString(double ETF_in_seconds) {
        int _h = (int) (ETF_in_seconds / 3600);
        int _m = (int) (((ETF_in_seconds % 86400) % 3600) / 60);
        int _s = (int) (((ETF_in_seconds % 86400) % 3600) % 60);
        return String.format("%02d:%02d:%02d", _h, _m, _s);
    }

    public String getFPSString(double FPS) {
        String fpsString;
        if (FPS > 1e3) fpsString = round(FPS / 1e3) + "k"+FPSString;
        else if (FPS > 10) fpsString = round(FPS) + FPSString;
        else fpsString = String.format("%.3g%n", FPS) + FPSString;
        return fpsString;
    }

    public void displayETF(String prefix, int n, int nCycles) {
        double averageCalculationTime = ((System.nanoTime()-timeStart)/n)/1e9;
        double FPS = 1 / averageCalculationTime;
        double ETF = (nCycles-n) * averageCalculationTime;
        String msg = prefix+" running at " + getFPSString(FPS) + " ETF " + getETFString(ETF);
        this.status(msg);
        this.msg(8, msg);
    }

    public synchronized void loadCATS() {
        if (_CATSStack == null) {
            try {
                File temp = getLocalFileFromResource("/calculating.gif");
                ImagePlus imp = IJ.openImage(temp.getAbsolutePath());
                _CATSStack = imp.getStack();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        _CATSThread = new ThreadedCats();
        _CATSThread.start();
    }

    public synchronized void closeCATS() {
        if (_CATSThread != null) _CATSThread.end();
        _CATSThread = null;
    }

    private synchronized void incrementCATS(int currentIndex, int finalIndex) {
        if (_CATSThread == null) loadCATS();
        _CATSThread.setProgress(round(100*currentIndex/finalIndex));
    }

    class ThreadedCats extends Thread {
        private int counter = 1;
        private boolean keepRunning = true;
        public ImagePlus CATSImage = new ImagePlus("Progress...", _CATSStack.getProcessor(1));

        public void run() {
            CATSImage.show();

            while (keepRunning) {
                nextProcessor();
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void end() {
            keepRunning = false;
            if (CATSImage != null) CATSImage.close();

        }

        public synchronized void setProgress(int v) {
            CATSImage.setTitle("Progress... "+v+"%");
        }

        private synchronized void nextProcessor() {
            if (counter > _CATSStack.getSize()) counter = 1;
            CATSImage.setProcessor(_CATSStack.getProcessor(counter));
            counter++;
        }


    }
}