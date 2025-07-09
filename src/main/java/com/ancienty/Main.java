package com.ancienty;

import com.ancienty.database.Database;
import com.ancienty.gui.*;
import com.ancienty.client.ServiceOperationsGUI;
import com.ancienty.server.ServerMain;

import javax.swing.*;
import java.awt.*;

/**
 * Main application entry point - Enhanced with server start option
 */
public class Main {
    
    private static Database database;
    private static JFrame mainFrame;

    public static void main(String[] args) {
        // Initialize database
        database = new Database();
        
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }

        // Show main menu
        showMainMenu();
    }

    private static void showMainMenu() {
        mainFrame = new JFrame("Hugin Fatura Yönetim Sistemi");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        
        JLabel title = new JLabel("Hugin Fatura Yönetim Sistemi", SwingConstants.CENTER);
        title.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        headerPanel.add(title, BorderLayout.CENTER);
        
        JLabel subtitle = new JLabel("Profesyonel Fatura Yönetimi ve API Servisleri", SwingConstants.CENTER);
        subtitle.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 20));
        headerPanel.add(subtitle, BorderLayout.SOUTH);
        
        mainFrame.add(headerPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Phase 1 Operations
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JPanel phase1Panel = createPhasePanel("Phase 1 - Desktop Uygulaması");
        contentPanel.add(phase1Panel, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1;
        JButton btnCreateInvoice = createStyledButton("Fatura Oluştur");
        btnCreateInvoice.addActionListener(e -> openCreateInvoice());
        contentPanel.add(btnCreateInvoice, gbc);

        gbc.gridx = 1;
        JButton btnDeleteInvoice = createStyledButton("Fatura Sil");
        btnDeleteInvoice.addActionListener(e -> openDeleteInvoice());
        contentPanel.add(btnDeleteInvoice, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JButton btnExportJson = createStyledButton("JSON Dışa Aktar");
        btnExportJson.addActionListener(e -> openJsonExport());
        contentPanel.add(btnExportJson, gbc);

        gbc.gridx = 1;
        JButton btnExportXml = createStyledButton("XML Dışa Aktar");
        btnExportXml.addActionListener(e -> openXmlExport());
        contentPanel.add(btnExportXml, gbc);

        // Separator
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        contentPanel.add(Box.createVerticalStrut(20), gbc);

        // Phase 2 Operations
        gbc.gridy = 4; gbc.gridwidth = 2;
        JPanel phase2Panel = createPhasePanel("Phase 2 - Client-Server İşlemleri");
        contentPanel.add(phase2Panel, gbc);

        gbc.gridy = 5; gbc.gridwidth = 1;
        JButton btnStartServer = createStyledButton("Sunucu Başlat");
        btnStartServer.addActionListener(e -> startServer());
        contentPanel.add(btnStartServer, gbc);

        gbc.gridx = 1;
        JButton btnServiceOps = createStyledButton("Servis İşlemleri");
        btnServiceOps.addActionListener(e -> openServiceOperations());
        contentPanel.add(btnServiceOps, gbc);

        // Exit button
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.insets = new Insets(30, 10, 10, 10);
        JButton btnExit = createStyledButton("Çıkış");
        btnExit.addActionListener(e -> System.exit(0));
        contentPanel.add(btnExit, gbc);

        mainFrame.add(contentPanel, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel footerLabel = new JLabel("Hem desktop hem de server-client mimarisi ile fatura yönetimi");
        footerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        footerPanel.add(footerLabel);
        mainFrame.add(footerPanel, BorderLayout.SOUTH);

        mainFrame.setSize(550, 600);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private static JPanel createPhasePanel(String title) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        JLabel label = new JLabel(title);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 2),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        panel.add(label);
        
        return panel;
    }

    private static JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(220, 45));
        return button;
    }

    private static void openCreateInvoice() {
        mainFrame.setVisible(false);
        new CreateInvoiceGUI(database, Main::returnToMain);
    }

    private static void openDeleteInvoice() {
        mainFrame.setVisible(false);
        new DeleteInvoiceGUI(database, Main::returnToMain);
    }

    private static void openJsonExport() {
        mainFrame.setVisible(false);
        new JsonExportGUI(database, Main::returnToMain);
    }

    private static void openXmlExport() {
        mainFrame.setVisible(false);
        new XmlExportGUI(database, Main::returnToMain);
    }

    private static void startServer() {
        // Start server in a separate thread
        new Thread(() -> {
            try {
                JOptionPane.showMessageDialog(mainFrame, 
                    "Sunucu başlatılıyor...\nKonsol çıktısını kontrol edin.", 
                    "Sunucu", 
                    JOptionPane.INFORMATION_MESSAGE);
                
                ServerMain.main(new String[0]);
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(mainFrame, 
                        "Sunucu başlatılamadı: " + e.getMessage(), 
                        "Hata", 
                        JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private static void openServiceOperations() {
        mainFrame.setVisible(false);
        new ServiceOperationsGUI(Main::returnToMain);
    }

    private static void returnToMain() {
        mainFrame.setVisible(true);
    }
} 