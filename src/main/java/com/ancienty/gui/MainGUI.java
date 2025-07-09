package com.ancienty.gui;

import com.ancienty.database.Database;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Enhanced Main GUI for Phase 1 operations
 */
public class MainGUI {

    private static final int BUTTON_WIDTH  = 160;
    private static final int BUTTON_HEIGHT = 40;
    private static final Dimension SECTION_SIZE = new Dimension(540, 160);

    private final Database database;
    private final Runnable exitApp;
    private final JFrame frame;

    public MainGUI(Database database, Runnable exitApp) {
        this.database = database;
        this.exitApp = exitApp;
        this.frame = new JFrame("Hugin Fatura Sistemi - Ana Menü");
        initializeGUI();
    }

    private static final int BTN_W = 120;
    private static final int BTN_H = 30;

    private void initializeGUI() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Title
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        JLabel titleLabel = new JLabel("Hugin Fatura Yönetim Sistemi", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        frame.add(titlePanel, BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(5, 5, 5, 5);
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // Customer
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(createCustomerManagementPanel(), gbc);

        // Invoice
        gbc.gridy = 1;
        mainPanel.add(createInvoiceManagementPanel(), gbc);

        // Export
        gbc.gridy = 2;
        mainPanel.add(createExportPanel(), gbc);

        frame.add(mainPanel, BorderLayout.CENTER);

        // Bottom Exit
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        JButton btnExit = createStyledButton("Çıkış", BTN_W, BTN_H);
        btnExit.addActionListener(e -> exitApp.run());
        bottom.add(btnExit);
        frame.add(bottom, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    private JPanel createCustomerManagementPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Müşteri Yönetimi"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(4, 4, 4, 4);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createStyledButton("Ekle", BTN_W, BTN_H), gbc);
        gbc.gridx = 1;
        panel.add(createStyledButton("Düzenle", BTN_W, BTN_H), gbc);

        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(createStyledButton("Sil", BTN_W, BTN_H), gbc);
        gbc.gridx = 1;
        panel.add(createStyledButton("Liste", BTN_W, BTN_H), gbc);

        return panel;
    }

    private JPanel createInvoiceManagementPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Fatura Yönetimi"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(4, 4, 4, 4);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createStyledButton("Oluştur", BTN_W, BTN_H), gbc);
        gbc.gridx = 1;
        panel.add(createStyledButton("Sil", BTN_W, BTN_H), gbc);

        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(createStyledButton("Liste", BTN_W, BTN_H), gbc);
        gbc.gridx = 1;
        panel.add(createStyledButton("Ürünler", BTN_W, BTN_H), gbc);

        return panel;
    }

    private JPanel createExportPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Dışa Aktar"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(4, 4, 4, 4);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createStyledButton("XML", BTN_W, BTN_H), gbc);
        gbc.gridx = 1;
        panel.add(createStyledButton("JSON", BTN_W, BTN_H), gbc);

        return panel;
    }

    private JButton createStyledButton(String text, int width, int height) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(width, height));
        return btn;
    }


    private void showNotImplemented(String feature) {
        JOptionPane.showMessageDialog(frame,
                feature + " özelliği henüz uygulanmamıştır.\n" +
                        "Bu özellik gelecek sürümlerde eklenecektir.",
                "Özellik Mevcut Değil",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void returnToMainMenu() {
        SwingUtilities.invokeLater(this::initializeGUI);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Database database = new Database();
            new MainGUI(database, () -> System.exit(0));
        });
    }
}
