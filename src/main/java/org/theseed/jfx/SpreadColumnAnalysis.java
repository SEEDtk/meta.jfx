/**
 *
 */
package org.theseed.jfx;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import org.apache.commons.math3.util.ResizableDoubleArray;

/**
 * This analyzer creates a scatter graph showing the spread of the column values for each label.  This gives us an idea of
 * the relationship between the column values and the classifications.  For a continuous column value, a scatter plot is used
 * with a category axis in the x-dimension.  For a discrete column value, a bar chart is used.
 *
 * @author Bruce Parrello
 *
 */
public class SpreadColumnAnalysis extends ColumnAnalysis {

    /**
     * Initialize the spread analysis.
     *
     * @param data		input lines
     * @param labelIdx	label column index
     * @param labels	list of label names
     */
    public SpreadColumnAnalysis(Collection<String[]> data, int labelIdx, List<String> labels) {
        super(data, labelIdx);
    }

    @Override
    protected Node getAnalysis(Iter column) {
        // First, we must determine what sort of graph we need.  We recognize two possibilities:  every value is 1.0 or 0.0, or
        // we have a spectrum.  We will create a map from label values to input column values.  If we find something other
        // than 1.0 or 0.0, we flag it.
        boolean continuous = false;
        Map<String, ResizableDoubleArray> labelMap = new TreeMap<String, ResizableDoubleArray>();
        // These are used to compute the mean.
        double total = 0.0;
        int count = 0;
        while (column.hasNext()) {
            try {
                // Get the value in this row.
                double value = Double.parseDouble(column.next());
                // Add it to the output map.
                String label = column.getLabel();
                ResizableDoubleArray labelArray = labelMap.computeIfAbsent(label, x -> new ResizableDoubleArray(100));
                labelArray.addElement(value);
                // Update the continuous flag.
                if (value != 0.0 && value != 1.0) continuous = true;
                // Count this value.
                total += value;
                count++;
            } catch (NumberFormatException e) {
                // For an invalid value, we simply skip the row.
            }
        }
        // We have three cases:  no data, continuous input values, and discrete input values.
        Node retVal;
        if (count == 0)
            retVal = new Label("No numeric data points found.");
        else if (continuous)
            retVal = this.continuousScatterChart(labelMap, total, count);
        else
            retVal = this.discreteBarChart(labelMap, count);
        return retVal;
    }

    /**
     * Create a bar chart for the case where we have two input values:  1.0 and 0.0.
     *
     * @param labelMap	map of label classes to value lists
     * @param count		number of input values
     *
     * @return a bar chart showing the correlation
     */
    private Node discreteBarChart(Map<String, ResizableDoubleArray> labelMap, int count) {
        // We have one bar for each label name.  The bar is divided between 1.0 and 0.0 counts.
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("class");
        ObservableList<String> labels = FXCollections.observableArrayList(labelMap.keySet());
        xAxis.setCategories(labels);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("count");
        StackedBarChart<String, Number> retVal = new StackedBarChart<String, Number>(xAxis, yAxis);
        // We have two rows of data to display:  1.0 (present) and 0.0 (absent).
        XYChart.Series<String, Number> presentSeries = new XYChart.Series<String, Number>();
        XYChart.Series<String, Number> absentSeries = new XYChart.Series<String, Number>();
        presentSeries.setName("present");
        absentSeries.setName("absent");
        // Loop through the label data.
        for (Map.Entry<String, ResizableDoubleArray> labelEntry : labelMap.entrySet()) {
            String label = labelEntry.getKey();
            // Count the 1s and 0s for this label.
            int nullCount = 0;
            int oneCount = 0;
            ResizableDoubleArray values = labelEntry.getValue();
            for (int i = 0; i < values.getNumElements(); i++) {
                double value = values.getElement(i);
                if (value == 0.0)
                    nullCount++;
                else
                    oneCount++;
            }
            absentSeries.getData().add(new XYChart.Data<String, Number>(label, nullCount));
            presentSeries.getData().add(new XYChart.Data<String, Number>(label, oneCount));
        }
        // Add the bars to the chart.
        retVal.getData().add(presentSeries);
        retVal.getData().add(absentSeries);
        return retVal;
    }

    /**
     * Create a scatter chart for the case where we have continuous input values.
     *
     * @param labelMap	map of label classes to value lists
     * @param total		sum of all input values
     * @param count		number of input values
     *
     * @return a scatter chart showing the spread
     */
    private Node continuousScatterChart(Map<String, ResizableDoubleArray> labelMap, double total, int count) {
        // Compute the mean.
        double mean = total / count;
        // Create the chart.  The x-axis is the labels, and is string data.
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("class");
        ObservableList<String> labels = FXCollections.observableArrayList(labelMap.keySet());
        xAxis.setCategories(labels);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("count");
        ScatterChart<String, Number> retVal = new ScatterChart<String, Number>(xAxis, yAxis);
        retVal.setLegendVisible(false);
        // We have two series-- one above the mean and one below.
        XYChart.Series<String, Number> highSeries = new XYChart.Series<String, Number>();
        XYChart.Series<String, Number> lowSeries = new XYChart.Series<String, Number>();
        // Loop through the labels, updating the two series.
        for (Map.Entry<String, ResizableDoubleArray> labelEntry : labelMap.entrySet()) {
            String label = labelEntry.getKey();
            ResizableDoubleArray values = labelEntry.getValue();
            // For each label, we loop through the values.
            for (int i = 0; i < values.getNumElements(); i++) {
                double value = values.getElement(i);
                XYChart.Data<String, Number> point = new XYChart.Data<String, Number>(label, value);
                if (value > mean)
                    highSeries.getData().add(point);
                else
                    lowSeries.getData().add(point);
            }
        }
        // Add the series to the chart.
        retVal.getData().add(lowSeries);
        retVal.getData().add(highSeries);
        return retVal;
    }

}
