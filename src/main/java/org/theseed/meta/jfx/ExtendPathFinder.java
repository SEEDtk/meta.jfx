/**
 *
 */
package org.theseed.meta.jfx;

import java.io.IOException;

import org.theseed.metabolism.Pathway;
import org.theseed.utils.ParseFailureException;

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
        Pathway startPath = processor.getStartPathway();
        if (startPath == null)
            throw new ParseFailureException("Path extension request cancelled by user.");
        if (this.getNumLeft() < 1)
            throw new ParseFailureException("At least one metabolite is required to extend a path.");
    }

    @Override
    public Pathway computePath() {
        return this.finishPath(this.path1);
    }

}
