/**
 *
 */
package org.theseed.dl4j.jfx;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.jfx.BaseController;
import org.theseed.jfx.ResizableController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * This dialog allows the user to join multiple tab-delimited files.  Its most common usage is to add meta-data from
 * other sources to a prediction file.
 *
 * @author Bruce Parrello
 *
 */
public class JoinDialog extends ResizableController {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(JoinDialog.class);
    /** output file */
    private File outFile;
    /** primary input file specification */
    private JoinSpec inFile;
    /** additional input file specifications */
    private List<JoinSpec> otherFiles;
    /** default directory for file dialogs */
    private File parentDirectory;

    // CONTROLS

    /** output file name */
    @FXML
    private TextField txtOutputFile;

    /** run button */
    @FXML
    private Button btnJoinFiles;

    /** container for join specs */
    @FXML
    private HBox joinBox;

    /** checkbox for left-join */
    @FXML
    private CheckBox chkLeftJoin;

    /**
     * Initialize the join dialog.
     */
    public JoinDialog() {
        super(200, 200, 800, 600);
    }

    @Override
    public String getIconName() {
        return "job-16.png";
    }

    @Override
    public String getWindowTitle() {
        return "Join Tab-Delimited Files";
    }

    /**
     * Initialize this join dialog.  We need to create the first file specification, for the
     * primary input file. This is the one that can't be deleted.
     *
     * @param inFile		primary input file to use
     *
     * @throws IOException
     */
    public void init(File inFile) throws IOException {
        // Denote we have no output file.
        this.outFile = null;
        // Denote we have no additional input files.
        this.otherFiles = new ArrayList<JoinSpec>();
        // Set the stage.
        Stage currStage = this.getStage();
        // Note that this window is modal.
        currStage.initModality(Modality.APPLICATION_MODAL);
        // Create the join specification and store it in the join box.
        this.inFile = createJoinSpec(inFile, true);
        // Configure the run button.
        this.configureButtons();
    }

