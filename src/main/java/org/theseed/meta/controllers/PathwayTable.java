/**
 *
 */
package org.theseed.meta.controllers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Pathway;
import org.theseed.metabolism.Reaction;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;

/**
 * This class manages a table that displays the reactions in a pathway.  If the pathway element
 * specifies an output metabolite, it will be bolded in the formula display.  If the pathway
 * element specifies that the reactions are reversed, they will be displayed in reverse order
 * in the formula.
 *
 * @author Bruce Parrello
 *
 */
public class PathwayTable {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(PathwayTable.class);
    /** pathway being displayed */
    private Pathway path;
    /** metabolic model containing the pathway */
    private MetaModel model;
    /** table view control */
    private TableView<Pathway.Element> table;
    /** number column; this is so the user can get the original sort order back */
    private TableColumn<Pathway.Element, Integer> numColumn;
    /** reaction ID column */
    private TableColumn<Pathway.Element, String> idColumn;
    /** reaction name column */
    private TableColumn<Pathway.Element, String> nameColumn;
    /** reaction rule column */
    private TableColumn<Pathway.Element, String> ruleColumn;
    /** reaction formula column */
    private TableColumn<Pathway.Element, String> formColumn;

    /**
     * This is the value factory class for displaying the reaction rule.  Creating a reaction rule is
     * a bit too complicated to put in a lambda expression.
     */
    public class RuleDisplayCallback implements Callback<CellDataFeatures<Pathway.Element, String>, ObservableValue<String>> {

        @Override
        public ObservableValue<String> call(CellDataFeatures<Pathway.Element, String> param) {
            Reaction reaction = param.getValue().getReaction();
            String rule = reaction.getReactionRule();
            // Here is the tricky part.  We translate BiGG IDs into gene names.
            String retVal = Reaction.getTranslatedRule(rule, PathwayTable.this.model);
            // Convert to a simple string property.
            return new SimpleStringProperty(retVal);
        }

    }

    /**
     * This is the cell factory class for formula cells.  Formula cells have the output metabolite boldfaced,
     * so they require a custom display.
     */
    public class FormulaCellCallback implements Callback<TableColumn<Pathway.Element, String>, TableCell<Pathway.Element, String>> {

        @Override
        public TableCell<Pathway.Element, String> call(TableColumn<Pathway.Element, String> param) {
            return new FormulaCell();
        }

    }

    /**
     * This object represents a formula cell.  It is responsible for generating and rendering the formula.
     */
    public class FormulaCell extends TableCell<Pathway.Element, String> {

        @Override
        public void updateItem(String form, boolean empty) {
            super.updateItem(form, empty);
            this.setText(null);
            if (empty)
                this.setGraphic(null);
            else {
                Pathway.Element element = this.getTableRow().getItem();
                // Compute the formula.
                String formula = element.getReaction().getFormula(element.isReversed());
                // This will be the graphic we display.
                Node flow;
                if (element.getOutput() == null) {
                    // If there is no target output, we just emit the formula string.
                    flow = new Text(formula);
                } else {
                    // Here we must bold the output metabolite.
                    Pattern search = Pattern.compile("\\b" + element.getOutput() + "\\b");
                    Matcher m = search.matcher(formula);
                    if (! m.find())
                        flow = new Text(formula);
                    else {
                        // Insure we've found the last version of it.
                        int start = m.start();
                        int end = m.end();
                        while (m.find()) {
                            start = m.start();
                            end = m.end();
                        }
                        // BOLD all the text for the output name.  This is very complicated.
                        TextFlow myFlow = new TextFlow();
                        if (start > 0)
                            myFlow.getChildren().add(new Text(formula.substring(0, start)));
                        Text bolded = new Text(formula.substring(start, end));
                        Font myFont = bolded.getFont();
                        bolded.setFont(Font.font(myFont.getName(),  FontWeight.BOLD, myFont.getSize()));
                        myFlow.getChildren().add(bolded);
                        if (end < formula.length())
                            myFlow.getChildren().add(new Text(formula.substring(end)));
                        flow = myFlow;
                    }
                }
                this.setGraphic(flow);
            }
        }
    }

    /**
     * Create the pathway table controller and fill in the specified table control.
     *
     * @param tableControl	table view control to manage
     * @param path1			pathway to display
     * @param owningModel	underlying metabolic model
     */
    public PathwayTable(TableView<Pathway.Element> tableControl, Pathway path1, MetaModel owningModel) {
        this.path = path1;
        this.model = owningModel;
        this.table = tableControl;
        // Set the row height.
        this.table.setFixedCellSize(30.0);
        // Create the table columns.
        this.numColumn = new TableColumn<Pathway.Element, Integer>("#");
        this.numColumn.setPrefWidth(30);
        this.numColumn.setCellValueFactory((e) -> new SimpleIntegerProperty(e.getValue().getSeqNum()).asObject());
        this.idColumn = new TableColumn<Pathway.Element, String>("Reaction");
        this.idColumn.setPrefWidth(100);
        this.idColumn.setCellValueFactory((e) -> new SimpleStringProperty(e.getValue().getReaction().getBiggId()));
        this.nameColumn = new TableColumn<Pathway.Element, String>("Name");
        this.nameColumn.setPrefWidth(200);
        this.nameColumn.setCellValueFactory((e) -> new SimpleStringProperty(e.getValue().getReaction().getName()));
        this.ruleColumn = new TableColumn<Pathway.Element, String>("Rule");
        this.ruleColumn.setPrefWidth(300);
        this.ruleColumn.setCellValueFactory(this.new RuleDisplayCallback());
        this.formColumn = new TableColumn<Pathway.Element, String>("Formula");
        this.formColumn.setPrefWidth(500);
        this.formColumn.setCellFactory(this.new FormulaCellCallback());
        // Add the columns to the table.
        this.table.getColumns().add(this.numColumn);
        this.table.getColumns().add(this.idColumn);
        this.table.getColumns().add(this.nameColumn);
        this.table.getColumns().add(this.ruleColumn);
        this.table.getColumns().add(this.formColumn);
        // Now add all the pathway elements to the table.
        final var items = this.table.getItems();
        this.path.stream().forEach(x -> items.add(x));
    }

}
