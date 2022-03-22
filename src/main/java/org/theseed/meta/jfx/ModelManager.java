/**
 *
 */
package org.theseed.meta.jfx;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Genome;
import org.theseed.jfx.BaseController;
import org.theseed.jfx.ResizableController;
import org.theseed.meta.controllers.CompoundList;
import org.theseed.meta.controllers.ICompoundFinder;
import org.theseed.meta.controllers.MetaCompound;
import org.theseed.meta.controllers.ModifierTable;
import org.theseed.meta.controllers.ObservableModifier;
import org.theseed.meta.finders.IEndHandler;
import org.theseed.meta.finders.PathFinder;
import org.theseed.meta.finders.SubsystemBuilder;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Pathway;
import org.theseed.metabolism.mods.ModifierList;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonException;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;

/**
 * This is the main window for the metabolic modeling utility.  It displays the main form, which allows
 * the user to select a model directory for processing, and provides controls for defining the desired
 * path through the cell's metabolism.
 *
 * @author Bruce Parrello
 *
 */
public class ModelManager extends ResizableController implements ICompoundFinder, PathFinder.IParms,
        SubsystemBuilder.IParms {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ModelManager.class);
    /** metabolic model to process */
    private MetaModel model;
    /** current model directory */
    private File modelDir;
    /** current flow file */
    private File flowFile;
    /** current subsystem directory */
    private File subsysDir;
    /** current loaded path */
    private Pathway savedPath;
    /** TRUE if the background task should stop */
    private boolean stopFlag;
    /** handler to run when the background task is complete */
    private IEndHandler endHandler;
    /** visible list of compounds for the current model */
    private ObservableList<MetaCompound> availableCompounds;
    /** map of compound IDs to compound descriptors */
    private Map<String, MetaCompound> metaCompoundMap;
    /** controller for compound search list */
    protected CompoundList searchListController;
    /** controller for modifier list */
    protected ModifierTable flowModifier;
    /** controller for path list */
    protected CompoundList pathListController;
    /** extension filter for flow files */
    public static final FileChooser.ExtensionFilter FLOW_FILES =
            new FileChooser.ExtensionFilter("Flow Command Files", "*.flow");
    public static final FileChooser.ExtensionFilter EXCEL_FILES =
            new FileChooser.ExtensionFilter("Excel Spreadsheet", "*.xlsx");
    /** extension filter for all files */
    public static final FileChooser.ExtensionFilter ALL_FILES =
            new FileChooser.ExtensionFilter("All Files", "*.*");
    /** extension filter for path files */
    public static final FileChooser.ExtensionFilter PATH_FILES =
            new FileChooser.ExtensionFilter("Pathway Files", "*" + Pathway.FILE_EXT);
    /** filename filter for path files */
    public static final FileFilter PATH_FILE_FILTER = new Pathway.FileFilter();

    // CONTROLS

    /** display field for model directory name */
    @FXML
    private TextField txtModelDirectory;

    /** message buffer */
    @FXML
    private TextField txtMessageBuffer;

    /** compound search control */
    @FXML
    private TextField txtSearchCompound;

    /** compound list control */
    @FXML
    private ListView<MetaCompound> lstCompounds;

    /** path list control */
    @FXML
    private ListView<MetaCompound> lstPath;

    /** select box for type of path */
    @FXML
    private ChoiceBox<PathFinder.Type> cmbPathStyle;

    /** clear-path button */
    @FXML
    private Button btnClearPath;

    /** show-commons button */
    @FXML
    private Button btnShowCommons;

    /** flow-file name */
    @FXML
    private TextField txtFlowFile;

    /** path-file name */
    @FXML
    private TextField txtPathFile;

    /** flow-file select button */
    @FXML
    private Button btnSelectFlow;

    /** compute-path button */
    @FXML
    private Button btnComputePath;

    /** select-path button */
    @FXML
    private Button btnSelectPath;

    /** display-path button */
    @FXML
    private Button btnShowPath;

    /** refresh-flow-file button */
    @FXML
    private Button btnFlowSave;

    /** want-loop checkbox */
    @FXML
    private CheckBox chkLooped;

    /** select-subsystem button */
    @FXML
    private Button btnSelectSubsys;

    /** current-subsystem display */
    @FXML
    private TextField txtSubsysDirectory;

    /** update-subsystem button */
    @FXML
    private Button btnUpdateSubsystem;

    /** method for subsystem update */
    @FXML
    private ChoiceBox<SubsystemBuilder.Type> cmbSubsysUpdateType;

    /** current subsystem list */
    @FXML
    private ListView<Pathway> lstSubsystem;

    /** load-subsystem-outputs button */
    @FXML
    private Button btnLoadOutputs;

    /** flow modifier table */
    @FXML
    private TableView<ObservableModifier> tblFlowMods;

    /** flow modifier command selector */
    @FXML
    private ChoiceBox<ModifierList.Command> cmbCommand;

    /** add-modifier button */
    @FXML
    private Button btnNewMod;

    /** progress bar for searches */
    @FXML
    private ProgressBar barProgress;

    /** button to abort a background task */
    @FXML
    private Button btnAbort;

    /**
     * This listener updates the compound list based on the content of the text property in the
     * search box.
     **/
    public class SearchListener implements ChangeListener<String> {

        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            ModelManager.this.filterList(newValue);
        }

    }


    public ModelManager() {
        super(200, 200, 1000, 800);
    }

    @Override
    public String getIconName() {
        return "BioMace-16.png";
    }

    @Override
    public String getWindowTitle() {
        return "BioMacE Metabolic Model Managger";
    }

    /**
     * Initialize the model manager.
     */
    public void init() {
        // Initialize the compound map.  Right now it's empty.
        this.metaCompoundMap = new HashMap<String, MetaCompound>(500);
        // Recall the previous directory selected.
        String dirName = this.getPref("modelDirectory", "");
        File newDir = null;
        if (! dirName.isEmpty())
            newDir = new File(dirName);
        else
            newDir = new File(System.getProperty("user.dir"));
        // Set up a change listener on the text field.
        this.txtSearchCompound.textProperty().addListener(this.new SearchListener());
        // Create controllers for the compound lists.
        this.pathListController = new CompoundList.Droppable(this.lstPath, this);
        this.searchListController = new CompoundList.Normal(this.lstCompounds, this);
        // Create a controller for the flow modifier table.
        this.flowModifier = new ModifierTable(this.tblFlowMods);
        // Set up the path styles.
        this.cmbPathStyle.getItems().addAll(PathFinder.Type.values());
        // Set up the subsystem methods.
        this.cmbSubsysUpdateType.getItems().addAll(SubsystemBuilder.Type.values());
        this.cmbSubsysUpdateType.getSelectionModel().clearAndSelect(0);
        // Set up the flow commands.
        this.cmbCommand.getItems().addAll(ModifierList.Command.values());
        this.cmbCommand.getSelectionModel().clearAndSelect(0);
        // Try to load the model.
        boolean ok = false;
        try {
            String flowName = "";
            ok = this.setupModel(newDir);
            // Save the directory if it's valid.
            if (ok) {
                this.modelDir = newDir;
                // Check for a default flow.
                flowName = this.getPref(newDir.getAbsolutePath() + ".flow", "");
            }
            // Toggle the buttons according to whether we have a valid directory.
            this.setState(ok);
            // Load the flow.  This must happen AFTER the state is set.
            if (! flowName.isEmpty()) {
                // Here we have a default flow.  Make sure it is still valid.
                File flowFile = new File(flowName);
                if (flowFile.exists()) {
                    // It's valid, so we load it.
                    this.loadFlowFile(flowFile);
                }
            }
        } catch (IOException e) {
            // An I/O error here just means a bad directory.  We ignore it.
        }
    }

    /**
     * Set the button states according to whether or not we have a valid model.
     *
     * @param valid		TRUE if the model is valid, else FALSE
     */
    private void setState(final boolean valid) {
        this.txtSearchCompound.setDisable(! valid);
        this.lstPath.setDisable(! valid);
        this.btnClearPath.setDisable(! valid);
        this.btnShowCommons.setDisable(! valid);
        this.btnSelectFlow.setDisable(! valid);
        this.btnFlowSave.setDisable(true);
        this.btnNewMod.setDisable(true);
        this.cmbPathStyle.setDisable(! valid);
        this.btnShowPath.setDisable(! valid);
        this.btnSelectPath.setDisable(! valid);
        this.chkLooped.setDisable(! valid);
        this.btnSelectSubsys.setDisable(! valid);
        this.btnUpdateSubsystem.setDisable(true);
        this.cmbSubsysUpdateType.setDisable(! valid);
        this.btnLoadOutputs.setDisable(true);
        this.btnAbort.setVisible(false);
    }

    /**
     * Setup this application with a new model directory.
     *
     * @param newDir	new model directory to use
     *
     * @return TRUE if successful, FALSE if the directory is not valid
     *
     * @throws IOException
     */
    private boolean setupModel(File newDir) throws IOException {
        boolean retVal = false;
        if (newDir != null) {
            // Check for the required files.
            File modelFile = new File(newDir, "model.json");
            File genomeFile = new File(newDir, "base.gto");
            if (modelFile.canRead() && genomeFile.canRead()) {
                // Here we have the necessary files.  Load the genome.
                Genome baseGenome = new Genome(genomeFile);
                // Now create the model.
                this.model = new MetaModel(modelFile, baseGenome);
                // Load the compounds into the list.
                this.setupCompounds();
                this.availableCompounds = this.lstCompounds.getItems();
                this.txtSearchCompound.setText("");
                this.filterList("");
                this.clearCurrentPath();
                this.cmbPathStyle.getSelectionModel().clearAndSelect(0);
                this.txtFlowFile.setText("");
                this.tblFlowMods.getItems().clear();
                this.txtSubsysDirectory.setText("");
                this.txtPathFile.setText("");
                this.savedPath = null;
                this.flowFile = null;
                this.subsysDir = null;
                this.txtFlowFile.setText("");
                this.lstSubsystem.getItems().clear();
                // Denote we have successfully loaded a model.
                String message = String.format("%d reactions and %d compounds loaded from  %s.",
                        this.model.getReactionCount(), this.model.getMetaboliteCount(),
                        this.model.toString());
                this.showMessage(message);
                this.txtModelDirectory.setText(newDir.getName());
                retVal = true;
            }
        }
        return retVal;
    }

    /**
     * Configure the list view to only show compounds that match the filter.
     *
     * @param string	filter string
     */
    private void filterList(String string) {
        // Convert the filter string to lower case.
        String lc = string.toLowerCase();
        // Clear and rebuild the list.
        this.availableCompounds.clear();
        Collection<MetaCompound> filtered = this.metaCompoundMap.values().stream().filter(x -> x.matches(lc))
                .sorted().collect(Collectors.toList());
        this.availableCompounds.addAll(filtered);
    }

    /**
     * Initialize the compound list using the current model.
     */
    private void setupCompounds() {
        // Clear our version of the map.
        this.metaCompoundMap.clear();
        // Loop through the names, building compounds to add to the main list.
        for (String compound : model.getMetaboliteMap().keySet()) {
            String name = model.getCompoundName(compound);
            this.metaCompoundMap.put(compound, new MetaCompound(compound, name));
        }
        log.info("{} compounds loaded from model {}.", this.metaCompoundMap.size(), this.model);
    }

    /**
     * Erase the path currently being constructed so the user can start over.
     *
     * @param event		event that triggered this action
     */
    @FXML
    protected void clearPath(ActionEvent event) {
        this.clearCurrentPath();
    }

    /**
     * Erase the path currently being constructed.
     */
    private void clearCurrentPath() {
        this.lstPath.getItems().clear();
    }

    @FXML
    protected void showCommonCompounds() {
        // Insure the common compounds from the flow model are active.
        this.applyFlow();
        // Fill the compound list with the common compounds.
        var commons = this.model.getCommons();
        this.availableCompounds.clear();
        Set<MetaCompound> compounds = commons.stream().map(x -> this.getCompound(x)).filter(x -> x != null)
                .collect(Collectors.toCollection(TreeSet<MetaCompound>::new));
        this.availableCompounds.addAll(compounds);
        this.showMessage(String.format("%d common compounds in this model.", compounds.size()));
    }

    /**
     * Allow the user to select the new model directory.
     */
    @FXML
    protected void selectModelDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select a Model Directory");
        // Start at the old model directory, or the current directory if there was none.
        File curDir = this.modelDir;
        if (curDir == null) curDir = new File(System.getProperty("user.dir"));
        chooser.setInitialDirectory(curDir);
        boolean found = false;
        while (! found) {
            curDir = chooser.showDialog(this.getStage());
            if (curDir == null) {
                // Here the user is canceling out.
                found = true;
            } else {
                // Make sure that if we retry, we start from here.
                chooser.setInitialDirectory(curDir);
                // Test the directory.
                try {
                    found = this.setupModel(curDir);
                    if (! found) {
                        // Directory was invalid.  Try again.
                        BaseController.messageBox(Alert.AlertType.WARNING, "Error Changing Model Directory",
                                curDir + " does not have a base.gto and a model.json.");
                    } else {
                        // Here we found the directory.
                        this.setState(true);
                        this.setPref("modelDirectory", curDir.getAbsolutePath());
                    }
                } catch (IOException e) {
                    BaseController.messageBox(AlertType.ERROR, "Error Loading Model", e.toString());
                }
            }
        }
    }

    /**
     * Specify a new flow modification file for the path.
     */
    @FXML
    protected void selectFlowFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Flow Modification File");
        chooser.setInitialDirectory(this.modelDir);
        // The preferred extension for the flow modification file is .flow.
        chooser.getExtensionFilters().addAll(FLOW_FILES, ALL_FILES);
        // We will loop until we find a good file or the user cancels out.
        boolean done = false;
        while (! done) {
            // Ask the user for the file.
            File userFlowFile = chooser.showOpenDialog(this.getStage());
            if (userFlowFile == null)
                done = true;
            else {
                try {
                    this.loadFlowFile(userFlowFile);
                    done = true;
                } catch (IOException e) {
                    BaseController.messageBox(AlertType.ERROR, "Invalid Flow File", e.toString());
                }
            }
        }
    }

    /**
     * Update the flow-modification file on disk.  Presumably, the user has just edited the flow table.
     */
    @FXML
    protected void saveFlowFile() {
        try {
            ModifierList modList = this.flowModifier.getModifiers();
            modList.save(this.flowFile);
            this.showMessage("Flow modifiers saved to " + this.flowFile.getAbsolutePath() + ".");
        } catch (IOException e) {
            BaseController.messageBox(AlertType.ERROR, "Error Reloading Flow Modifications", e.toString());
        }
    }

    /**
     * Load the specified flow-modification file into the model.
     *
     * @param userFlowFile	flow modification file to load
     *
     * @throws IOException
     */
    private void loadFlowFile(File userFlowFile) throws IOException {
        // Load the flow modifiers into the display table.
        ModifierList flowMods = new ModifierList(userFlowFile);
        this.flowModifier.store(flowMods);
        // Reset the model and apply the new modifier list.
        this.txtFlowFile.setText(userFlowFile.getName());
        this.flowFile = userFlowFile;
        this.btnFlowSave.setDisable(false);
        this.btnNewMod.setDisable(false);
        // Save the flow file name in our preferences.
        String prefName = this.modelDir.getAbsolutePath() + ".flow";
        this.setPref(prefName, this.flowFile.getAbsolutePath());
    }

    /**
     * Compute and display a path based on the current parameters.
    */
    @FXML
    protected void computePath() {
        if (this.endHandler != null)
            BaseController.messageBox(AlertType.WARNING, "Background Error", "A background task is already running.");
        else try {
            // Apply the flow modifiers.
            this.applyFlow();
            // Create the path finder.
            PathFinder finder = this.cmbPathStyle.getSelectionModel().getSelectedItem().create(this);
            // Run in the background to get the path.
            this.enableButtons(false);
            this.stopFlag = false;
            var runner = finder.new Runner();
            this.endHandler = this.new PathDisplayer(runner);
            new Thread(runner).start();
        } catch (Exception e) {
            BaseController.messageBox(AlertType.ERROR, "Error Computing Path", e.toString());
        }
    }

    /**
     * Apply the current flow modifiers to the model.
     */
    private void applyFlow() {
        this.showMessage("Applying flow modifiers.");
        ModifierList flowMods = this.flowModifier.getModifiers();
        flowMods.apply(this.model);
        this.model.buildReactionNetwork();
    }

    /**
     * Load a pathway from a user-selected file.
     */
    @FXML
    protected void selectPathFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Pathway File");
        chooser.setInitialDirectory(this.modelDir);
        // The preferred extension for the pathway files is .path.json.
        chooser.getExtensionFilters().addAll(PATH_FILES, ALL_FILES);
        // We will loop until we find a good file or the user cancels out.
        boolean done = false;
        while (! done) {
            // Ask the user for the file.
            File pathFile = chooser.showOpenDialog(this.getStage());
            if (pathFile == null)
                done = true;
            else {
                try {
                    this.savedPath = new Pathway(pathFile, this.model);
                    this.txtPathFile.setText(pathFile.getName());
                    done = true;
                } catch (IOException | ParseFailureException | JsonException e) {
                    BaseController.messageBox(AlertType.ERROR, "Invalid Path File", e.toString());
                }
            }
        }
    }

    /**
     * Load a subsystem from a subsystem directory.  The subsystem in here is actually a set of
     * path files that are stored as a collection of pathways.
     */
    @FXML
    protected void selectSubsysDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Subsystem Directory");
        chooser.setInitialDirectory(this.modelDir);
        // Loop until we find a directory or the user cancels out.
        boolean found = false;
        while (! found) {
            File subDir = chooser.showDialog(this.getStage());
            if (subDir == null) {
                // Here the user is canceling out.
                found = true;
            } else {
                // Make sure that if we retry, we start from here.
                chooser.setInitialDirectory(subDir);
                // Test the directory.
                try {
                    this.loadSubsys(subDir);
                    found = true;
                } catch (Exception e) {
                    BaseController.messageBox(AlertType.ERROR, "Error Loading Subsystem", e.toString());
                }
            }
        }
    }

    /**
     * This event adds a new modifier row to the flow-modifier table.
     */
    @FXML
    protected void addModifier() {
        // Create a new modifier with the currently-selected command.
        ObservableModifier newMod = new ObservableModifier();
        newMod.setActive(false);
        newMod.setCommand(this.cmbCommand.getValue().toString());
        // Add the modifier at the end of the table.
        var items = this.tblFlowMods.getItems();
        items.add(newMod);
        this.tblFlowMods.getSelectionModel().clearAndSelect(items.size() - 1);
        this.tblFlowMods.scrollTo(newMod);
    }

    /**
     * This event processes a click on the subsystem path list.  If it is a double-click, we
     * will display the subsystem path.
     *
     * @param event		mouse click event descriptor
     */
    @FXML
    protected void showSubsysPath(MouseEvent event) {
        if (event.getClickCount() >= 2) {
            // Here we have a double-click.  Get the selected path.
            Pathway path = this.lstSubsystem.getSelectionModel().getSelectedItem();
            if (path != null) try {
                this.displayPath(path);
            } catch (IOException e) {
                BaseController.messageBox(AlertType.ERROR, "Error Loading Path", e.toString());
            }
        }
    }

    /**
     * This event fires when the user wants to abort a background task.
     */
    @FXML
    protected void abortCommand() {
        this.stopFlag = true;
    }

    /**
     * This method updates the current subsystem. If no subsystem directory is specified, the
     * user will will be asked to specify one.  In a subsystem update, we create a path for
     * each compound in the main pathway list, and store it in the subsystem directory.
     */
    @FXML
    protected void updateSubsystem() {
        // Apply the current flow modifiers to the model.
        this.applyFlow();
        // Set up the subsystem builder of the appropriate type.
        try {
            // Create the appropriate subsystem builder.
            SubsystemBuilder.Type type = this.cmbSubsysUpdateType.getSelectionModel().getSelectedItem();
            SubsystemBuilder builder = type.create(this);
            // Run it in the background.
            this.stopFlag = false;
            this.enableButtons(false);
            this.stopFlag = false;
            var runner = builder.new Runner();
            this.endHandler = this.new SubsystemRefresher(runner);
            new Thread(runner).start();
        } catch (Exception e) {
            BaseController.messageBox(AlertType.ERROR, "Error Building Subsystem", e.toString());
        }
    }

    /**
     * Load the output compounds from the subsystem into the main path list.
     */
    @FXML
    protected void loadSubsystemOutputs() {
           // Insure we have a subsystem to load.
        if (this.subsysDir == null)
            BaseController.messageBox(AlertType.ERROR, "Error Loading Subsystem", "No subsystem is selected.");
        else {
            // Loop through the subsystem paths, adding any output compounds not already in the path list.
            int count = 0;
            for (Pathway path : this.lstSubsystem.getItems()) {
                String compoundId = path.getOutput();
                if (! this.pathListController.contains(compoundId)) {
                    MetaCompound compound = this.getCompound(compoundId);
                    if (compound != null) {
                        this.lstPath.getItems().add(compound);
                        count++;
                    }
                }
            }
            this.showMessage(String.format("%d compounds added to path list.", count));
        }
    }

    /**
     * Load a subsystem's paths into memory.
     *
     * @param subDir	directory containing the subsystem's paths
     *
     * @return TRUE if successful, FALSE if no paths were found
     *
     * @throws IOException
     * @throws JsonException
     * @throws ParseFailureException
     */
    private boolean loadSubsys(File subDir) throws IOException, ParseFailureException, JsonException {
        File[] pathFiles = subDir.listFiles(PATH_FILE_FILTER);
        // Set up the path list.
        var pathList = new ArrayList<Pathway>(pathFiles.length);
        if (pathFiles.length > 0) {
            // Here we have paths to load.
            for (File pathFile : pathFiles) {
                Pathway path = new Pathway(pathFile, this.model);
                pathList.add(path);
            }
            // Sort them in order from shortest to longest.
            Collections.sort(pathList);
        }
        // Save the loaded paths.
        String subsysName = subDir.getName();
        this.lstSubsystem.getItems().clear();
        this.lstSubsystem.getItems().addAll(pathList);
        this.txtMessageBuffer.setText(String.format("%d pathways loaded from subsystem %s.", pathList.size(),
                subsysName));
        // Save the subsystem directory.
        this.txtSubsysDirectory.setText(subsysName);
        this.subsysDir = subDir;
        this.btnLoadOutputs.setDisable(false);
        this.btnUpdateSubsystem.setDisable(false);
        return true;
    }

    /**
     * Display the currently-loaded path.
     */
    @FXML
    protected void showPathFile() {
        if (savedPath == null)
            BaseController.messageBox(AlertType.ERROR, "Path Display", "No path loaded.");
        else try {
            this.displayPath(this.savedPath);
        } catch (Exception e) {
            BaseController.messageBox(AlertType.ERROR, "Path Display Error", e.toString());
        }
    }

    /**
     * Display a pathway in the pathway viewer.
     *
     * @param path		pathway to display
     *
     * @throws IOException
     */
    private void displayPath(Pathway path) throws IOException {
        this.applyFlow();
        Stage pathStage = new Stage();
        PathDisplay pathViewer = (PathDisplay) BaseController.loadFXML(App.class, "PathDisplay", pathStage);
        pathViewer.init(path, this);
        pathStage.show();
    }

    @Override
    public MetaCompound getCompound(String id) {
        MetaCompound retVal = this.metaCompoundMap.get(id);
        if (retVal == null)
            log.error("No compound found for {}.", id);
        return retVal;
    }

    /**
     * @return the metabolic model being managed
     */
    @Override
    public MetaModel getModel() {
        return this.model;
    }

    /**
     * @return the current model directory
     */
    public File getModelDir() {
        return this.modelDir;
    }

    @Override
    public List<MetaCompound> getCompounds() {
        return this.lstPath.getItems();
    }

    @Override
    public Collection<Pathway> getSubsysPathways() {
        if (this.subsysDir == null) {
            // There is no subsystem selected.  Ask the user to select one.
            this.selectSubsysDirectory();
        }
        Collection<Pathway> retVal = null;
        if (this.subsysDir != null)
            retVal = this.lstSubsystem.getItems();
        return retVal;
    }

    @Override
    public Pathway getStartPathway() {
        Pathway retVal = this.savedPath;
        if (retVal == null) {
            // There is no saved pathway.  Ask the user to select one.
            this.selectPathFile();
            retVal = this.savedPath;
        }
        return retVal;
    }

    public void showMessage(String message) {
        this.txtMessageBuffer.setText(message);
        log.info("Status: {}", message);
    }

    @Override
    public boolean getLoopFlag() {
        return this.chkLooped.isSelected();
    }

    @Override
    public File getSubsysDirectory() {
        File retVal = this.subsysDir;
        if (retVal == null) {
            // There is no subsystem selected.  Ask the user to select one.
            this.selectSubsysDirectory();
            retVal = this.subsysDir;
        }
        return retVal;
    }

    /**
     * Here we display progress.  We also process the stop-flag.  If the stop-flag is set, the
     * thread will be terminated by our own special InterruptException.
     */
    @Override
    public void showProgress(double p) {
        if (this.stopFlag)
            throw new InterruptException();
        else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    barProgress.setProgress(p);
                }
            });
        }
    }

    @Override
    public void showStatus(String msg) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                txtMessageBuffer.setText(msg);
            }
        });
    }

    @Override
    public void showCompleted() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (endHandler != null)
                    endHandler.handleCompletion();
                enableButtons(true);
                endHandler = null;            }
        });

    }

    /**
     * Disable the buttons prior to processing a background task.
     */
    protected void enableButtons(final boolean state) {
        btnClearPath.setDisable(! state);
        btnComputePath.setDisable(! state);
        btnFlowSave.setDisable(! state);
        btnLoadOutputs.setDisable(! state);
        btnNewMod.setDisable(! state);
        btnSelectFlow.setDisable(! state);
        btnSelectPath.setDisable(! state);
        btnSelectSubsys.setDisable(! state);
        btnShowPath.setDisable(! state);
        btnUpdateSubsystem.setDisable(! state);
        chkLooped.setDisable(! state);
        cmbCommand.setDisable(! state);
        cmbPathStyle.setDisable(! state);
        cmbSubsysUpdateType.setDisable(! state);
        lstCompounds.setDisable(! state);
        lstPath.setDisable(! state);
        lstSubsystem.setDisable(! state);
        btnAbort.setVisible(! state);
    }

    /**
     * This is the end-of-task handler for the completion of a path search.  The pathway is
     * extracted, and if one was found, it is displayed in a window.
     */
    private class PathDisplayer implements IEndHandler {

        /** background task being run */
        private PathFinder.Runner task;

        /**
         * Construct the handler for the path search.
         *
         * @param task		background task performing the search
         */
        protected PathDisplayer(PathFinder.Runner task) {
            this.task = task;
        }

        @Override
        public void handleCompletion() {
            Pathway newPath = this.task.getResult();
            if (newPath != null)
                try {
                    ModelManager.this.displayPath(newPath);
                } catch (IOException e) {
                    BaseController.messageBox(AlertType.ERROR, "Error Displaying Path", e.toString());
                }
        }

    }

    /**
     * This is the end-of-task handler for the completion of a subsystem build.  If the build was
     * successful, the subsystem is redisplayed in the subsystem window.
     */
    private class SubsystemRefresher implements IEndHandler {

        /** background task being run */
        private SubsystemBuilder.Runner task;

        /**
         * Construct the handler for the subsystem build.
         *
         * @param task		background task performing the build
         */
        protected SubsystemRefresher(SubsystemBuilder.Runner task) {
            this.task = task;
        }

        @Override
        public void handleCompletion() {
            boolean okFlag = task.getResult();
            if (okFlag) {
                // The subsystem has updated, so we need to reload it.
                try {
                    ModelManager.this.loadSubsys(ModelManager.this.subsysDir);
                } catch (IOException | ParseFailureException | JsonException e) {
                    BaseController.messageBox(AlertType.ERROR, "Error Displaying Subsystem", e.toString());
                }
            }
        }

    }

}
