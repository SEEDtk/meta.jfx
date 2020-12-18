/**
 *
 */
package org.theseed.jfx;

import javafx.stage.Stage;

/**
 * This interface is used by controller classes to specify the icon and window title.
 *
 * Note that by convention, dialogs use init() to take in parameters and getResult()
 * to return results.
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

}
