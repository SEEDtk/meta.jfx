/**
 *
 */
package org.theseed.meta.finders;

/**
 * This interface describes a handler to be invoked when a background task is complete.
 *
 * @author Bruce Parrello
 *
 */
public interface IEndHandler {

    /**
     * Process the completion of a task.
     */
    public void handleCompletion();

}
