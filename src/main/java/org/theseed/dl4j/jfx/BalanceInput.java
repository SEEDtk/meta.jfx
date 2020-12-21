/**
 *
 */
package org.theseed.dl4j.jfx;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.theseed.dl4j.train.TrainingProcessor;
import org.theseed.io.BalancedOutputStream;
import org.theseed.io.LineReader;
import org.theseed.io.ParmDescriptor;
import org.theseed.io.ParmFile;
import org.theseed.io.Shuffler;
import org.theseed.jfx.BaseController;
import org.theseed.jfx.MovableController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.StageStyle;

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
    /** current input row index */
    private int rowIdx;
    /** current model type */
    private TrainingProcessor.Type modelType;

    // CONTROLS

    /** choice box for input format */
    @FXML
    private ChoiceBox<InputFormat> cmbFormat;

    /** text field for displaying input file name */
    @FXML
    private TextField txtInputFile;

    /** input for fuzz factor */
    @FXML
    private Slider numFuzzFactor;

    /** button to start the process */
    @FXML
    private Button btnRun;

    /** checked if we need to add an ID field */
    @FXML
    private CheckBox chkMakeIDs;

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
    public void init(File modelDir, File parmFile, TrainingProcessor.Type type) throws IOException {
        // Make this window fixed-size and modal.
        this.getStage().initStyle(StageStyle.UTILITY);
        this.getStage().setResizable(false);
        // Save the parameters and the model directory.
        this.modelDirectory = modelDir;
        this.parmFile = parmFile;
        this.parms = new ParmFile(parmFile);
        this.modelType = type;
        // Initialize the combo box and the slider.
        this.cmbFormat.getItems().addAll(InputFormat.values());
        this.cmbFormat.getSelectionModel().select(InputFormat.PATRIC);
        this.numFuzzFactor.setValue(1.0);
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
            // Denote we have not written any output.
            this.rowIdx = 0;
            // Process the input to produce the output.
            boolean ok;
            if (this.modelType == TrainingProcessor.Type.CLASS)
                ok = balanceClassInput(trainingFile, type, inStream, headers);
            else
                ok = scrambleRegressionInput(trainingFile, type, inStream, headers);
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
     * This is the balancer for a regression file.  The output is written in scrambled order.
     *
     *
     * @param trainingFile	name of the output training file
     * @param type			type of input
     * @param inStream		input stream
     * @param headers		headers from the training file
     *
     * @return TRUE if successful, else FALSE
     */
    private boolean scrambleRegressionInput(File trainingFile, InputFormat type, Iterator<String> inStream,
            String[] headers) {
        boolean retVal = false;
        try (PrintWriter outStream = new PrintWriter(trainingFile)) {
            // Note we use -1 to denote there is no label column to remove.
            outStream.write(this.dataPart(headers, -1));
            // Read the input file.
            Shuffler<String[]> buffer = new Shuffler<String[]>(5000);
            while (inStream.hasNext()) {
                String[] items = type.getLine(inStream);
                buffer.add(items);
            }
            // Scramble it.
            buffer.shuffle(buffer.size());
            // Write the scrambled lines.
            int outCount = 0;
            for (String[] items : buffer) {
                String line = this.dataPart(items, -1);
                outStream.write(line);
                outCount++;
            }
            // Make the necessary updates to the parm file.
            this.updateParmFile(outCount);
            // Denote we've succeeded.
            retVal = true;
        } catch (IOException e) {
            BaseController.messageBox(Alert.AlertType.ERROR, "Error Writing Output", e.getMessage());
        }
        return retVal;
    }

    /**
     * This is the balancer for a classification file.  The output
     * is written in such a way as to provide an even distribution
     * of classes in each part of the output file.
     *
     * @param trainingFile	name of the output training file
     * @param type			type of input
     * @param inStream		input stream
     * @param headers		headers from the training file
     *
     * @return TRUE if successful, else FALSE
     */
    public boolean balanceClassInput(File trainingFile, InputFormat type, Iterator<String> inStream,
            String[] headers) {
        boolean retVal = false;
        // Find the label and build the new header line.
        String labelCol = this.parms.get("col").getValue();
        int labelIdx = -1;
        if (headers[0].contentEquals(labelCol))
            labelIdx = 0;
        else {
            OptionalInt found = IntStream.range(1, headers.length).filter(i -> labelCol.contentEquals(headers[i])).findFirst();
            if (! found.isPresent())
                BaseController.messageBox(Alert.AlertType.ERROR, "Error in Training File",
                        "Label column \"" + labelCol + "\" is not found.");
            else
                labelIdx = found.getAsInt();
        }
        if (labelIdx >= 0) {
            // Open the training file for output.
            try (BalancedOutputStream outStream = new BalancedOutputStream(this.numFuzzFactor.getValue(),
                    trainingFile)) {
                // Write the new header.
                outStream.writeImmediate(headers[labelIdx],
                        this.dataPart(headers, labelIdx));
                // Process the data lines.
                while (inStream.hasNext()) {
                    String[] items = type.getLine(inStream);
                    // Note we skip blank lines, which are a hazard in public datasets.
                    if (items.length > 0) {
                        String label = items[labelIdx];
                        String data = this.dataPart(items, labelIdx);
                        outStream.write(label, data);
                    }
                }
                // Make the necessary updates to the parm file.
                this.updateParmFile(outStream.getOutputCount());
                // Denote we've succeeded.
                retVal = true;
            } catch (IOException e) {
                BaseController.messageBox(Alert.AlertType.ERROR, "Error Writing Output", e.getMessage());
            }
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
            else
                newValue = "id," + newValue;
            colParm.setValue(newValue);
            colParm = this.parms.get("id");
            colParm.setCommented(false);
            colParm.setValue("id");
        }
        this.parms.save(this.parmFile);
    }

    /**
     * @return the data part of the specified line
     *
     * @param items			array of input columns
     * @param labelIdx		array index of the label column
     */
    private String dataPart(String[] items, int labelIdx) {
        // Check to see if we are generating IDs.
        String retVal = "";
        if (this.chkMakeIDs.isSelected()) {
            // Here we need to add the ID column.
            if (this.rowIdx == 0)
                retVal = "id\t";
            else
                retVal = String.format("row-%04d\t", rowIdx);
            this.rowIdx++;
        }
        // Add the rest of the data.
        retVal += IntStream.range(0, items.length).filter(i -> i != labelIdx)
                .mapToObj(i -> items[i]).collect(Collectors.joining("\t"));
        return retVal;
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
