/**
 *
 */
package org.theseed.dl4j.jfx.parms;

import org.theseed.io.ParmDescriptor;
import org.theseed.io.ParmFile;

import javafx.scene.layout.GridPane;

/**
 * This is a simple utility class for putting parameters on a grid pane.
 *
 * @author Bruce Parrello
 *
 */
public class ParmPaneBuilder {

    // FIELDS
    /** parent grid pane */
    private GridPane parent;
    /** current number of rows */
    private int row;
    /** parameter descriptor map */
    private ParmFile parms;

    /**
     * Construct a pane builder for a specified pane and parameter map.
     *
     * @param parent	parent grid pane
     * @param parms		parameter descriptor map
     */
    public ParmPaneBuilder(GridPane parent, ParmFile parms) {
        this.parent = parent;
        this.row = 0;
        this.parms = parms;
    }

    /**
     * Add a text parameter.
     *
     * @param name	parameter name
     *
     * @return the group added, or NULL if the parameter does not exist
     */
    public ParmDialogGroup addText(String name) {
        ParmDialogGroup retVal = null;
        ParmDescriptor desc = this.parms.get(name);
         if (desc != null) {
             retVal = new ParmDialogText(this.parent, this.row, desc);
             row++;
         }
         return retVal;
    }

    /**
     * Add a choices parameter.
     *
     * @param name		parameter name
     * @param values	list of possible choices
     *
     * @return the group added, or NULL if the parameter does not exist
     */
    public ParmDialogGroup addChoices(String name, Enum<?>[] values) {
        ParmDialogGroup retVal = null;
        ParmDescriptor desc = this.parms.get(name);
         if (desc != null) {
             retVal = new ParmDialogChoices(this.parent, this.row, desc, values);
             row++;
         }
         return retVal;
    }

    /**
     * Add a flag parameter.
     *
     * @param name	parameter name
     *
     * @return the group added, or NULL if the parameter does not exist
     */
    public ParmDialogGroup addFlag(String name) {
        ParmDialogGroup retVal = null;
        ParmDescriptor desc = this.parms.get(name);
         if (desc != null) {
             retVal = new ParmDialogFlag(this.parent, this.row, desc);
             row++;
         }
         return retVal;
    }

}
