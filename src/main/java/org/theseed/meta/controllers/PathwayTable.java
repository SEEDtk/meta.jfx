/**
 *
 */
package org.theseed.meta.controllers;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Pathway;
import org.theseed.metabolism.Pathway.Element;
import org.theseed.metabolism.Reaction;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
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
     * This is the cell factory class for reaction ID cells.  These require special rendering because
     * reversed reactions have their ID shown in italics.
     */
    public class BiggIdDisplayCallback implements Callback<TableColumn<Element, String>, TableCell<Element, String>> {

        @Override
        public TableCell<Element, String> call(TableColumn<Element, String> param) {
            return new BiggIdCell();
        }

    }

    /**
     * This object represents a reaction ID cell.  Reaction IDs have the text italicized if they are
     * reversed, so they require a custom display.
     */
    public class BiggIdCell extends TableCell<Pathway.Element, String> {

        @Override
        public void updateItem(String bigg_id, boolean empty) {
            super.updateItem(bigg_id, empty);
            this.setText(null);
            if (empty)
                this.setGraphic(null);
            else {
                Pathway.Element element = this.getTableRow().getItem();
                String reaction_id = element.getReaction().getBiggId();
                Text reactionText = new Text(reaction_id);
                if (element.isReversed()) {
                    Font myFont = reactionText.getFont();
                    reactionText.setFont(Font.font(myFont.getName(), FontPosture.ITALIC, myFont.getSize()));
                    reactionText.setUnderline(true);
                }
                this.setGraphic(reactionText);
            }
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
                List<String> formulaParts = element.getReaction().getParsedFormula(element.isReversed());
                // This will be the graphic we display.
                TextFlow flow = new TextFlow();
                // Get the input and output elements.  These are bolded.
                Element prev = PathwayTable.this.path.getPrevious(element);
                String input_id = (prev == null ? PathwayTable.this.path.getInput() : prev.getOutput());
                String output_id = element.getOutput();
                // Build the text flow from the parts of the formula.
                final int nParts = formulaParts.size();
                for (int i = 0; i < nParts; i += 2) {
                    // The connector is simply text.
                    flow.getChildren().add(new Text(formulaParts.get(i)));
                    int i1 = i + 1;
                    // Some formulae have nothing on the right of the connector.  For these we use an X.
                    if (i1 >= nParts)
                        flow.getChildren().add(new Text("X"));
                    else {
                        String compound = formulaParts.get(i1);
                        Text compoundText = new Text(compound);
                        // If this is a special compound, bold it.
                        if (compound.equals(input_id) || compound.equals(output_id)) {
                            Font myFont = compoundText.getFont();
                            compoundText.setFont(Font.font(myFont.getName(),  FontWeight.BOLD, myFont.getSize()));
                            compoundText.setUnderline(true);
                        }
                        // Install a tooltip for the text that gives the compound's full name.
                        String outputName = PathwayTable.this.model.getCompoundName(compound);
                        Tooltip nameTip = new Tooltip(outputName);
                        Tooltip.install(compoundText, nameTip);
                        // Add the compound to the formula.
                        flow.getChildren().add(compoundText);
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
        this.table.setFixedCellSize(30);
        // Create the table columns.
        this.numColumn = new TableColumn<Pathway.Element, Integer>("#");
        this.numColumn.setPrefWidth(30);
        this.numColumn.setCellValueFactory((e) -> new SimpleIntegerProperty(e.getValue().getSeqNum()).asObject());
        this.idColumn = new TableColumn<Pathway.Element, String>("Reaction");
        this.idColumn.setPrefWidth(100);
        this.idColumn.setCellFactory(this.new BiggIdDisplayCallback());
        this.nameColumn = new TableColumn<Pathway.Element, String>("Name");
        this.nameColumn.setPrefWidth(300);
        this.nameColumn.setCellValueFactory((e) -> new SimpleStringProperty(e.getValue().getReaction().getName()));
        this.ruleColumn = new TableColumn<Pathway.Element, String>("Rule");
        this.ruleColumn.setPrefWidth(200);
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
