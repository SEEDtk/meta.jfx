/**
 *
 */
package org.theseed.dl4j.jfx;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.theseed.dl4j.DistributedOutputStream;
import org.theseed.dl4j.train.TrainingProcessor;
import org.theseed.io.LineReader;
import org.theseed.io.ParmDescriptor;
import org.theseed.io.ParmFile;
import org.theseed.jfx.BaseController;
import org.theseed.jfx.MovableController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * This controller manages the process of creating a training file from external data.  There must
 * already be a training.tbl file in the model directory, but it can contain just headers.  The
 * old file will be destroyed.  For a classification model, the data will be balanced.  For
 * a regression model, the data will be scrambled.
 *
 * @author Bruce Parrello
 *
 */
public class BalanceInput extends MovableController {

    // FIELDS
    /** model directory */
    private File modelDirectory;
    /** input file */
    private File inputFile;
    /** parameter file controller */
    private ParmFile parms;
    /** parameter file name */
    private File parmFile;
    /** current model processor */
    private TrainingProcessor processor;

    // CONTROLS

    /** choice box for input format */
    @FXML
    private ChoiceBox<InputFormat> cmbFormat;

    /** text field for displaying input file name */
    @FXML
    private TextField txtInputFile;

    /** button to start the process */
    @FXML
    private Button btnRun;

    /** checked if we need to add an ID field */
    @FXML
    private CheckBox chkMakeIDs;

    /** choice box for selecting label of interest */
    @FXML
    private ChoiceBox<String> cmbLabel;

    public BalanceInput() {
        super(200, 200);
    }

    @Override
    public String getIconName() {
        return "job-16.png";
    }

    @Override
    public String getWindowTitle() {
        return "Create Balanced Training File";
    }

    /**
     * Use the model directory to prime the input file search.
     *
     * @param	modelDir	input model directory
     * @param	parmFile	relevant parameter file
     * @param	type		model type
     *
     * @throws IOException
     */
    public void init(File modelDir, File parmFile, TrainingProcessor processor) throws IOException {
        // Save the parameters and the model directory.
        this.modelDirectory = modelDir;
        this.parmFile = parmFile;
        this.parms = new ParmFile(parmFile);
        this.processor = processor;
        // Initialize the format combo box.
        this.cmbFormat.getItems().addAll(InputFormat.values());
        this.cmbFormat.getSelectionModel().select(InputFormat.PATRIC);
        // Set up the label combo box.
        List<String> labelCols = processor.getLabelCols();
        this.cmbLabel.getItems().addAll(labelCols);
        this.cmbLabel.getSelectionModel().select(0);
        // Find out if we should generate IDs.
        this.chkMakeIDs.setSelected(this.parms.get("id").isCommented());
        // Denote we are not ready to run.
        btnRun.setDisable(true);
    }

    /**
     * Select the input file.
     *
     * @param event		event descriptor
     */
    public void selectInput(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Raw Input File");
        chooser.setInitialDirectory(this.modelDirectory);
        // We add a special filter for the preferred extension of the selected format.
        ExtensionFilter specialFilter = this.cmbFormat.getValue().getFilter();
        chooser.getExtensionFilters().addAll(specialFilter,
                new ExtensionFilter("All Files", "*.*"));
        // Get the file.
        this.inputFile = chooser.showOpenDialog(this.getStage());
        if (inputFile != null) {
            btnRun.setDisable(false);
            txtInputFile.setText(this.inputFile.getName());
            btnRun.setDisable(false);
        }
    }

    /**
     * Create the balanced input file.  Note that we suppress a dead-code warning
     * because it's a false positive.  If an exception occurs in one of the
     * subroutines, "outStream" will be non-null in the handler.  Java cannot
     * figure this out.
     *
     * @param event		event descriptor
     */
    public void runBalancer(ActionEvent event) {
        // Compute the output file name and the backup file name.
        File trainingFile = new File(this.modelDirectory, "training.tbl");
        File backupFile = new File(this.modelDirectory, "training.old.tbl");
        // Get the input type.
        InputFormat type = this.cmbFormat.getValue();
        // We use this to determine if we need to restore from backup.
        boolean restoreNeeded = false;
        // Open the input file.
        try (LineReader inStream = new LineReader(this.inputFile)) {
            // Get the headers.
            String[] headers = type.getHeaders(inStream, trainingFile);
            // Make a safety copy of the training file.
            FileUtils.copyFile(trainingFile, backupFile);
            // Process the input to produce the output.
            boolean ok = balanceInput(trainingFile, type, inStream, headers);
            // End the dialog.
            if (ok) {
                this.getStage().close();
                restoreNeeded = false;
                BaseController.messageBox(Alert.AlertType.INFORMATION, "Input Conversion", trainingFile.getPath() + " has been updated.");
            }
        } catch (IOException e) {
            BaseController.messageBox(Alert.AlertType.ERROR, "Error Converting Input", e.getMessage());
        }
        if (restoreNeeded) {
            // Here we must restore the training.tbl file from backup.
            try {
                FileUtils.copyFile(backupFile, trainingFile);
            } catch (IOException e) {
                // Here we couldn't restore the backup, which is really bad.
                BaseController.messageBox(Alert.AlertType.ERROR, "Error Restoring Training File",
                        e.getMessage());
            }
        }
    }

