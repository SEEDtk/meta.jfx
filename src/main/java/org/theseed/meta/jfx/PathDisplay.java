/**
 *
 */
package org.theseed.meta.jfx;

import org.theseed.jfx.ResizableController;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Pathway;
import org.theseed.metabolism.Reaction;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
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

    // CONTROLS

    /** main pathway display table */
    @FXML
    private TableView<Reaction> tblPathway;

    /** pathway display table column for reaction ID */
    @FXML
    private TableColumn<Reaction, String> colReaction;

    /** pathway display table column for reaction name */
    @FXML
    private TableColumn<Reaction, String> colReactionName;

    /** pathway display table column for reaction rule */
    @FXML
    private TableColumn<Reaction, String> colRule;

    /** pathway display table column for reaction formula */
    @FXML
    private TableColumn<Reaction, String> colFormula;

    /** list control for triggering proteins */
    @FXML
    private ListView<ReactionTrigger> lstTriggers; // TODO not String, but some sort of trigger object

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
        // TODO code for init
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
