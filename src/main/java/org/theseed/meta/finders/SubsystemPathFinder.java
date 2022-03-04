/**
 *
 */
package org.theseed.meta.finders;

import java.io.IOException;
import java.util.Collection;

import org.theseed.metabolism.Pathway;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonException;

/**
 * This finder starts from a set of pathways belonging to a subsystem, and finds the shortest pathway out of
 * the subsystem through the specified metabolites.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemPathFinder extends PathFinder {

    // FIELDS
    /** collection of subsystem pathways */
    private Collection<Pathway> subsysPaths;

    public SubsystemPathFinder(IParms processor) throws ParseFailureException, IOException, JsonException {
        super(processor);
        // Get the subsystem pathways.
        this.subsysPaths = processor.getSubsysPathways();
        if (this.subsysPaths == null) {
            // Here the user cancelled out.
            throw new ParseFailureException("A subsystem is required to do a subsystem path search.");
        } else if (this.subsysPaths.size() < 1) {
            // Here we have a subsystem, but it has no paths in it.
            throw new ParseFailureException("Cannot do a search using an empty subsystem.");
        } else if (this.getNumLeft() < 1) {
            // Here the is no compound in the compound list.
            throw new ParseFailureException("At least one metabolite is required for the search.");
        }
    }

    @Override
    public Pathway computePath() {
        // Get the first compound.
        String goal = this.nextCompound();
        // Build the starting path.
        Pathway path1 = this.getModel().findPathway(this.subsysPaths, goal);
        // Extend it through whatever metabolites are left.
        Pathway retVal = this.finishPath(path1);
        return retVal;
    }

}
