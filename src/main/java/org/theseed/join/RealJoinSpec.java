/**
 *
 */
package org.theseed.join;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.theseed.io.KeyedFileMap;

/**
 * This is the base class for the two join specifications with input files.  The only difference is
 * the way in which they treat missing keys in the input file.
 *
 * @author Bruce Parrello
 *
 */
public abstract class RealJoinSpec extends JoinSpec {

    // FIELDS
    /** map of input records by key */
    private Map<String, List<String>> inputMap;

    @Override
    protected void initialConfigure() {
        // Insure we have an input map.
        this.inputMap = new HashMap<String, List<String>>(100);
    }

    @Override
    protected void processLine(KeyedFileMap keyedMap, String key, List<String> data) {
        // Add this record to the input map.
        this.inputMap.put(key, data);
    }

    @Override
    protected void finishFile(KeyedFileMap keyedMap, int width) {
        // Now we iterate through the output map and apply the keyed map.
        Iterator<Map.Entry<String, List<String>>> iter = keyedMap.iterator();
        while (iter.hasNext()) {
            Map.Entry<String, List<String>> current = iter.next();
            String key = current.getKey();
            List<String> data = current.getValue();
            // Is there an input record for this key?
            List<String> newRecord = this.inputMap.get(key);
            if (newRecord == null) {
                // No, do special processing.
                this.processMissing(iter, data, width);
            } else {
                // Yes, add the input columns.
                data.addAll(newRecord);
            }
        }
        // Clear the input map for next time.
        this.inputMap.clear();
    }

    /**
     * Processing a missing record in the input file.
     *
     * @param iter		iterator through the output map
     * @param data		data portion of the output record
     * @param width		number of data columns in the input file
     */
    protected abstract void processMissing(Iterator<Entry<String, List<String>>> iter, List<String> data, int width);

}
