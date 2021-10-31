/**
 *
 */
package org.theseed.join;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.KeyedFileMap;
import org.theseed.io.TabbedLineReader;
import org.theseed.jfx.BaseController;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;

/**
 * This is the base class for the two filtering operations.
 *
 * @author Bruce Parrello
 *
 */
public abstract class FilterSpec implements IJoinSpec {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(JoinSpec.class);
    /** input file */
    private File inFile;
    /** parent dialog */
    private JoinDialog parent;
    /** display node for this join specification */
    private Node node;

    // CONTROLS

    /** input file name */
    @FXML
    private TextField txtInputFile;

    /** key column selector */
    @FXML
    private ChoiceBox<String> cmbKeyColumn;

    @Override
    public void init(JoinDialog parent, Node node) {
        // Save the parent dialog information.
        this.parent = parent;
        this.node = node;
        // Denote there is no input file.
        this.inFile = null;
    }

    @Override
    public boolean isValid() {
        return (this.inFile != null && ! this.cmbKeyColumn.getSelectionModel().isEmpty());
    }

    @Override
    public Node getNode() {
        return this.node;
    }

    /**
     * Here the user has selected a file as the input file.  We update the
     * file name display and fill in the list box and the choice box.
     *
     * @param inputFile		new input file to use
     *
     * @throws IOException
     */
    private void setupFile(File inputFile) throws IOException {
        // Remember this directory.
        this.parent.setParentDirectory(inputFile.getParentFile());
        // Get the column headers for this file and fill the controls.
        ObservableList<String> keyColumn = this.cmbKeyColumn.getItems();
        try (TabbedLineReader inStream = new TabbedLineReader(inputFile)) {
            if (inStream.size() < 1)
                throw new IOException("Input file must contain at least one column.");
            for (String header : inStream.getLabels())
                keyColumn.add(header);
        }
        // Select the first column as the key.
        this.cmbKeyColumn.getSelectionModel().clearAndSelect(0);
        // Save the file.
        this.inFile = inputFile;
        this.txtInputFile.setText(inputFile.getName());
        // Configure the parent.
        this.parent.configureButtons();
    }

    /**
     * This event fires when the user wants to select a new input file.
     *
     * @param	event	event for button press
     */
    @FXML
    private void selectInput(ActionEvent event) {
        File retVal = JoinSpec.chooseFile(this.parent.getParentDirectory(), this.parent.getStage());
        if (retVal != null) {
            try {
                this.setupFile(retVal);
            } catch (IOException e) {
                BaseController.messageBox(AlertType.ERROR, "Error in Input File", e.toString());
            }
        }
    }

    /**
     * Here the user wants to delete the file from the file list.
     *
     * @param event		event for button press
     */
    @FXML
    private void deleteFile(ActionEvent event) {
        boolean confirmed = JoinSpec.confirmDelete(this.inFile);
        if (confirmed)
            this.parent.deleteFile(this);
    }

    @Override
    public void apply(KeyedFileMap outputMap) throws IOException {
        // Get the set of keys in this input file.
        String keyColumn = this.cmbKeyColumn.getValue();
        Set<String> keys = TabbedLineReader.readSet(this.inFile, keyColumn);
        // Loop through the output map, checking each record against the set.
        Iterator<Map.Entry<String, List<String>>> iter = outputMap.iterator();
        while (iter.hasNext()) {
            String key = iter.next().getKey();
            if (! this.keep(key, keys))
                iter.remove();
        }
    }

    /**
     * Determine whether or not this key belongs in the output file.
     *
     * @param key	current output key
     * @param keys	set of keys in this input file
     *
     * @return TRUE if the key should be kept, else FALSE
     */
    protected abstract boolean keep(String key, Set<String> keys);

}
