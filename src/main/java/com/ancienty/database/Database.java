package com.ancienty.database;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Phase 1 Database implementation with direct access.
 * Uses the exact schema specified in Phase 1 requirements.
 */
public class Database {

    // Phase 1 Schema - exact field names as specified
    private static final String CUSTOMER_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS customer (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "  name TEXT NOT NULL, " +
                    "  ssn TEXT NOT NULL," +
                    "  isCompany BOOLEAN NOT NULL" +
                    ");";

    private static final String ITEMS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS items (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "  name TEXT NOT NULL," +
                    "  price DOUBLE NOT NULL" +
                    ");";

    private static final String INVOICE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS invoice (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "  series TEXT NOT NULL," +
                    "  number TEXT NOT NULL," +
                    "  customerId INTEGER NOT NULL," +
                    "  discount DOUBLE NOT NULL," +
                    "  totalBefore DOUBLE NOT NULL," +
                    "  totalAfter DOUBLE NOT NULL" +
                    ");";

    private static final String INVOICE_ITEMS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS invoiceItems (" +
                    "  invoiceId INTEGER NOT NULL, " +
                    "  itemId INTEGER NOT NULL," +
                    "  quantity DOUBLE NOT NULL," +
                    "  lineTotal DOUBLE NOT NULL" +
                    ");";

    private Connection connection;
    private final String databasePath;

    public Database() {
        this("upload_system.db"); // Default client database
    }
    
    public Database(String databasePath) {
        this.databasePath = databasePath;
        initialize();
    }

    public Connection getConnection() {
        if (connection == null) {
            initialize();
        }
        return connection;
    }

    private void initialize() {
        File dbFile = new File(".", databasePath);
        boolean exists = dbFile.exists();
        String path;
        try {
            path = dbFile.getCanonicalPath();
        } catch (IOException e) {
            System.err.println("Failed to resolve DB path: " + e.getMessage());
            return;
        }
        String url = "jdbc:sqlite:" + path;
        try {
            connection = DriverManager.getConnection(url);
            if (!exists) {
                createTables(connection);
            }
        } catch (SQLException e) {
            System.err.println("Error connecting to DB: " + e.getMessage());
        }
    }

    protected void createTables(Connection conn) throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute(CUSTOMER_TABLE_SQL);
            s.execute(ITEMS_TABLE_SQL);
            s.execute(INVOICE_TABLE_SQL);
            s.execute(INVOICE_ITEMS_TABLE_SQL);
            System.out.println("Database tables created");
        }
    }

    // ──────────────────────────────── CUSTOMER OPERATIONS ────────────────────────────────

    /**
     * Returns an array like ["Yeni Müşteri", "(1) Alice", "(2) Bob", ...].
     */
    public String[] getCustomers() {
        List<String> out = new ArrayList<>();
        out.add("Yeni Müşteri");
        String sql = "SELECT id, name FROM customer ORDER BY id";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add("("
                        + rs.getInt("id")
                        + ") "
                        + rs.getString("name")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching customers: " + e.getMessage());
        }
        return out.toArray(new String[0]);
    }

    /**
     * Returns customer data as {id, name, ssn, isCompany} or null if not found.
     */
    public Customer getCustomer(int customerId) {
        String sql = "SELECT id, name, ssn, isCompany FROM customer WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Customer(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("ssn"),
                        rs.getBoolean("isCompany")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching customer: " + e.getMessage());
        }
        return null;
    }

    public boolean addCustomer(String name, String ssn, boolean isCompany) {
        String sql = "INSERT INTO customer(name, ssn, isCompany) VALUES(?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, ssn);
            ps.setBoolean(3, isCompany);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error inserting customer: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find customer by name and SSN
     */
    public int findCustomer(String name, long ssn) {
        String sql = "SELECT id FROM customer WHERE name = ? AND ssn = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setLong(2, ssn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding customer: " + e.getMessage());
        }
        return -1; // Customer not found
    }

    // ──────────────────────────────── ITEM OPERATIONS ────────────────────────────────

