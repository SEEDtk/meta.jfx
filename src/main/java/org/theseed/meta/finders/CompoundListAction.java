/**
 *
 */
package org.theseed.meta.finders;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.meta.controllers.MetaCompound;
import org.theseed.meta.jfx.IModelManager;
import org.theseed.metabolism.MetaModel;

/**
 * This is the base class for actions that operate on the main list of compounds.  It is
 * generally subclassed two layers deep-- one for the type of action and one for the specific
 * action being taken.  All such actions require working with a starting pathway or the first
 * few compounds and then iterating through the rest.
 *
 * Compound list actions take place in the background, so the GUI needs to be frozen against
 * modification while it runs.
 *
 * @author Bruce Parrello
 *
 */
public abstract class CompoundListAction {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(PathFinder.class);
    /** underlying model */
    private MetaModel model;
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
     * Display a status message on the controlling GUI model manager.
     *
     * @param msg		message to display
     */
    public void showStatus(String msg) {
        this.processor.showStatus(msg);
    }

    /**
     * Display progress on the controlling GUI model manager.
     *
     * @param p			progress fraction, from 0 (no progress) to 1 (completed)
     */
    public void showProgress(double p) {
        this.processor.showProgress(p);
    }

    /**
     * Record completion of the background task.
     */
    public void showCompleted() {
        this.processor.showCompleted();
    }

}
