/**
 *
 */
package org.theseed.meta.finders;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
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
public class SubsystemPathFinder extends BaseSubsystemFinder {

    public SubsystemPathFinder(IParms processor) throws ParseFailureException, IOException, JsonException {
        super(processor);
    }

    @Override
    protected Entry<Pathway, Pathway> findBest(Map<Pathway, Pathway> outputs) {
        // Find the total path with the shortest length.
        Iterator<Map.Entry<Pathway, Pathway>> iter = outputs.entrySet().iterator();
        // We are guaranteed not to be called if the map is empty.
        var retVal = iter.next();
        var bestLen = retVal.getKey().size() + retVal.getValue().size();
        while (iter.hasNext()) {
            var curr = iter.next();
            int currLen = curr.getValue().size() + curr.getKey().size();
            if (currLen < bestLen) {
                retVal = curr;
                bestLen = currLen;
            }
        }
        return retVal;
    }


}
