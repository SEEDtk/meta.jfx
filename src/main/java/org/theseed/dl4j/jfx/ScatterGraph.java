/**
 *
 */
package org.theseed.dl4j.jfx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.theseed.jfx.Prediction;
import org.theseed.jfx.Stat;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;

/**
 * This is the controller for the scatter graph that displays expected vs. predicted values of a regression model.
 * The scatter graph is a useful visual guide to the model's accuracy.
 *
 * @author Bruce Parrello
 *
 */
public class ScatterGraph extends ValidationDisplayReport {

    // FIELDS
    /** index of the current label of interest */
    private int labelIdx;
    /** training series */
    private XYChart.Series<Double, Double> trainingPoints;
    /** testing series */
    private XYChart.Series<Double, Double> testingPoints;
    /** outlier list */
    private SortedSet<Prediction> outliers;
    /** outlier table items */
    private ObservableList<Prediction> outlierItems;
    /** recommended maximum outliers */
    private static final int MAX_OUTLIERS = 20;

    // CONTROLS

    /** combo box for selecting label */
    @FXML
    private ComboBox<String> cmbLabel;

    /** graph control */
    @FXML
    private ScatterChart<Double, Double> scatterGraph;

    /**
     * The constructor simply calls through to the base class.
     */
    public ScatterGraph() {
        super();
    }

    /**
     * Initialize the display controls.
     *
     * @param labels	list of the labels for the parent model
     */
    @Override
    public void init(List<String> labels) {
        // Fill in the combo box for choosing the labels.
        ObservableList<String> labelList = FXCollections.observableArrayList(labels);
        this.cmbLabel.setItems(labelList);
        this.cmbLabel.getSelectionModel().select(0);
        // Create the two series sets.  We put a dummy point in each to insure the legend plots correctly.
        // The legend does not update when we update the series values.
        this.trainingPoints = new XYChart.Series<Double, Double>();
        this.trainingPoints.setName("training");
        this.trainingPoints.getData().add(new XYChart.Data<Double, Double>(0.0, 0.0));
        this.testingPoints = new XYChart.Series<Double, Double>();
        this.testingPoints.setName("testing");
        this.testingPoints.getData().add(new XYChart.Data<Double, Double>(0.0, 0.0));
        // Put the data series into the graph.
        this.scatterGraph.getData().add(this.trainingPoints);
        this.scatterGraph.getData().add(this.testingPoints);
    }

    @Override
    public void startReport(List<String> metaCols, List<String> labels) {
        // Save the index of the label of interest.
        this.labelIdx = this.cmbLabel.getSelectionModel().getSelectedIndex();
        // Clear the data series.
        this.trainingPoints.getData().clear();
        this.testingPoints.getData().clear();
        // Clear the outlier list.
        this.outliers = new TreeSet<Prediction>();
    }

    @Override
    public void reportOutput(List<String> metaData, INDArray expected, INDArray output) {
        // Save the testing data points in here.  We want them on top.
        List<XYChart.Data<Double, Double>> testingData = new ArrayList<>(1000);
        // Loop through the metadata, peeling off predictions.
        for (int r = 0; r < metaData.size(); r++) {
            String id = this.getId(metaData.get(r));
            // Create the data point.
            double expect = expected.getDouble(r, this.labelIdx);
            double predict = output.getDouble(r, this.labelIdx);
            XYChart.Data<Double, Double> dataPoint = new XYChart.Data<Double, Double>(expect, predict);
            // Update the outlier list.
            this.processOutlier(id, expect, predict);
            // Determine which series should contain the data.
            if (this.isTrained(id))
                this.trainingPoints.getData().add(dataPoint);
            else
                testingData.add(dataPoint);
        }
        // Unspool the testing points.
        this.testingPoints.getData().addAll(testingData);
        // Fill in the outlier table.
        this.outlierItems.clear();
        this.outlierItems.addAll(this.outliers);
    }

    /**
     * Add this data point to the outlier list.  We allow the outlier list to grow past the max if all the
     * predictions at the end have the same error value.
     *
     * @param id		ID of the data point
     * @param expect	expected value
     * @param predict	predicted value
     */
    private void processOutlier(String id, double expect, double predict) {
        Prediction newPoint = new Prediction(id, expect, predict);
        while (this.outliers.size() >= MAX_OUTLIERS && newPoint.errorValue() > this.outliers.last().errorValue())
            this.outliers.remove(this.outliers.last());
        if (this.outliers.size() < MAX_OUTLIERS || newPoint.errorValue() > this.outliers.last().errorValue())
            this.outliers.add(newPoint);
    }

    @Override
    public List<Stat> getStats() {
        return Collections.emptyList();
    }

    @Override
    public void registerOutlierTable(TableView<Prediction> outlierTable) {
        // Set up the table columns.
        Prediction.setupTable(outlierTable);
        // Get the table's row list.
        this.outlierItems = outlierTable.getItems();
    }

}
