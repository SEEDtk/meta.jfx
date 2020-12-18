/**
 *
 */
package org.theseed.jfx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.utils.ICommand;

import javafx.concurrent.Task;

/**
 * This object
 * @author Bruce Parrello
 *
 */
public class BackgroundTask extends Task<Boolean> {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(BackgroundTask.class);
    /** parent context for error messages */
    private IBackgroundController parent;
    /** processor to run */
    private ICommand processor;

    /**
     * Construct this thread to run the specified processor.
     *
     * @param parent		parent window manager
     * @param processor		command object to run
     */
    public BackgroundTask(IBackgroundController parent, ICommand processor) {
        this.processor = processor;
        this.parent = parent;
    }

    /**
     * This method runs in the background thread.
     */
    @Override
    protected Boolean call() throws Exception {
        boolean retVal = false;
        try {
            // Run the command.
            processor.run();
            retVal = true;
        } catch (Exception e) {
            log.error("Error running command.", e);
        }
        parent.recordCompletion(retVal);
        return retVal;
    }

}
