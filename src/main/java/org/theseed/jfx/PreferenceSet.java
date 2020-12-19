/**
 *
 */
package org.theseed.jfx;

import java.util.prefs.Preferences;

import javafx.stage.Stage;

/**
 * This class contains utilities for managing preferences.
 *
 * @author Bruce Parrello
 *
 */
public class PreferenceSet {

    // FIELDS
    /** current preference set */
    private Preferences prefs;
    /** class name */
    private String name;

    /**
     * Construct the preferences for an object.
     *
     * @param object	object whose preferences are desired
     */
    public PreferenceSet(Object target) {
        this.prefs = Preferences.userNodeForPackage(target.getClass());
        this.name = target.getClass().getSimpleName();
    }

    /**
     * Set the location of a window.
     *
     * @param window		stage object for the window
     * @param top			default top location
     * @param left			default left location
     */
    public void setLocation(Stage window, double top, double left) {
        double x = this.prefs.getDouble(this.name + "_x", left);
        double y = this.prefs.getDouble(this.name + "_y", top);
        window.setX(x);
        window.setY(y);
    }

    /**
     * Set the location and size of a window.
     *
     * @param window		stage object for the window
     * @param top			default top location
     * @param left			default left location
     * @param width			default width
     * @param height		default height
     */
    public void setLocationAndSize(Stage window, double top, double left, double width, double height) {
        this.setLocation(window, top, left);
        double w = this.prefs.getDouble(this.name + "_w", width);
        double h = this.prefs.getDouble(this.name + "_h", height);
        window.setWidth(w);
        window.setHeight(h);
    }

    /**
     * Save the location of a window.
     *
     * @param window		stage object for the window
     */
    public void saveLocation(Stage window) {
        this.prefs.putDouble(this.name + "_x", window.getX());
        this.prefs.putDouble(this.name + "_y", window.getY());
    }

    /** Save the location and size of a window.
     *
     * @param window		stage object for the window
     */
    public void saveLocationandSize(Stage window) {
        this.saveLocation(window);
        this.prefs.putDouble(this.name + "_w", window.getWidth());
        this.prefs.putDouble(this.name + "_h", window.getHeight());
    }

    /**
     * @return the value of a string preference
     *
     * @param name		name of the preference
     * @param defValue	default value
     */
    public String get(String name, String defValue) {
        return this.prefs.get(name, defValue);
    }

    /**
     * Store the value of a string preference.
     *
     * @param name		name of the preference
     * @param newValue	value to store
     */
    public void put(String name, String newValue) {
        this.prefs.put(name, newValue);
    }

}
