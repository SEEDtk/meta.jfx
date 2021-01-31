/**
 *
 */
package org.theseed.jfx;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * This class contains a statistic for display in a stats table.
 */
public class Stat {

    // FIELDS
    /** name of statistic */
    private String name;
    /** value of statistic */
    protected double value;

    /**
     * Create a statistic descriptor.
     *
     * @param name		statistic name
     * @param value 	statistic value
     */
    public Stat(String name, double value) {
        this.name = name;
        this.value = value;
    }

    /**
     * @return the name of the statistic
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the formatted value of the statistic
     */
    public String getValue() {
        return String.format("%14.6f", this.value);
    }

    /**
     * Initialize the table to contain the statistics.
     *
     * @param table		table to contain the statistics
     */
    public static void setupTable(TableView<Stat> table) {
        TableColumn<Stat, String> nameColumn = new TableColumn<Stat, String>("name");
        table.getColumns().add(nameColumn);
        nameColumn.setCellValueFactory(new PropertyValueFactory<Stat, String>("name"));
        TableColumn<Stat, String> valueColumn = new TableColumn<Stat, String>("value");
        table.getColumns().add(valueColumn);
        valueColumn.setCellValueFactory(new PropertyValueFactory<Stat, String>("value"));
    }

    /**
     * This is a subclass that displays the value as an integer.
     */
    public static class Int extends Stat {

        /**
         * Create an integer-valued statistic.
         *
         * @param name		name of statistic
         * @param value		value of statistic
         */
        public Int(String name, int value) {
            super(name, (double) value);
        }

        @Override
        public String getValue() {
            return String.format("%14d", (int) this.value);
        }

    }

}
