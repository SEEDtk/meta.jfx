/**
 *
 */
package org.theseed.meta.finders;

import java.io.IOException;

import org.theseed.metabolism.Pathway;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonException;

/**
 * This builder creates a subsystem of compounds with no pre-built pathways.  When using this subsystem,
 * the shortest path is found from one of the compounds to the target, and then the path is backfilled
 * to the desired starting point.
 *
 * @author Bruce Parrello
 *
 */
public class PointSubsystemBuilder extends SubsystemBuilder {

    public PointSubsystemBuilder(IParms processor) throws IOException, JsonException, ParseFailureException {
        super(processor);
    }

    @Override
    protected Pathway getPath(String target) {
        return new Pathway(target);
    }

}
