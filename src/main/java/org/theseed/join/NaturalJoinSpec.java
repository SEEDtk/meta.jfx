/**
 *
 */
package org.theseed.join;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * This is a natural join.  It performs both a filtering operation (removing keys on the left
 * not found in the right) and a concatenation (adding fields on the right to the data on the left).
 *
 * @author Bruce Parrello
 *
 */
public class NaturalJoinSpec extends RealJoinSpec {

    @Override
    protected void processMissing(Iterator<Entry<String, List<String>>> iter, List<String> data, int width) {
        // In a natural join, if there are no records in the input, the key is deleted from the output map.
        iter.remove();
    }

}
