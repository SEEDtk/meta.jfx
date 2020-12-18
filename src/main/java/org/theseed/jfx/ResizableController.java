/**
 *
 */
package org.theseed.jfx;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * This is a base class for a resizable window.  It saves the window size and position.
 *
 * @author Bruce Parrello
 *
 */
public abstract class ResizableController extends BaseController {

    // FIELDS
    private double top;
    /** default left */
    private double left;
    /** default width */
    private double width;
    /** default height */
    private double height;

    /**
     * Construct a resizable stage.
     *
     * @param top		default top position
     * @param left		default left position
     * @param width		default width
     * @param height	default height
     */
    public ResizableController(double top, double left, double width, double height) {
        super();
        this.top = top;
        this.left = left;
        this.width = width;
        this.height = height;
    }

    @Override
    public void setup(Stage stage) {
        this.internalSetup(stage);
        this.setLocationAndSize(this.top, this.left, this.width, this.height);
        stage.setOnCloseRequest((final WindowEvent event) ->
        { this.saveLocationAndSize(); });
    }

}
