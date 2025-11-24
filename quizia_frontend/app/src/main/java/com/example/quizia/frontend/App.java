
package com.example.quizia.frontend;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.example.quizia.frontend.util.FxmlLoaderUtil;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FxmlLoaderUtil.load("/fxml/auth.fxml");
        primaryStage.setTitle("Quizia");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}