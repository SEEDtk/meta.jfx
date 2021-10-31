/**
 *
 */
package org.theseed.jfx;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.dl4j.jfx.App;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
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
    public Stage getStage() {
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
    public static IController loadFXML(String fxml, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        Parent parent = fxmlLoader.load();
        IController retVal = (IController) fxmlLoader.getController();
        stage.setTitle(retVal.getWindowTitle());
        String iconFile = retVal.getIconName();
        if (iconFile != null) {
            Image icon = new Image(App.class.getResourceAsStream(iconFile));
            stage.getIcons().add(icon);
        }
        Scene scene = new Scene(parent);
        stage.setScene(scene);
        retVal.setup(stage);
        return retVal;
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

}
