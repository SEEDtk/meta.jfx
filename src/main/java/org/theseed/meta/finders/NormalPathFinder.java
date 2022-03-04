/**
 *
 */
package org.theseed.meta.finders;

import java.io.IOException;

import org.theseed.metabolism.Pathway;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonException;

/**
 * The normal path finder simply finds a pathway that runs through all the input metabolites.
 *
 * @author Bruce Parrello
 *
 */
public class NormalPathFinder extends PathFinder {

    public NormalPathFinder(IParms processor) throws ParseFailureException, IOException, JsonException {
        super(processor);
        if (this.getNumLeft() < 2)
            throw new ParseFailureException("At least two compounds required to find a pathway.");
    }

    @Override
    public Pathway computePath() {
        // Get the first two compounds and build a pathway from them.
        String start = this.nextCompound();
        String end = this.nextCompound();
        this.showStatus("Computing pathway from " + start + " to " + end + ".");
        Pathway path1 = this.getModel().getPathway(start, end);
        // Finish the path through the rest of the compounds.
        Pathway retVal = this.finishPath(path1);
        return retVal;
    }

}
