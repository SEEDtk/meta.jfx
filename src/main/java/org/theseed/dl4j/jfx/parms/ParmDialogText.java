/**
 *
 */
package org.theseed.dl4j.jfx.parms;

import org.theseed.io.ParmDescriptor;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

/**
 * This is a parameter edit control group for a text parameter.
 *
 * @author Bruce Parrello
 *
 */
public class ParmDialogText extends ParmDialogGroup {

    // CONTROLS

    /** text box containing parameter value */
    private TextField txtMainValue;

    /**
     * Create this parameter dialog group.
     *
     * @param parent		parent grid pane
     * @param row			index of the grid row for this parameter
     * @param descriptor	parameter descriptor
     */
    public ParmDialogText(GridPane parent, int row, ParmDescriptor descriptor) {
        init(parent, row, descriptor);
    }

    @Override
    protected Region createMainControl() {
        this.txtMainValue = new TextField(this.getDescriptor().getValue());
        return this.txtMainValue;
    }

    @Override
    protected void initMainControl() {
        // Here we add a listener to keep the descriptor up to date.
        this.txtMainValue.textProperty().addListener(this.new TextListener());
    }

    /**
     * This is the change listener for the text value.
     */
    private class TextListener implements ChangeListener<String> {

        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            ParmDialogText.this.getDescriptor().setValue(newValue);
        }

    }

}
