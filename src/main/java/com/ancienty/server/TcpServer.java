package com.ancienty.server;

import com.ancienty.database.Database;
import com.ancienty.database.ServerDatabase;
import com.ancienty.server.model.InvoiceUploadData;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Phase 2 TCP Server implementation with CORRECT BINARY PROTOCOL
 * 
 * Protocol Format (as specified in requirements):
 * - 2 bytes: Message Length (total message size including command and type)
 * - 1 byte: Command (1=UploadInvoice, 2=QueryInvoice)
 * - 1 byte: Type (1=XML/series, 2=JSON/name)
 * - (Message Length - 1) bytes: Message Content
 * 
 * Response Format:
 * - Same 2 bytes: Message Length
 * - Same 1 byte: Command 
 * - Same 1 byte: Type
 * - Response content
 */
public class TcpServer {
    
    // Protocol constants
    private static final int COMMAND_UPLOAD_INVOICE = 1;
    private static final int COMMAND_QUERY_INVOICE = 2;
    
    private static final int TYPE_XML_OR_SERIES = 1;
    private static final int TYPE_JSON_OR_NAME = 2;
    
    private final Database database;
    private final ServerConfig config;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running = false;
    
    // Proper library instances
    private final Gson gson;
    private final XmlMapper xmlMapper;

