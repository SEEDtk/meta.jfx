/**
 *
 */
package org.theseed.dl4j.jfx;

import java.io.File;
import java.io.IOException;

import org.theseed.io.TabbedLineReader;
import org.theseed.jfx.BaseController;
import org.theseed.jfx.ResizableController;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
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

    // CONTROLS

    /** name of training file */
    @FXML
    private Label lblFileName;

    /** table to display the data */
    @FXML
    private TableView<String[]> tblView;

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
            String[] headers = inStream.getLabels();
            for (int i = 0; i < headers.length; i++) {
                TableColumn<String[], String> newCol = new TableColumn<>(headers[i]);
                newCol.setCellValueFactory(new CellDisplayer(i));
                this.tblView.getColumns().add(newCol);
            }
            // Now loop through the input, storing each line as a table row.
            ObservableList<String[]> data = FXCollections.observableArrayList();
            for (TabbedLineReader.Line line : inStream)
                data.add(line.getFields());
            this.tblView.setItems(data);
        }  catch (IOException e) {
            BaseController.messageBox(Alert.AlertType.ERROR, "Error Readining Training File", e.getMessage());
        }
    }
}
