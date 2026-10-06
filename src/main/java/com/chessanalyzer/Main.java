package com.chessanalyzer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("Chess Game Analyzer");
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        loadScreen("/fxml/Main.fxml");
        stage.show();
    }

    /** Loads the given FXML as the current scene's root and returns its controller. */
    public static <T> T loadScreen(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, 900, 640);
            scene.getStylesheets().add(
                    Objects.requireNonNull(Main.class.getResource("/css/style.css")).toExternalForm());
            primaryStage.setScene(scene);
            return loader.getController();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load screen: " + fxmlPath, e);
        }
    }

    /**
     * Shows a programmatically built view (such as the analysis board) as the current scene,
     * using the application stylesheets, and makes sure the window is large enough for it.
     */
    public static void showView(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(
                Objects.requireNonNull(Main.class.getResource("/css/style.css")).toExternalForm());
        scene.getStylesheets().add(
                Objects.requireNonNull(Main.class.getResource("/css/board.css")).toExternalForm());
        primaryStage.setScene(scene);
        if (primaryStage.getWidth() < width) primaryStage.setWidth(width);
        if (primaryStage.getHeight() < height) primaryStage.setHeight(height);
        primaryStage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
