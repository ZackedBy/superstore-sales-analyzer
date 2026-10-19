package com.salesanalysis.superstore;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Launches the SuperStore Sales Analyzer window. */
public class MainApp extends Application {

  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("sales_data_view.fxml"));
    if (loader.getLocation() == null) {
      throw new IOException("Sales view was not found on the classpath.");
    }
    stage.setTitle("SuperStore Sales Analyzer");
    stage.setScene(new Scene(loader.load()));
    stage.setMinWidth(960);
    stage.setMinHeight(600);
    stage.setWidth(1280);
    stage.setHeight(720);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
