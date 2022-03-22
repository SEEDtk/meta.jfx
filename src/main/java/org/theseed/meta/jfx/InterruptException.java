/**
 *
 */
package org.theseed.meta.jfx;

/**
 * This is an exception class for an unchecked exception caused by a user demanding
 * early termination of a search operation.
 *
 * @author Bruce Parrello
 *
 */
public class InterruptException extends RuntimeException {

    /** version number for serialization */
    private static final long serialVersionUID = 560104364511681582L;

    /**
     * Construct an interrupt exception with a default message.
     */
    public InterruptException() {
        super("Operation terminated by user request.");
    }

}
