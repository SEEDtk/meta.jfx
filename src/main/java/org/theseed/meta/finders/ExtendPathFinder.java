/**
 *
 */
package org.theseed.meta.finders;

import java.io.IOException;

import org.theseed.basic.ParseFailureException;
import org.theseed.metabolism.Pathway;

import com.github.cliftonlabs.json_simple.JsonException;

/**
 * This finder extends an existing path through the list of metabolites.
 *
 * @author Bruce Parrello
 *
 */
public class ExtendPathFinder extends PathFinder {

    // FIELDS
    /** starting pathway */
    private Pathway path1;

    public ExtendPathFinder(IParms processor) throws ParseFailureException, IOException, JsonException {
        super(processor);
        this.path1 = processor.getStartPathway();
        if (this.path1 == null)
            throw new ParseFailureException("Path extension request cancelled by user.");
        if (this.getNumLeft() < 1)
            throw new ParseFailureException("At least one metabolite is required to extend a path.");
    }

    @Override
    public Pathway computePath() {
        return this.finishPath(this.path1);
    }

}
