/**
 *
 */
package org.theseed.meta.finders;

import java.io.IOException;

import org.theseed.metabolism.Pathway;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonException;

/**
 * This method is used to create a pathway with the starting path at the end instead of the beginning.
 * (Sometimes this is needed.)
 *
 * @author Bruce Parrello
 *
 */
public class BackfillPathFinder extends PathFinder {

    // FIELDS
    /** saved starting path */
    private Pathway path2;

    public BackfillPathFinder(IParms processor) throws ParseFailureException, IOException, JsonException {
        super(processor);
        this.path2 = processor.getStartPathway();
        if (this.path2 == null)
            throw new ParseFailureException("Path backfill request cancelled by user.");
        if (this.getNumLeft() < 1)
            throw new ParseFailureException("At least one metabolite is required to backfill a path.");
    }

    @Override
    public Pathway computePath() {
        // Get the first two compounds and build a pathway from them.
        String start = this.nextCompound();
        String end = this.nextCompound();
        this.showStatus("Computing pathway from " + start + " to " + end + ".");
        Pathway retVal = this.getModel().getPathway(start, end);
        // Finish the path through the rest of the compounds.
        retVal = this.finishPath(retVal);
        if (retVal != null) {
            // Extend the pathway to the input compound of the starting path.
            this.showStatus("Extending computed pathway to " + path2.toString() + ".");
            retVal = this.getModel().extendPathway(retVal, path2.getInput());
            // Now append the starting path to the path being constructed.
            retVal.append(path2);
        }
        return retVal;
    }

}
