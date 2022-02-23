/**
 *
 */
package org.theseed.meta.jfx;

import org.theseed.jfx.ResizableController;
import org.theseed.meta.controllers.CompoundList;
import org.theseed.meta.controllers.MetaCompound;
import org.theseed.meta.controllers.PathwayTable;
import org.theseed.meta.controllers.ReactionTrigger;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Pathway;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;

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
        // Set up the table control.
        this.tableController = new PathwayTable(this.tblPathway, this.path, this.model);
        // Set up the list of input compounds.
        this.inputController = new CompoundList.Normal(this.lstInputCompounds, parent);
        // Load the input compounds.  Note that this will only be uncommon compounds.
        var inputs = path.getUncommonInputs(this.model);
        var inputItems = this.lstInputCompounds.getItems();
        inputs.sortedCounts().stream().forEach(x -> inputItems.add(parent.getCompound(x.getKey())));
        // TODO compute and load the triggers
    }

    /**
     * Save the current pathway to a file.
     *
     * @param event		triggering event
     */
    @FXML
    protected void savePathFile(ActionEvent event) {
        // TODO save path to file
    }

}
