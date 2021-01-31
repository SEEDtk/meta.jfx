/**
 *
 */
package org.theseed.jfx;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * This object represents a prediction for a regression model.  It contains the ID, the actual and predicted values, and is
 * sorted by absolute error (highest to lowest) followed by ID.
 *
 * @author Bruce Parrello
 */
public class Prediction implements Comparable<Prediction> {

    // FIELDS
    /** ID of the data point */
    private String id;
    /** expected value */
    private double expected;
    /** predicted value */
    private double predicted;
    /** absolute error */
    private double absError;
    /** format for display values */
    private static final String DISPLAY_FORMAT = "%14.6f";

    /**
     * Construct a prediction from an ID and the values.
     */
    public Prediction(String id, double expected, double predicted) {
        this.id = id;
        this.expected = expected;
        this.predicted = predicted;
        this.absError = Math.abs(expected - predicted);
    }

    @Override
    public int compareTo(Prediction o) {
        int retVal = Double.compare(o.absError, this.absError);
        if (retVal == 0)
            retVal = this.id.compareTo(o.id);
        return retVal;
    }

    /**
     * @return the ID
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return the display string for the expected value
     */
    public String getActual() {
        return String.format(DISPLAY_FORMAT, this.expected);
    }

    /**
     * @return the display string for the predicted value
     */
    public String getPredicted() {
        return String.format(DISPLAY_FORMAT, this.predicted);
    }

    /**
     * @return the display string for the error value
     */
    public String getError() {
        return String.format(DISPLAY_FORMAT, this.absError);
    }

    /**
     * Initialize a table to contain predictions.
     *
     * @param table		table to set up
     */
    public static void setupTable(TableView<Prediction> table) {
        TableColumn<Prediction, String> newColumn = new TableColumn<Prediction, String>("Outlier");
        table.getColumns().add(newColumn);
        newColumn.setCellValueFactory(new PropertyValueFactory<Prediction, String>("id"));
        newColumn.setResizable(true);

        newColumn = new TableColumn<Prediction, String>("Predicted");
        table.getColumns().add(newColumn);
        newColumn.setCellValueFactory(new PropertyValueFactory<Prediction, String>("predicted"));

        newColumn = new TableColumn<Prediction, String>("Actual");
        table.getColumns().add(newColumn);
        newColumn.setCellValueFactory(new PropertyValueFactory<Prediction, String>("actual"));

        newColumn = new TableColumn<Prediction, String>("Error");
        table.getColumns().add(newColumn);
        newColumn.setCellValueFactory(new PropertyValueFactory<Prediction, String>("error"));
    }

    /**
     * @return the absolute error value
     */
    public double errorValue() {
        return this.absError;
    }

}
