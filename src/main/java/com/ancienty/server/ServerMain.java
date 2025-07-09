package com.ancienty.server;

import com.ancienty.database.Database;
import com.ancienty.database.ServerDatabase;

/**
 * Phase 2 Server Main - Headless server application
 * Starts both HTTP and TCP servers as specified in Phase 2
 */
public class ServerMain {
    
    private static HttpServer httpServer;
    private static TcpServer tcpServer;
    private static Database database;

    public static void main(String[] args) {
        System.out.println("=== Hugin Invoice System - Phase 2 Server ===");
        System.out.println("Headless server with HTTP and TCP APIs");
        System.out.println();

        try {
            // Load configuration
            ServerConfig config = new ServerConfig();
            System.out.println("Configuration loaded:");
            System.out.println("  HTTP Port: " + config.getHttpPort());
            System.out.println("  TCP Host: " + config.getTcpHost());
            System.out.println("  TCP Port: " + config.getTcpPort());
            System.out.println("  Database: " + config.getDatabasePath());
            System.out.println();

            // Initialize server database (separate from client database with enhanced schema)
            database = new ServerDatabase(config.getDatabasePath());
            System.out.println("Server database initialized: " + config.getDatabasePath());

            // Start HTTP server
            httpServer = new HttpServer(database, config);
            httpServer.start();

            // Start TCP server
            tcpServer = new TcpServer(database, config);
            tcpServer.start();

            System.out.println();
            System.out.println("=== Server Started Successfully ===");
            System.out.println("HTTP Server: http://localhost:" + config.getHttpPort());
            System.out.println("TCP Server: " + config.getTcpHost() + ":" + config.getTcpPort());
            
            // Show database statistics if using ServerDatabase
            if (database instanceof ServerDatabase) {
                ServerDatabase serverDb = (ServerDatabase) database;
                var stats = serverDb.getInvoiceStatsBySource();
                System.out.println();
                System.out.println("=== Database Statistics ===");
                System.out.println("Total invoices by source:");
                stats.forEach((source, count) -> System.out.println("  " + source + ": " + count));
                
                String[] recent = serverDb.getRecentInvoices();
                if (recent.length > 0) {
                    System.out.println();
                    System.out.println("Recent invoices:");
                    for (int i = 0; i < Math.min(5, recent.length); i++) {
                        System.out.println("  " + recent[i]);
                    }
                }
            }
            
            System.out.println();
            System.out.println("Press Ctrl+C to stop the server");

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down servers...");
                if (httpServer != null) {
                    httpServer.stop();
                }
                if (tcpServer != null) {
                    tcpServer.stop();
                }
                System.out.println("Server shutdown complete");
            }));

            // Keep the main thread alive
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 