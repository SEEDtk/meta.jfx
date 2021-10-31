/**
 *
 */
package org.theseed.join;

import java.util.Set;

/**
 * This is an exclusion filter step.  We remove records on the left that DO have a matching
 * key on the right.
 *
 * @author Bruce Parrello
 *
 */
public class ExcludeFilterSpec extends FilterSpec {

    @Override
    protected boolean keep(String key, Set<String> keys) {
        return ! keys.contains(key);
    }


}
