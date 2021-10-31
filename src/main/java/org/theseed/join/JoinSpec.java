/**
 *
 */
package org.theseed.join;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.dl4j.jfx.MeanBiasDialog;
import org.theseed.io.KeyedFileMap;
import org.theseed.io.TabbedLineReader;
import org.theseed.jfx.BaseController;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 * This control group represents the data for a single file in a join operation.  It allows specification of
 * the file name, the key column, the columns to keep, and a checkbox indicating that the desired operation is
 * a negative filtering instead of a join.  (Positive filtering is a join with all the columns turned off.)
 * For the first file, the negative filtering option will be suppressed.  When a file is first specified, all the
 * columns will be pre-selected.
 *
 * @author Bruce Parrello
 *
 */
public abstract class JoinSpec implements IJoinSpec {

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

    /** list view for selecting columns to include */
    @FXML
    private ListView<String> lstColumns;

    /** button to delete the file spec */
    @FXML
    protected Button btnDelete;

    /**
     * Create a standard join-specification control.
     */
    public JoinSpec() {
    }

    /**
     * Initialize this specifier for a specific file.
     *
     * @param parent	master dialog for all the join specs
     * @param node		display node for this controller
     *
     * @throws IOException
     */
    @Override
    public void init(JoinDialog parent, Node node) {
        // Remember the parent dialog.
        this.parent = parent;
        this.node = node;
        // Clear the input file.
        this.inFile = null;
        // Perform any additional subclass initialization.
        this.initialConfigure();
        // Allow multiple selections in the column list.
        this.lstColumns.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    /**
     * Perform any special setup for the subclass.
     */
    protected abstract void initialConfigure();

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
        ObservableList<String> dataColumns = this.lstColumns.getItems();
        try (TabbedLineReader inStream = new TabbedLineReader(inputFile)) {
            if (inStream.size() < 1)
                throw new IOException("Input file must contain at least one column.");
            for (String header : inStream.getLabels()) {
                keyColumn.add(header);
                dataColumns.add(header);
            }
        }
        // Select the first column as the key.
        this.cmbKeyColumn.getSelectionModel().clearAndSelect(0);
        // Denote we want to include all columns.
        this.lstColumns.getSelectionModel().selectAll();
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
        File retVal = chooseFile(this.parent.getParentDirectory(), this.parent.getStage());
        if (retVal != null) {
            try {
                this.setupFile(retVal);
            } catch (IOException e) {
                BaseController.messageBox(AlertType.ERROR, "Error in Input File", e.toString());
            }
        }
    }

    /**
     * @return an input file chosen by the user
     *
     * @param parentFile	directory to start in
     * @param stage			stage on which controls are being displayed
     */
    public static File chooseFile(File parentFile, Stage stage) {
        // Initialize the chooser dialog.
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select File to Join");
        chooser.setInitialDirectory(parentFile);
        chooser.getExtensionFilters().addAll(MeanBiasDialog.TEXT_FILES,
                new ExtensionFilter("All Files", "*.*"));
        // Get the file.
        File retVal = chooser.showOpenDialog(stage);
        return retVal;
    }

    /**
     * Here the user wants to select all columns.
     *
     * @param	event	event for button press
     */
    @FXML
    private void allColumns(ActionEvent event) {
        this.lstColumns.getSelectionModel().selectAll();
    }

    /**
     * Here the user wants to select no columns.
     *
     * @param	event	event for button press
     */
    @FXML
    private void noColumns(ActionEvent event) {
        this.lstColumns.getSelectionModel().clearSelection();
    }

    /**
     * Here the user wants to delete the file from the file list.
     *
     * @param event		event for button press
     */
    @FXML
    private void deleteFile(ActionEvent event) {
        boolean confirmed = confirmDelete(this.inFile);
        if (confirmed)
            this.parent.deleteFile(this);

    }

    /**
     * Confirm with the user if it's ok to delete a join specification.
     *
     * @param oldFile		current input file
     */
    protected static boolean confirmDelete(File oldFile) {
        // If there is a file in here, get the user to confirm the delete.
        boolean retVal = true;
        if (oldFile != null) {
            Alert a = new Alert(AlertType.CONFIRMATION);
            a.setTitle("Remove Input File");
            a.setContentText("Delete file " + oldFile + " from the input list?");
            Optional<ButtonType> buttonSelected = a.showAndWait();
            retVal = (buttonSelected.isPresent() && buttonSelected.get() == ButtonType.OK);
        }
        return retVal;
    }