    /**
     * Returns map of itemName→price.
     */
    public Map<String, Double> getItems() {
        Map<String, Double> m = new LinkedHashMap<>();
        String sql = "SELECT name, price FROM items ORDER BY name";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.put(rs.getString("name"), rs.getDouble("price"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching items: " + e.getMessage());
        }
        return m;
    }

    public boolean addItem(String name, double price) {
        String sql = "INSERT INTO items(name, price) VALUES(?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error inserting item: " + e.getMessage());
            return false;
        }
    }

    private Integer lookupItemId(String name) {
        String sql = "SELECT id FROM items WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }
        } catch (SQLException e) {
            System.err.println("Error looking up item: " + e.getMessage());
            return null;
        }
    }

    // ──────────────────────────────── INVOICE OPERATIONS ────────────────────────────────

    public boolean saveInvoice(String series, String number, int customerId, double discount, 
                               Map<String, Double> items) {
        String invSql = "INSERT INTO invoice(series, number, customerId, discount, totalBefore, totalAfter) VALUES(?,?,?,?,?,?)";
        String itemSql = "INSERT INTO invoiceItems(invoiceId, itemId, quantity, lineTotal) VALUES(?,?,?,?)";

        try {
            connection.setAutoCommit(false);

            // Calculate totals
            double totalBefore = 0;
            Map<String, Double> itemPrices = getItems();
            for (var entry : items.entrySet()) {
                double lineTotal = itemPrices.get(entry.getKey()) * entry.getValue();
                totalBefore += lineTotal;
            }
            double totalAfter = totalBefore - discount;

            // Insert invoice header
            int invoiceId;
            try (PreparedStatement ps = connection.prepareStatement(invSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, series);
                ps.setString(2, number);
                ps.setInt(3, customerId);
                ps.setDouble(4, discount);
                ps.setDouble(5, totalBefore);
                ps.setDouble(6, totalAfter);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No invoice ID generated");
                    invoiceId = keys.getInt(1);
                }
            }

            // Insert invoice items
            try (PreparedStatement ps = connection.prepareStatement(itemSql)) {
                for (var entry : items.entrySet()) {
                    Integer itemId = lookupItemId(entry.getKey());
                    if (itemId == null) continue;
                    double quantity = entry.getValue();
                    double lineTotal = itemPrices.get(entry.getKey()) * quantity;
                    ps.setInt(1, invoiceId);
                    ps.setInt(2, itemId);
                    ps.setDouble(3, quantity);
                    ps.setDouble(4, lineTotal);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            connection.commit();
            return true;
        } catch (Exception ex) {
            System.err.println("Error saving invoice: " + ex.getMessage());
            try { connection.rollback(); } catch (SQLException ignore) {}
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignore) {}
        }
    }

    /**
     * Returns list of invoices as ["(1) ABC - 123", "(2) DEF - 456", ...]
     */
    public String[] getInvoiceList() {
        List<String> out = new ArrayList<>();
        String sql = "SELECT id, series, number FROM invoice ORDER BY id";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add("("
                        + rs.getInt("id")
                        + ") "
                        + rs.getString("series")
                        + " - "
                        + rs.getString("number")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching invoices: " + e.getMessage());
        }
        return out.toArray(new String[0]);
    }

    /**
     * Get invoice by ID
     */
    public Invoice getInvoice(int invoiceId) {
        String sql = "SELECT i.*, c.name as customerName, c.ssn, c.isCompany " +
                     "FROM invoice i JOIN customer c ON i.customerId = c.id " +
                     "WHERE i.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Invoice(
                        rs.getInt("id"),
                        rs.getString("series"),
                        rs.getString("number"),
                        rs.getInt("customerId"),
                        rs.getString("customerName"),
                        rs.getString("ssn"),
                        rs.getBoolean("isCompany"),
                        rs.getDouble("discount"),
                        rs.getDouble("totalBefore"),
                        rs.getDouble("totalAfter")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching invoice by ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get invoice by series and number
     */
    public Invoice getInvoiceBySeriesAndNumber(String series, String number) {
        String sql = "SELECT i.*, c.name as customerName, c.ssn, c.isCompany " +
                     "FROM invoice i JOIN customer c ON i.customerId = c.id " +
                     "WHERE i.series = ? AND i.number = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, series);
            ps.setString(2, number);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Invoice(
                        rs.getInt("id"),
                        rs.getString("series"),
                        rs.getString("number"),
                        rs.getInt("customerId"),
                        rs.getString("customerName"),
                        rs.getString("ssn"),
                        rs.getBoolean("isCompany"),
                        rs.getDouble("discount"),
                        rs.getDouble("totalBefore"),
                        rs.getDouble("totalAfter")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error searching invoice: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get invoice items for export (compatible with both client and server database schemas)
     */
    public List<InvoiceItem> getInvoiceItems(int invoiceId) {
        List<InvoiceItem> items = new ArrayList<>();
        
        // Try server schema first (with unitPrice column), then fall back to client schema
        String serverSql = "SELECT it.name, ii.unitPrice, ii.quantity, ii.lineTotal " +
                          "FROM invoiceItems ii JOIN items it ON ii.itemId = it.id " +
                          "WHERE ii.invoiceId = ?";
        
        String clientSql = "SELECT it.name, it.price, ii.quantity, ii.lineTotal " +
                          "FROM invoiceItems ii JOIN items it ON ii.itemId = it.id " +
                          "WHERE ii.invoiceId = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(serverSql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new InvoiceItem(
                            rs.getString("name"),
                            rs.getDouble("unitPrice"), // Use stored unitPrice from server schema
                            rs.getDouble("quantity"),
                            rs.getDouble("lineTotal")
                    ));
                }
            }
        } catch (SQLException e) {
            // Server schema failed, try client schema
            try (PreparedStatement ps = connection.prepareStatement(clientSql)) {
                ps.setInt(1, invoiceId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(new InvoiceItem(
                                rs.getString("name"),
                                rs.getDouble("price"), // Use current price from items table
                                rs.getDouble("quantity"),
                                rs.getDouble("lineTotal")
                        ));
                    }
                }
            } catch (SQLException clientEx) {
                System.err.println("Error loading invoice items (both schemas tried): " + clientEx.getMessage());
            }
        }
        return items;
    }

    /**
     * Delete invoice and its items
     */
    public boolean deleteInvoice(String series, String number) {
        try {
            connection.setAutoCommit(false);

            // Get invoice ID first
            String findSql = "SELECT id FROM invoice WHERE series = ? AND number = ?";
            int invoiceId;
            try (PreparedStatement ps = connection.prepareStatement(findSql)) {
                ps.setString(1, series);
                ps.setString(2, number);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    invoiceId = rs.getInt("id");
                }
            }

            // Delete invoice items first
            String delItems = "DELETE FROM invoiceItems WHERE invoiceId = ?";
            try (PreparedStatement ps = connection.prepareStatement(delItems)) {
                ps.setInt(1, invoiceId);
                ps.executeUpdate();
            }

            // Delete invoice
            String delInvoice = "DELETE FROM invoice WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(delInvoice)) {
                ps.setInt(1, invoiceId);
                ps.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting invoice: " + e.getMessage());
            try { connection.rollback(); } catch (SQLException ignore) {}
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignore) {}
        }
    }

    /**
     * Get invoices by customer name - for name-based queries
     */
    public Database.Invoice[] getInvoicesByCustomerName(String customerName) {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT i.*, c.name as customerName, c.ssn, c.isCompany " +
                     "FROM invoice i JOIN customer c ON i.customerId = c.id " +
                     "WHERE LOWER(c.name) LIKE LOWER(?) ORDER BY i.id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "%" + customerName + "%"); // Support partial matches
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    invoices.add(new Invoice(
                            rs.getInt("id"),
                            rs.getString("series"),
                            rs.getString("number"),
                            rs.getInt("customerId"),
                            rs.getString("customerName"),
                            rs.getString("ssn"),
                            rs.getBoolean("isCompany"),
                            rs.getDouble("discount"),
                            rs.getDouble("totalBefore"),
                            rs.getDouble("totalAfter")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching invoices by customer name: " + e.getMessage());
        }
        return invoices.toArray(new Invoice[0]);
    }

    /**
     * Get all invoices from database
     */
    public List<Invoice> getAllInvoices() {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT i.*, c.name as customerName, c.ssn, c.isCompany " +
                     "FROM invoice i JOIN customer c ON i.customerId = c.id " +
                     "ORDER BY i.id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                invoices.add(new Invoice(
                        rs.getInt("id"),
                        rs.getString("series"),
                        rs.getString("number"),
                        rs.getInt("customerId"),
                        rs.getString("customerName"),
                        rs.getString("ssn"),
                        rs.getBoolean("isCompany"),
                        rs.getDouble("discount"),
                        rs.getDouble("totalBefore"),
                        rs.getDouble("totalAfter")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all invoices: " + e.getMessage());
        }
        return invoices;
    }

    /**
     * Get invoice by ID
     */
    public Invoice getInvoiceById(int invoiceId) {
        return getInvoice(invoiceId); // Delegate to existing method
    }

    /**
     * Delete invoice by ID (overloaded method for GUI compatibility)
     */
    public boolean deleteInvoice(int invoiceId) {
        try {
            connection.setAutoCommit(false);

            // Delete invoice items first
            String delItems = "DELETE FROM invoiceItems WHERE invoiceId = ?";
            try (PreparedStatement ps = connection.prepareStatement(delItems)) {
                ps.setInt(1, invoiceId);
                ps.executeUpdate();
            }

            // Delete invoice
            String delInvoice = "DELETE FROM invoice WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(delInvoice)) {
                ps.setInt(1, invoiceId);
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected == 0) {
                    connection.rollback();
                    return false;
                }
            }

            connection.commit();
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting invoice by ID: " + e.getMessage());
            try { connection.rollback(); } catch (SQLException ignore) {}
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignore) {}
        }
    }

    // ──────────────────────────────── DATA CLASSES ────────────────────────────────

    public static class Customer {
        public final int id;
        public final String name;
        public final String ssn;
        public final boolean isCompany;

        public Customer(int id, String name, String ssn, boolean isCompany) {
            this.id = id;
            this.name = name;
            this.ssn = ssn;
            this.isCompany = isCompany;
        }
    }

    public static class Invoice {
        public final int id;
        public final String series;
        public final String number;
        public final int customerId;
        public final String customerName;
        public final String customerSsn;
        public final boolean isCompany;
        public final double discount;
        public final double totalBefore;
        public final double totalAfter;
        
        // Additional fields for GUI compatibility
        public final double totalAmount; // Alias for totalAfter
        public final java.time.LocalDate date; // Invoice date

        public Invoice(int id, String series, String number, int customerId, String customerName,
                       String customerSsn, boolean isCompany, double discount, double totalBefore, double totalAfter) {
            this.id = id;
            this.series = series;
            this.number = number;
            this.customerId = customerId;
            this.customerName = customerName;
            this.customerSsn = customerSsn;
            this.isCompany = isCompany;
            this.discount = discount;
            this.totalBefore = totalBefore;
            this.totalAfter = totalAfter;
            
            // Set GUI compatibility fields
            this.totalAmount = totalAfter; // For GUI compatibility
            this.date = java.time.LocalDate.now(); // Default to current date (could be enhanced to store actual date)
        }
        
        // Constructor with explicit date
        public Invoice(int id, String series, String number, int customerId, String customerName,
                       String customerSsn, boolean isCompany, double discount, double totalBefore, double totalAfter,
                       java.time.LocalDate date) {
            this.id = id;
            this.series = series;
            this.number = number;
            this.customerId = customerId;
            this.customerName = customerName;
            this.customerSsn = customerSsn;
            this.isCompany = isCompany;
            this.discount = discount;
            this.totalBefore = totalBefore;
            this.totalAfter = totalAfter;
            this.totalAmount = totalAfter;
            this.date = date;
        }
    }

    public static class InvoiceItem {
        public final String name;
        public final double unitPrice;
        public final double quantity;
        public final double lineTotal;

        public InvoiceItem(String name, double unitPrice, double quantity, double lineTotal) {
            this.name = name;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
            this.lineTotal = lineTotal;
        }
    }
} 