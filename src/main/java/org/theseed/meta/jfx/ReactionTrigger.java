/**
 *
 */
package org.theseed.meta.jfx;

/**
 * This class represents a feature that triggers a reaction.  The reaction can be either a
 * mainline reaction or a branch.  It is identified by the feature name and the reaction ID.
 * Triggers are sorted with mainline triggers first and then branch triggers.
 *
 * @author Bruce Parrello
 *
 */
public class ReactionTrigger implements Comparable<ReactionTrigger> {

    // FIELDS
    // TODO data members for ReactionTrigger

    /**
     *
     */
    public ReactionTrigger() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public int compareTo(ReactionTrigger o) {
        // TODO code for compareTo
        return 0;
    }

    // TODO methods for ReactionTrigger; need toString, hashCode, equals
}