    /**
     * Here the user wants to invert the selections.
     *
     * @param event		event for button press
     */
    @FXML
    private void invertColumns(ActionEvent event) {
        MultipleSelectionModel<String> selectionModel = this.lstColumns.getSelectionModel();
        // Get a map of the selected items.
        boolean[] selected = new boolean[this.lstColumns.getItems().size()];
        IntStream.range(0, selected.length).forEach(i -> selected[i] = selectionModel.isSelected(i));
        // Clear all current selections.
        selectionModel.clearSelection();
        // Select all the de-selected items from before.
        IntStream.range(0,  selected.length).filter(i -> ! selected[i]).forEach(i -> selectionModel.select(i));
    }

    /**
     * @return TRUE if this join specification is valid, else FALSE
     */
    public boolean isValid() {
        return (this.inFile != null && ! this.cmbKeyColumn.getSelectionModel().isEmpty());
    }

    /**
     * @return the key column name
     */
    public String getKeyColumn() {
        return this.cmbKeyColumn.getSelectionModel().getSelectedItem();
    }

    /**
     * @return the selected list of column headers
     */
    public List<String> getHeaders() {
        // Get the key column name.  The key column is always excluded.
        String keyColumn = this.getKeyColumn();
        // Get the list of column names from the column list.
        List<String> retVal = this.lstColumns.getSelectionModel().getSelectedItems().stream().filter(x -> ! x.contentEquals(keyColumn)).collect(Collectors.toList());
        return retVal;
    }

    /**
     * This method reads the input file, and creates a linked hash map of the records.  The key is the key column, and the
     * value is a list of data column strings, in the same order as the headers.
     *
     * @return a linked hash map from the key values to the data columns
     *
     * @throws IOException
     */
    public LinkedHashMap<String, List<String>> getRecordMap() throws IOException {
        // Create the hash map.
        int size_estimate = ((int) this.inFile.length()) / 40;
        LinkedHashMap<String, List<String>> retVal = new LinkedHashMap<String, List<String>>(size_estimate);
        // Open the file and get the list of data columns.
        try (TabbedLineReader inStream = new TabbedLineReader(this.inFile)) {
            int[] cols = this.getHeaders().stream().mapToInt(x -> inStream.findColumn(x)).toArray();
            // Verify we found all the fields.
            boolean ok = true;
            for (int i = 0; i < cols.length && ok; i++)
                ok = (cols[i] >= 0);
            if (! ok)
                throw new IOException("Input file " + this.inFile + " has changed on disk.  Reselect and try again.");
            // Get the key column.
            int keyIdx = inStream.findField(this.getKeyColumn());
            // Now read in the file.
            for (TabbedLineReader.Line line : inStream) {
                String key = line.get(keyIdx);
                // We don't use a standard stream here because we need a mutable list.
                List<String> data = new ArrayList<String>(cols.length);
                Arrays.stream(cols).forEach(i -> data.add(line.get(i)));
                // Associate the key with the data.  Note the order is preserved by the linked hash map,
                // but if there are duplicate keys, the last one will be the one kept.
                retVal.put(key, data);
            }
        }
        // Return the filled map.
        return retVal;
    }

    /**
     * @return the display node for this file specification
     */
    @Override
    public Node getNode() {
        return this.node;
    }

    /**
     * @return the input file
     */
    protected File getInFile() {
        return this.inFile;
    }

    @Override
    public void apply(KeyedFileMap keyedMap) throws IOException {
        // Here we need to read the input file and extract the key column and the data columns.
        File inFile = this.getInFile();
        try (TabbedLineReader inStream = new TabbedLineReader(inFile)) {
            // Get the key column name and index.
            String keyName = this.getKeyColumn();
            int keyIdx = inStream.findField(keyName);
            // Get the names and indices of the data columns.
            List<String> headers = this.getHeaders();
            int[] cols = new int[headers.size()];
            for (int i = 0; i < cols.length; i++)
                cols[i] = inStream.findField(headers.get(i));
            // Append the headers to the output headers.
            keyedMap.addHeaders(headers);
            // Now read in the file.
            for (TabbedLineReader.Line line : inStream) {
                String key = line.get(keyIdx);
                List<String> data = Arrays.stream(cols).mapToObj(i -> line.get(i)).collect(Collectors.toList());
                this.processLine(keyedMap, key, data);
            }
            // Apply the file to the keyed map.
            this.finishFile(keyedMap, headers.size());
        }
    }

    /**
     * Finish all processing for the current input file.
     *
     * @param keyedMap	current output map
     * @param width		number of new columns for this input file
     */
    protected abstract void finishFile(KeyedFileMap keyedMap, int width);

    /**
     * Apply a line of data to the current output map.
     *
     * @param keyedMap		output map
     * @param key			key of the current line
     * @param data			data fields in the current line
     */
    protected abstract void processLine(KeyedFileMap keyedMap, String key, List<String> data);

}
