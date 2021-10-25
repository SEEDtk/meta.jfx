/**
 * 
 */
package org.theseed.dl4j.jfx;

import java.io.File;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.dl4j.MeanBiasProcessor;
import org.theseed.jfx.BaseController;
import org.theseed.jfx.MovableController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * This controller gets the name of a training file and an output file and produces a mean-bias
 * analysis in the output file.
 * 
 * @author Bruce Parrello
 *
 */
public class MeanBiasDialog extends MovableController {

	// FIELDS
	/** logging facility */
	protected static Logger log = LoggerFactory.getLogger(MeanBiasDialog.class);
	/** model directory */
	private File modelDir;
	/** input training file */
	private File trainFile;
	/** output file */
	private File biasFile;
	/** extension filter */
	private static ExtensionFilter TEXT_FILES = new ExtensionFilter("Tab-delimited file", "*.tbl", "*.txt", "*.tsv");
	
	// CONTROLS

    /** text field for displaying input file name */
    @FXML
    private TextField txtInputFile;

    /** text field for displaying output file name */
    @FXML
    private TextField txtOutputFile;

    /** button to start the process */
    @FXML
    private Button btnRun;
	
	/**
	 * Construct a dialog for running a mean-bias analysis.
	 */
	public MeanBiasDialog() {
		super(200, 200);
	}

	@Override
	public String getIconName() {
        return "job-16.png";
	}

	@Override
	public String getWindowTitle() {
		return "Compute Column Bias";
	}

	/**
	 * Initialize this dialog for the specified model directory.
	 * 
	 * @param modelDirectory	input model directory
	 */
	public void init(File modelDirectory) {
		this.modelDir = modelDirectory;
		this.trainFile = new File(this.modelDir, "training.tbl");
		this.biasFile = new File(this.modelDir, "bias.tbl");
		// Display the file names and configure the run button.
		this.configureFiles();
	}

	/**
	 * When one of the files has changed, this updates all the controls.
	 */
	private void configureFiles() {
		// Only enable the run button if we can read the training file.
		this.btnRun.setDisable(! this.trainFile.canRead());
		// Store the file names.
		txtInputFile.setText(this.trainFile.getName());
		txtOutputFile.setText(this.biasFile.getName());
	}
	
	@FXML
	private void selectInput(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Training File");
        chooser.setInitialDirectory(this.modelDir);
        chooser.getExtensionFilters().addAll(TEXT_FILES,
                new ExtensionFilter("All Files", "*.*"));
        // Get the file.
        File retVal = chooser.showOpenDialog(this.getStage());
		if (retVal != null) {
			this.trainFile = retVal;
			this.configureFiles();
		}
	}

	@FXML
	private void selectOutput(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Bias Output File");
        chooser.setInitialDirectory(this.modelDir);
        chooser.getExtensionFilters().addAll(TEXT_FILES,
                new ExtensionFilter("All Files", "*.*"));
        // Get the file.
        File retVal = chooser.showSaveDialog(this.getStage());
		if (retVal != null) {
			this.biasFile = retVal;
			this.configureFiles();
		}
	}
	
	@FXML
	private void runBias(ActionEvent event) {
		try {
			// Create the bias processor.
			MeanBiasProcessor processor = new MeanBiasProcessor();
			// Perform the analysis.
			processor.analyzeLabelBias(this.modelDir, this.trainFile);
			// Write the results to the output file.
			try (PrintWriter writer = new PrintWriter(this.biasFile)) {
				processor.writeReport(writer);
			}
			// Here we were successful.
			BaseController.messageBox(Alert.AlertType.INFORMATION, "Bias Computed", "Bias data written to "
					+ this.biasFile.getAbsolutePath() + ".");
			// Dismiss the dialog.
			this.close();
		} catch (Exception e) {
			BaseController.messageBox(Alert.AlertType.ERROR, "Error Computing Bias", e.toString());
		}
	}

}
