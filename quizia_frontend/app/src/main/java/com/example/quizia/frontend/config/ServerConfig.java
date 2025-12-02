package com.example.quizia.frontend.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ServerConfig {
    private static final String CONFIG_FILE = "quizia-config.properties";
    private static final String DEFAULT_BACKEND_URL = "https://quizia-backend-operation.onrender.com";
    
    private static String backendUrl;
    
    static {
        loadConfig();
    }
    
    private static void loadConfig() {
        Properties props = new Properties();
        Path configPath = Paths.get(System.getProperty("user.home"), ".quizia", CONFIG_FILE);
        
        // Try to load from user home directory
        if (Files.exists(configPath)) {
            try (InputStream input = new FileInputStream(configPath.toFile())) {
                props.load(input);
                backendUrl = props.getProperty("backend.url", DEFAULT_BACKEND_URL);
                return;
            } catch (IOException e) {
                System.err.println("Error loading config from " + configPath + ": " + e.getMessage());
            }
        }
        
        // Use default
        backendUrl = DEFAULT_BACKEND_URL;
        
        // Create config file if it doesn't exist
        try {
            Files.createDirectories(configPath.getParent());
            Properties defaultProps = new Properties();
            defaultProps.setProperty("backend.url", DEFAULT_BACKEND_URL);
            try (OutputStream output = new FileOutputStream(configPath.toFile())) {
                defaultProps.store(output, "Quizia Configuration\nEdit backend.url to point to your server");
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not create config file: " + e.getMessage());
        }
    }
    
    public static String getBackendUrl() {
        return backendUrl;
    }
    
    public static void setBackendUrl(String url) {
        backendUrl = url;
    }
}
