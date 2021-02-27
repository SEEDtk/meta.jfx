/**
 *
 */
package org.theseed.jfx;

import java.util.Collection;
import org.theseed.counters.CountMap;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;

/**
 * This class produces a pie chart displaying how often different values appear in a column.
 *
 * @author Bruce Parrello
 *
 */
public class DistributionAnalysis extends ColumnAnalysis {

    /**
     * Construct an object for creating the pie charts.
     *
     * @param data		collection of data lines to analyze
     * @param labelIdx	label column index
     */
    public DistributionAnalysis(Collection<String[]> data, int labelIdx) {
        super(data, labelIdx);
    }

    @Override
    protected Node getAnalysis(Iter column) {
        CountMap<String> counts = new CountMap<String>();
        while (column.hasNext())
            counts.count(column.next());
        // We only display values that occur more than 1% of the time, so compute the 1% here
        // and set up an accumulator for it.
        int limit = this.size() / 100;
        int smalls = 0;
        // Build the pie chart data list.
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (CountMap<String>.Count count : counts.counts()) {
            if (count.getCount() <= limit)
                smalls += count.getCount();
            else
                pieData.add(new PieChart.Data(count.getKey(), count.getCount()));
        }
        // Add the residual if there is any.
        if (smalls > 0)
            pieData.add(new PieChart.Data("others (<1%)", smalls));
        // Create the pie chart.
        PieChart chart = new PieChart(pieData);
        chart.setPrefHeight(DEFAULT_SIZE);
        chart.setPrefWidth(DEFAULT_SIZE);
        return chart;
    }

}
