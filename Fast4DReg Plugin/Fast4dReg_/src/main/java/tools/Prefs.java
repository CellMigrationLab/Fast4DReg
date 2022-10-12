package tools;

import ij.IJ;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;


/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 5/2/13
 * Time: 2:31 PM
 */
public class Prefs extends ij.Prefs {
    public final static int STREAM_BUFFER_SIZE = (int) 100e6; // used in BufferedOutputStream

    public Map<String, String> getNanoJPrefs(String prefsHeader) {
        String prefsPath = this.getPrefsDir()+this.getFileSeparator()+"IJ_Prefs.txt";
        String prefsTxt = IJ.openAsString(prefsPath);

        Map<String, String> metadata = new LinkedHashMap<String, String>();
        metadata.put("CreationDate", DateTime.getDateTime());

        for (String line: prefsTxt.split("\n")) {
            if (line.startsWith("."+prefsHeader)){
                line = line.replaceFirst("."+prefsHeader, "");
                if (line.startsWith(".")) line = line.substring(1);
                int splitIndex = line.indexOf("=");
                metadata.put(line.substring(0, splitIndex), line.substring(splitIndex+1));
            }
        }
        return metadata;
    }

    public String getNanoJPrefsText(String prefsHeader) {
        Map<String, String> metadata = getNanoJPrefs(prefsHeader);
        StringWriter sw = new StringWriter();
        for (String variableName: metadata.keySet()) {
            String variableNameNoUnderscore = variableName.replace("_.", ".");
            sw.write("#-C:"+variableNameNoUnderscore+":"+metadata.get(variableName)+"\n");
        }
        return sw.toString();
    }

    public void saveNanoJPrefsIntoFile(String path, String prefsHeader) {
        this.savePreferences();

        try {
            FileWriter fw = new FileWriter(path, false);
            fw.write(getNanoJPrefsText(prefsHeader));
            fw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean showWhatsNew() {
        if (get("NJ.showWhatsNew", true)) {
            return true;
        }
        return false;
    }

    public boolean getDontShowAgain(String label) {
        return get("NJ."+label, false);
    }

    public void setDontShowAgain(String label, boolean flag) {
        set("NJ."+label, flag);
        savePreferences();
    }


    public void setShowWhatsNew(boolean show) {
        set("NJ.showWhatsNew", show);
        savePreferences();
    }

    public int getCompressionLevel() {
        return (int) get("NJ.compressionLevel", 3);
    }

    public void setCompressionLevel(int level) {
        assert (level>=0 && level <= 9);
        set("NJ.compressionLevel", level);
    }

    public boolean isNewUser() {
        if(!get("NJ.isNewUser", true)) return false;
        set("NJ.isNewUser", false);
        set("NJ.uid", String.format("%06d", new Random().nextInt((int) 1e6)));
        savePreferences();
        return true;
    }

    public String getUID() {

        return get("NJ.uid", "0");
    }

    public void setUID(String newUID) {
        set("NJ.uid", newUID);
        savePreferences();
    }

    public int getUsage() {
        return (int) get("NJ.usage", 0);
    }

    public void incrementUsage() {
        int usage = getUsage()+1;
        set("NJ.usage", usage);
        savePreferences();
    }

    public void startNanoJCommand() {
        set("NJ.stopWhatYouAreDoing", false);
    }

    public void stopNanoJCommand() {
        set("NJ.stopWhatYouAreDoing", true);
    }

    public boolean continueNanoJCommand() {
        boolean stop = get("NJ.stopWhatYouAreDoing", false);
        set("NJ.stopWhatYouAreDoing", false);
        return !stop;
    }

    public void changed() {
        this.set("NJ.prefsChanged", true);
    }
}