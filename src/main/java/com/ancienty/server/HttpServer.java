package com.ancienty.server;

import com.ancienty.database.Database;
import com.ancienty.database.ServerDatabase;
import com.ancienty.server.model.InvoiceUploadData;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 2 HTTP Server implementation with proper JSON/XML libraries
 * Uses Gson for JSON and Jackson for XML processing
 * Enhanced with better error handling and debugging
 */
public class HttpServer {
    
    private final Database database;
    private final ServerConfig config;
    private com.sun.net.httpserver.HttpServer server;
    
    // Proper library instances
    private final Gson gson;
    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;

    public HttpServer(Database database, ServerConfig config) {
        this.database = database;
        this.config = config;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.jsonMapper = new ObjectMapper();
        this.xmlMapper = new XmlMapper();
        
        // Configure to ignore unknown properties
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void start() throws IOException {
        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(config.getHttpPort()), 0);
        
        // Register endpoints
        server.createContext(config.getUploadInvoiceEndpoint(), new UploadInvoiceHandler());
        server.createContext(config.getQueryInvoiceEndpoint(), new QueryInvoiceHandler());
        
        server.setExecutor(null); // Use default executor
        server.start();
        
        System.out.println("HTTP Server started on port " + config.getHttpPort());
        System.out.println("Endpoints:");
        System.out.println("  POST " + config.getUploadInvoiceEndpoint());
        System.out.println("  GET  " + config.getQueryInvoiceEndpoint());
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("HTTP Server stopped");
        }
    }

    /**
     * POST /UploadInvoice
     * Accepts parameters 'tür' ('xml' or 'json') and 'fatura' (payload)
     * Uses proper libraries to parse the data
     */
    private class UploadInvoiceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                // Parse form data
                String requestBody = readRequestBody(exchange);
                System.out.println("Received upload request body: " + requestBody); // Debug logging
                
                Map<String, String> params = parseFormData(requestBody);
                System.out.println("Parsed parameters: " + params); // Debug logging
                
                String tur = params.get("tür");
                String fatura = params.get("fatura");
                
                if (tur == null || fatura == null) {
                    System.err.println("Missing parameters - tür: " + tur + ", fatura: " + (fatura != null ? "present" : "null"));
                    sendResponse(exchange, 400, "Fatura Kaydedilemedi");
                    return;
                }
                
                // Parse invoice data using proper libraries
                InvoiceUploadData.UploadSystem uploadData;
                try {
                    if ("xml".equals(tur)) {
                        uploadData = xmlMapper.readValue(fatura, InvoiceUploadData.UploadSystem.class);
                    } else if ("json".equals(tur)) {
                        uploadData = gson.fromJson(fatura, InvoiceUploadData.UploadSystem.class);
                    } else {
                        System.err.println("Invalid format type: " + tur);
                        sendResponse(exchange, 400, "Fatura Kaydedilemedi");
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing invoice data (" + tur + "): " + e.getMessage());
                    e.printStackTrace();
                    sendResponse(exchange, 400, "Fatura Kaydedilemedi");
                    return;
                }
                
                if (uploadData == null || uploadData.customer == null || uploadData.invoiceData == null) {
                    System.err.println("Invalid upload data structure");
                    sendResponse(exchange, 400, "Fatura Kaydedilemedi");
                    return;
                }
                
                // Save to database
                System.out.println("HTTP Upload - Attempting to save to database");
                if (saveInvoiceToDatabase(uploadData)) {
                    System.out.println("HTTP Upload - Successfully saved to database");
                    sendResponse(exchange, 200, "Fatura Kaydedildi");
                } else {
                    System.err.println("HTTP Upload - Failed to save to database");
                    sendResponse(exchange, 500, "Fatura Kaydedilemedi - Database Error");
                }
                
            } catch (Exception e) {
                System.err.println("Error in UploadInvoice: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Fatura Kaydedilemedi");
            }
        }
    }

    /**
     * GET /QueryInvoice  
     * Supports 'seri' and 'liste' query types
     * Returns data in proper XML/JSON format
     */
    private class QueryInvoiceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                // Parse query parameters
                String queryString = exchange.getRequestURI().getQuery();
                System.out.println("Received query string: " + queryString); // Debug logging
                
                Map<String, String> params = parseQueryString(queryString);
                System.out.println("Parsed query parameters: " + params); // Debug logging
                
                String tur = params.get("tur");
                System.out.println("Query type (tur): '" + tur + "'"); // Debug logging
                
                if ("seri".equals(tur)) {
                    handleSeriesQuery(exchange, params);
                } else if ("name".equals(tur)) {
                    handleNameQuery(exchange, params);
                } else if ("liste".equals(tur)) {
                    handleListQuery(exchange, params);
                } else {
                    System.err.println("Invalid query type received: '" + tur + "'");
                    System.err.println("Available types: 'seri', 'name', 'liste'");
                    sendResponse(exchange, 400, "Geçersiz sorgu türü: " + tur);
                }
                
            } catch (Exception e) {
                System.err.println("Error in QueryInvoice: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Sorgu hatası");
            }
        }
        
        private void handleSeriesQuery(HttpExchange exchange, Map<String, String> params) throws IOException {
            String seri = params.get("seri");
            String no = params.get("no");
            
            System.out.println("Series query - seri: " + seri + ", no: " + no);
            
            if (seri == null || no == null) {
                sendResponse(exchange, 400, "Seri ve numara gerekli");
                return;
            }
            
            Database.Invoice invoice = database.getInvoiceBySeriesAndNumber(seri, no);
            if (invoice == null) {
                System.out.println("Invoice not found: " + seri + "-" + no);
                sendResponse(exchange, 404, "Kayıt bulunamadı");
                return;
            }
            
            // Return only total amount as specified in documentation
            String response = String.format("%.2f", invoice.totalAfter);
            sendResponse(exchange, 200, response);
        }
        
        private void handleNameQuery(HttpExchange exchange, Map<String, String> params) throws IOException {
            String name = params.get("name");
            
            System.out.println("Name query - name: " + name);
            
            if (name == null || name.trim().isEmpty()) {
                sendResponse(exchange, 400, "Müşteri adı gerekli");
                return;
            }
            
            Database.Invoice[] invoices = database.getInvoicesByCustomerName(name);
            if (invoices.length == 0) {
                System.out.println("No invoices found for customer: " + name);
                sendResponse(exchange, 404, "Kayıt bulunamadı");
                return;
            }
            
            // Return total amounts of all invoices for this customer
            StringBuilder response = new StringBuilder();
            double totalSum = 0;
            for (Database.Invoice invoice : invoices) {
                if (response.length() > 0) {
                    response.append("\n");
                }
                response.append(String.format("Fatura %s-%s: %.2f TL", 
                    invoice.series, invoice.number, invoice.totalAfter));
                totalSum += invoice.totalAfter;
            }
            response.append(String.format("\nToplam: %.2f TL", totalSum));
            
            sendResponse(exchange, 200, response.toString());
        }
        
        private void handleListQuery(HttpExchange exchange, Map<String, String> params) throws IOException {
            String[] invoiceList = database.getInvoiceList();
            StringBuilder response = new StringBuilder();
            response.append("Fatura Listesi:\n");
            for (String invoice : invoiceList) {
                response.append(invoice).append("\n");
            }
            sendResponse(exchange, 200, response.toString());
        }
    }

    // Helper methods
    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toString("UTF-8");
        }
    }

    private Map<String, String> parseFormData(String data) {
        Map<String, String> params = new HashMap<>();
        if (data == null || data.isEmpty()) return params;
        
        String[] pairs = data.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    String key = URLDecoder.decode(keyValue[0], "UTF-8");
                    String value = URLDecoder.decode(keyValue[1], "UTF-8");
                    params.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    System.err.println("Error decoding form data pair: " + pair);
                }
            }
        }
        return params;
    }
    
    private Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    String key = URLDecoder.decode(keyValue[0], "UTF-8");
                    String value = URLDecoder.decode(keyValue[1], "UTF-8");
                    params.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    System.err.println("Error decoding query parameter: " + pair);
                }
            }
        }
        return params;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        sendResponse(exchange, statusCode, response, "text/plain; charset=UTF-8");
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // Allow CORS
        byte[] responseBytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private boolean saveInvoiceToDatabase(InvoiceUploadData.UploadSystem uploadData) {
        try {
            // Get customer and invoice data
            InvoiceUploadData.Customer customer = uploadData.customer;
            InvoiceUploadData.InvoiceData invoiceData = uploadData.invoiceData;
            
            // Find or create customer
            int customerId = findOrCreateCustomer(customer.name, String.valueOf(customer.ssn), customer.isCompany());
            
            // Ensure items exist and convert to map
            Map<String, Double> itemQuantities = new HashMap<>();
            for (InvoiceUploadData.Item item : invoiceData.items) { // Using descriptive field name
                if (!itemExists(item.name)) {
                    database.addItem(item.name, item.unitPrice);
                }
                itemQuantities.put(item.name, item.quantity);
            }
            
            // Save invoice with HTTP source tracking if using ServerDatabase
            if (database instanceof ServerDatabase) {
                return ((ServerDatabase) database).saveInvoiceWithSource(invoiceData.seri, invoiceData.number, customerId, 
                                                                        invoiceData.discount, itemQuantities, "HTTP");
            } else {
                return database.saveInvoice(invoiceData.seri, invoiceData.number, customerId, 
                                           invoiceData.discount, itemQuantities);
            }
        } catch (Exception e) {
            System.err.println("Error saving invoice to database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private int findOrCreateCustomer(String name, String ssn, boolean isCompany) {
        // Try to find existing customer by SSN
        String[] customers = database.getCustomers();
        for (String customerStr : customers) {
            if (customerStr.equals("Yeni Müşteri")) continue;
            
            // Parse customer ID
            String idStr = customerStr.substring(1, customerStr.indexOf(")"));
            int customerId = Integer.parseInt(idStr);
            Database.Customer customer = database.getCustomer(customerId);
            
            if (customer != null && customer.ssn.equals(ssn)) {
                return customer.id;
            }
        }
        
        // Customer doesn't exist, create new one
        database.addCustomer(name, ssn, isCompany);
        
        // Find the newly created customer
        customers = database.getCustomers();
        for (String customerStr : customers) {
            if (customerStr.equals("Yeni Müşteri")) continue;
            
            String idStr = customerStr.substring(1, customerStr.indexOf(")"));
            int customerId = Integer.parseInt(idStr);
            Database.Customer customer = database.getCustomer(customerId);
            
            if (customer != null && customer.ssn.equals(ssn)) {
                return customer.id;
            }
        }
        
        return -1; // Error case
    }
    
    private boolean itemExists(String itemName) {
        Map<String, Double> items = database.getItems();
        return items.containsKey(itemName);
    }
    
    private InvoiceUploadData.UploadSystem createUploadSystemFromInvoice(Database.Invoice invoice, List<Database.InvoiceItem> items) {
        // Create customer
        String customerType = invoice.isCompany ? "SIRKET" : "SAHIS";
        InvoiceUploadData.Customer customer = new InvoiceUploadData.Customer(
            invoice.customerName, invoice.customerSsn, customerType);
        
        // Create items
        java.util.List<InvoiceUploadData.Item> itemList = new java.util.ArrayList<>();
        for (Database.InvoiceItem item : items) {
            itemList.add(new InvoiceUploadData.Item(item.name, item.quantity, item.unitPrice, item.lineTotal));
        }
        
        // Create invoice data using updated field names  
        InvoiceUploadData.InvoiceData invoiceData = new InvoiceUploadData.InvoiceData(
            invoice.series, invoice.number, itemList, invoice.totalBefore, invoice.discount, invoice.totalAfter);
        
        return new InvoiceUploadData.UploadSystem(customer, invoiceData);
    }
} 