/**
 *
 */
package org.theseed.jfx;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TableView;

/**
 * This analyzes a column full of floating-point values and produces useful statistical metrics.
 *
 * @author Bruce Parrello
 */
public class StatisticsAnalysis extends ColumnAnalysis {

    /**
     * Create the statistical analyzer.
     *
     * @param data	list of lines to analyze
     */
    public StatisticsAnalysis(Collection<String[]> data) {
        super(data);
    }

    @Override
    protected Node getAnalysis(Iterator<String> column) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        // Note we have to convert from string to double, and we ignore failures.
        while (column.hasNext()) try {
            double value = Double.parseDouble(column.next());
            stats.addValue(value);
        } catch (NumberFormatException e) { }
        // Get the number of values processed.
        int n = (int) stats.getN();
        // Create the output table.
        TableView<Stat> retVal = new TableView<Stat>();
        retVal.setPrefSize(DEFAULT_SIZE, DEFAULT_SIZE);
        Stat.setupTable(retVal);
        ObservableList<Stat> tableData = FXCollections.observableArrayList(new Stat.Int("Count", n));
        if (n > 0) {
            // Here there was actual data.
            tableData.add(new Stat("Minimum", stats.getMin()));
            tableData.add(new Stat("Median", stats.getPercentile(50)));
            tableData.add(new Stat("Mean", stats.getMean()));
            tableData.add(new Stat("Maximum", stats.getMax()));
            tableData.add(new Stat("Std Dev", stats.getStandardDeviation()));
            tableData.add(new Stat("Skewness", stats.getSkewness()));
        }
        retVal.setItems(tableData);
        return retVal;
    }
    // FIELDS
    // TODO data members for StatisticsAnalysis

    // TODO constructors and methods for StatisticsAnalysis
}
