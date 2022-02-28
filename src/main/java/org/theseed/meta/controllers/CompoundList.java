/**
 *
 */
package org.theseed.meta.controllers;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;

/**
 * The compound list controller manages a list control that displays compounds.  It handles
 * all the fancy event handling.  All of the compound lists are draggable.  The subclass
 * can be a drop target or not.
 *
 * @author Bruce Parrello
 *
 */
public abstract class CompoundList {

    // FIELDS
    /** list view being controlled */
    private ListView<MetaCompound> list;
    /** compound locator */
    private ICompoundFinder finder;

    /**
     * This class handles a drag-over event for the list control.  It is only called if we are in
     * an empty section of the list; otherwise, the event is consumed by the cell handler.
     */
    private class ListDragOverListener implements EventHandler<DragEvent> {

        /** event handler for target list control */
        private CompoundDisplayCell.IHandler listHandler;

        public ListDragOverListener(CompoundDisplayCell.IHandler handler) {
            this.listHandler = handler;
        }

        @Override
        public void handle(DragEvent event) {
            MetaCompound source = CompoundDisplayCell.getDragSource(event, CompoundList.this.finder);
            if (source != null) {
                boolean ok = this.listHandler.onDragOver(event, source, null);
                if (ok)
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
        }

    }

    /**
     * This class handles a drag-dropped event for the list control.  It is only called if we are in
     * an empty section of the list; otherwise, the event is consumed by the cell handler.
     */
    private class ListDragDropListener implements EventHandler<DragEvent> {

        /** event handler for target list control */
        private CompoundDisplayCell.IHandler listHandler;

        public ListDragDropListener(CompoundDisplayCell.IHandler handler) {
            this.listHandler = handler;
        }

        @Override
        public void handle(DragEvent event) {
            MetaCompound source = CompoundDisplayCell.getDragSource(event, CompoundList.this.finder);
            if (source != null)
                this.listHandler.onDragDrop(event, source, null);
        }

    }

    /**
     * This class intercepts a keypress for the list control.  If it is a DELETE key or BACKSPACE
     * key, it is passed to the delete event for the current list item.
     */
    protected class KeyPressListener implements EventHandler<KeyEvent> {

        /** event handler for target list control */
        private CompoundDisplayCell.IHandler listHandler;

        public KeyPressListener(CompoundDisplayCell.IHandler handler) {
            this.listHandler = handler;
        }

        @Override
        public void handle(KeyEvent event) {
            switch (event.getCode()) {
            case DELETE :
            case BACK_SPACE :
                // Compute the list item with the focus.
                MetaCompound target = CompoundList.this.list.getFocusModel().getFocusedItem();
                if (target != null)
                    this.listHandler.onDelete(event, target);
                event.consume();
                break;
            default:
                break;
            }
        }

    }

    /**
     * Set up a compound display list.
     *
     * @param listControl	list to manage
     * @param parent		compound finder for resolving IDs
     */
    public CompoundList(ListView<MetaCompound> listControl, ICompoundFinder parent) {
        this.list = listControl;
        this.finder = parent;
        this.setupListeners();
    }

    /**
     * Set up the listeners for this control.  The subclass should use this to call "setupListControl"
     * with the appropriate handler.
     */
    protected abstract void setupListeners();

    /**
     * @return TRUE if the specified compound is already in the list
     *
     * @param compound		BiGG ID of the compound to check
     */
    public boolean contains(String compound) {
        // Nothing fancy here, as the list is usually very small.
        return this.list.getItems().stream().anyMatch(x -> x.getId().equals(compound));
    }

    /**
     * Set up the cell factory for the list control.  The cell factory creates list cells that use
     * a particular handler for the major events and are of the type CompoundDisplayCell.
     *
     * @param myList		list control to set up
     * @param myHandler		event handler for the list control
     */
    protected void setupListControl(CompoundDisplayCell.IHandler myHandler) {
        // Set up the cell factory.
        this.list.setCellFactory(new Callback<ListView<MetaCompound>,ListCell<MetaCompound>>() {

            @Override
            public ListCell<MetaCompound> call(ListView<MetaCompound> list) {
                return new CompoundDisplayCell(myHandler, CompoundList.this.finder);
            }

        });
        // Set up the fallback listeners.
        this.list.setOnDragOver(this.new ListDragOverListener(myHandler));
        this.list.setOnDragDropped(this.new ListDragDropListener(myHandler));
        this.list.setOnKeyPressed(this.new KeyPressListener(myHandler));
    }

