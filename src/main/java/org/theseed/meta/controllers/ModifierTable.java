/**
 *
 */
package org.theseed.meta.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.metabolism.mods.Modifier;
import org.theseed.metabolism.mods.ModifierList;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;

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
    private TableView<Modifier> table;
    /** checkbox column, indicating active (checked) or suppressed */
    private TableColumn<Modifier, Boolean> activeColumn;
    /** command column */
    private TableColumn<Modifier, String> commandColumn;
    /** parameter column */
    private TableColumn<Modifier, String> parmColumn;


    /**
     * Create the modifier table controller.
     *
     * @param tableControl	table view control to manage
     */
    public ModifierTable(TableView<Modifier> tableControl) {
        this.table = tableControl;
        // Set the row height.
        this.table.setFixedCellSize(30);
        // Create the checkbox column.
        this.activeColumn = new TableColumn<Modifier, Boolean>("X");
        this.activeColumn.setPrefWidth(30);
        this.activeColumn.setCellFactory(CheckBoxTableCell.forTableColumn(this.activeColumn));
        this.activeColumn.setCellValueFactory((e) -> new SimpleBooleanProperty(e.getValue().isActive()));
        // Create the normal columns.
        this.commandColumn = new TableColumn<Modifier, String>("command");
        this.commandColumn.setPrefWidth(100);
        this.commandColumn.setCellValueFactory((e) -> new SimpleStringProperty(e.getValue().getCommand()));
        this.parmColumn = new TableColumn<Modifier, String>("parms");
        this.parmColumn.setPrefWidth(300);
        this.parmColumn.setCellValueFactory((e) -> new SimpleStringProperty(e.getValue().getParms()));
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
        List<Modifier> items = this.table.getItems();
        items.clear();
        for (Modifier mod : modList)
            items.add(mod);
    }

    /**
     * @return a modifier list for the active modifiers in this table
     */
    public ModifierList getModifiers() {
        List<Modifier> items = this.table.getItems();
        // Fix the active flags.
        for (int i = 0; i < items.size(); i++) {
            boolean active = this.activeColumn.getCellData(i);
            items.get(i).setActive(active);
        }
        // Return the modifiers in the table.
        ModifierList retVal = new ModifierList(items);
        return retVal;
    }

}
