/**
 *
 */
package org.theseed.meta.finders;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.theseed.basic.ParseFailureException;
import org.theseed.metabolism.Pathway;

import com.github.cliftonlabs.json_simple.JsonException;

/**
 * This is a variation of the subsystem path finder that finds the shortest extension of a subsystem path.
 * The insures that the subsystem is maximally utilized, rather than the standard path finder, which is still
 * looking for the shortest path.
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemExtensionFinder extends BaseSubsystemFinder {

    public SubsystemExtensionFinder(IParms processor) throws ParseFailureException, IOException, JsonException {
        super(processor);
    }

    @Override
    protected Map.Entry<Pathway, Pathway> findBest(Map<Pathway, Pathway> outputs) {
        // Find the key (output) path with the shortest length.
        Iterator<Map.Entry<Pathway, Pathway>> iter = outputs.entrySet().iterator();
        // We are guaranteed not to be called if the map is empty.
        var retVal = iter.next();
        while (iter.hasNext()) {
            var curr = iter.next();
            int cmp = curr.getKey().size() - retVal.getKey().size();
            if (cmp < 0) {
                // Here the current path is strictly shorter.
                retVal = curr;
            } else if (cmp == 0) {
                // Here the paths are the same size.  Choose the one with the shortest incoming path.
                if (curr.getValue().size() < retVal.getValue().size())
                    retVal = curr;
            }
        }
        return retVal;
    }


}
