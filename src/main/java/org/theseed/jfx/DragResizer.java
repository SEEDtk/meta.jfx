package org.theseed.jfx;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * This class can be used to add mouse listeners to a Region
 * and make it resizable by the user by clicking and dragging the corner in the
 * same way as a window.  Currently, you have to drag the corner, not an edge.
 *
 * Usage: DragResizer.makeResizable(myAnchorPane);
 *
 * @author atill, modified by Bruce Parrello
 *
 */
public class DragResizer {

    // FIELDS
    /** margin for detecting a click on the corner */
    private static final int RESIZE_MARGIN = 5;
    /** region being resized */
    private final Region region;
    /** last known y-position of mouse */
    private double y;
    /** last known x-position of mouse */
    private double x;
    /** TRUE if we've initialized the preferred dimensions */
    private boolean initPrefs;
    /** TRUE if we are in the middle of a drag */
    private boolean dragging;
    /** controller to call after resizing */
    private CallBack controller;

    public interface CallBack {

        /**
         * Process a resize for the specified region.
         *
         * @param target	region resized
         */
        public void handleResize(Region target);

    }

    /**
     * Make a region resizable.
     *
     * @param aRegion		region to be made resizable
     * @param controller	controller to be called after the resize
     */
    private DragResizer(Region aRegion, CallBack controller) {
        this.region = aRegion;
        this.controller = controller;
    }

    /**
     * Make a region resizable without callbacks.
     *
     * @param aRegion		region to be made resizable
     * @param controller	controller to be called after the resize
     */
    public static DragResizer makeResizable(Region region, CallBack controller) {
        // Create this object.
        final DragResizer retVal = new DragResizer(region, controller);
        // Attach mouse events to the region.
        region.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                retVal.mousePressed(event);
            }});
        region.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                retVal.mouseDragged(event);
            }});
        region.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                retVal.mouseReleased(event);
            }});
        return retVal;
    }

    /**
     * When this is fired, the drag is complete.
     *
     * @param event		event descriptor
     */
    protected void mouseReleased(MouseEvent event) {
        this.dragging = false;
        this.region.setCursor(Cursor.DEFAULT);
        if (this.controller != null)
            controller.handleResize(this.region);
    }

    /**
     * @return TRUE if the user is at the corner, else FALSE
     *
     * @param event		event descriptor
     */
    protected boolean isInDraggableZone(MouseEvent event) {
        return event.getY() > (region.getHeight() - RESIZE_MARGIN)
                && event.getX() > (region.getWidth() - RESIZE_MARGIN);
    }

    /**
     * When this is fired, the user is moving the mouse with the button down.
     * If we are dragging, we resize the control.
     *
     * @param event		event descriptor
     */
    protected void mouseDragged(MouseEvent event) {
        if (this.dragging) {
            // Get the mouse location.
            double mouseX = event.getX();
            double mouseY = event.getY();
            // Compute the new dimensions.
            double newHeight = region.getPrefHeight() + (mouseY - y);
            double newWidth = region.getPrefWidth() + (mouseX - x);
            // Store the new values.
            region.setPrefHeight(newHeight);
            region.setPrefWidth(newWidth);
            // Save the current position.
            this.y = mouseY;
            this.x = mouseX;
        }
    }

    /**
     * When this is fired, the mouse has been clicked.  If we are in the drag zone,
     * we start dragging.
     *
     * @param event		event descriptor
     */
    protected void mousePressed(MouseEvent event) {
        // Ignore clicks outside of the draggable margin.
        if(isInDraggableZone(event)) {
            this.dragging = true;
            // Make sure the preferred width and height are set.
            if (! this.initPrefs) {
                region.setPrefHeight(region.getHeight());
                region.setPrefWidth(region.getWidth());
                this.initPrefs = true;
            }
            // Initialize the saved mouse location.
            this.y = event.getY();
            this.x = event.getX();
        }
    }

}