    /**
     * Create a new join specification for this operation.
     *
     * @param inFile	chosen input file
     * @param first		TRUE if this is the first file, else FALSE
     *
     * @return the new join specification created
     *
     * @throws IOException
     */
    public JoinSpec createJoinSpec(File inFile, boolean first) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("JoinSpec.fxml"));
        Node displayNode = fxmlLoader.load();
        this.joinBox.getChildren().add(displayNode);
        // Initialize the join specification.
        JoinSpec retVal = (JoinSpec) fxmlLoader.getController();
        retVal.init(inFile, first, this);
        retVal.setNode(displayNode);
        return retVal;
    }

    /**
     * Insure the run button is only enabled if we are ready.
     */
    private void configureButtons() {
        this.btnJoinFiles.setDisable(! this.inFile.isValid() || this.outFile == null);
    }

    /**
     * This event fires when the user wants to add a new file.
     *
     * @param event		button-click event
     */
    @FXML
    private void addFileSpec(ActionEvent event) {
        try {
            // Ask for a new input file.
            File newFile = JoinSpec.chooseFile(this.parentDirectory, this.getStage());
            if (newFile != null) {
                JoinSpec newSpec = this.createJoinSpec(newFile, false);
                this.otherFiles.add(newSpec);
                this.configureJoinBox();
            }
        } catch (Exception e) {
            BaseController.messageBox(AlertType.ERROR, "Error Processing Input File", e.toString());
        }
    }

    /**
     * Insure the join box is the correct width.
     */
    private void configureJoinBox() {
        this.joinBox.setPrefWidth(400.0 * (1 + this.otherFiles.size()));
    }

    /**
     * This event fires when the user wants to select the output file.
     *
     * @param event		button-click event
     */
    @FXML
    private void selectOutputFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Join Output File");
        chooser.setInitialDirectory(this.parentDirectory);
        chooser.getExtensionFilters().addAll(MeanBiasDialog.TEXT_FILES,
                new ExtensionFilter("All Files", "*.*"));
        // Get the file.
        File retVal = chooser.showSaveDialog(this.getStage());
        if (retVal != null) {
            this.outFile = retVal;
            this.parentDirectory = retVal.getParentFile();
            this.txtOutputFile.setText(this.outFile.getName());
            this.configureButtons();
        }
    }

    /**
     * This event first when the user wants to join the files.
     *
     * @param event		button-click event
     */
    @FXML
    private void joinFiles(ActionEvent event) {
        try {
            // Determine if this is a left join.
            boolean leftJoin = chkLeftJoin.isSelected();
            // This list will contain the headers.
            List<String> headers = new ArrayList<String>();
            // This hash map will contain the output records.
            LinkedHashMap<String, List<String>> outputMap = this.inFile.getRecordMap();
            // Start with the input file.
            headers.add(this.inFile.getKeyColumn());
            headers.addAll(this.inFile.getHeaders());
            // Loop through the other files.
            for (JoinSpec otherFile : this.otherFiles) {
                // Only proceed if the input file is valid.
                if (otherFile.isValid()) {
                    // Read this input file.
                    Map<String, List<String>> inputMap = otherFile.getRecordMap();
                    if (otherFile.isNegative()) {
                        // Here the file is for negative filtering.  Remove its keys from the
                        // input file.
                        for (String badKey : inputMap.keySet())
                            outputMap.remove(badKey);
                    } else {
                        // Add this file's columns to the header list.
                        List<String> newHeaders = otherFile.getHeaders();
                        headers.addAll(newHeaders);
                        int width = newHeaders.size();
                        // Loop through the output map, modifying it with the input data.
                        Iterator<Map.Entry<String, List<String>>> outputIter = outputMap.entrySet().iterator();
                        while (outputIter.hasNext()) {
                            Map.Entry<String, List<String>> outputEntry = outputIter.next();
                            // Get the current output record.
                            List<String> outRecord = outputEntry.getValue();
                            // Get the corresponding record in the current input file.
                            List<String> record = inputMap.get(outputEntry.getKey());
                            if (record != null) {
                                // This output record has a correspondent in the input file.  Append the
                                // input file's data.
                                outRecord.addAll(record);
                            } else if (leftJoin) {
                                // Here we are doing a left join.  Blank all the columns.
                                IntStream.range(0, width).forEach(i -> outRecord.add(""));
                            } else {
                                // Here we are doing a normal join.  There is no matching input record,
                                // so the output record is deleted.
                                outputIter.remove();
                            }
                        }
                    }
                }
            }
            // Now the output map contains all the output records, fully-populated.
            try (PrintWriter writer = new PrintWriter(this.outFile)) {
                // Write the header line.
                writer.println(StringUtils.join(headers, '\t'));
                // Write all the data lines.
                for (Map.Entry<String, List<String>> outputEntry : outputMap.entrySet()) {
                    // Write the key.
                    writer.print(outputEntry.getKey());
                    // Write the data columns.
                    writer.println(StringUtils.join(outputEntry.getValue(), '\t'));
                }
            }
        } catch (Exception e) {
            BaseController.messageBox(AlertType.ERROR, "Error in Join Operation", e.toString());
        }
    }

    /**
     * @return the parent directory for the next file operation
     */
    protected File getParentDirectory() {
        return this.parentDirectory;
    }

    /**
     * @param parentDirectory specify the parent directory for the next file operation
     */
    protected void setParentDirectory(File parentDirectory) {
        this.parentDirectory = parentDirectory;
    }

    /**
     * Remove a file from the file input list.
     *
     * @param fileSpec	join specification for file to remove
     */
    public void deleteFile(JoinSpec fileSpec) {
        // Remove the join specification from the data structures.
        this.joinBox.getChildren().remove(fileSpec.getNode());
        this.otherFiles.remove(fileSpec);
        // Insure the join box is the correct size.
        this.configureJoinBox();
    }

}
