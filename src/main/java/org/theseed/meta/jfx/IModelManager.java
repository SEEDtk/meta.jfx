/**
 *
 */
package org.theseed.meta.jfx;

import java.util.Collection;
import java.util.List;

import org.theseed.meta.controllers.MetaCompound;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Pathway;
import org.theseed.shared.meta.IProgressReporter;

/**
 * This interface defines the common parameters action classes need from the
 * model manager.
 *
 * @author Bruce Parrello
 *
 */
public interface IModelManager extends IProgressReporter {

    /**
     * @return the list of compounds to process, in order
     */
    public List<MetaCompound> getCompounds();

    /**
     * @return the underlying metabolic model
     */
    public MetaModel getModel();

    /**
     * @return the single starting pathway (can be NULL)
     */
    public Pathway getStartPathway();

    /**
     * @return the collection of subsystem pathways (can be NULL)
     */
    public Collection<Pathway> getSubsysPathways();


}
