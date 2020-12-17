/**
 *
 */
package org.theseed.jfx;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.theseed.dl4j.jfx.App;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
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

    /**
     * Construct the preferences for an object.
     *
     * @param object	object whose preferences are desired
     */
    public PreferenceSet(Object target) {
        this.prefs = Preferences.userNodeForPackage(target.getClass());
    }

    /**
     * Set the location of a window.
     *
     * @param window		stage object for the window
     * @param top			default top location
     * @param left			default left location
     */
    public void setLocation(Stage window, double top, double left) {
        double x = this.prefs.getDouble("_x", left);
        double y = this.prefs.getDouble("_y", top);
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
        double w = this.prefs.getDouble("_w", width);
        double h = this.prefs.getDouble("_h", height);
        window.setWidth(w);
        window.setHeight(h);
    }

    /**
     * Save the location of a window.
     *
     * @param window		stage object for the window
     */
    public void saveLocation(Stage window) {
        this.prefs.putDouble("_x", window.getX());
        this.prefs.putDouble("_y", window.getY());
    }

    /** Save the location and size of a window.
     *
     * @param window		stage object for the window
     */
    public void saveLocationandSize(Stage window) {
        this.saveLocation(window);
        this.prefs.putDouble("_w", window.getWidth());
        this.prefs.putDouble("_h", window.getHeight());
    }

    /**
     * Load a window from its FXML file and return the controller.  The scene will be attached,
     * but the stage will not have been shown.
     *
     * @param fxml		name of the FXML file (without the extension)
     * @param stage		stage onto which the view will be loaded
     * @param parms		one or more string parameters to pass to the controller
     *
     * @throws IOException
     */
    public static IController loadFXML(String fxml, Stage stage, String... parms) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        Parent retVal = fxmlLoader.load();
        IController controller = (IController) fxmlLoader.getController();
        stage.setTitle(controller.getWindowTitle());
        String iconFile = controller.getIconName();
        if (iconFile != null) {
            Image icon = new Image(App.class.getResourceAsStream(iconFile));
            stage.getIcons().add(icon);
        }
        Scene scene = new Scene(retVal);
        stage.setScene(scene);
        controller.setup(stage);
        controller.init(parms);
        return controller;
    }

    /**
     * Display an alert message.
     *
     * @param type		icon type
     * @param header	header text
     * @param message	message text
     */
    public static void messageBox(Alert.AlertType type, String header, String message) {
        Alert messageBox = new Alert(type);
        messageBox.setHeaderText(header);
        messageBox.setContentText(message);
        messageBox.showAndWait();
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
