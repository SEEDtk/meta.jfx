/**
 *
 */
package org.theseed.meta.jfx;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Genome;
import org.theseed.jfx.BaseController;
import org.theseed.jfx.ResizableController;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.mods.ModifierList;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
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
    /** visible list of compounds for the current model */
    private ObservableList<MetaCompound> availableCompounds;
    /** map of compound IDs to compound descriptors */
    private Map<String, MetaCompound> metaCompoundMap;
    /** current flow modifier */
    private ModifierList flowModifier;
    /** extension filter for flow files */
    private static final FileChooser.ExtensionFilter FLOW_FILES =
            new FileChooser.ExtensionFilter("Flow Command Files", "*.flow");
    /** extension filter for all files */
    private static final FileChooser.ExtensionFilter ALL_FILES =
            new FileChooser.ExtensionFilter("All Files", "*.*");

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

    /**
     * This class defines the event handler for the compound search list.  This list can be
     * a drag source (COPY), and the double-click event adds the selected compound to the
     * current path.
     */
    public class SearchListListener implements CompoundDisplayCell.IHandler {

        @Override
        public void onDoubleClick(MouseEvent event, MetaCompound target) {
            // Double-clicking automatically adds the compound to the path.
            var pathItems = ModelManager.this.lstPath.getItems();
            if (! pathItems.contains(target))
                pathItems.add(target);
        }

        @Override
        public TransferMode[] onDragStart(MouseEvent event, MetaCompound target) {
            // We allow dragging of compounds in COPY mode.
            return CompoundDisplayCell.DRAG_COPY;
        }

        @Override
        public boolean onDragOver(DragEvent event, MetaCompound source, MetaCompound target) {
            return false;
        }

        @Override
        public void onDragDrop(DragEvent event, MetaCompound source, MetaCompound target) { }

        @Override
        public void onDragDone(DragEvent event, MetaCompound source) { }

        @Override
        public void onDelete(KeyEvent event, MetaCompound target) { }

    }

    /**
     * This class defines the event handlers for the two compound lists that form the query-- Path and Avoid.
     * These lists can be a drag source (MOVE) or a target.  Double-click has no effect, but DELETE will
     * delete a node.
     */
    public class CompoundListListener implements CompoundDisplayCell.IHandler {

        /** list control being managed */
        private ListView<MetaCompound> listControl;
        /** index at which last drop took place on this control */
        private int dropIndex;

        /**
         * Construct a listener for a specified list.
         *
         * @param list		list control being managed.
         */
        public CompoundListListener(ListView<MetaCompound> list) {
            this.listControl = list;
            this.dropIndex = -1;
        }

        @Override
        public void onDoubleClick(MouseEvent event, MetaCompound target) { }

        @Override
        public TransferMode[] onDragStart(MouseEvent event, MetaCompound target) {
            // Clear the drop index.  It will remain cleared until something is dropped here.
            this.dropIndex = -1;
            // Denote we're moving.
            return CompoundDisplayCell.DRAG_MOVE;
        }

        @Override
        public boolean onDragOver(DragEvent event, MetaCompound source, MetaCompound target) {
            return true;
        }

        @Override
        public void onDragDrop(DragEvent event, MetaCompound source, MetaCompound target) {
            // Here we are dropping.  If we are dropping on an empty cell, we add at the
            // end; otherwise, we add before the target.
            var items = this.listControl.getItems();
            if (target == null) {
                // Add to the end of the list.
                items.add(source);
                this.dropIndex = items.size() - 1;
            } else {
                // Add before the target.
                int idx = items.indexOf(target);
                items.add(idx, source);
                this.dropIndex = idx;
            }
            // Insure we don't have two copies of the compound in this list.  If we do, we delete
            // the ones that weren't just dropped.
            for (int i = 0; i < items.size(); i++) {
                MetaCompound curr = items.get(i);
                if (i != this.dropIndex && curr.equals(source)) {
                    items.remove(i);
                    if (i < this.dropIndex) this.dropIndex--;
                }
            }
        }

        @Override
        public void onDragDone(DragEvent event, MetaCompound source) {
            // Here we must delete the compound from this list, because it has been moved.
            // We only do this if the drop index is -1, indicating that the drag started
            // here but did not end here.
            if (this.dropIndex == -1) {
                var items = this.listControl.getItems();
                items.remove(source);
            }
        }

        @Override
        public void onDelete(KeyEvent event, MetaCompound target) {
            // Here the user wants us to delete the current item.
            var items = this.listControl.getItems();
            items.remove(target);
        }

    }

    /**
     * This class handles a drag-over event for the list control.  It is only called if we are in
     * an empty section of the list; otherwise, the event is consumed by the cell handler.
     */
    private class ListDragOverListener implements EventHandler<DragEvent> {

        /** event handler for target list control */
        private CompoundDisplayCell.IHandler listHandler;

        public ListDragOverListener(CompoundDisplayCell.IHandler handler) {
            this.listHandler = handler;
        }

        @Override
        public void handle(DragEvent event) {
            MetaCompound source = CompoundDisplayCell.getDragSource(event, ModelManager.this);
            if (source != null) {
                boolean ok = this.listHandler.onDragOver(event, source, null);
                if (ok)
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
        }

    }

    /**
     * This class handles a drag-dropped event for the list control.  It is only called if we are in
     * an empty section of the list; otherwise, the event is consumed by the cell handler.
     */
    private class ListDragDropListener implements EventHandler<DragEvent> {

        /** event handler for target list control */
        private CompoundDisplayCell.IHandler listHandler;

        public ListDragDropListener(CompoundDisplayCell.IHandler handler) {
            this.listHandler = handler;
        }

        @Override
        public void handle(DragEvent event) {
            MetaCompound source = CompoundDisplayCell.getDragSource(event, ModelManager.this);
            if (source != null)
                this.listHandler.onDragDrop(event, source, null);
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
        this.setupListControl(this.lstCompounds, this.new SearchListListener());
        this.setupListControl(this.lstPath, this.new CompoundListListener(this.lstPath));
        this.setupListControl(this.lstAvoid, this.new CompoundListListener(this.lstAvoid));
        // Try to load the model.
        boolean ok = false;
        try {
            ok = this.setupModel(newDir);
        } catch (IOException e) {
            // An I/O error here just means a bad directory.  We ignore it.
        }
        // Toggle the buttons according to whether we have a valid directory.
        this.setState(ok);
    }

    /**
     * Set up the cell factory for a list control.  The cell factory creates list cells that use
     * a particular handler for the major events and are of the type CompoundDisplayCell.
     *
     * @param myList		list control to set up
     * @param myHandler		event handler for the list control
     */
    private void setupListControl(ListView<MetaCompound> myList, CompoundDisplayCell.IHandler myHandler) {
        // Set up the cell factory.
        myList.setCellFactory(new Callback<ListView<MetaCompound>,ListCell<MetaCompound>>() {

            @Override
            public ListCell<MetaCompound> call(ListView<MetaCompound> list) {
                return new CompoundDisplayCell(myHandler, ModelManager.this);
            }

        });
        // Set up the fallback listeners.
        myList.setOnDragOver(this.new ListDragOverListener(myHandler));
        myList.setOnDragDropped(this.new ListDragDropListener(myHandler));
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
                // Denote we have successfully loaded a model.
                String message = String.format("%d reactions loaded from model for %s.",
                        this.model.getReactionCount(), baseGenome.toString());
                this.txtMessageBuffer.setText(message);
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
        this.txtMessageBuffer.setText(String.format("%d common compounds in this model.", compounds.size()));
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

    @Override
    public MetaCompound getCompound(String id) {
        return this.metaCompoundMap.get(id);
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
            File flowFile = chooser.showOpenDialog(this.getStage());
            if (flowFile == null)
                done = true;
            else {
                try {
                    this.flowModifier = new ModifierList(flowFile);
                    done = true;
                    this.txtMessageBuffer.setText(String.format("%d flow modifiers loaded.", this.flowModifier.size()));
                } catch (IOException e) {
                    BaseController.messageBox(AlertType.ERROR, "Invalid Flow File", e.toString());
                }
            }
        }
    }

    /**
     * Compute and display a path based on the current parameters.
     *
     * @param event		triggering event
     */
    @FXML
    protected void computePath(ActionEvent event) {
    	// Compute the pathway.
        // TODO compute the path
    	// TODO display the path in the PathDisplay
    }

}
