/**
 *
 */
package org.theseed.meta.controllers;

/**
 * This is a simple interface that provides a method to find a compound descriptor given a compound ID.
 *
 * @author Bruce Parrello
 *
 */
public interface ICompoundFinder {

    /**
     * @return the compound descriptor for the specified BiGG ID
     *
     * @param id	ID of the desired compound
     */
    public MetaCompound getCompound(String id);

}
