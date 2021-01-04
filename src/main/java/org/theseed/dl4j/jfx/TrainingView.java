/**
 *
 */
package org.theseed.dl4j.jfx;

import java.io.File;
import java.io.IOException;
import org.theseed.jfx.ColumnAnalysis;
import org.theseed.io.TabbedLineReader;
import org.theseed.jfx.BaseController;
import org.theseed.jfx.DistributionAnalysis;
import org.theseed.jfx.ResizableController;
import org.theseed.jfx.StatisticsAnalysis;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.util.Callback;

/**
 * This class displays the contents of the training file.  The file is tab-delimited, so it is loaded
 * into a table view.
 *
 * @author Bruce Parrello
 *
 */
public class TrainingView extends ResizableController {

    /**
     * This is a simple nested class to display the appropriate field of a line in a table cell.
     */
    public class CellDisplayer implements Callback<CellDataFeatures<String[], String>, ObservableValue<String>> {

        private int idx;

        /**
         * Construct this object to display the specified table column's fields.
         *
         * @param idx		table column index
         */
        public CellDisplayer(int idx) {
            this.idx = idx;
        }

        @Override
        public ObservableValue<String> call(CellDataFeatures<String[], String> param) {
            return new SimpleStringProperty(param.getValue()[idx]);
        }

    }

    // FIELDS
    /** list of data lines from the file */
    private ObservableList<String[]> lineBuffer;
    /** headers from the file */
    private String[] headers;

    // CONTROLS

    /** name of training file */
    @FXML
    private Label lblFileName;

    /** table to display the data */
    @FXML
    private TableView<String[]> tblView;

    /** list of columns to select */
    @FXML
    private ListView<String> lstColumns;

    /** output panel for analysis graphs */
    @FXML
    private Pane paneResults;

    /**
     * Position and size this window.
     */
    public TrainingView() {
        super(200, 200, 1000, 750);
    }

    @Override
    public String getIconName() {
        return "job-16.png";
    }

    @Override
    public String getWindowTitle() {
        return "Training File Display";
    }

    /**
     * Initialize this page.  All of the columns will be created in the table and the data lines read in.
     */
    public void init(File trainingFile) {
        // Save the file name for display.
        this.lblFileName.setText(trainingFile.getAbsolutePath());
        try (TabbedLineReader inStream = new TabbedLineReader(trainingFile)) {
            // Use the header to create the columns.
            this.headers = inStream.getLabels();
            for (int i = 0; i < headers.length; i++) {
                TableColumn<String[], String> newCol = new TableColumn<>(this.headers[i]);
                newCol.setCellValueFactory(new CellDisplayer(i));
                this.tblView.getColumns().add(newCol);
            }
            // Now loop through the input, storing each line as a table row.
            this.lineBuffer = FXCollections.observableArrayList();
            for (TabbedLineReader.Line line : inStream)
                this.lineBuffer.add(line.getFields());
            this.tblView.setItems(this.lineBuffer);
            // Populate the list on the stats panel.
            this.lstColumns.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            this.lstColumns.setItems(FXCollections.observableArrayList(this.headers));
        }  catch (IOException e) {
            BaseController.messageBox(Alert.AlertType.ERROR, "Error Readining Training File", e.getMessage());
        }
    }

    /**
     * Here the user wants to do a distribution analysis of the selected columns.
     *
     * @param event		event descriptor
     */
    @FXML
    public void analyzeColumns(ActionEvent event) {
        ColumnAnalysis analyzer = new DistributionAnalysis(this.lineBuffer);
        this.processColumns(analyzer);
    }

    /**
     * Here the user wants mean and standard deviation statistcs on the selected columns.
     *
     * @param event		event descriptor
     */
    @FXML
    public void computeStatistics(ActionEvent event) {
        ColumnAnalysis analyzer = new StatisticsAnalysis(this.lineBuffer);
        this.processColumns(analyzer);
    }

    /**
     * Here the user wants to erase all the results displayed in the result panel.
     *
     * @param event		event descriptor
     */
    @FXML
    public void clearResults(ActionEvent event) {
        this.paneResults.getChildren().clear();
    }

    /**
     * Produce results for the currently-selected columns and add them to the result panel.
     *
     * @param analyzer		analyzer to apply for producing the results.
     */
    private void processColumns(ColumnAnalysis analyzer) {
        // Get the selected column indices.
        ObservableList<Integer> selected = this.lstColumns.getSelectionModel().getSelectedIndices();
        for (int selectIdx : selected) {
            TitledPane result = analyzer.getDisplay(this.headers[selectIdx], selectIdx);
            this.paneResults.getChildren().add(result);
        }
        // De-select everything.
        this.lstColumns.getSelectionModel().clearSelection();
    }

}
