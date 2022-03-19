/**
 *
 */
package org.theseed.meta.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Genome;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.ProteinRating;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/**
 * This class manages a table that displays pathway modification genes, either ones that promote the
 * pathway (inserts) or hinder it (deletes).  Each gene is represented by a protein rating, and we display
 * its weight, its name, its location information, and the main reaction it triggers.
 *
 * @author Bruce Parrello
 *
 */
public class GeneTable {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(GeneTable.class);
    /** underlying model */
    private MetaModel model;
    /** base genome */
    private Genome baseGenome;
    /** table being controlled */
    private TableView<ProteinRating> tblGenes;
    /** column of weights */
    private TableColumn<ProteinRating, Double> colWeights;
    /** column of gene names */
    private TableColumn<ProteinRating, String> colGenes;
    /** column of locations */
    private TableColumn<ProteinRating, String> colLocations;
    /** column of formulae */
    private TableColumn<ProteinRating, String> colFormula;

    /**
     * This is the cell factory class for formula cells.  Formula cells have the font variations and tooltips,
     * so they require a custom display.
     */
    public class FormulaCellCallback implements Callback<TableColumn<ProteinRating, String>, TableCell<ProteinRating, String>> {

        @Override
        public TableCell<ProteinRating, String> call(TableColumn<ProteinRating, String> param) {
            return new FormulaCell<ProteinRating>(GeneTable.this.model);
        }

    }

    /**
     * Set up to manage a table of modification genes.
     *
     * @param table		table control to manage
     * @param model		underlying metabolic model
     * @param ratings	list of protein ratings to display
     */
    public GeneTable(TableView<ProteinRating> table, MetaModel model) {
        // Get the base genome and save the model and table control.
        this.baseGenome = model.getBaseGenome();
        this.model = model;
        this.tblGenes = table;
        // Set the row height.
        this.tblGenes.setFixedCellSize(30);
        // Create the table columns.
        // TODO build the gene table columns
        // Add the columns to the table.
        this.tblGenes.getColumns().add(this.colWeights);
        this.tblGenes.getColumns().add(this.colGenes);
        this.tblGenes.getColumns().add(this.colLocations);
        this.tblGenes.getColumns().add(this.colFormula);
        // Now add the protein ratings.
        // TODO proteinratings
    }

}
