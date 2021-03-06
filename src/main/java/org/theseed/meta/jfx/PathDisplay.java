/**
 *
 */
package org.theseed.meta.jfx;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.theseed.jfx.BaseController;
import org.theseed.jfx.ResizableController;
import org.theseed.meta.controllers.CompoundList;
import org.theseed.meta.controllers.GeneTable;
import org.theseed.meta.controllers.ICompoundFinder;
import org.theseed.meta.controllers.MetaCompound;
import org.theseed.meta.controllers.PathwayTable;
import org.theseed.meta.controllers.ReactionTrigger;
import org.theseed.meta.controllers.ReactionTriggerCell;
import org.theseed.metabolism.CompoundRating;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Pathway;
import org.theseed.metabolism.ProteinRating;
import org.theseed.metabolism.Reaction;

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
    /** output compound manager */
    protected CompoundList outputController;
    /** gene list manager */
    protected GeneTable geneController;
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

    /** list control for output compounds */
    @FXML
    private ListView<MetaCompound> lstOutputCompounds;

    /** table control for gene list */
    @FXML
    private TableView<ProteinRating> tblGenes;


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
        // Set up the list of output compounds.
        this.outputController = new CompoundList.Normal(this.lstOutputCompounds, parent);
        // Load the input compounds.
        var inputs = path.getInputs(this.model, true);
        Set<String> inputSet = inputs.sortedCounts().stream().map(x -> x.getKey()).collect(Collectors.toSet());
        this.fillCompoundList(this.lstInputCompounds, inputSet, parent);
        // Load the output compounds.
        var outputs = path.getOutputs();
        this.fillCompoundList(this.lstOutputCompounds, outputs, parent);
        // Get the weight map and the branch list.
        Map<String, CompoundRating> weightMap = CompoundRating.getRatingMap(this.path, this.model);
        Map<String, Set<Reaction>> branches = this.path.getBranches(model);
        // Set up the trigger list.
        this.lstTriggers.setCellFactory((x) -> new ReactionTriggerCell());
        Set<ReactionTrigger> triggers = this.getTriggers(weightMap, branches);
        this.lstTriggers.getItems().addAll(triggers);
        // Set up the gene table.
        var ratings = path.getProteinRatings(this.model, weightMap, branches);
        this.geneController = new GeneTable(this.tblGenes, this.model, ratings);
    }

    /**
     * Fill a list control with the specified compounds.
     *
     * @param list			list control to fill
     * @param compounds		collection of BiGG IDs for the compounds
     * @param parent		parent compound finder
     */
    private void fillCompoundList(ListView<MetaCompound> list, Collection<String> compounds, ICompoundFinder parent) {
        var items = list.getItems();
        for (String compound : compounds) {
            var meta = parent.getCompound(compound);
            items.add(meta);
        }
    }

    /**
     * Compute the reaction triggers.
     *
     * @param branches		map of compound IDs to branching reactions
     * @param weightMap 	map of compound IDs to compound ratings
     *
     * @return a sorted set of the reaction triggers for the pathway
     */
    private Set<ReactionTrigger> getTriggers(Map<String, CompoundRating> weightMap, Map<String, Set<Reaction>> branches) {
        // Compute the weight map.
        // We want the triggers sorted, so we put them in a tree set.
        var retVal = new TreeSet<ReactionTrigger>();
        // Loop through the reactions.
        for (Pathway.Element element : this.path) {
            Reaction reaction = element.getReaction();
            // Compute the reaction weight.
            double weight = reaction.getWeight(weightMap, ! element.isReversed());
            // Loop through the feature IDs of the triggers, adding them to the main line.
            reaction.getTriggers().stream().flatMap(x -> model.fidsOf(x).stream())
                    .forEach(x -> retVal.add(new ReactionTrigger.Main(x, reaction, this.model, weight)));
        }
        // Loop through the branches.
        for (Map.Entry<String, Set<Reaction>> branchEntry : branches.entrySet()) {
            String consumed = branchEntry.getKey();
            for (Reaction reaction : branchEntry.getValue()) {
                double weight = reaction.getWeight(weightMap, reaction.isProduct(consumed));
                reaction.getTriggers().stream().flatMap(x -> model.fidsOf(x).stream())
                        .forEach(x -> retVal.add(new ReactionTrigger.Branch(x, reaction, this.model, consumed, weight)));
            }
        }
        // Return the accumulated triggers.
        return retVal;
    }

    /**
     * Save the current pathway to a file.
     */
    @FXML
    protected void savePathFile() {
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

    /**
     * Save the current pathway to an Excel spreadsheet.
     */
    @FXML
    protected void savePathExcel() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(ModelManager.EXCEL_FILES, ModelManager.ALL_FILES);
        chooser.setTitle("Save Path to Excel");
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
                    this.path.saveToExcel(saveFile, model);
                    BaseController.messageBox(AlertType.INFORMATION, "Save Path to Excel",
                            "Pathway saved to " + saveFile.toString() + ".");
                    done = true;
                }
            } catch (Exception e) {
                BaseController.messageBox(AlertType.ERROR, "Error Saving Pathway", e.toString());
            }
        }
    }

}
