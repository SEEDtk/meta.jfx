/**
 *
 */
package org.theseed.meta.jfx;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.theseed.jfx.BaseController;
import org.theseed.jfx.ResizableController;
import org.theseed.meta.controllers.CompoundList;
import org.theseed.meta.controllers.MetaCompound;
import org.theseed.meta.controllers.PathwayTable;
import org.theseed.meta.controllers.ReactionTrigger;
import org.theseed.meta.controllers.ReactionTriggerCell;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Pathway;
import org.theseed.metabolism.Reaction;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

/**
 * This window displays a pathway.  It also allows the user to save the pathway to a file and load other
 * pathways.  The display includes a list of input compounds that is draggable onto the main form.
 *
 * @author Bruce Parrello
 *
 */
public class PathDisplay extends ResizableController {

    // FIELDS
    /** current metabolic model */
    private MetaModel model;
    /** path being displayed */
    private Pathway path;
    /** table control manager */
    protected PathwayTable tableController;
    /** input compound manager */
    protected CompoundList inputController;
    /** saved model directory */
    private File modelDir;

    // CONTROLS

    /** main pathway display table */
    @FXML
    private TableView<Pathway.Element> tblPathway;

    /** list control for triggering proteins */
    @FXML
    private ListView<ReactionTrigger> lstTriggers;

    /** list control for input compounds */
    @FXML
    private ListView<MetaCompound> lstInputCompounds;



    public PathDisplay() {
        super(200, 200, 1000, 600);
    }

    @Override
    public String getIconName() {
        return "fig-gear-16.png";
    }

    @Override
    public String getWindowTitle() {
        return "Metabolic Path Display";
    }

    /**
     * Initialize the display for a particular path.
     *
     * @param path		initial pathway to display
     * @param model		underlying metabolic model
     */
    public void init(Pathway path, ModelManager parent) {
        // Save the path and model.
        this.path = path;
        this.model = parent.getModel();
        this.modelDir = parent.getModelDir();
        // Set up the table control.
        this.tableController = new PathwayTable(this.tblPathway, this.path, this.model);
        // Set up the list of input compounds.
        this.inputController = new CompoundList.Normal(this.lstInputCompounds, parent);
        // Load the input compounds.  Note that this will only be uncommon compounds.
        var inputs = path.getUncommonInputs(this.model);
        var inputItems = this.lstInputCompounds.getItems();
        inputs.sortedCounts().stream().forEach(x -> inputItems.add(parent.getCompound(x.getKey())));
        // Set up the trigger list.
        this.lstTriggers.setCellFactory((x) -> new ReactionTriggerCell());
        Set<ReactionTrigger> triggers = this.getTriggers();
        this.lstTriggers.getItems().addAll(triggers);
    }

    /**
     * @return a sorted set of the reaction triggers for the pathway
     */
    private Set<ReactionTrigger> getTriggers() {
        // We want the triggers sorted, so we put them in a tree set.
        var retVal = new TreeSet<ReactionTrigger>();
        // Loop through the reactions.
        for (Pathway.Element element : this.path) {
            Reaction reaction = element.getReaction();
            // Loop through the feature IDs of the triggers, adding them to the main line.
            reaction.getTriggers().stream().flatMap(x -> model.fidsOf(x).stream())
                    .forEach(x -> retVal.add(new ReactionTrigger.Main(x, reaction, this.model)));
        }
        // Loop through the branches.
        var branchSets = this.path.getBranches(model).values();
        for (Set<Reaction> branchSet : branchSets) {
            for (Reaction reaction : branchSet) {
                reaction.getTriggers().stream().flatMap(x -> model.fidsOf(x).stream())
                        .forEach(x -> retVal.add(new ReactionTrigger.Branch(x, reaction, this.model)));
            }
        }
        // Return the accumulated triggers.
        return retVal;
    }

    /**
     * Save the current pathway to a file.
     *
     * @param event		triggering event
     */
    @FXML
    protected void savePathFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(ModelManager.PATH_FILES, ModelManager.ALL_FILES);
        chooser.setTitle("Save Current Path");
        chooser.setInitialDirectory(this.modelDir);
        // Loop until we save or the user cancels out.
        boolean done = false;
        while (! done) {
            try {
                File saveFile = chooser.showSaveDialog(this.getStage());
                if (saveFile == null) {
                    // Here the user cancelled out.
                    done = true;
                } else {
                    this.path.save(saveFile);
                    BaseController.messageBox(AlertType.INFORMATION, "Save Current Path",
                            "Pathway saved to " + saveFile.toString() + ".");
                    done = true;
                }
            } catch (IOException e) {
                BaseController.messageBox(AlertType.ERROR, "Error Saving Pathway", e.toString());
            }
        }
    }

}
