/**
 *
 */
package org.theseed.meta.jfx;

import java.util.Collection;

import org.theseed.jfx.SemiResizableController;
import org.theseed.meta.controllers.PathwayTable;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Pathway;
import org.theseed.metabolism.Reaction;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

/**
 * This window displays the producers or consumers of a compound in a model.  The
 * reaction formulas are all active, so that the pathways can be traced through the entire model.
 *
 * @author Bruce Parrello
 *
 */
public class CompoundDisplay extends SemiResizableController {

    // FIELDS
    /** underlying model for all the compound data */
    @SuppressWarnings("unused")
    private MetaModel model;
    /** main table controller */
    @SuppressWarnings("unused")
    private PathwayTable pathController;

    // CONTROLS

    /** label for displaying the compound name */
    @FXML
    private Label lblTitle;

    /** table for displaying the reactions */
    @FXML
    private TableView<Pathway.Element> tblReactions;

    /**
     * Construct a compound display window.
     */
    public CompoundDisplay() {
        super(200, 200, 1000, 600);
    }

    @Override
    public String getIconName() {
        return "fig-gear-16.png";
    }

    @Override
    public String getWindowTitle() {
        return "Compound Reaction Display";
    }

    /**
     * Initialize this compound display.
     *
     * @param model		underlying metabolic model
     * @param type		TRUE of producers, FALSE for consumers
     * @param x			desired x-location of window
     * @param y			desired y-location of window
     */
    public void init(MetaModel model, String compoundId, boolean type, double x, double y) {
        // Save the metabolic model.
        this.model = model;
        // Set the window location.
        Stage stage = this.getStage();
        stage.setX(x);
        stage.setY(y);
        // Get the name of the compound.
        String name = model.getCompoundName(compoundId);
        // This will hold the set of reactions to display.
        Collection<Reaction> reactions;
        // This will hold the type of reaction to display.
        String reactionType;
        // Process according to the type.
        if (type) {
            // Here we want producing reactions.
            reactions = model.getProducers(compoundId);
            reactionType = "producing";
        } else {
            // Here we want consuming reactions.
            reactions = model.getSuccessors(compoundId);
            reactionType = "consuming";
        }
        // Specify the window label.
        this.lblTitle.setText("Reactions " + reactionType + " " + name + ".");
        // Specify the compound ID as the main window title.
        this.getStage().setTitle(compoundId);
        // Build the pseudo-path.
        Pathway path = new Pathway(compoundId);
        for (Reaction reaction : reactions)
            path.add(reaction, compoundId, reaction.isProduct(compoundId) != type);
        // Set up the display controller for the main table.
        this.pathController = new PathwayTable(this.tblReactions, path, model);
    }

}
