/**
 *
 */
package org.theseed.meta.finders;

import java.io.File;
import java.io.IOException;

import org.theseed.meta.jfx.IModelManager;
import org.theseed.metabolism.Pathway;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonException;

/**
 * This class manages a subsystem update.  There are two types of updates, depending on
 * whether we are starting from a compound or a path.
 *
 * @author Bruce Parrello
 *
 */
public abstract class SubsystemBuilder extends CompoundListAction {

    // FIELDS
    /** subsystem directory */
    private File subDir;

    /**
     * This interface defines the parameters needed by the subsystem builders.
     */
    public interface IParms extends IModelManager {

        /**
         * @return the name of the subsystem directory (can be NULL)
         */
        public File getSubsysDirectory();

    }

    /**
     * This enum specifies the different types of subsystem builders.
     */
    public static enum Type {
        /** each path goes from the first compound to one of the others */
        SIMPLE {
            @Override
            public SubsystemBuilder create(IParms processor) throws IOException, JsonException, ParseFailureException {
                return new SimpleSubsystemBuilder(processor);
            }
        },
        /** each path extends from the input path to one of the compounds */
        PATH {
            @Override
            public SubsystemBuilder create(IParms processor) throws IOException, JsonException, ParseFailureException {
                return new PathSubsystemBuilder(processor);
            }
        },
        /** each path consists solely of a single starting compound */
        POINT {
            @Override
            public SubsystemBuilder create(IParms processor) throws IOException, JsonException, ParseFailureException {
                return new PointSubsystemBuilder(processor);
            }
        };

        /**
         * @return a subsystem builder of this type
         *
         * @param processor		controlling command processor
         *
         * @throws IOException
         * @throws JsonException
         * @throws ParseFailureException
         */
        public abstract SubsystemBuilder create(IParms processor)
                throws IOException, JsonException, ParseFailureException;
    }

    /**
     * Construct a subsystem builder.
     *
     * @param processor		controlling command processor
     */
    public SubsystemBuilder(IParms processor) throws IOException, JsonException, ParseFailureException {
        super(processor);
        this.subDir = processor.getSubsysDirectory();
    }

    /**
     * Update the selected subsystem.
     *
     * @return TRUE if successful, FALSE if the user cancelled
     *
     * @throws IOException
     */
    public boolean updateSubsystem() throws IOException {
        boolean retVal = false;
        if (this.subDir != null) {
            // Here we have a subsystem to update.
            int count = 0;
            // Loop through the remaining compounds, generating pathways.
            while (this.hasNextCompound()) {
                String target = this.nextCompound();
                Pathway path = this.getPath(target);
                if (path == null)
                    throw new IOException("Could not create a path for " + target + ".");
                // Store the path in the subsystem.
                File outFile = new File(this.subDir, target + Pathway.FILE_EXT);
                path.save(outFile);
                this.showStatus("Pathway saved to " + outFile.getName() + ".");
                count++;
            }
            this.showStatus(String.format("%d pathways updated in %s.", count, this.subDir));
            retVal = true;
        }
        return retVal;
    }

    /**
     * Generate the subsystem pathway for a specified target.
     *
     * @param target	target compound for the pathway
     *
     * @return the pathway computed
     */
    protected abstract Pathway getPath(String target);

}
