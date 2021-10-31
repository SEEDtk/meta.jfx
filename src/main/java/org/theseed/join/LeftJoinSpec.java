/**
 *
 */
package org.theseed.join;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * This is a standard join specification, but all records from the left side are kept.  When a
 * record is missing from the right side, the columns are filled with empty strings.
 *
 * @author Bruce Parrello
 *
 */
public class LeftJoinSpec extends RealJoinSpec {

    @Override
    protected void processMissing(Iterator<Entry<String, List<String>>> iter, List<String> data, int width) {
        // Add empty columns.
        for (int i = 0; i < width; i++)
            data.add("");
    }
}
