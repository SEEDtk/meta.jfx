/**
 *
 */
package org.theseed.dl4j.jfx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.theseed.dl4j.train.ClassPredictError;
import org.theseed.jfx.DragResizer;
import org.theseed.jfx.Stat;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.util.Callback;

/**
 * This displays the results for a classification model.  For each set
 * (testing, training, all), there is a table and a bar chart.  In the
 * bar chart, each data series represents a predicted value, and each
 * position in the series an expected value.  (Each series thus has
 * one value per bar.)  Conversely, in the table, each column is a
 * predicted value and each row an expected value.  The net effect is
 * predictions are horizontal and actual values are on the X-axis.
 *
 * @author Bruce Parrello
 *
 */
public class ConfusionMatrix extends ValidationDisplayReport implements DragResizer.CallBack {

    /** confusion matrix for the training set [o][e] */
    private int[][] trainMatrix;
    /** confusion matrix for the testing set [o][e] */
    private int[][] testMatrix;
    /** confusion matrix for the sum of the two sets [o][e] */
    private int[][] fullMatrix;
    /** list of labels */
    private List<String> labels;
    /** testing data series for each label */
    private List<XYChart.Series<String, Integer>> testData;
    /** training data series for each label */
    private List<XYChart.Series<String, Integer>> trainData;
    /** full data series for each label */
    private List<XYChart.Series<String, Integer>> fullData;
    /** array of table regions */
    private Region[] tables;

    // CONTROLS

    /** testing set graph */
    @FXML
    private StackedBarChart<String, Integer> testBars;

    /** testing set table */
    @FXML
    private TableView<MatrixRow> testTable;

    /** testing set label column in table */
    @FXML
    private TableColumn<MatrixRow, String> testLabels;

    /** training set graph */
    @FXML
    private StackedBarChart<String, Integer> trainBars;

    /** training set table */
    @FXML
    private TableView<MatrixRow> trainTable;

    /** training set label column in table */
    @FXML
    private TableColumn<MatrixRow, String> trainLabels;

    /** full set graph */
    @FXML
    private StackedBarChart<String, Integer> fullBars;

    /** full set table */
    @FXML
    private TableView<MatrixRow> fullTable;

    /** full set label column in table */
    @FXML
    private TableColumn<MatrixRow, String> fullLabels;


    /**
     * This class is used for the table views.  A matrix row is constructed using
     * a confusion matrix and a row number.
     */
    private class MatrixRow {

        private int[][] matrix;
        private int rowIdx;

        public MatrixRow(int[][] matrix, int rowIdx) {
            this.matrix = matrix;
            this.rowIdx = rowIdx;
        }

        /**
         * @return the label to put in the first column
         */
        public String getLabel() {
            return ConfusionMatrix.this.labels.get(this.rowIdx);
        }

        /**
         * @return the value to put in the specified column
         */
        public Integer getValue(int i) {
            return this.matrix[i][this.rowIdx];
        }

    }

    /**
     * This class implements the callback for getting a label column value.
     */
    private static class CountFinder implements Callback<TableColumn.CellDataFeatures<MatrixRow,Integer>,ObservableValue<Integer>> {

        private int colIdx;

        public CountFinder(int colIdx) {
            this.colIdx = colIdx;
        }

        @Override
        public ObservableValue<Integer> call(CellDataFeatures<MatrixRow, Integer> param) {
            MatrixRow value = param.getValue();
            return new SimpleIntegerProperty(value.getValue(this.colIdx)).asObject();
        }

    }
    /**
     * Initialize this object.  This is handled by the base class.
     */
    public ConfusionMatrix() {
        super();
    }

    @Override
    public void init(List<String> labels) {
        // Save the labels.
        this.labels = labels;
        // Initialize each array.
        this.testMatrix = new int[labels.size()][labels.size()];
        this.trainMatrix = new int[labels.size()][labels.size()];
        this.fullMatrix = new int[labels.size()][labels.size()];
        // Initialize each series and attach it to its graph.
        this.testData = this.createData(this.testBars);
        this.trainData = this.createData(this.trainBars);
        this.fullData = this.createData(this.fullBars);
        // Build the table columns.  Note that unlike the graphs, the tables will
        // refill themselves automatically when we update the internal matrices.
        this.setupTable(this.trainTable, this.trainLabels, this.trainMatrix);
        this.setupTable(this.testTable, this.testLabels, this.testMatrix);
        this.setupTable(this.fullTable, this.fullLabels, this.fullMatrix);
        // Put all the tables into an array.
        this.tables = new TableView[] { this.trainTable, this.testTable, this.fullTable };
    }

