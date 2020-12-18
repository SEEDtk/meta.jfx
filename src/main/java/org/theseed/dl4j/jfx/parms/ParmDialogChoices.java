/**
 *
 */
package org.theseed.dl4j.jfx.parms;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.theseed.io.ParmDescriptor;

import javafx.event.EventHandler;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

/**
 * This control group manages an enumeration parameter.  The user chooses one or more possible values using checkboxes.
 *
 * @author Bruce Parrello
 */
public class ParmDialogChoices extends ParmDialogGroup {

    // FIELDS
    /** list of legal values */
    private List<String> possibilities;
    /** set of selected values */
    private Set<String> selected;
    /** map of option names to checkboxes */
    private Map<String, CheckBox> checkBoxMap;

    // CONTROLS

    /** flow pane containing the checkboxes */
    private FlowPane container;

    /**
     * Create a parameter dialog group for an enum type.
     *
     * @param parent	grid pane to contain the control group
     * @param row		index of the row to contain the controls
     * @param desc		parameter descriptor
     * @param values	array of possible choices
     */
    public ParmDialogChoices(GridPane parent, int row, ParmDescriptor desc, Enum<?>[] values) {
        this.possibilities = Arrays.stream(values).map(x -> x.name()).collect(Collectors.toList());
        this.selected = new TreeSet<String>(Arrays.asList(StringUtils.split(desc.getValue(), ", ")));
        this.checkBoxMap = new TreeMap<String, CheckBox>();
        init(parent, row, desc);
    }

    @Override
    protected Region createMainControl() {
        // Create the checkbox tool tip.
        Tooltip note = new Tooltip("SHIFT-click to select all, CTRL-click to select only one.");
        // Create the checkboxes and put then in the map.
        for (String choice : possibilities) {
            CheckBox choiceBox = new CheckBox(choice);
            this.checkBoxMap.put(choice, choiceBox);
            choiceBox.setSelected(selected.contains(choice));
            choiceBox.setOnMouseClicked(new ClickListener(choice));
            Tooltip.install(choiceBox, note);
        }
        // The main control is a flow pane.  Here we specify gap values of 5.
        this.container = new FlowPane(5, 5);
        this.container.getChildren().addAll(this.checkBoxMap.values());
        return this.container;
    }

    @Override
    protected void initMainControl() {
    }

    /**
     * Update the parm value to reflect the state of the checkboxes.
     */
    public void updateValue() {
        getDescriptor().setValue(selected.stream().collect(Collectors.joining(", ")));
    }

    /**
     * Update the checkboxes to match the current state of the selected set.
     */
    public void fixCheckBoxes() {
        for (Map.Entry<String, CheckBox> chkEntry : this.checkBoxMap.entrySet())
            chkEntry.getValue().setSelected(this.selected.contains(chkEntry.getKey()));
    }


    /**
     * This is the click event handler.  A single click will change the checkbox and this needs to be
     * reflected in the parm descriptor.  A ctrl-click unchecks everything but the current choice
     * and a shift-click checks everything.
     *
     * @author Bruce Parrello
     *
     */
    public class ClickListener implements EventHandler<MouseEvent> {

        private String choice;

        public ClickListener(String choice) {
            this.choice = choice;
        }

        @Override
        public void handle(MouseEvent event) {
            if (event.isControlDown()) {
                // Here we are selecting only this checkbox.
                selected.clear();
                selected.add(this.choice);
                fixCheckBoxes();
            } else if (event.isShiftDown()) {
                // Here we are selecting everything.
                selected.addAll(possibilities);
                fixCheckBoxes();
            } else {
                // Here we are toggling the current checkbox.
                if (checkBoxMap.get(choice).isSelected()) {
                    // The box is toggled on.
                    selected.add(choice);
                } else {
                    // The box is toggled off.
                    selected.remove(choice);
                }
            }
            // Update the parameter value.
            updateValue();
        }

    }


}