    /**
     * Produce the output by balancing the input.
     *
     * @param trainingFile	name of the output training file
     * @param type			type of input
     * @param inStream		input stream
     * @param headers		headers from the training file
     *
     * @return TRUE if successful, else FALSE
     */
    private boolean balanceInput(File trainingFile, InputFormat type, Iterator<String> inStream,
            String[] headers) {
        boolean retVal = false;
        // Add the ID to the headers if we need it.
        String[] actualHeaders = headers;
        if (this.chkMakeIDs.isSelected())
            actualHeaders = ArrayUtils.add(headers, "id");
        int idCounter = 1;
        try {
            DistributedOutputStream outStream = DistributedOutputStream.create(trainingFile, this.processor, this.cmbLabel.getValue(), actualHeaders);
            while (inStream.hasNext()) {
                String[] items = type.getLine(inStream);
                // Only proceed if there is data in the line.  A lot of datasets out there have blanks in them.
                if (items.length > 0) {
                    if (this.chkMakeIDs.isSelected()) {
                        String id = String.format("row-%04d", idCounter++);
                        items = ArrayUtils.add(items, id);
                    }
                    outStream.write(items);
                }
            }
            outStream.close();
            // Make the necessary updates to the parm file.
            this.updateParmFile(outStream.getOutputCount());
            // Denote we've succeeded.
            retVal = true;
        } catch (IOException e) {
            BaseController.messageBox(Alert.AlertType.ERROR, "Error Writing Output", e.getMessage());
        }
        return retVal;
    }

    /**
     * Update the parameter file.  We adjust the testing set size, and possible modify the ID column.
     *
     * @param dataCount		number of records written
     *
     * @throws IOException
     */
    public void updateParmFile(int dataCount) throws IOException {
        // Update the testing set size and save the parameter file.
        ParmDescriptor testSizeParm = this.parms.get("testSize");
        int testSizeCount = dataCount / 10;
        if (testSizeCount < 1) testSizeCount = 1;
        testSizeParm.setValue(Integer.toString(testSizeCount));
        // Now we add the ID column, if any.
        if (this.chkMakeIDs.isSelected()) {
            ParmDescriptor colParm = this.parms.get("meta");
            colParm.setCommented(false);
            String newValue = colParm.getValue();
            if (newValue.isEmpty())
                newValue = "id";
            else {
                String[] parts = StringUtils.split(newValue, ',');
                if (! Arrays.stream(parts).anyMatch(x -> x.contentEquals("id")))
                    newValue = "id," + newValue;
            }
            colParm.setValue(newValue);
            colParm = this.parms.get("id");
            colParm.setCommented(false);
            colParm.setValue("id");
        }
        this.parms.save(this.parmFile);
    }

    /**
     * This enum handles the differences in input handling between the various formats.
     *
     * KERAS 	comma-delimited, no headers
     * PATRIC	tab-delimited, with headers
     */
    private static enum InputFormat {
        KERAS {
            @Override
            public String[] getLine(Iterator<String> inStream) throws IOException {
                String[] retVal = StringUtils.stripAll(StringUtils.split(inStream.next(), ','), "\"");
                return retVal;
            }

            @Override
            public String[] getHeaders(Iterator<String> inStream, File trainingFile) throws IOException {
                String[] retVal = null;
                try (LineReader hdrStream = new LineReader(trainingFile)) {
                    retVal = StringUtils.split(hdrStream.next(), '\t');
                }
                return retVal;
            }

            @Override
            protected ExtensionFilter getFilter() {
                return new ExtensionFilter("Comma-separated file", "*.csv", "*.data");
            }
        },
        PATRIC {
            @Override
            public String[] getLine(Iterator<String> inStream) throws IOException {
                return StringUtils.split(inStream.next(), '\t');
            }

            @Override
            public String[] getHeaders(Iterator<String> inStream, File trainingFile) throws IOException {
                return StringUtils.split(inStream.next(), '\t');
            }

            @Override
            protected ExtensionFilter getFilter() {
                return new ExtensionFilter("Tab-delimited file", "*.tbl", "*.txt", "*.tsv");
            }
        };

        /**
         * @return the fields of the input line as an array
         *
         * @param inStream	iterator from which the next input line should be retrieved
         */
        public abstract String[] getLine(Iterator<String> inStream) throws IOException;

        /**
         * @return the special filename filter for this input type
         */
        protected abstract ExtensionFilter getFilter();

        /**
         * @return the array of headers for this operation
         *
         * @param inStream		iterator from which the first input line should be retrieved
         * @param trainingFile	original training file, used for headers in KERAS mode
         */
        public abstract String[] getHeaders(Iterator<String> inStream, File trainingFile) throws IOException;
    }


}
