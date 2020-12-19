package org.theseed.dl4j.jfx;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

import org.theseed.jfx.BaseController;

/**
 * JavaFX Version of the DL4J GUI application
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        TrainingManager trainer = (TrainingManager) BaseController.loadFXML("TrainingManager", stage);
        trainer.init();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
