/**
 *
 */
package org.theseed.meta.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.jfx.BaseController;
import org.theseed.metabolism.mods.Modifier;
import org.theseed.metabolism.mods.ModifierList;

import javafx.event.EventHandler;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * This class manages a table that displays the modifiers in a flow modification file.  The first column
 * is a checkbox that allows the user to turn the modifier on or off.  The remaining two columns are
 * the modifier's command and its parameter string.
 *
 * @author Bruce Parrello
 *
 */
public class ModifierTable {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ModifierTable.class);
    /** table view control */
    private TableView<ObservableModifier> table;
    /** checkbox column, indicating active (checked) or suppressed */
    private TableColumn<ObservableModifier, Boolean> activeColumn;
    /** command column */
    private TableColumn<ObservableModifier, String> commandColumn;
    /** parameter column */
    private TableColumn<ObservableModifier, String> parmColumn;

    /**
     * This event handler intercepts the DELETE key.  The user is asked to confirm, and then
     * the current flow modifier is deleted.
     */
    public class DeleteHandler implements EventHandler<KeyEvent> {

        @Override
        public void handle(KeyEvent event) {
            var tableControl = ModifierTable.this.table;
            var selectedModifier = tableControl.getSelectionModel().getSelectedItem();
            if (selectedModifier != null && event.getCode() == KeyCode.DELETE) {
                boolean ok = BaseController.confirmationBox("Delete Flow Modifier",
                        "Delete the selected " + selectedModifier.getCommand() + " modifier?");
                if (ok)
                    tableControl.getItems().remove(selectedModifier);
                event.consume();
            }
        }

    }


    /**
     * Create the modifier table controller.
     *
     * @param tableControl	table view control to manage
     */
    public ModifierTable(TableView<ObservableModifier> tableControl) {
        this.table = tableControl;
        // Set the row height.
        this.table.setFixedCellSize(30);
        this.table.setEditable(true);
        this.table.setOnKeyPressed(this.new DeleteHandler());
        // Create the checkbox column.
        this.activeColumn = new TableColumn<ObservableModifier, Boolean>("X");
        this.activeColumn.setPrefWidth(30);
        this.activeColumn.setCellFactory(CheckBoxTableCell.forTableColumn(this.activeColumn));
        this.activeColumn.setCellValueFactory(e -> e.getValue().getActiveProperty());
        // Create the normal columns.
        this.commandColumn = new TableColumn<ObservableModifier, String>("command");
        this.commandColumn.setPrefWidth(100);
        this.commandColumn.setCellValueFactory((e) -> e.getValue().getCommandProperty());
        this.parmColumn = new TableColumn<ObservableModifier, String>("parms");
        this.parmColumn.setPrefWidth(300);
        this.parmColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        this.parmColumn.setCellValueFactory(new PropertyValueFactory<ObservableModifier, String>("parms"));
        this.parmColumn.setEditable(true);
        this.parmColumn.setOnEditCommit((e) -> e.getRowValue().setParms(e.getNewValue()));
        // Add the columns to the table.
        this.table.getColumns().add(this.activeColumn);
        this.table.getColumns().add(this.commandColumn);
        this.table.getColumns().add(this.parmColumn);
    }

    /**
     * Store a modifier list in this table.
     *
     * @param modList	modifier list to display
     */
    public void store(ModifierList modList) {
        var items = this.table.getItems();
        items.clear();
        for (Modifier mod : modList)
            items.add(new ObservableModifier(mod));
    }

    /**
     * @return a modifier list for the active modifiers in this table
     */
    public ModifierList getModifiers() {
        // Create modifiers from the observables in the table.
        List<Modifier> items = this.table.getItems().stream().map(x -> x.get()).collect(Collectors.toList());
        // Return them as a modifier list.
        ModifierList retVal = new ModifierList(items);
        return retVal;
    }

}
