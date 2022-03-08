/**
 *
 */
package org.theseed.meta.controllers;

import org.theseed.metabolism.mods.Modifier;
import org.theseed.metabolism.mods.ModifierList;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * This object is a version of the modifier that has observable properties for editing in
 * a table.  It can be constructed from a normal modifier or converted to one.
 *
 * @author Bruce Parrello
 *
 */
public class ObservableModifier {

    // FIELDS
    /** TRUE if this modifier is active, else FALSE */
    private SimpleBooleanProperty active;
    /** command for this modifier */
    private SimpleStringProperty command;
    /** parameter string for this modifier */
    private SimpleStringProperty parms;

    /**
     * Construct a blank, empty modifier.
     */
    public ObservableModifier() {
        this.active = new SimpleBooleanProperty(true);
        this.command = new SimpleStringProperty("SUPPRESS");
        this.parms = new SimpleStringProperty("");
    }

    /**
     * Construct a modifier from a normal modifier.
     *
     * @param modifier		source normal modifier
     */
    public ObservableModifier(Modifier modifier) {
        this.active = new SimpleBooleanProperty(modifier.isActive());
        this.command = new SimpleStringProperty(modifier.getCommand());
        this.parms = new SimpleStringProperty(modifier.getParms());
    }

    /**
     * @return the active-flag property
     */
    public SimpleBooleanProperty getActiveProperty() {
        return this.active;
    }

    /**
     * @return TRUE if this modifier is active
     */
    public boolean isActive() {
        return this.active.getValue();
    }

    /**
     * Specify whether or not this modifier is active.
     *
     * @param activeFlag	TRUE to activate this modifier, else FALSE
     */
    public void setActive(boolean activeFlag) {
        this.active.set(activeFlag);
    }

    /**
     * @return the command property
     */
    public SimpleStringProperty getCommandProperty() {
        return this.command;
    }

    /**
     * @return the command string
     */
    public String getCommand() {
        return this.command.getValue();
    }

    /**
     * Specify a new command string.
     *
     * @param newValue	new command to use
     */
    public void setCommand(String newValue) {
        this.command.set(newValue);
    }

    /**
     * @return the parameter property
     */
    public SimpleStringProperty getParmsProperty() {
        return this.parms;
    }

    /**
     * @return the parameter string
     */
    public String getParms() {
        return this.parms.getValue();
    }

    /**
     * Specify a new parameter string.
     *
     * @param newValue	new parameter string to use
     */
    public void setParms(String newValue) {
        this.parms.set(newValue);
    }

    /**
     * @return a normal modifier with the same values as this object
     */
    public Modifier get() {
        ModifierList.Command commandType = ModifierList.Command.valueOf(this.getCommand());
        Modifier retVal = commandType.create(this.getParms());
        retVal.setActive(this.isActive());
        return retVal;
    }

}
