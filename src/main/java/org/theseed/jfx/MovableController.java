/**
 *
 */
package org.theseed.jfx;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * This is the base class for a fixed-size window.  It saves and restores the position but not the size.
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
        this.internalSetup(stage);
        this.setLocation(this.top, this.left);
        stage.setOnCloseRequest((final WindowEvent event) ->
        { this.saveLocation(); });
    }

    /**
     * Perform any initialization required by the subclass.
     *
     * @param parms		array of parameters
     */
    public abstract void init(String[] parms);


}
