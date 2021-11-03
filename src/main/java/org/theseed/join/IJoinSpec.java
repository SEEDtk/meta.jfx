/**
 *
 */
package org.theseed.join;

import java.io.IOException;

import org.theseed.io.KeyedFileMap;

import javafx.scene.Node;

/**
 * This interface must be implemented by every specification controller in a join operation.
 * It provides the basic methods for initializing the specification, verifying
 * that it is ready, and applying the operation.
 *
 * @author Bruce Parrello
 *
 */
public interface IJoinSpec {

    /**
     * Initialize this object and fill in the controls.
     *
     * @param parent	parent join dialog
     * @param node		display node containing this controller
     */
    public void init(JoinDialog parent, Node node);

    /**
     * Verify that this specification is ready.
     */
    public boolean isValid();

    /**
     * Apply this specification's operation to a keyed file map.
     *
     * @param keyedMap	keyed map to process
     *
     * @throws IOException
     */
    public void apply(KeyedFileMap keyedMap) throws IOException;

    /**
     * @return the display node for this controller
     */
    public Node getNode();

    /**
     * Set the title of the spec.
     *
     * @param title		title to display
     */
    public void setTitle(String title);

    /**
     * @return TRUE if this is an output specification
     */
    public boolean isOutput();

}
