/**
 *
 */
package org.theseed.jfx;

/**
 * This is an extended version of IController that provides a method for
 * a background task to communicate completion to the main thread.
 *
 * @author Bruce Parrello
 *
 */
public interface IBackgroundController extends IController {

    /**
     * Record the termination of the background task.
     *
     * @param success	TRUE if it was successful, else FALSE
     */
    public void recordCompletion(boolean success);
}
