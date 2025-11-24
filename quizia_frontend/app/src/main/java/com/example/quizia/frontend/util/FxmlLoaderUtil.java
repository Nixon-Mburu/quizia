package com.example.quizia.frontend.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.io.IOException;

public class FxmlLoaderUtil {
    public static Parent load(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(FxmlLoaderUtil.class.getResource(fxmlPath));
        return loader.load();
    }
}