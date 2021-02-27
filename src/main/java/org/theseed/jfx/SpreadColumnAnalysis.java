/**
 *
 */
package org.theseed.jfx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

/**
 * This analyzer creates a scatter graph showing the spread of the column values for each label.  This gives us an idea of
 * the relationship between the label values and the classifications.  A scatter plot is used with a category axis in
 * the x-dimension.
 *
 * @author Bruce Parrello
 *
 */
public class SpreadColumnAnalysis extends ColumnAnalysis {

    // FIELDS
    /** list of label names */
    private List<String> labelNames;

    /**
     * Initialize the spread analysis.
     *
     * @param data		input lines
     * @param labelIdx	label column index
     * @param labels	list of label names
     */
    public SpreadColumnAnalysis(Collection<String[]> data, int labelIdx, List<String> labels) {
        super(data, labelIdx);
        this.labelNames = labels;
    }

    @Override
    protected Node getAnalysis(Iter column) {
        // The x-axis is a string, because it is a label.  The y-axis is a double.
        ObservableList<String> labels = FXCollections.observableArrayList(labelNames);
        // We will separate the points into above the mean and below the mean.
        XYChart.Series<String, Number> lowSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> highSeries = new XYChart.Series<>();
        // Queue the points in here.
        List<XYChart.Data<String, Number>> points = new ArrayList<XYChart.Data<String, Number>>(100);
        // This is used to compute the mean.
        double total = 0.0;
        int count = 0;
        // Loop through the iterator.
        while (column.hasNext()) {
            try {
                // Get the value in this row.
                double value = Double.parseDouble(column.next());
                // Add it along with the class label.
                points.add(new XYChart.Data<String, Number>(column.getLabel(), value));
                count++;
                total += value;
            } catch (NumberFormatException e) {
                // For an invalid value, we simply skip the row.
            }
        }
        Node retVal;
        if (count == 0) {
            retVal = new Label("No numeric data points found.");
        } else {
            // Compute the mean.
            double mean = total / count;
            // Split the points.
            for (XYChart.Data<String, Number> point : points) {
                if (point.getYValue().doubleValue() > mean)
                    highSeries.getData().add(point);
                else
                    lowSeries.getData().add(point);
            }
            // Now we build a chart from the series.
            CategoryAxis xAxis = new CategoryAxis(labels);
            xAxis.setLabel("class");
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("value");
            ScatterChart<String, Number> graph = new ScatterChart<String, Number>(xAxis, yAxis);
            graph.setLegendVisible(false);
            graph.getData().add(lowSeries);
            graph.getData().add(highSeries);
            retVal = graph;
        }
        return retVal;
    }

}
