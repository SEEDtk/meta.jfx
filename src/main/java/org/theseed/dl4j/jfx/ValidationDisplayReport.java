/**
 *
 */
package org.theseed.dl4j.jfx;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.theseed.dl4j.train.IPredictError;
import org.theseed.io.LineReader;
import org.theseed.reports.IValidationReport;

import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

/**
 * This is the base class for the two validation reports.  It mostly handles the testing/training divide.
 *
 * @author Bruce Parrello
 *
 */
public abstract class ValidationDisplayReport implements IValidationReport {

    /** IDs of records that were used in training */
    private Set<String> trained;
    /** ID column index in metadata */
    private int idColIdx;
    /** next available ID number */
    private int idNum;
    /** table for statistical output */
    private ObservableList<ResultDisplay.Stat> statsList;

    /**
     * Construct a blank result report object.
     */
    public ValidationDisplayReport() {
        this.trained = Collections.emptySet();
        this.idNum = 1;
    }

    /**
     * Initialize for display.
     *
     * @param labels	list of labels for the governing model
     */
    public abstract void init(List<String> labels);

    /**
     * Save the table used to display statistics.
     *
     * @param statsTable	table for displaying prediction statistics
     */
    public void register(TableView<ResultDisplay.Stat> statsTable) {
        this.statsList = statsTable.getItems();
    }

    /**
     * @return the ID string from a row's metadata
     *
     * @param metaData	metadata string for this row
     */
    public String getId(String metaData) {
        String[] metaCols = StringUtils.split(metaData, '\t');
        String retVal;
        if (this.idColIdx >= 0 && metaCols.length > this.idColIdx)
            retVal = metaCols[this.idColIdx];
        else
            retVal = String.format("item %d", this.idNum++);
        return retVal;
    }

    /**
     * @return TRUE if the specified ID is in the training set, else FALSE
     *
     * @param id		ID to check
     */
    public boolean isTrained(String id) {
        return this.trained.contains(id);
    }

    @Override
    public void setupIdCol(File modelDir, String idCol, List<String> metaList, Collection<String> trainList) throws IOException {
        this.idColIdx = metaList.indexOf(idCol);
        if (trainList != null)
            this.trained = new HashSet<String>(trainList);
        else {
            File trainedFile = new File(modelDir, "trained.tbl");
            this.trained = LineReader.readSet(trainedFile);
        }
    }

    @Override
    public void finishReport(IPredictError errors) {
        // Let the subclass finish its report.
        this.finishReport();
        // Now fill in the stats table.
        this.statsList.clear();
        String[] names = errors.getTitles();
        double[] values = errors.getStats();
        for (int i = 0; i < names.length; i++)
            this.statsList.add(new ResultDisplay.Stat(names[i], values[i]));
        // Add any other stats from the subclass.
        List<ResultDisplay.Stat> others = this.getStats();
        others.stream().forEach(x -> this.statsList.add(x));
    }

    /**
     * @return a list of additional statistics to display
     */
    public abstract List<ResultDisplay.Stat> getStats();

    /**
     * This method allows the subclass to do its own special processing at the end of the report.
     */
    public void finishReport() {
    }

    @Override
    public void close() {
    }

}
