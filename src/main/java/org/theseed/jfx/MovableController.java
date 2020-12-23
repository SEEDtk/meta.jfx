/**
 *
 */
package org.theseed.jfx;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/**
 * This is the base class for a fixed-size window.  It saves and restores the position but not the size.
 * The window is always application modal with minimal decoration.
 *
 * @author Bruce Parrello
 *
 */
public abstract class MovableController extends BaseController implements IController {


    /** default top */
    private double top;
    /** default left */
    private double left;

    /**
     * Construct a resizable stage.
     *
     * @param top		default top position
     * @param left		default left position
     */
    public MovableController(double top, double left) {
        super();
        this.top = top;
        this.left = left;
    }

    @Override
    public final void setup(Stage stage) {
        stage.initStyle(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        this.internalSetup(stage);
        this.setLocation(this.top, this.left);
        stage.setOnCloseRequest((final WindowEvent event) ->
        { this.saveLocation(); });
    }

}
