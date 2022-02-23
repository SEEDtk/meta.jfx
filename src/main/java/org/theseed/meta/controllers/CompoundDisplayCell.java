/**
 *
 */
package org.theseed.meta.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.event.EventHandler;
import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

/**
 * This object is a listview cell for a compound in a metabolic compound list control.  It provides
 * an anchor for events that need to be aware of the underlying compound.
 *
 * @author Bruce Parrello
 *
 */
public class CompoundDisplayCell extends ListCell<MetaCompound> {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(CompoundDisplayCell.class);
    /** event handler */
    private IHandler handler;
    /** compound map from model manager */
    private ICompoundFinder compoundOwner;
    /** transfer mode array for copying */
    public static final TransferMode[] DRAG_COPY = new TransferMode[] { TransferMode.COPY };
    /** transfer mode array for moving */
    public static final TransferMode[] DRAG_MOVE = new TransferMode[] { TransferMode.MOVE };

    /**
     * This interface defines an object that handles events for the cell.
     */
    public static interface IHandler {

        /**
         * Handle a double-click event for this cell.
         *
         * @param event		triggering click event
         * @param target	metabolic compound clicked
         */
        public void onDoubleClick(MouseEvent event, MetaCompound target);

        /**
         * Handle a start-drag event for this cell.
         *
         * @param event		triggering click event
         * @param target	metabolic compound being dragged
         *
         * @return array of transfer modes supported, or NULL to prevent dragging
         */
        public TransferMode[] onDragStart(MouseEvent event, MetaCompound target);

        /**
         * Handle a drag-over event for this cell.
         *
         * @param event		triggering drag event
         * @param source	metabolic compound being dragged
         * @param target	metabolic compound under the drag
         *
         * @return TRUE if this is a valid drop site, else FALSE
         */
        public boolean onDragOver(DragEvent event, MetaCompound source, MetaCompound target);

        /**
         * Handle a drop event for this cell.  This occurs on the target control when the drag is
         * over.
         *
         * @param event		triggering drag event
         * @param source	metabolic compound being dragged
         * @param target	metabolic compound under the drag
         *
         */
        public void onDragDrop(DragEvent event, MetaCompound source, MetaCompound target);

        /**
         * Handle a drag-done event for this cell.  This occurs on the control that was the
         * drag source when the source is dropped.
         *
         * @param event		triggering drag event
         * @param source	metabolic compound that was dragged
         */
        public void onDragDone(DragEvent event, MetaCompound source);

        /**
         * Handle a delete-key event for this cell.
         *
         * @param event		triggering keypress event
         * @param target	metabolic compound being deleted
         */
        public void onDelete(KeyEvent event, MetaCompound target);

    }

    /**
     * This class defines the handler for the click event.
     */
    protected class ClickHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent event) {
            MetaCompound target = CompoundDisplayCell.this.getItem();
            if (target != null && event.getClickCount() >= 2) {
                CompoundDisplayCell.this.handler.onDoubleClick(event, target);
            }
            event.consume();
        }

    }

    /**
     * This class defines the handler for the drag start.
     */
    protected class DragStartHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent event) {
            MetaCompound target = CompoundDisplayCell.this.getItem();
            if (target != null) {
                TransferMode[] modes = CompoundDisplayCell.this.handler.onDragStart(event, target);
                if (modes != null) {
                    Dragboard db = CompoundDisplayCell.this.startDragAndDrop(modes);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(target.getId());
                    db.setContent(content);
                }
            }
            event.consume();
        }

    }

    /**
     * This class defines the handler for the drag-over.
     */
    protected class DragOverHandler implements EventHandler<DragEvent> {

        @Override
        public void handle(DragEvent event) {
            MetaCompound target = CompoundDisplayCell.this.getItem();
            // Make sure we have a valid source.
            MetaCompound source = getDragSource(event, CompoundDisplayCell.this.compoundOwner);
            if (source != null) {
                boolean ok = CompoundDisplayCell.this.handler.onDragOver(event, source, target);
                if (ok)
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        }

    }

    /**
     * This class defines the handler for the drop.
     */
    protected class DragDropHandler implements EventHandler<DragEvent> {

        @Override
        public void handle(DragEvent event) {
            MetaCompound target = CompoundDisplayCell.this.getItem();
            // Make sure we have a valid source.
            MetaCompound source = getDragSource(event, CompoundDisplayCell.this.compoundOwner);
            if (source != null) {
                CompoundDisplayCell.this.handler.onDragDrop(event, source, target);
            }
            event.consume();
        }

    }

    /**
     * This class defines the handler for the drag-done event.
     */
    protected class DragDoneHandler implements EventHandler<DragEvent> {

        @Override
        public void handle(DragEvent event) {
            MetaCompound source = getDragSource(event, CompoundDisplayCell.this.compoundOwner);
            if (source != null) {
                CompoundDisplayCell.this.handler.onDragDone(event, source);
            }
            event.consume();
        }

    }

    /**
     * This class handles keypress events.  Currently we only care about the DELETE key.
     */
    protected class KeyPressHandler implements EventHandler<KeyEvent> {

        @Override
        public void handle(KeyEvent event) {
            if (event.getCode() == KeyCode.DELETE) {
                MetaCompound target = CompoundDisplayCell.this.getItem();
                if (target != null) {
                    CompoundDisplayCell.this.handler.onDelete(event, target);
                }
                event.consume();
            }
        }

    }

    /**
     * Construct this cell and attach the event handlers.
     *
     * @param handler		event-handling object to use
     * @param owner			object that can translate compound IDs to descriptors
     */
    public CompoundDisplayCell(IHandler handler, ICompoundFinder owner) {
        super();
        this.handler = handler;
        this.compoundOwner = owner;
        this.setOnMouseClicked(this.new ClickHandler());
        this.setOnDragDetected(this.new DragStartHandler());
        this.setOnDragOver(this.new DragOverHandler());
        this.setOnDragDropped(this.new DragDropHandler());
        this.setOnDragDone(this.new DragDoneHandler());
        this.setOnKeyPressed(this.new KeyPressHandler());
    }

    /**
     * @return the compound currently being dragged by a drag event, or NULL if none
     *
     * @param event		drag event in progress
     * @param owner		compound-finding object
     */
    public static MetaCompound getDragSource(DragEvent event, ICompoundFinder owner) {
        MetaCompound retVal = null;
        String id = event.getDragboard().getString();
        if (id != null)
            retVal = owner.getCompound(id);
        return retVal;
    }

    @Override
    public void updateItem(MetaCompound item, boolean empty) {
        super.updateItem(item, empty);
        this.setText(null);
        if (! empty && item != null) {
            this.setGraphic(item.getRendering());
        } else {
            this.setGraphic(null);
        }
    }


}
