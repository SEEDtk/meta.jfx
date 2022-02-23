/**
 *
 */
package org.theseed.meta.controllers;

import javafx.scene.control.ListCell;

/**
 * This object manages the display cell for a reaction trigger.  We put a special icon in each
 * cell to indicate whether the trigger is mainline or branch.
 *
 * @author Bruce Parrello
 *
 */
public class ReactionTriggerCell extends ListCell<ReactionTrigger> {

    @Override
    public void updateItem(ReactionTrigger trigger, boolean empty) {
        super.updateItem(trigger, empty);
        if (empty) {
            this.setText(null);
            this.setGraphic(null);
        } else {
            this.setText(trigger.toString());
            this.setGraphic(trigger.getIcon());
        }

    }

}
