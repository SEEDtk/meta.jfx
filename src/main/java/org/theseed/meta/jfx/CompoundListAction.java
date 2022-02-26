/**
 *
 */
package org.theseed.meta.jfx;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.meta.controllers.MetaCompound;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.PathwayFilter;

/**
 * This is the base class for actions that operate on the main list of compounds.  It is
 * generally subclassed two layers deep-- one for the type of action and one for the specific
 * action being taken.  All such actions require working with a starting pathway or the first
 * few compounds and then iterating through the rest.
 *
 * @author Bruce Parrello
 *
 */
public class CompoundListAction {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(PathFinder.class);
    /** underlying model */
    private MetaModel model;
    /** pathway filters */
    private PathwayFilter[] filters;
    /** list of compounds to process */
    private Iterator<MetaCompound> compoundIter;
    /** number of compounds to process */
    private int compoundCount;
    /** controlling command processor */
    private IModelManager processor;

    /**
     * Construct a compound list action for a specified controlling command processor.
     *
     * @parma processor		controlling command processor
     */
    public CompoundListAction(IModelManager processor) {
        this.processor = processor;
        this.model = processor.getModel();
        this.compoundIter = processor.getCompounds().iterator();
        this.compoundCount = processor.getCompounds().size();
        this.filters = processor.getFilters();
    }

    /**
     * @return the BiGG ID of the next compound to process
     */
    protected String nextCompound() {
        this.compoundCount--;
        return this.compoundIter.next().getId();
    }

    /**
     * @return the number of compounds remaining in the compound list
     */
    protected int getNumLeft() {
        return this.compoundCount;
    }

    /**
     * @return TRUE if there are more compounds left to process
     */
    protected boolean hasNextCompound() {
        return this.compoundIter.hasNext();
    }

    /**
     * @return the model that should contain the path
     */
    protected MetaModel getModel() {
        return model;
    }

    /**
     * @return the pathway search filters
     */
    protected PathwayFilter[] getFilters() {
        return this.filters;
    }

    /**
     * Ask the controlling processor to display a status message.
     *
     * @param string	message to display
     */
    protected void showStatus(String string) {
        this.processor.showMessage(string);
    }

}
