package com.ancienty.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Phase 2 Server Configuration
 * Reads configuration from server-config.json
 */
public class ServerConfig {
    
    public static class Config {
        public ServerSettings server;
    }
    
    public static class ServerSettings {
        public HttpSettings http;
        public TcpSettings tcp;
        public DatabaseSettings database;
    }
    
    public static class HttpSettings {
        public int port;
        public EndpointSettings endpoints;
    }
    
    public static class EndpointSettings {
        public String uploadInvoice;
        public String queryInvoice;
    }
    
    public static class TcpSettings {
        public String host;
        public int port;
    }
    
    public static class DatabaseSettings {
        public String path;
    }
    
    private Config config;
    
    public ServerConfig() {
        loadConfig();
    }
    
    private void loadConfig() {
        try {
            Gson gson = new GsonBuilder().create();
            try (FileReader reader = new FileReader("server-config.json")) {
                config = gson.fromJson(reader, Config.class);
                if (config == null || config.server == null) {
                    setDefaultConfig();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
            // Use default values
            setDefaultConfig();
        }
    }
    
    private void setDefaultConfig() {
        config = new Config();
        config.server = new ServerSettings();
        config.server.http = new HttpSettings();
        config.server.http.port = 8080;
        config.server.http.endpoints = new EndpointSettings();
        config.server.http.endpoints.uploadInvoice = "/UploadInvoice";
        config.server.http.endpoints.queryInvoice = "/QueryInvoice";
        config.server.tcp = new TcpSettings();
        config.server.tcp.host = "localhost";
        config.server.tcp.port = 8888;
        config.server.database = new DatabaseSettings();
        config.server.database.path = "server_database.db"; // Fixed: Use server database, not client database
    }
    
    // Configuration parsing now handled by Gson - no manual parsing needed
    
    // Getters
    public int getHttpPort() { return config.server.http.port; }
    public String getUploadInvoiceEndpoint() { return config.server.http.endpoints.uploadInvoice; }
    public String getQueryInvoiceEndpoint() { return config.server.http.endpoints.queryInvoice; }
    public String getTcpHost() { return config.server.tcp.host; }
    public int getTcpPort() { return config.server.tcp.port; }
    public String getDatabasePath() { return config.server.database.path; }
} 