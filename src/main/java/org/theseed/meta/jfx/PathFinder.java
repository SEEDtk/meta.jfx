/**
 *
 */
package org.theseed.meta.jfx;

import java.io.IOException;
import org.theseed.metabolism.Pathway;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonException;

/**
 * This object manages the search for a metabolic pathway.  It allows customizing the search using
 * a path type.
 *
 * @author Bruce Parrello
 *
 */
public abstract class PathFinder extends CompoundListAction {

    /** TRUE if the pathway should be looped */
    private boolean wantLoop;

    /**
     * This interface defines the parameters the client must have available to the path finder.  Often,
     * these depend on the path finder type.
     */
    public interface IParms extends IModelManager {

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
        super(processor);
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
        while (path != null && this.hasNextCompound()) {
            String next = nextCompound();
            this.showStatus("Extending pathway to " + next + ".");
            retVal = getModel().extendPathway(retVal, next, this.getFilters());
        }
        // Check for a looped path.
        if (retVal != null && this.wantLoop) {
            this.showStatus("Looping pathway back to " + retVal.getInput() + ".");
            retVal = getModel().loopPathway(retVal, this.getFilters());
        }
        return retVal;
    }

}
