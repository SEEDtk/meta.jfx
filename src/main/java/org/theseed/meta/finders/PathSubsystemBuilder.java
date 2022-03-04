/**
 *
 */
package org.theseed.meta.finders;

import java.io.IOException;

import org.theseed.metabolism.Pathway;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonException;

/**
 * This class builds a subsystem using a stored path as the starting point.  The stored path is
 * extended to each compound in the compound list.
 *
 * @author Bruce Parrello
 *
 */
public class PathSubsystemBuilder extends SubsystemBuilder {

    // FIELDS
    /** starting pathway */
    private Pathway path1;

    public PathSubsystemBuilder(IParms processor) throws IOException, JsonException, ParseFailureException {
        super(processor);
        this.path1 = processor.getStartPathway();
        if (this.path1 == null)
            throw new ParseFailureException("No starting pathway available for subsystem build.");
    }

    @Override
    protected Pathway getPath(String target) {
        return this.getModel().extendPathway(path1, target);
    }

}
