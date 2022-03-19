/**
 *
 */
package org.theseed.meta.controllers;

import java.io.IOException;
import java.util.List;

import org.theseed.jfx.BaseController;
import org.theseed.meta.jfx.App;
import org.theseed.meta.jfx.CompoundDisplay;
import org.theseed.metabolism.IReactionSource;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Reaction;

import javafx.event.EventHandler;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

/**
 * This is a utility class for a table cell that displays a reaction formula.
 * @author Bruce Parrello
 *
 */
public class FormulaCell<T extends IReactionSource> extends TableCell<T, String> {

    // FIELDS
    /** underlying metabolic model */
    private MetaModel model;

    /**
     * This is an event handler that displays compound information.  For an input compound,
     * it shows the reactions that produce it.  For an output compound, it shows the reactions
     * that consume it.
     */
    public class ShowCompoundHandler implements EventHandler<MouseEvent> {

        // FIELDS
        /** compound ID */
        private String compound;
        /** TRUE for an input compound, else FALSE */
        private boolean inputFlag;

        /**
         * Construct a display for the specified compound.
         *
         * @param biggId	BiGG ID of the compound
         * @param input		TRUE if it is an input compound
         */
        protected ShowCompoundHandler(String biggId, boolean input) {
            this.compound = biggId;
            this.inputFlag = input;
        }

        @Override
        public void handle(MouseEvent event) {
            Stage compoundStage = new Stage();
            try {
                CompoundDisplay compoundViewer =
                        (CompoundDisplay) BaseController.loadFXML(App.class, "CompoundDisplay", compoundStage);
                compoundViewer.init(model, this.compound, this.inputFlag, event.getScreenX(), event.getScreenY());
                compoundStage.show();
            } catch (IOException e) {
                BaseController.messageBox(AlertType.ERROR, "Compound Display Error",
                        "Error loading compound display: " + e.toString());
            }
        }

    }

    /**
     * Construct a new formula cell.
     *
     * @param model		underlying metabolic model
     */
    public FormulaCell(MetaModel model) {
        this.model = model;
    }

    @Override
    public void updateItem(String form, boolean empty) {
        super.updateItem(form, empty);
        this.setText(null);
        if (empty)
            this.setGraphic(null);
        else {
            // Here we have something to display.  It is done entirely in the graphic.
            T item = this.getTableRow().getItem();
            // Get the elements to bold.
            var specials = item.getSpecial();
            // Compute the formula.
            boolean reversed = item.isReversed();
            Reaction react = item.getReaction();
            List<String> formulaParts = react.getParsedFormula(reversed);
            // Start the output graphic, which is a text flow of the formula parts.
            TextFlow flow = new TextFlow();
            // Build the text flow from the parts of the formula.
            final int nParts = formulaParts.size();
            for (int i = 0; i < nParts; i += 2) {
                // The connector is simply text.
                flow.getChildren().add(new Text(formulaParts.get(i)));
                int i1 = i + 1;
                // Some formulae have nothing on the right of the connector.  For these we use an X.
                if (i1 >= nParts)
                    flow.getChildren().add(new Text("X"));
                else {
                    String compound = formulaParts.get(i1);
                    Text compoundText = new Text(compound);
                    // If this is a special compound, bold it.
                    if (specials.contains(compound)) {
                        Font myFont = compoundText.getFont();
                        compoundText.setFont(Font.font(myFont.getName(),  FontWeight.BOLD, myFont.getSize()));
                        compoundText.setUnderline(true);
                    }
                    // Install a tooltip for the text that gives the compound's full name.
                    String outputName = this.model.getCompoundName(compound);
                    Tooltip nameTip = new Tooltip(outputName);
                    Tooltip.install(compoundText, nameTip);
                    compoundText.addEventHandler(MouseEvent.MOUSE_CLICKED,
                            this.new ShowCompoundHandler(compound, react.isProduct(compound) == reversed));
                    // Add the compound to the formula.
                    flow.getChildren().add(compoundText);
                }
            }
            // Display the formula.
            this.setGraphic(flow);
        }
    }

}
