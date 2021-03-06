/**
 *
 */
package org.theseed.meta.controllers;

import org.theseed.genome.Feature;
import org.theseed.meta.jfx.App;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Reaction;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * This class represents a feature that triggers a reaction.  The reaction can be either a
 * mainline reaction or a branch.  It is identified by the feature name and the reaction ID.
 * Triggers are sorted with mainline triggers first and then branch triggers.
 *
 * @author Bruce Parrello
 *
 */
public abstract class ReactionTrigger implements Comparable<ReactionTrigger> {

    // FIELDS
    /** reaction being triggered */
    private Reaction reaction;
    /** feature triggering the reaction */
    private Feature feat;
    /** gene name of the feature, for sorting */
    private String sortName;
    /** weight of this reaction */
    private double weight;
    /** code for an unnamed trigger (must sort to the end) */
    private static final String NO_NAME = "~peg";

    /**
     * Construct a reaction trigger.
     *
     * @param fid		feature ID of the trigger
     * @param reaction	reaction being triggered
     * @param model		underlying model for the reaction
     */
    public ReactionTrigger(String fid, Reaction reaction, MetaModel model, double weight) {
        this.reaction = reaction;
        this.feat = model.getBaseGenome().getFeature(fid);
        this.sortName = this.feat.getGeneName();
        this.weight = weight;
        // Sort un-named features to the end, all else being equal.
        if (this.sortName.isEmpty())
            this.sortName = NO_NAME;
    }

    @Override
    public int compareTo(ReactionTrigger o) {
        // Sort by weight first, with highest winning.
        int retVal = Double.compare(o.weight, this.weight);
        if (retVal == 0) {
            // Now sort by visual components.
            retVal = this.getTarget().compareTo(o.getTarget());
            if (retVal == 0) {
                retVal = this.sortName.compareTo(o.sortName);
                if (retVal == 0)
                    retVal = this.feat.compareTo(o.feat);
            }
        }
        return retVal;
    }

    /**
     * @return the BiGG ID of the reaction being triggered
     */
    public String getReactionId() {
        return this.reaction.getBiggId();
    }

    /**
     * @return the type index used to sort this reaction by type (mainline before branch)
     */
    protected abstract int getTypeIdx();

    /**
     * @return the icon to display for this trigger
     */
    protected abstract Node getIcon();


    @Override
    public String toString() {
        StringBuilder retVal = new StringBuilder(100);
        retVal.append(this.getTarget());
        retVal.append(" <== ");
        retVal.append(this.feat.getId());
        if (! this.sortName.equals(NO_NAME))
            retVal.append(" (").append(this.sortName).append(")");
        retVal.append(": ");
        retVal.append(feat.getPegFunction());
        return retVal.toString();
    }

    /**
     * @return the string to display for the trigger target
     */
    protected abstract String getTarget();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.feat == null) ? 0 : this.feat.hashCode());
        result = prime * result + ((this.reaction == null) ? 0 : this.reaction.hashCode());
        result = prime * result + this.getTypeIdx();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ReactionTrigger)) {
            return false;
        }
        ReactionTrigger other = (ReactionTrigger) obj;
        if (this.feat == null) {
            if (other.feat != null) {
                return false;
            }
        } else if (!this.feat.equals(other.feat)) {
            return false;
        }
        if (this.reaction == null) {
            if (other.reaction != null) {
                return false;
            }
        } else if (!this.reaction.equals(other.reaction)) {
            return false;
        }
        if (this.getTypeIdx() != other.getTypeIdx()) {
            return false;
        }
        return true;
    }


    /**
     * This class represents a trigger for a mainline reaction.
     */
    public static class Main extends ReactionTrigger {

        /** icon for a mainline trigger */
        private static Image MAIN_ICON = new Image(App.class.getResourceAsStream("plus-16.png"));

        public Main(String fid, Reaction reaction, MetaModel model, double weight) {
            super(fid, reaction, model, weight);
        }

        @Override
        protected int getTypeIdx() {
            return 0;
        }

        @Override
        protected Node getIcon() {
            return new ImageView(MAIN_ICON);
        }

        @Override
        protected String getTarget() {
            return this.getReactionId();
        }

    }

    /**
     * This class represents a trigger for a branching reaction.
     */
    public static class Branch extends ReactionTrigger {

        /** icon for a mainline trigger */
        private static Image BRANCH_ICON = new Image(App.class.getResourceAsStream("minus-16.png"));
        /** compound being consumed by this branch */
        private String compound;

        /**
         * Create a reaction trigger for a branch reaction.
         *
         * @param fid		feature ID triggering the reaction
         * @param reaction	reaction being triggered
         * @param model		underlying model
         * @param compound	BiGG ID of the compound being consumed
         * @param weight	weight of the reaction
         */
        public Branch(String fid, Reaction reaction, MetaModel model, String compound, double weight) {
            super(fid, reaction, model, weight);
            this.compound = compound;
        }

        @Override
        protected int getTypeIdx() {
            return 1;
        }

        @Override
        protected Node getIcon() {
            return new ImageView(BRANCH_ICON);
        }

        @Override
        protected String getTarget() {
            return this.compound;
        }

    }

}
