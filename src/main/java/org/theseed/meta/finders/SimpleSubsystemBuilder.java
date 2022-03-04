/**
 *
 */
package org.theseed.meta.finders;

import java.io.IOException;

import org.theseed.metabolism.Pathway;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonException;

/**
 * This subsystem builder uses the first compound in the compound list as a starting point and
 * builds the subsystem from the rest of the compounds.
 *
 * @author Bruce Parrello
 *
 */
public class SimpleSubsystemBuilder extends SubsystemBuilder {

    // FIELDS
    /** starting compound */
    private String start;

    public SimpleSubsystemBuilder(IParms processor) throws IOException, JsonException, ParseFailureException {
        super(processor);
        if (this.getNumLeft() < 2)
            throw new ParseFailureException("At least two compounds required to build a subsystem without a starting path.");
        this.start = this.nextCompound();
    }

    @Override
    protected Pathway getPath(String target) {
        return this.getModel().getPathway(start, target);
    }

}
