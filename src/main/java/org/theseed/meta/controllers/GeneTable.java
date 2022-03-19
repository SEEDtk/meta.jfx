/**
 *
 */
package org.theseed.meta.controllers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Genome;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.ProteinRating;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
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
    /** alias map for base genome */
    private Map<String, Set<String>> aliasMap;
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
     * This is the value factory for the location cells, which require a display too complicated to put in
     * a lambda expression.
     */
    public class ShowLocations implements Callback<CellDataFeatures<ProteinRating, String>, ObservableValue<String>> {

        @Override
        public ObservableValue<String> call(CellDataFeatures<ProteinRating, String> param) {
            // Get all the features for the named gene.
            String gene = param.getValue().getProteinId();
            var fids = GeneTable.this.aliasMap.get(gene);
            // Get a list of the locations for the features.
            String retVal = fids.stream().map(x -> GeneTable.this.baseGenome.getFeature(x).getLocation().toSeedString())
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(retVal);
        }

    }

    /**
     * Set up to manage a table of modification genes.
     *
     * @param table		table control to manage
     * @param model		underlying metabolic model
     * @param ratings	list of protein ratings to display
     */
    public GeneTable(TableView<ProteinRating> table, MetaModel model, List<ProteinRating> ratings) {
        // Get the base genome and save the model and table control.
        this.baseGenome = model.getBaseGenome();
        this.aliasMap = this.baseGenome.getAliasMap();
        this.model = model;
        this.tblGenes = table;
        // Set the row height.
        this.tblGenes.setFixedCellSize(30);
        // Create the table columns.
        this.colWeights = new TableColumn<ProteinRating, Double>("Weight");
        this.colWeights.setPrefWidth(50);
        this.colWeights.setCellValueFactory((e) -> new SimpleDoubleProperty(e.getValue().getWeight()).asObject());
        this.colGenes = new TableColumn<ProteinRating, String>("Gene");
        this.colGenes.setPrefWidth(50);
        this.colGenes.setCellValueFactory((e) -> new SimpleStringProperty(e.getValue().getProteinSpec()));
        this.colLocations = new TableColumn<ProteinRating, String>("Locations");
        this.colLocations.setPrefWidth(250);
        this.colLocations.setCellValueFactory(this.new ShowLocations());
        this.colFormula = new TableColumn<ProteinRating, String>("Reaction");
        this.colFormula.setPrefWidth(400);
        this.colFormula.setCellFactory(this.new FormulaCellCallback());
        // Add the columns to the table.
        this.tblGenes.getColumns().add(this.colWeights);
        this.tblGenes.getColumns().add(this.colGenes);
        this.tblGenes.getColumns().add(this.colLocations);
        this.tblGenes.getColumns().add(this.colFormula);
        // Now add the protein ratings.
        this.tblGenes.getItems().addAll(ratings);
    }

}
