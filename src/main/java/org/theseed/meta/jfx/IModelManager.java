/**
 *
 */
package org.theseed.meta.jfx;

import java.util.Collection;
import java.util.List;

import org.theseed.meta.controllers.MetaCompound;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Pathway;
import org.theseed.metabolism.PathwayFilter;

/**
 * This interface defines the common parameters action classes need from the
 * model manager.
 *
 * @author Bruce Parrello
 *
 */
public interface IModelManager {

    /**
     * @return the list of compounds to process, in order
     */
    public List<MetaCompound> getCompounds();

    /**
     * @return the array of pathway filters
     */
    public PathwayFilter[] getFilters();

    /**
     * @return the underlying metabolic model
     */
    public MetaModel getModel();

    /**
     * @return the single starting pathway (can be NULL)
     */
    public Pathway getStartPathway();

   /**
     * Send a status message to the client.
     *
     * @param message	message to send
     */
    public void showMessage(String message);

    /**
     * @return the collection of subsystem pathways (can be NULL)
     */
    public Collection<Pathway> getSubsysPathways();


}
