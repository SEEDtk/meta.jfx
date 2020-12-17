package org.theseed.dl4j.jfx;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

import org.theseed.jfx.PreferenceSet;

/**
 * JavaFX Version of the DL4J GUI application
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        PreferenceSet.loadFXML("TrainingManager", stage);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}
