/**
 *
 */
package org.theseed.join;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.KeyedFileMap;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * This saves a current copy of the output in an excel spreadsheet as a table.
 *
 * @author Bruce Parrello
 *
 */
public class ExcelSaveSpec implements IJoinSpec {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ExcelSaveSpec.class);
    /** output file */
    private File outFile;
    /** parent dialog */
    private JoinDialog parent;
    /** display node for this join specification */
    private Node node;

    // CONTROLS

    /** display field for output file name */
    @FXML
    private TextField txtOutputFile;

    /** sheet name to use */
    @FXML
    private TextField txtSheetName;

    @Override
    public void init(JoinDialog parent, Node node) {
        // Connect the parent dialog.
        this.parent = parent;
        this.node = node;
        // Denote there is no output file.
        this.outFile = null;
        // Default a sheet name.
        this.txtSheetName.setText("New File");
    }

    @Override
    public boolean isValid() {
        return (this.outFile != null && ! this.txtSheetName.getText().isBlank());
    }

    /**
     * Here the user wants to specify the excel output file.
     *
     * @param event		event for button press
     */
    @FXML
    private void selectOutput(ActionEvent event) {
        // Initialize the chooser dialog.
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Output Spreadsheet");
        chooser.setInitialDirectory(this.parent.getParentDirectory());
        chooser.getExtensionFilters().addAll(new ExtensionFilter("Excel File", "*.xlsm", "*.xlsx"),
                new ExtensionFilter("All Files", "*.*"));
        // Get the file.
        File newOutFile = chooser.showSaveDialog(this.parent.getStage());
        if (newOutFile != null) {
            // We have a new output file.  Save the parent directory for next time.
            this.parent.setParentDirectory(newOutFile.getParentFile());
            // Save the file name.
            this.outFile = newOutFile;
            this.txtOutputFile.setText(this.outFile.getName());
            // Update the state of the parent.
            this.parent.configureButtons();
        }
    }

    @Override
    public void apply(KeyedFileMap keyedMap) {
        // TODO code for apply

    }

    @Override
    public Node getNode() {
        return this.node;
    }

}
