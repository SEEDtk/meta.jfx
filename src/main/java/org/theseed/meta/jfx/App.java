package org.theseed.meta.jfx;

import javafx.application.Application;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.jfx.BaseController;

/**
 * JavaFX Version of the DL4J GUI application
 */
public class App extends Application {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(App.class);


    /**
      * This is an exception handler that tries to give us good error information when we
      * are getting a weird error outside of normal channels.
      */
    public class Handler implements UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            // Try to log the full stack trace.
            try (StringWriter writer = new StringWriter();
                    PrintWriter printer = new PrintWriter(writer)) {
                e.printStackTrace(printer);
                log.error(writer.toString());
            } catch (IOException e2) {
                // Out of memory, we will still write the minimal message.
            }
            BaseController.messageBox(AlertType.ERROR, "Java Error", e.toString());
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        ModelManager manager = (ModelManager) BaseController.loadFXML(App.class, "ModelManager", stage);
        Thread.setDefaultUncaughtExceptionHandler(new Handler());
        manager.init();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