    public TcpServer(Database database, ServerConfig config) {
        this.database = database;
        this.config = config;
        this.executor = Executors.newCachedThreadPool();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.xmlMapper = new XmlMapper();
        
        // Configure to ignore unknown properties
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(config.getTcpPort());
        running = true;
        
        System.out.println("TCP Server started on " + config.getTcpHost() + ":" + config.getTcpPort());
        System.out.println("Using BINARY protocol as specified in Phase 2 requirements");
        System.out.println("Commands: 1=UploadInvoice, 2=QueryInvoice");
        System.out.println("Types: 1=XML/Series, 2=JSON/Name");
        
        // Accept connections in background
        executor.submit(this::acceptConnections);
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (executor != null) {
                executor.shutdown();
            }
            System.out.println("TCP Server stopped");
        } catch (IOException e) {
            System.err.println("Error stopping TCP server: " + e.getMessage());
        }
    }

    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting TCP connection: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (DataInputStream input = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream())) {
            
            System.out.println("TCP Client connected: " + clientSocket.getRemoteSocketAddress());
            
            // Read binary message according to protocol
            BinaryMessage request = readBinaryMessage(input);
            if (request == null) {
                System.err.println("Failed to read binary message from client");
                return;
            }
            
            System.out.println("Received binary message:");
            System.out.println("  Message Length: " + request.messageLength);
            System.out.println("  Command: " + request.command + " (" + getCommandName(request.command) + ")");
            System.out.println("  Type: " + request.type + " (" + getTypeName(request.command, request.type) + ")");
            System.out.println("  Content Length: " + request.content.length());
            
            // Process the message
            String responseContent = processMessage(request);
            
            // Send binary response with same command and type
            sendBinaryResponse(output, request.command, request.type, responseContent);
            
        } catch (IOException e) {
            System.err.println("Error handling TCP client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }

    private BinaryMessage readBinaryMessage(DataInputStream input) throws IOException {
        try {
            // Read 2 bytes for message length
            int messageLength = input.readUnsignedShort();
            
            // Read 1 byte for command
            int command = input.readUnsignedByte();
            
            // Read 1 byte for type
            int type = input.readUnsignedByte();
            
            // Read message content (messageLength - 1 bytes, since command and type are included in length)
            int contentLength = messageLength - 1; // Subtract 1 byte for command + type
            if (contentLength < 0) {
                System.err.println("Invalid message length: " + messageLength);
                return null;
            }
            
            byte[] contentBytes = new byte[contentLength];
            input.readFully(contentBytes);
            
            String content = new String(contentBytes, StandardCharsets.UTF_8);
            
            return new BinaryMessage(messageLength, command, type, content);
        } catch (IOException e) {
            System.err.println("Error reading binary message: " + e.getMessage());
            return null;
        }
    }

    private void sendBinaryResponse(DataOutputStream output, int command, int type, String responseContent) throws IOException {
        byte[] contentBytes = responseContent.getBytes(StandardCharsets.UTF_8);
        int messageLength = 1 + contentBytes.length; // 1 byte for command+type, plus content
        
        System.out.println("Sending binary response:");
        System.out.println("  Message Length: " + messageLength);
        System.out.println("  Command: " + command);
        System.out.println("  Type: " + type);
        System.out.println("  Response: " + responseContent);
        
        // Write 2 bytes for message length
        output.writeShort(messageLength);
        
        // Write 1 byte for command (same as request)
        output.writeByte(command);
        
        // Write 1 byte for type (same as request)
        output.writeByte(type);
        
        // Write response content
        output.write(contentBytes);
        
        output.flush();
    }

    private String processMessage(BinaryMessage message) {
        try {
            if (message.command == COMMAND_UPLOAD_INVOICE) {
                return handleUploadInvoice(message);
            } else if (message.command == COMMAND_QUERY_INVOICE) {
                return handleQueryInvoice(message);
            } else {
                System.err.println("Unknown command: " + message.command);
                return "Unknown command";
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
            return "Processing error: " + e.getMessage();
        }
    }

    private String handleUploadInvoice(BinaryMessage message) {
        try {
            String invoiceData = message.content;
            
            System.out.println("Processing UploadInvoice:");
            System.out.println("  Type: " + message.type + " (" + (message.type == TYPE_XML_OR_SERIES ? "XML" : "JSON") + ")");
            System.out.println("  Data length: " + invoiceData.length());
            System.out.println("  Raw data preview (first 200 chars): " + invoiceData.substring(0, Math.min(200, invoiceData.length())));
            
            // Parse invoice data using proper libraries
            InvoiceUploadData.UploadSystem uploadData;
            try {
                if (message.type == TYPE_XML_OR_SERIES) {
                    System.out.println("Parsing as XML...");
                    uploadData = xmlMapper.readValue(invoiceData, InvoiceUploadData.UploadSystem.class);
                } else if (message.type == TYPE_JSON_OR_NAME) {
                    System.out.println("Parsing as JSON...");
                    uploadData = gson.fromJson(invoiceData, InvoiceUploadData.UploadSystem.class);
                } else {
                    System.err.println("Invalid type for UploadInvoice: " + message.type);
                    return "Fatura Kaydedilemedi";
                }
                System.out.println("Successfully parsed invoice data");
                
                // Debug invoice data structure
                InvoiceUploadData.Customer customer = uploadData.customer;
                InvoiceUploadData.InvoiceData invoiceDataObj = uploadData.invoiceData;
                
                System.out.println("Customer details:");
                System.out.println("  Name: " + customer.name);
                System.out.println("  SSN: " + customer.ssn);
                System.out.println("  Type: " + customer.type);
                System.out.println("  Is Company: " + customer.isCompany());
                
                System.out.println("Invoice details:");
                System.out.println("  Series: " + invoiceDataObj.seri);
                System.out.println("  Number: " + invoiceDataObj.number);
                System.out.println("  Total Amount: " + invoiceDataObj.totalAmount);
                System.out.println("  Discount: " + invoiceDataObj.discount);
                System.out.println("  Amount to Pay: " + invoiceDataObj.amountToPay);
                
                if (invoiceDataObj.items != null) {
                    System.out.println("Items (" + invoiceDataObj.items.size() + "):");
                    for (int i = 0; i < invoiceDataObj.items.size(); i++) {
                        InvoiceUploadData.Item item = invoiceDataObj.items.get(i);
                        System.out.println("    " + (i+1) + ". " + item.name + 
                            " - Qty: " + item.quantity + 
                            ", Unit Price: " + item.unitPrice + 
                            ", Line Total: " + item.lineTotal);
                    }
                } else {
                    System.out.println("Items: null");
                }
                
            } catch (Exception e) {
                System.err.println("Error parsing invoice data: " + e.getMessage());
                e.printStackTrace();
                return "Fatura Kaydedilemedi";
            }
            
            if (uploadData == null || uploadData.customer == null || uploadData.invoiceData == null) {
                System.err.println("Invalid upload data structure");
                System.err.println("  uploadData is null: " + (uploadData == null));
                if (uploadData != null) {
                    System.err.println("  customer is null: " + (uploadData.customer == null));
                    System.err.println("  invoiceData is null: " + (uploadData.invoiceData == null));
                }
                return "Fatura Kaydedilemedi";
            }
            
            // Save to database
            if (saveInvoiceToDatabase(uploadData)) {
                System.out.println("Invoice successfully saved to database");
                return "Fatura Kaydedildi";
            } else {
                System.err.println("Failed to save invoice to database");
                return "Fatura Kaydedilemedi";
            }
            
        } catch (Exception e) {
            System.err.println("Error in UploadInvoice: " + e.getMessage());
            e.printStackTrace();
            return "Fatura Kaydedilemedi";
        }
    }

    private String handleQueryInvoice(BinaryMessage message) {
        try {
            String queryData = message.content.trim();
            
            System.out.println("Processing QueryInvoice:");
            System.out.println("  Type: " + message.type + " (" + (message.type == TYPE_XML_OR_SERIES ? "Series" : "Name") + ")");
            System.out.println("  Query data: " + queryData);
            
            if (message.type == TYPE_XML_OR_SERIES) {
                // Type 1: Query by series-number
                // Expected format: "series number" (space separated)
                String[] parts = queryData.split("\\s+", 2);
                if (parts.length < 2) {
                    System.err.println("Invalid series query format. Expected: 'series number'");
                    return "Kayıt bulunamadı";
                }
                
                String series = parts[0];
                String number = parts[1];
                
                Database.Invoice invoice = database.getInvoiceBySeriesAndNumber(series, number);
                if (invoice == null) {
                    System.out.println("Invoice not found: " + series + "-" + number);
                    return "Kayıt bulunamadı";
                }
                
                // Return only total amount as specified
                String result = String.format("%.2f", invoice.totalAfter);
                System.out.println("Found invoice, returning total: " + result);
                return result;
                
            } else if (message.type == TYPE_JSON_OR_NAME) {
                // Type 2: Query by customer name OR special "ALL_INVOICES" command for listing
                if ("ALL_INVOICES".equals(queryData)) {
                    // Handle list query - return all invoices
                    System.out.println("Processing invoice list query");
                    String[] invoiceList = database.getInvoiceList();
                    if (invoiceList.length == 0) {
                        return "Kayıt bulunamadı";
                    }
                    
                    StringBuilder result = new StringBuilder();
                    result.append("Fatura Listesi:\n");
                    for (String invoice : invoiceList) {
                        result.append(invoice).append("\n");
                    }
                    System.out.println("Returning invoice list with " + invoiceList.length + " invoices");
                    return result.toString();
                } else {
                    // Regular customer name query
                    String customerName = queryData;
                    
                    Database.Invoice[] invoices = database.getInvoicesByCustomerName(customerName);
                    if (invoices.length == 0) {
                        System.out.println("No invoices found for customer: " + customerName);
                        return "Kayıt bulunamadı";
                    }
                    
                    // Return total amount of all invoices for this customer
                    double totalSum = 0;
                    for (Database.Invoice invoice : invoices) {
                        totalSum += invoice.totalAfter;
                    }
                    
                    String result = String.format("%.2f", totalSum);
                    System.out.println("Found " + invoices.length + " invoices for customer, total sum: " + result);
                    return result;
                }
                
            } else {
                System.err.println("Invalid type for QueryInvoice: " + message.type);
                return "Kayıt bulunamadı";
            }
            
        } catch (Exception e) {
            System.err.println("Error in QueryInvoice: " + e.getMessage());
            e.printStackTrace();
            return "Kayıt bulunamadı";
        }
    }

    private boolean saveInvoiceToDatabase(InvoiceUploadData.UploadSystem uploadData) {
        try {
            InvoiceUploadData.Customer customer = uploadData.customer;
            InvoiceUploadData.InvoiceData invoiceData = uploadData.invoiceData;
            
            // Find or create customer
            int customerId = findOrCreateCustomer(customer.name, customer.ssn, customer.isCompany());
            if (customerId == -1) {
                System.err.println("Failed to find or create customer");
                return false;
            }
            
            // Ensure items exist and convert to map
            Map<String, Double> itemQuantities = new HashMap<>();
            for (InvoiceUploadData.Item item : invoiceData.items) {
                if (!itemExists(item.name)) {
                    database.addItem(item.name, item.unitPrice);
                }
                itemQuantities.put(item.name, item.quantity);
            }
            
            // Save invoice with TCP source tracking if using ServerDatabase
            if (database instanceof ServerDatabase) {
                return ((ServerDatabase) database).saveInvoiceWithSource(
                    invoiceData.seri, invoiceData.number, customerId, 
                    invoiceData.discount, itemQuantities, "TCP");
            } else {
                return database.saveInvoice(
                    invoiceData.seri, invoiceData.number, customerId, 
                    invoiceData.discount, itemQuantities);
            }
        } catch (Exception e) {
            System.err.println("Error saving invoice to database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private int findOrCreateCustomer(String name, String ssn, boolean isCompany) {
        try {
            // Validate inputs
            if (name == null || name.trim().isEmpty()) {
                System.err.println("Customer name is null or empty");
                return -1;
            }
            if (ssn == null || ssn.trim().isEmpty()) {
                System.err.println("Customer SSN is null or empty");
                return -1;
            }
            
            String cleanName = name.trim();
            String cleanSsn = ssn.trim();
            
            // Try to parse SSN as long for validation
            long ssnNumber;
            try {
                ssnNumber = Long.parseLong(cleanSsn);
            } catch (NumberFormatException e) {
                System.err.println("Invalid SSN format: " + cleanSsn);
                return -1;
            }
            
            System.out.println("Finding or creating customer:");
            System.out.println("  Name: '" + cleanName + "'");
            System.out.println("  SSN: " + ssnNumber);
            System.out.println("  Is Company: " + isCompany);
            
            // Try to find existing customer
            int existingCustomerId = database.findCustomer(cleanName, ssnNumber);
            if (existingCustomerId != -1) {
                System.out.println("Found existing customer with ID: " + existingCustomerId);
                return existingCustomerId;
            }
            
            // Create new customer
            boolean success = database.addCustomer(cleanName, cleanSsn, isCompany);
            if (success) {
                int newCustomerId = database.findCustomer(cleanName, ssnNumber);
                System.out.println("Created new customer with ID: " + newCustomerId);
                return newCustomerId;
            } else {
                System.err.println("Failed to add customer to database");
                return -1;
            }
        } catch (Exception e) {
            System.err.println("Error inserting customer: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    private boolean itemExists(String itemName) {
        Map<String, Double> items = database.getItems();
        return items.containsKey(itemName);
    }

    private String getCommandName(int command) {
        switch (command) {
            case COMMAND_UPLOAD_INVOICE: return "UploadInvoice";
            case COMMAND_QUERY_INVOICE: return "QueryInvoice";
            default: return "Unknown";
        }
    }

    private String getTypeName(int command, int type) {
        if (command == COMMAND_UPLOAD_INVOICE) {
            return type == TYPE_XML_OR_SERIES ? "XML" : type == TYPE_JSON_OR_NAME ? "JSON" : "Unknown";
        } else if (command == COMMAND_QUERY_INVOICE) {
            return type == TYPE_XML_OR_SERIES ? "Series" : type == TYPE_JSON_OR_NAME ? "Name" : "Unknown";
        }
        return "Unknown";
    }

    // Helper class for binary messages
    private static class BinaryMessage {
        final int messageLength;
        final int command;
        final int type;
        final String content;

        BinaryMessage(int messageLength, int command, int type, String content) {
            this.messageLength = messageLength;
            this.command = command;
            this.type = type;
            this.content = content;
        }
    }
} 