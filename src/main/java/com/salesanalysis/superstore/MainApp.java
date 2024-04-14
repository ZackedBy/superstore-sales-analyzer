package com.salesanalysis.superstore;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The main class for the SuperStore Sales Analyzer application.
 * This class launches the JavaFX application and loads the sales data view.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass()
                .getResource("sales_data_view.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setScene(scene);
        stage.setTitle("SuperStore Sales Analyzer");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
