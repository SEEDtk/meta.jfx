/**
 *
 */
package org.theseed.meta.jfx;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.meta.controllers.MetaCompound;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Pathway;
import org.theseed.metabolism.PathwayFilter;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonException;

/**
 * This object manages the search for a metabolic pathway.  It allows customizing the search using
 * a path type.
 *
 * @author Bruce Parrello
 *
 */
public abstract class PathFinder {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(PathFinder.class);
    /** controlling command processor */
    private IParms processor;
    /** underlying model */
    private MetaModel model;
    /** pathway filters */
    private PathwayFilter[] filters;
    /** list of compounds to process */
    private Iterator<MetaCompound> compoundIter;
    /** number of compounds to process */
    private int compoundCount;
    /** TRUE if the pathway should be looped */
    private boolean wantLoop;

    /**
     * This interface defines the parameters the client must have available to the path finder.  Often,
     * these depend on the path finder type.
     */
    public interface IParms {

        /**
         * @return the list of compounds to process, in order
         */
        public List<MetaCompound> getCompounds();

        /**
         * @return the array of pathway filters
         */
        public PathwayFilter[] getFilters();

        /**
         * @return the underlying metabolic model
         */
        public MetaModel getModel();

        /**
         * @return a collection of starting pathways (can be NULL)
         */
        public Collection<Pathway> getStartPathways();

        /**
         * @return the single starting pathway (can be NULL)
         */
        public Pathway getStartPathway();

       /**
         * Send a status message to the client.
         *
         * @param message	message to send
         */
        public void showMessage(String message);

        /**
         * @return TRUE if the path should be looped
         */
        public boolean getLoopFlag();

    }

    /**
     * This enumeration describes the types of path searches.
     */
    public static enum Type {
        /** perform a normal, straight-line search */
        NORMAL {
            @Override
            public PathFinder create(IParms processor) throws ParseFailureException, IOException, JsonException {
                return new NormalPathFinder(processor);
            }
        },
        /** extend the saved starting path through the specified metabolites */
        EXTEND {
            @Override
            public PathFinder create(IParms processor) throws ParseFailureException, IOException, JsonException {
                return new ExtendPathFinder(processor);
            }
        },
        /** find the best extension of one of the subsystem paths through the specified metabolites */
        SUBSYTEM {
            @Override
            public PathFinder create(IParms processor) throws ParseFailureException, IOException, JsonException {
                return new SubsystemPathFinder(processor);
            }
        };

        /**
         * @return a path finder for this type of search.
         *
         * @param processor		controlling command processor
         *
         * @throws ParseFailureException
         * @throws IOException
         * @throws JsonException
         */
        public abstract PathFinder create(IParms processor)
                throws ParseFailureException, IOException, JsonException;
    }

    /**
     * Construct a path finder.
     *
     * @param processor		controlling command processor
     *
     * @throws IOException
     * @throws ParseFailureException
     * @throws JsonException
     */
    public PathFinder(IParms processor) throws ParseFailureException, IOException, JsonException {
        this.processor = processor;
        this.model = processor.getModel();
        this.compoundIter = processor.getCompounds().iterator();
        this.compoundCount = processor.getCompounds().size();
        this.filters = processor.getFilters();
        this.wantLoop = processor.getLoopFlag();
    }

    /**
     * @return the desired path
     *
     * @param iter		iterator through the list of compounds; should be used for all compound retrieval
     */
    public abstract Pathway computePath();

    /**
     * This is a workhorse method that extends the current pathway through the remaining compounds
     * in the iterator.  Pretty much all of the subclasses will end with this after starting the
     * path.
     */
    protected Pathway finishPath(Pathway path) {
        // Save the current path.
        Pathway retVal = path;
        // Loop through the rest of the metabolites.
        while (path != null && this.compoundIter.hasNext()) {
            String next = nextCompound();
            this.showStatus("Extending pathway to " + next + ".");
            retVal = getModel().extendPathway(retVal, next, this.filters);
        }
        // Check for a looped path.
        if (retVal != null && this.wantLoop) {
            this.showStatus("Looping pathway back to " + retVal.getInput() + ".");
            retVal = getModel().loopPathway(retVal, this.filters);
        }
        return path;
    }

    /**
     * @return the BiGG ID of the next compound to process
     */
    protected String nextCompound() {
        this.compoundCount--;
        return this.compoundIter.next().getId();
    }

    /**
     * @return the number of compounds remaining in the compound list
     */
    protected int getNumLeft() {
        return this.compoundCount;
    }

    /**
     * Ask the controlling processor to display a status message.
     *
     * @param string	message to display
     */
    protected void showStatus(String string) {
        this.processor.showMessage(string);
    }

    /**
     * @return the model that should contain the path
     */
    protected MetaModel getModel() {
        return model;
    }

    /**
     * @return the pathway search filters
     */
    protected PathwayFilter[] getFilters() {
        return this.filters;
    }

}
