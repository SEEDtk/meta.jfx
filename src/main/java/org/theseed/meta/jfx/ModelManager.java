/**
 *
 */
package org.theseed.meta.jfx;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.theseed.metabolism.AvoidPathwayFilter;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Pathway;
import org.theseed.metabolism.PathwayFilter;
import org.theseed.metabolism.mods.ModifierList;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
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
    /** current loaded subsystem */
    private Collection<Pathway> subsysPaths;
    /** array of path filters */
    private PathwayFilter[] filters;
    /** visible list of compounds for the current model */
    private ObservableList<MetaCompound> availableCompounds;
    /** map of compound IDs to compound descriptors */
    private Map<String, MetaCompound> metaCompoundMap;
    /** current flow modifier */
    private ModifierList flowModifier;
    /** controller for compound search list */
    protected CompoundList searchListController;
    /** controller for path list */
    protected CompoundList pathListController;
    /** controller for avoid list */
    protected CompoundList avoidListController;
    /** extension filter for flow files */
    public static final FileChooser.ExtensionFilter FLOW_FILES =
            new FileChooser.ExtensionFilter("Flow Command Files", "*.flow");
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

    /** avoid list control */
    @FXML
    private ListView<MetaCompound> lstAvoid;

    /** select box for type of path */
    @FXML
    private ChoiceBox<PathFinder.Type> cmbPathStyle;

    /** clear-path button */
    @FXML
    private Button btnClearPath;

    /** clear-avoid button */
    @FXML
    private Button btnClearAvoid;

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
    private Button btnFlowRefresh;

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

    /** flag indicating how to start new subsystem paths */
    @FXML
    private CheckBox chkPathSubsys;


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
        return "multi-gear-16.png";
    }

    @Override
    public String getWindowTitle() {
        return "Metabolic Model Managger";
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
        // Create cell factories for the three compound lists.
        this.avoidListController = new CompoundList.Droppable(this.lstAvoid, this);
        this.pathListController = new CompoundList.Droppable(this.lstPath, this);
        this.searchListController = new CompoundList.Normal(this.lstCompounds, this);
        // Try to load the model.
        boolean ok = false;
        try {
            ok = this.setupModel(newDir);
            // Save the directory if it's valid.
            if (ok)
                this.modelDir = newDir;
        } catch (IOException e) {
            // An I/O error here just means a bad directory.  We ignore it.
        }
        // Toggle the buttons according to whether we have a valid directory.
        this.setState(ok);
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
        this.lstAvoid.setDisable(! valid);
        this.btnClearAvoid.setDisable(! valid);
        this.btnShowCommons.setDisable(! valid);
        this.btnSelectFlow.setDisable(! valid);
        this.btnFlowRefresh.setDisable(true);
        this.cmbPathStyle.setDisable(! valid);
        this.btnShowPath.setDisable(! valid);
        this.btnSelectPath.setDisable(! valid);
        this.chkLooped.setDisable(! valid);
        this.btnSelectSubsys.setDisable(! valid);
        this.btnUpdateSubsystem.setDisable(! valid);
        this.chkPathSubsys.setDisable(! valid);
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
                this.clearCurrentAvoid();
                this.cmbPathStyle.getSelectionModel().clearAndSelect(0);
                this.txtFlowFile.setText("");
                this.txtSubsysDirectory.setText("");
                this.txtPathFile.setText("");
                this.savedPath = null;
                this.flowFile = null;
                this.subsysPaths = null;
                this.subsysDir = null;
                this.txtFlowFile.setText("");
                // Denote we have successfully loaded a model.
                String message = String.format("%d reactions loaded from model for %s.",
                        this.model.getReactionCount(), baseGenome.toString());
                showStatus(message);
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
        var compoundMap = this.model.getCompoundMap();
        // Clear our version of the map.
        this.metaCompoundMap.clear();
        // Loop through the names, building compounds to add to the main list.
        for (Map.Entry<String, Set<String>> compoundEntry : compoundMap.entrySet()) {
            // Note that the key is the name and the value is a set of IDs.
            String name = compoundEntry.getKey();
            compoundEntry.getValue().stream().forEach(x -> this.metaCompoundMap.put(x, new MetaCompound(x, name)));
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
     * Erase the avoid list.
     *
     * @param event		event that triggered this action
     */
    @FXML
    protected void clearAvoid(ActionEvent event) {
        this.clearCurrentAvoid();
    }

    /**
     * Erase the path currently being constructed.
     */
    private void clearCurrentPath() {
        this.lstPath.getItems().clear();
    }

    /**
     * Erase the current avoid list.
     */
    private void clearCurrentAvoid() {
        this.lstAvoid.getItems().clear();
    }

    @FXML
    protected void showCommonCompounds() {
        // Fill the compound list with the common compounds.
        var commons = this.model.getCommons();
        this.availableCompounds.clear();
        Set<MetaCompound> compounds = commons.stream().map(x -> this.getCompound(x)).filter(x -> x != null)
                .collect(Collectors.toCollection(TreeSet<MetaCompound>::new));
        this.availableCompounds.addAll(compounds);
        this.showStatus(String.format("%d common compounds in this model.", compounds.size()));
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
     * Refresh the flow-modification file from disk.  Presumably, the user has just updated it.
     */
    @FXML
    protected void refreshFlowFile() {
        try {
            this.loadFlowFile(this.flowFile);
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
        this.flowModifier = new ModifierList(userFlowFile);
        // Reset the model and apply the new modifier list.
        this.model.resetFlow();
        int modCount = this.flowModifier.apply(this.model);
        this.model.buildReactionNetwork();
        this.showStatus(String.format("%d reactions modified by flow modifiers.",
                modCount, this.flowModifier.size()));
        this.txtFlowFile.setText(userFlowFile.getName());
        this.flowFile = userFlowFile;
        this.btnFlowRefresh.setDisable(false);
    }

    /**
     * Compute and display a path based on the current parameters.
    */
    @FXML
    protected void computePath() {
        try {
            // Create the avoid filters.
            var avoidItems = this.lstAvoid.getItems();
            if (avoidItems.size() == 0)
                this.filters = new PathwayFilter[0];
            else {
                String[] avoids = this.lstAvoid.getItems().stream().map(x -> x.getId()).toArray(String[]::new);
                this.filters = new PathwayFilter[] { new AvoidPathwayFilter(avoids) };
            }
            // Create the path finder.
            PathFinder finder = this.cmbPathStyle.getSelectionModel().getSelectedItem().create(this);
            // Get the path.
            Pathway path = finder.computePath();
            if (path == null)
                BaseController.messageBox(AlertType.WARNING, "Error Computing Path", "Could not the desired pathway.");
            else {
                this.showStatus(String.format("%d reactions found in pathway.", path.size()));
                this.displayPath(path);
            }
        } catch (Exception e) {
            BaseController.messageBox(AlertType.ERROR, "Error Computing Path", e.toString());
        }
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
                    found = this.loadSubsys(subDir);
                    if (! found) {
                        // Directory was invalid.  Try again.
                        BaseController.messageBox(Alert.AlertType.WARNING, "Error Loading Subsystem",
                                subDir + " does not have any path.json files.");
                    }
                } catch (Exception e) {
                    BaseController.messageBox(AlertType.ERROR, "Error Loading Subsystem", e.toString());
                }
            }

        }
    }

    /**
     * This method updates the current subsystem. If no subsystem directory is specified, the
     * user will will be asked to specify one.  In a subsystem update, we create a path for
     * each compound in the main pathway list, and store it in the subsystem directory.
     */
    @FXML
    protected void updateSubsystem() {
        // Set up the subsystem builder of the appropriate type.
        try {
            SubsystemBuilder builder;
            if (this.chkPathSubsys.isSelected())
                builder = new PathSubsystemBuilder(this);
            else
                builder = new SimpleSubsystemBuilder(this);
            boolean ok = builder.updateSubsystem();
            if (ok) {
                // The subsystem has updated, so we need to reload it.
                this.loadSubsys(this.subsysDir);
            }
        } catch (IOException | ParseFailureException | JsonException e) {
            BaseController.messageBox(AlertType.ERROR, "Error Building Subsystem", e.toString());
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
        boolean retVal = false;
        if (pathFiles.length > 0) {
            // Here we have paths to load.
            var pathList = new ArrayList<Pathway>(pathFiles.length);
            for (File pathFile : pathFiles) {
                Pathway path = new Pathway(pathFile, this.model);
                pathList.add(path);
            }
            // Save the loaded paths.
            String subsysName = subDir.getName();
            this.subsysPaths = pathList;
            this.txtMessageBuffer.setText(String.format("%d pathways loaded from subsystem %s.", pathList.size(),
                    subsysName));
            // Save the subsystem directory.
            this.txtSubsysDirectory.setText(subsysName);
            this.subsysDir = subDir;
        }
        return retVal;
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
        Stage pathStage = new Stage();
        PathDisplay pathViewer = (PathDisplay) BaseController.loadFXML(App.class, "PathDisplay", pathStage);
        pathViewer.init(path, this);
        pathStage.show();
    }

    /**
     * Display a message in the status bar.
     *
     * @param message		message to display
     */
    private void showStatus(String message) {
        this.txtMessageBuffer.setText(message);
    }

    @Override
    public MetaCompound getCompound(String id) {
        return this.metaCompoundMap.get(id);
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
    public PathwayFilter[] getFilters() {
        return this.filters;
    }

    @Override
    public Collection<Pathway> getSubsysPathways() {
        Collection<Pathway> retVal = this.subsysPaths;
        if (retVal == null) {
            // There is no subsystem selected.  Ask the user to select one.
            this.selectSubsysDirectory();
            retVal = this.subsysPaths;
        }
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

    @Override
    public void showMessage(String message) {
        this.txtMessageBuffer.setText(message);

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

}
