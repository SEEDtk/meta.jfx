/**
 *
 */
package org.theseed.meta.controllers;

import org.apache.commons.lang3.StringUtils;

import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * This object contains a compound name and BiGG ID.  It is used to represent a compound in an observable
 * list.  The toString displays both the name and ID, and there is a filtering method for selecting a
 * subset based on a search string.
 *
 * @author Bruce Parrello
 *
 */
public class MetaCompound implements Comparable<MetaCompound> {

    // FIELDS
    /** name of compound */
    private String name;
    /** ID of compound */
    private String id;

    /**
     * Construct a new metabolic compound object.
     *
     * @param id		BiGG ID of compound
     * @param name		name of compound
     */
    public MetaCompound(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * @return TRUE if this compound should be found by the specified search string
     *
     * @param string	search string for filtering, converted to lower-case
     */
    public boolean matches(String string) {
        return StringUtils.contains(this.name.toLowerCase(), string) || StringUtils.startsWith(this.id.toLowerCase(), string);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MetaCompound)) {
            return false;
        }
        MetaCompound other = (MetaCompound) obj;
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.id + ": " + this.name;
    }

    @Override
    public int compareTo(MetaCompound o) {
        // We order by name and then ID.
        int retVal = this.name.compareTo(o.name);
        if (retVal == 0)
            retVal = this.id.compareTo(o.id);
        return retVal;
    }

    /**
     * @return the BiGG ID of this compound
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return the name of this compound
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the rendering of this compound
     */
    public Node getRendering() {
        // We pre-generate the display representation for performance.
        Text idText = new Text(this.id);
        Font defaultFont = idText.getFont();
        idText.setFont(Font.font(defaultFont.getName(), FontWeight.BOLD, defaultFont.getSize()));
        idText.setUnderline(true);
        Text nameText = new Text(": " + this.name);
        return new TextFlow(idText, nameText);
    }

}
