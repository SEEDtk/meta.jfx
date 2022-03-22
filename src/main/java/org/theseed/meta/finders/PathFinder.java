/**
 *
 */
package org.theseed.meta.finders;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.meta.jfx.IModelManager;
import org.theseed.metabolism.Pathway;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonException;

import javafx.concurrent.Task;

/**
 * This object manages the search for a metabolic pathway.  It allows customizing the search using
 * a path type.
 *
 * @author Bruce Parrello
 *
 */
public abstract class PathFinder extends CompoundListAction {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(PathFinder.class);
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
        SUBSYSTEM {
            @Override
            public PathFinder create(IParms processor) throws ParseFailureException, IOException, JsonException {
                return new SubsystemPathFinder(processor);
            }
        },
        /** find the most direct extension of one of the subsystem paths through the specified metabolites */
        SUBSYSPATH {
            @Override
            public PathFinder create(IParms processor) throws ParseFailureException, IOException, JsonException {
                return new SubsystemExtensionFinder(processor);
            }
        },
        /** perform a normal, straight-line search and then append the starting path */
        BACKFILL {
            @Override
            public PathFinder create(IParms processor) throws ParseFailureException, IOException, JsonException {
                return new BackfillPathFinder(processor);
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
        processor.getModel().setReporter(processor);
        this.wantLoop = processor.getLoopFlag();
    }

    /**
     * Find a pathway.  This method may be run in the background.
     *
     * @param iter		iterator through the list of compounds; should be used for all compound retrieval
     *
     * @return the desired path, or NULL if none was found
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
        while (retVal != null && this.hasNextCompound()) {
            String next = nextCompound();
            this.showStatus("Extending pathway to " + next + ".");
            retVal = getModel().extendPathway(retVal, next);
        }
        // Check for a looped path.
        if (retVal != null && this.wantLoop) {
            this.showStatus("Looping pathway back to " + retVal.getInput() + ".");
            retVal = getModel().loopPathway(retVal);
        }
        return retVal;
    }

    /**
     * This object runs the path search in the background.  Note that the status message should not be
     * overridden after it is done, since it may contain error information.
     */
    public class Runner extends Task<Boolean> {

        /** pathway found */
        private Pathway path;

        @Override
        protected Boolean call() throws Exception {
            boolean retVal = false;
            try {
                this.path = PathFinder.this.computePath();
                if (this.path == null) {
                    PathFinder.this.showStatus("Could not find requested pathway.");
                    PathFinder.this.showProgress(0.0);
                } else {
                    PathFinder.this.showStatus("Pathway contains " + Integer.toString(this.path.size())
                            + " reactions.");
                    PathFinder.this.showProgress(1.0);
                    retVal = true;
                }
            } catch (Exception e) {
                PathFinder.this.showStatus("Error: " + e.toString());
                this.path = null;
            }
            // Denote this task is done.  To avoid a race condition, we save the result immediately.
            PathFinder.this.showCompleted();
            return retVal;
        }

        /**
         * @return the path found
         */
        public Pathway getResult() {
            return this.path;
        }

    }

}
