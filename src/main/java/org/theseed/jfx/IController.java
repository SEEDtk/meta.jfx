/**
 *
 */
package org.theseed.jfx;

import javafx.stage.Stage;

/**
 * This interface is used by controller classes to specify the icon and window title.
 *
 * @author Bruce Parrello
 *
 */
public interface IController {

    /**
     * @return the resource file name for the window icon
     */
    public String getIconName();

    /**
     * @return the window title
     */
    public String getWindowTitle();

    /**
     * Initialize the window.
     *
     * @param stage		parent stage object
     */
    public void setup(Stage stage);

    /**
     * Perform any initialization required by the subclass.
     *
     * @param parms		array of parameters
     */
    public abstract void init(String[] parms);

}
