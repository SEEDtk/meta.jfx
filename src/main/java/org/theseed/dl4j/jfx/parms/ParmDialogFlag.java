/**
 *
 */
package org.theseed.dl4j.jfx.parms;

import org.theseed.io.ParmDescriptor;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

/**
 * This is the dialog group for a very simple parameter that has no value.  In this case, the parameter is either
 * enabled (on) or disabled (off).  The main control is a label containing the parameter description.
 *
 * @author Bruce Parrello
 */
public class ParmDialogFlag extends ParmDialogGroup {

    /**
     * Construct the flag parameter controls.
     *
     * @param parent	parent grid pane
     * @param row		row index of control
     * @param desc		parameter descriptor
     */
    public ParmDialogFlag(GridPane parent, int row, ParmDescriptor desc) {
        init(parent, row, desc);
    }

    @Override
    protected Region createMainControl() {
        Label retVal = new Label(this.getDescription());
        return retVal;
    }

    @Override
    protected void initMainControl() {
    }

}
