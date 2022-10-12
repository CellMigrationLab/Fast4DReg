package gui.tools.io;

import ij.plugin.filter.Analyzer;
import gui._BaseDialog_;

import java.io.IOException;

import static io.SaveNanoJTable.saveNanoJTable;

/**
 * Created with IntelliJ IDEA.
 * User: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 29/01/16
 * Time: 19:30
 */
public class SaveResultsTableAsNJT_ extends _BaseDialog_ {

    public String filePath;

    @Override
    public boolean beforeSetupDialog(String arg) {
        useSettingsObserver = false;
        autoOpenImp = false;

        if (Analyzer.getResultsTable() == null) {
            log.warning("No Results-Table open...");
            return false;
        }

        return true;
    }

    @Override
    public void setupDialog(){
    }

    @Override
    public boolean loadSettings() {
        return true;
    }

    public void execute() throws InterruptedException, IOException {
        saveNanoJTable(null, Analyzer.getResultsTable());
    }
}