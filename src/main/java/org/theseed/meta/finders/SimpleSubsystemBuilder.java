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
 * builds the subsystem from the rest of the compounds, in order.
 *
 * @author Bruce Parrello
 *
 */
public class SimpleSubsystemBuilder extends SubsystemBuilder {

    // FIELDS
    /** starting compound */
    private Pathway current;

    public SimpleSubsystemBuilder(IParms processor) throws IOException, JsonException, ParseFailureException {
        super(processor);
        if (this.getNumLeft() < 2)
            throw new ParseFailureException("At least two compounds required to build a subsystem without a starting path.");
        this.current = new Pathway(this.nextCompound());
    }

    @Override
    protected Pathway getPath(String target) {
        Pathway retVal = this.getModel().extendPathway(this.current, target);
        this.current = retVal;
        return retVal;
    }

}
