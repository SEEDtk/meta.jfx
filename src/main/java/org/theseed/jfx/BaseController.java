/**
 *
 */
package org.theseed.jfx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.stage.Stage;

/**
 * This is the base class for all types of windows.  It handles preferences, saving the stage, and all of the common utilities.
 *
 * @author Bruce Parrello
 *
 */
public abstract class BaseController implements IController {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(BaseController.class);
    /** preferences */
    private PreferenceSet prefs;
    /** stage containing this window */
    private Stage stage;

    /**
     * This internal setup method saves the stage and initializes the preferences.
     *
     * @param stage		stage containing the window
     */
    protected final void internalSetup(Stage stage) {
        this.stage = stage;
        this.prefs = new PreferenceSet(this);
    }

    /**
     * @return the preferences for this window
     */
    protected PreferenceSet getPrefs() {
        return this.prefs;
    }

    /**
     * @return the stage for this window
     */
    protected Stage getStage() {
        return this.stage;
    }

    /**
     * Set the location of the window.
     *
     * @param top			default top location
     * @param left			default left location
     */
    protected void setLocation(double top, double left) {
        this.prefs.setLocation(this.stage, top, left);
    }

    /**
     * Set the location and size of a window.
     *
     * @param top			default top location
     * @param left			default left location
     * @param width			default width
     * @param height		default height
     */
    protected void setLocationAndSize(double top, double left, double width, double height) {
        this.prefs.setLocationAndSize(this.stage, top, left, width, height);
    }

    /**
     * Save the location of the window.
     */
    protected void saveLocation() {
        this.prefs.saveLocation(this.stage);
    }

    /**
     * Save the location and size of the window.
     */
    protected void saveLocationAndSize() {
        this.prefs.saveLocationandSize(this.stage);
    }

    /**
     * @return the value of a string preference
     *
     * @param name		name of the preference
     * @param defValue	default value
     */
    public String getPref(String name, String defValue) {
        return this.prefs.get(name, defValue);
    }

    /**
     * Store a new value for a preference.
     *
     * @param name		name of the preference
     * @param newValue	new value to store
     */
    public void setPref(String name, String newValue) {
        this.prefs.put(name, newValue);
    }

    /**
     * Close this window.
     */
    public void close() {
        this.stage.close();
    }

}
