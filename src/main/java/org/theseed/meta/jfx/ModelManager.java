/**
 *
 */
package org.theseed.meta.jfx;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

/**
 * This is the main window for the metabolic modeling utility.  It displays the main form, which allows
 * the user to select a model directory for processing, and provides controls for defining the desired
 * path through the cell's metabolism.
 *
 * @author Bruce Parrello
 *
 */
public class ModelManager extends ResizableController implements ICompoundFinder {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ModelManager.class);
    /** metabolic model to process */
    private MetaModel model;
    /** current model directory */
    private File modelDir;
    /** current flow file */
    private File flowFile;
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
    public static final FileChooser.ExtensionFilter PATH_FILES =
            new FileChooser.ExtensionFilter("All Files", "*.path.json");

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

    /** checkbox for a looped path */
    @FXML
    private CheckBox chkLooped;

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

    /** flow-file select button */
    @FXML
    private Button btnSelectFlow;

    /** compute-path button */
    @FXML
    private Button btnComputePath;

    /** load-and-display-path button */
    @FXML
    private Button btnLoadPath;

    /** refresh-flow-file button */
    @FXML
    private Button btnFlowRefresh;


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
                this.chkLooped.setSelected(false);
                this.flowFile = null;
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
     *
     * @param event		event that triggered this action
     */
    @FXML
    protected void selectModelDirectory(ActionEvent event) {
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
     *
     * @param event		triggering event
     */
    @FXML
    protected void selectFlowFile(ActionEvent event) {
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
     *
     * @param event		event that triggered this action
     */
    @FXML
    protected void refreshFlowFile(ActionEvent event) {
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
     *
     * @param event		triggering event
     */
    @FXML
    protected void computePath(ActionEvent event) {
        var items = lstPath.getItems();
        if (items.size() < 2)
            BaseController.messageBox(AlertType.WARNING, "Error Computing Path",
                    "At least two metabolites required in path list.");
        else try {
            // Create the avoid filters.
            var avoidItems = this.lstAvoid.getItems();
            PathwayFilter[] filters;
            if (avoidItems.size() == 0)
                filters = new PathwayFilter[0];
            else {
                String[] avoids = this.lstAvoid.getItems().stream().map(x -> x.getId()).toArray(String[]::new);
                filters = new PathwayFilter[] { new AvoidPathwayFilter(avoids) };
            }
            // Get the first two metabolites and start the path.
            Iterator<MetaCompound> iter = items.iterator();
            String start = iter.next().getId();
            String next = iter.next().getId();
            this.showStatus("Searching for path from " + start + " to " + next + ".");
            Pathway path = model.getPathway(start, next, filters);
            // Loop through the rest of the metabolites.
            while (path != null && iter.hasNext()) {
                next = iter.next().getId();
                this.showStatus("Extending pathway to " + next + ".");
                path = model.extendPathway(path, next, filters);
            }
            // Check for a looped path.
            if (path != null && this.chkLooped.isSelected()) {
                this.showStatus("Looping pathway back to " + start + ".");
                path = model.loopPathway(path, start, filters);
            }
            if (path == null)
                BaseController.messageBox(AlertType.WARNING, "Error Computing Path", "Could not find a pathway to " + next + ".");
            else {
                this.showStatus(String.format("%d reactions found in pathway.", path.size()));
                this.displayPath(path);
            }
        } catch (Exception e) {
            BaseController.messageBox(AlertType.ERROR, "Error Computing Path", e.toString());
        }
    }

    /**
     * Load a pathway from a file and display it.
     *
     * @param event		event that triggered this action
     */
    @FXML
    protected void loadPathFile(ActionEvent event) {
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
                    Pathway path = new Pathway(pathFile, this.model);
                    this.displayPath(path);
                    done = true;
                } catch (IOException | ParseFailureException | JsonException e) {
                    BaseController.messageBox(AlertType.ERROR, "Invalid Flow File", e.toString());
                }
            }
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
    public MetaModel getModel() {
        return this.model;
    }

    /**
     * @return the current model directory
     */
    public File getModelDir() {
        return this.modelDir;
    }
}
