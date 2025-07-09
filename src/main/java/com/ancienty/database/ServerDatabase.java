package com.ancienty.database;

import java.sql.*;
import java.util.*;

/**
 * Enhanced Database for Server use with additional features:
 * - Timestamps for auditing
 * - Source tracking (DESKTOP, HTTP, TCP)
 * - Better data integrity with unique constraints
 * - Enhanced invoice item tracking
 */
public class ServerDatabase extends Database {

    // Enhanced server schema with additional fields
    private static final String SERVER_INVOICE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS invoice (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "  series TEXT NOT NULL," +
                    "  number TEXT NOT NULL," +
                    "  customerId INTEGER NOT NULL," +
                    "  discount DOUBLE NOT NULL," +
                    "  totalBefore DOUBLE NOT NULL," +
                    "  totalAfter DOUBLE NOT NULL," +
                    "  createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  uploadedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  source TEXT DEFAULT 'DESKTOP'," + // DESKTOP, HTTP, TCP
                    "  UNIQUE(series, number)" + // Prevent duplicate invoices
                    ");";

    private static final String SERVER_INVOICE_ITEMS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS invoiceItems (" +
                    "  invoiceId INTEGER NOT NULL, " +
                    "  itemId INTEGER NOT NULL," +
                    "  quantity DOUBLE NOT NULL," +
                    "  lineTotal DOUBLE NOT NULL," +
                    "  unitPrice DOUBLE NOT NULL," + // Store unit price for better auditing
                    "  PRIMARY KEY (invoiceId, itemId)" +
                    ");";

    public ServerDatabase(String databasePath) {
        super(databasePath);
        System.out.println("ServerDatabase initialized with enhanced schema: " + databasePath);
    }

    @Override
    protected void createTables(Connection conn) throws SQLException {
        try (Statement s = conn.createStatement()) {
            // Create standard tables first
            s.execute("CREATE TABLE IF NOT EXISTS customer (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "  name TEXT NOT NULL, " +
                    "  ssn TEXT NOT NULL," +
                    "  isCompany BOOLEAN NOT NULL" +
                    ");");

            s.execute("CREATE TABLE IF NOT EXISTS items (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "  name TEXT NOT NULL," +
                    "  price DOUBLE NOT NULL" +
                    ");");

            // Create enhanced server tables
            s.execute(SERVER_INVOICE_TABLE_SQL);
            s.execute(SERVER_INVOICE_ITEMS_TABLE_SQL);

            // Create indexes for better performance
            s.execute("CREATE INDEX IF NOT EXISTS idx_invoice_series_number ON invoice(series, number);");
            s.execute("CREATE INDEX IF NOT EXISTS idx_invoice_customer ON invoice(customerId);");
            s.execute("CREATE INDEX IF NOT EXISTS idx_invoice_source ON invoice(source);");
            s.execute("CREATE INDEX IF NOT EXISTS idx_invoice_date ON invoice(uploadedAt);");

            System.out.println("Enhanced server database tables created with indexes");
        }
    }

    /**
     * Save invoice with source tracking for server database
     */
    public boolean saveInvoiceWithSource(String series, String number, int customerId, double discount, 
                                       Map<String, Double> items, String source) {
        String invSql = "INSERT INTO invoice(series, number, customerId, discount, totalBefore, totalAfter, source, uploadedAt) VALUES(?,?,?,?,?,?,?,CURRENT_TIMESTAMP)";
        String itemSql = "INSERT INTO invoiceItems(invoiceId, itemId, quantity, lineTotal, unitPrice) VALUES(?,?,?,?,?)";

        try {
            getConnection().setAutoCommit(false);

            // Calculate totals
            double totalBefore = 0;
            Map<String, Double> itemPrices = getItems();
            for (var entry : items.entrySet()) {
                double lineTotal = itemPrices.get(entry.getKey()) * entry.getValue();
                totalBefore += lineTotal;
            }
            double totalAfter = totalBefore - discount;

            // Insert invoice header with source tracking
            int invoiceId;
            try (PreparedStatement ps = getConnection().prepareStatement(invSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, series);
                ps.setString(2, number);
                ps.setInt(3, customerId);
                ps.setDouble(4, discount);
                ps.setDouble(5, totalBefore);
                ps.setDouble(6, totalAfter);
                ps.setString(7, source);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No invoice ID generated");
                    invoiceId = keys.getInt(1);
                }
            }

            // Insert invoice items with unit prices
            try (PreparedStatement ps = getConnection().prepareStatement(itemSql)) {
                for (var entry : items.entrySet()) {
                    Integer itemId = lookupItemId(entry.getKey());
                    if (itemId == null) continue;
                    double quantity = entry.getValue();
                    double unitPrice = itemPrices.get(entry.getKey());
                    double lineTotal = unitPrice * quantity;
                    ps.setInt(1, invoiceId);
                    ps.setInt(2, itemId);
                    ps.setDouble(3, quantity);
                    ps.setDouble(4, lineTotal);
                    ps.setDouble(5, unitPrice);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            getConnection().commit();
            System.out.println("Invoice saved to server database with source: " + source);
            return true;
        } catch (Exception ex) {
            System.err.println("Error saving invoice to server database: " + ex.getMessage());
            try { getConnection().rollback(); } catch (SQLException ignore) {}
            return false;
        } finally {
            try { getConnection().setAutoCommit(true); } catch (SQLException ignore) {}
        }
    }

    /**
     * Get invoice statistics by source
     */
    public Map<String, Integer> getInvoiceStatsBySource() {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT source, COUNT(*) as count FROM invoice GROUP BY source";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                stats.put(rs.getString("source"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching invoice statistics: " + e.getMessage());
        }
        return stats;
    }

    /**
     * Get recent invoices (last 10)
     */
    public String[] getRecentInvoices() {
        List<String> out = new ArrayList<>();
        String sql = "SELECT id, series, number, source, uploadedAt FROM invoice ORDER BY uploadedAt DESC LIMIT 10";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add("("
                        + rs.getInt("id")
                        + ") "
                        + rs.getString("series")
                        + " - "
                        + rs.getString("number")
                        + " [" + rs.getString("source") + "]"
                        + " @ " + rs.getString("uploadedAt")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching recent invoices: " + e.getMessage());
        }
        return out.toArray(new String[0]);
    }

    // Make lookupItemId accessible to this class
    private Integer lookupItemId(String name) {
        String sql = "SELECT id FROM items WHERE name = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }
        } catch (SQLException e) {
            System.err.println("Error looking up item: " + e.getMessage());
            return null;
        }
    }
} 