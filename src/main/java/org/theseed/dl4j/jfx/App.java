package org.theseed.dl4j.jfx;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.jfx.PreferenceSet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

/**
 * JavaFX Version of the DL4J GUI application
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        TrainingManager trainer = (TrainingManager) PreferenceSet.loadFXML("TrainingManager", stage);
        trainer.init();
        stage.show();
    }

    public static void main(String[] args) {
        // Configure logging.
        Level logLevel = Level.ERROR;
        if (args.length >= 1 && args[0].contentEquals("-v")) logLevel = Level.INFO;
        LoggerContext logging = (LoggerContext) LoggerFactory.getILoggerFactory();
        logging.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(logLevel);
        launch();
    }

}