    /**
     * Set up a table to contain the appropriate confusion matrix.
     *
     * @param table		target table view
     * @param labelCol	label column of the table
     * @param matrix	relevant confusion matrix
     */
    private void setupTable(TableView<MatrixRow> table, TableColumn<MatrixRow, String> labelCol,
            int[][] matrix) {
        labelCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<MatrixRow,String>,ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<MatrixRow, String> param) {
                MatrixRow value = param.getValue();
                return new SimpleStringProperty(value.getLabel());
            }
        });
        // Create one more column per label.
        for (int e = 0; e < this.labels.size(); e++) {
            TableColumn<MatrixRow, Integer> newColumn = new TableColumn<MatrixRow, Integer>(this.labels.get(e));
            newColumn.setCellValueFactory(new CountFinder(e));
            newColumn.setStyle("-fx-alignment: CENTER-RIGHT");
            newColumn.setPrefWidth(40.0);
            table.getColumns().add(newColumn);
        }
        // Now add the rows.
        ObservableList<MatrixRow> rows = FXCollections.observableArrayList();
        for (int o = 0; o < this.labels.size(); o++) {
            MatrixRow row = new MatrixRow(matrix, o);
            rows.add(row);
        }
        table.setItems(rows);
        table.setPrefSize(80.0 + 40.0 * this.labels.size(), (this.labels.size() + 1) * 25.0);
        // Make the table resizable.
        DragResizer.makeResizable(table, this);
    }

    /**
     * Initialize the data series for a chart.  Each series begins with a dummy value of 1 in each position to insure
     * the legend is filled in.
     *
     * @param chart			chart these series are for
     *
     * @return the list of chart series in label order
     */
    private List<XYChart.Series<String, Integer>> createData(StackedBarChart<String, Integer> chart) {
        // Create the series this.  This is our return value.
        List<XYChart.Series<String, Integer>> retVal = new ArrayList<>(this.labels.size());
        // Set the x-axis categories.
        CategoryAxis xAxis = (CategoryAxis) chart.getXAxis();
        xAxis.setCategories(FXCollections.observableArrayList(this.labels));
        // Create a dummy series for each label.  Each series represents the predictions for an actual value, one per label.
        // The strange part is the series labels are the same as the category labels.
        for (String label : this.labels) {
            ObservableList<XYChart.Data<String, Integer>> data = FXCollections.observableArrayList(
                    this.labels.stream().map(x -> new XYChart.Data<String, Integer>(x, 1)).collect(Collectors.toList()));
            XYChart.Series<String, Integer> series = new XYChart.Series<String, Integer>(label, data);
            // Add the series to the chart.
            chart.getData().add(series);
            // Save a copy to the output list for easy access.
            retVal.add(series);
        }
        return retVal;
    }

    @Override
    public void startReport(List<String> metaCols, List<String> labels) {
        // Clear the matrices.
        this.clear(this.testMatrix);
        this.clear(this.trainMatrix);
        this.clear(this.fullMatrix);
    }

    /**
     * Clear a confusion matrix.
     *
     * @param matrix	matrix to clear
     */
    private void clear(int[][] matrix) {
        for (int i = 0; i < this.labels.size(); i++)
            Arrays.fill(matrix[i], 0);
    }

    @Override
    public void reportOutput(List<String> metaData, INDArray expected, INDArray output) {
        // Loop through the metadata, recording data points.
        for (int r = 0; r < metaData.size(); r++) {
            // Get the ID for this row.
            String id = this.getId(metaData.get(r));
            // Compute the expected and actual values.
            int e = ClassPredictError.computeBest(expected, r);
            int o = ClassPredictError.computeBest(output, r);
            // Now we have the expected and output values.
            if (this.isTrained(id))
                this.trainMatrix[o][e]++;
            else
                this.testMatrix[o][e]++;
            // Every point goes in the full matrix.
            this.fullMatrix[o][e]++;
        }

    }

    @Override
    public void finishReport() {
        // Now we add the data points to the series.
        this.updateSeries(this.trainMatrix, this.trainData);
        this.updateSeries(this.testMatrix, this.testData);
        this.updateSeries(this.fullMatrix, this.fullData);
        // Update the tables.
        this.trainTable.refresh();
        this.testTable.refresh();
        this.fullTable.refresh();
    }

    /**
     * Store the data from the confusion matrix in the series elements.
     *
     * @param matrix		confusion matrix containing the counts; the first dimension is the category,
     * 						the second is the series position
     * @param seriesList	list of data series connected to the relevant graph, one per category
     */
    private void updateSeries(int[][] matrix, List<Series<String, Integer>> seriesList) {
        // Loop through the predicted values.  Each corresponds to a series.
        for (int o = 0; o < this.labels.size(); o++) {
            ObservableList<XYChart.Data<String, Integer>> data = seriesList.get(o).getData();
            // Loop through the predicted values.  Each corresponds to a data point in the series.
            for (int e = 0; e < this.labels.size(); e++) {
                int count = matrix[o][e];
                data.get(e).setYValue(count);
            }
        }
    }

    @Override
    public void handleResize(Region target) {
        // One of the tables was resized.  Resize the others to match.
        double newWidth = target.getPrefWidth();
        double newHeight = target.getPrefHeight();
        for (Region other : this.tables) {
            if (other != target) {
                other.setPrefWidth(newWidth);
                other.setPrefHeight(newHeight);
            }
        }

    }

    @Override
    public List<Stat> getStats() {
        // Compute the testing set accuracy.
        int totalError = 0;
        int total = 0;
        for (int o = 0; o < this.labels.size(); o++) {
            for (int e = 0; e < this.labels.size(); e++) {
                int count = this.testMatrix[o][e];
                if (o != e) totalError += count;
                total += count;
            }
        }
        List<Stat> retVal = new ArrayList<>();
        retVal.add(new Stat("Testing error", ((double) totalError) / total));
        return retVal;
    }

}
