/**
 *
 */
package org.theseed.meta.jfx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

/**
 * This is the main class.  In order to run under Maven, we cannot have a main class that
 * derives from Application.
 *
 * @author Bruce Parrello
 *
 */
public class Main {

    public static void main(String[] args) {
        // Configure logging.
        Level logLevel = Level.ERROR;
        if (args.length >= 1 && args[0].contentEquals("-v")) logLevel = Level.INFO;
        LoggerContext logging = (LoggerContext) LoggerFactory.getILoggerFactory();
        logging.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(logLevel);
        // Launch the application.
        App.main(args);
    }

}