    /**
     * @return the list control
     */
    protected ObservableList<MetaCompound> getItems() {
        return this.list.getItems();
    }

    /**
     * This subclass creates a non-droppable list control.
     */
    public static class Normal extends CompoundList {

        public Normal(ListView<MetaCompound> listControl, ICompoundFinder parent) {
            super(listControl, parent);
        }

        /**
         * This class defines the event handler for the compound search list.  This list can be
         * a drag source (COPY), and the double-click event adds the selected compound to the
         * current path.
         */
        public class Listener implements CompoundDisplayCell.IHandler {

            @Override
            public TransferMode[] onDragStart(MouseEvent event, MetaCompound target) {
                // We allow dragging of compounds in COPY mode.
                return CompoundDisplayCell.DRAG_COPY;
            }

            @Override
            public boolean onDragOver(DragEvent event, MetaCompound source, MetaCompound target) {
                return false;
            }

            @Override
            public void onDragDrop(DragEvent event, MetaCompound source, MetaCompound target) { }

            @Override
            public void onDragDone(DragEvent event, MetaCompound source) { }

            @Override
            public void onDelete(KeyEvent event, MetaCompound target) { }

        }

        @Override
        protected void setupListeners() {
            this.setupListControl(this.new Listener());
        }

    }

    /**
     * This subclass creates a droppable list control.
     */
    public static class Droppable extends CompoundList {

        public Droppable(ListView<MetaCompound> listControl, ICompoundFinder parent) {
            super(listControl, parent);
        }

        /**
         * This class defines the event handlers for the two compound lists that form the query-- Path and Avoid.
         * These lists can be a drag source (MOVE) or a target.  Double-click has no effect, but DELETE will
         * delete a node.
         */
        public class Listener implements CompoundDisplayCell.IHandler {

            /** index at which last drop took place on this control */
            private int dropIndex;

            public Listener() {
                this.dropIndex = -1;
            }

            @Override
            public TransferMode[] onDragStart(MouseEvent event, MetaCompound target) {
                // Clear the drop index.  It will remain cleared until something is dropped here.
                this.dropIndex = -1;
                // Denote we're moving.
                return CompoundDisplayCell.DRAG_MOVE;
            }

            @Override
            public boolean onDragOver(DragEvent event, MetaCompound source, MetaCompound target) {
                return true;
            }

            @Override
            public void onDragDrop(DragEvent event, MetaCompound source, MetaCompound target) {
                // Here we are dropping.  If we are dropping on an empty cell, we add at the
                // end; otherwise, we add before the target.
                var items = CompoundList.Droppable.this.getItems();
                if (target == null) {
                    // Add to the end of the list.
                    items.add(source);
                    this.dropIndex = items.size() - 1;
                } else {
                    // Add before the target.
                    int idx = items.indexOf(target);
                    items.add(idx, source);
                    this.dropIndex = idx;
                }
                // Insure we don't have two copies of the compound in this list.  If we do, we delete
                // the ones that weren't just dropped.
                for (int i = 0; i < items.size(); i++) {
                    MetaCompound curr = items.get(i);
                    if (i != this.dropIndex && curr.equals(source)) {
                        items.remove(i);
                        if (i < this.dropIndex) this.dropIndex--;
                    }
                }
            }

            @Override
            public void onDragDone(DragEvent event, MetaCompound source) {
                // Here we must delete the compound from this list, because it has been moved.
                // We only do this if the drop index is -1, indicating that the drag started
                // here but did not end here.
                if (this.dropIndex == -1) {
                    var items = CompoundList.Droppable.this.getItems();
                    items.remove(source);
                }
            }

            @Override
            public void onDelete(KeyEvent event, MetaCompound target) {
                // Here the user wants us to delete the current item.
                var items = CompoundList.Droppable.this.getItems();
                items.remove(target);
            }

        }

        @Override
        protected void setupListeners() {
            this.setupListControl(this.new Listener());
        }

    }
}
