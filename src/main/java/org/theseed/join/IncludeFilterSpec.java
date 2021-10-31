/**
 *
 */
package org.theseed.join;

import java.util.Set;

/**
 * This is a filtering operation.  We eliminate records on the left that do not have a matching
 * key on the right.
 *
 * @author Bruce Parrello
 *
 */
public class IncludeFilterSpec extends FilterSpec {

    @Override
    protected boolean keep(String key, Set<String> keys) {
        return keys.contains(key);
    }

}
