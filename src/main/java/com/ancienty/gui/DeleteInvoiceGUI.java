package com.ancienty.gui;

import com.ancienty.database.Database;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Enhanced Invoice Deletion GUI with better list view and confirmation
 */
public class DeleteInvoiceGUI {

    private static final int BUTTON_WIDTH         = 160;
    private static final int BUTTON_HEIGHT        = 35;
    private static final Dimension TABLE_SCROLL_SIZE = new Dimension(680, 300);

    private final Database database;
    private final Runnable returnToMain;
    private final JFrame frame;

    private DefaultTableModel tableModel;
    private JTable            invoiceTable;
    private JTextField        txtSearch;

    public DeleteInvoiceGUI(Database database, Runnable returnToMain) {
        this.database     = database;
        this.returnToMain = returnToMain;
        this.frame        = new JFrame("Fatura Sil");
        initializeGUI();
    }

    private static final int BTN_W = 100;
    private static final int BTN_H = 28;

    private void initializeGUI() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        JLabel titleLabel = new JLabel("Fatura Sil", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        frame.add(titlePanel, BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        mainPanel.add(createSearchPanel(), BorderLayout.NORTH);
        mainPanel.add(createInvoiceListPanel(), BorderLayout.CENTER);
        frame.add(mainPanel, BorderLayout.CENTER);

        // Bottom buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        buttonPanel.setBorder(new EmptyBorder(6, 0, 6, 0));

        JButton btnDelete  = createStyledButton("Sil", BTN_W, BTN_H);
        btnDelete.addActionListener(e -> deleteSelectedInvoice());

        JButton btnRefresh = createStyledButton("Yenile", BTN_W, BTN_H);
        btnRefresh.addActionListener(e -> refreshInvoiceList());

        JButton btnCancel  = createStyledButton("Geri", BTN_W, BTN_H);
        btnCancel.addActionListener(e -> cancel());

        buttonPanel.add(btnDelete);
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnCancel);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        refreshInvoiceList();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Fatura Arama"));
        panel.setBorder(BorderFactory.createCompoundBorder(
                panel.getBorder(),
                new EmptyBorder(6, 6, 6, 6)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Arama:"), gbc);

        gbc.gridx   = 1;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtSearch   = new JTextField();
        txtSearch.setPreferredSize(new Dimension(150, 24));
        panel.add(txtSearch, gbc);

        gbc.gridx   = 2;
        gbc.fill    = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton btnSearch = createStyledButton("Ara", BTN_W, BTN_H);
        btnSearch.addActionListener(e -> performSearch());
        panel.add(btnSearch, gbc);

        txtSearch.addActionListener(e -> performSearch());
        return panel;
    }

    private JPanel createInvoiceListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Fatura Listesi"));
        panel.setBorder(BorderFactory.createCompoundBorder(
                panel.getBorder(),
                new EmptyBorder(6, 6, 6, 6)
        ));

        String[] cols = {"ID","Seri","Numara","Müşteri","Toplam","Tarih"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        invoiceTable = new JTable(tableModel);
        invoiceTable.setRowHeight(22);
        invoiceTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) deleteSelectedInvoice();
            }
        });

        JScrollPane scroll = new JScrollPane(invoiceTable);
        scroll.setPreferredSize(new Dimension(500, 200));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JButton createStyledButton(String text, int w, int h) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(w, h));
        return b;
    }

    private void styleTextField(JTextField tf) {
        tf.setPreferredSize(new Dimension(200, 28));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }

    private void refreshInvoiceList() {
        tableModel.setRowCount(0);
        List<Database.Invoice> invoices = database.getAllInvoices();
        if (invoices.isEmpty()) {
            tableModel.addRow(new Object[]{"Kayıt yok", "", "", "", "", ""});
        } else {
            for (Database.Invoice inv : invoices) {
                tableModel.addRow(new Object[]{
                        inv.id,
                        inv.series,
                        inv.number,
                        inv.customerName,
                        String.format("%.2f TL", inv.totalAmount),
                        inv.date.toString()
                });
            }
        }
    }

    private void performSearch() {
        String term = txtSearch.getText().trim().toLowerCase();
        if (term.isEmpty()) {
            refreshInvoiceList();
            return;
        }
        tableModel.setRowCount(0);
        boolean found = false;
        for (Database.Invoice inv : database.getAllInvoices()) {
            if (inv.series.toLowerCase().contains(term)
                    || inv.number.toLowerCase().contains(term)
                    || inv.customerName.toLowerCase().contains(term)) {
                tableModel.addRow(new Object[]{
                        inv.id, inv.series, inv.number,
                        inv.customerName,
                        String.format("%.2f TL", inv.totalAmount),
                        inv.date.toString()
                });
                found = true;
            }
        }
        if (!found) {
            tableModel.addRow(new Object[]{"Arama sonucu bulunamadı", "", "", "", "", ""});
        }
    }

    private void deleteSelectedInvoice() {
        int row = invoiceTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(frame,
                    "Lütfen silmek için bir fatura seçin.",
                    "Fatura Seçilmedi",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object idVal = tableModel.getValueAt(row, 0);
        if ("Kayıt yok".equals(idVal) || "Arama sonucu bulunamadı".equals(idVal)) {
            JOptionPane.showMessageDialog(frame,
                    "Geçerli bir fatura seçilmedi.",
                    "Geçersiz Seçim",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int id = Integer.parseInt(idVal.toString());
            String series   = tableModel.getValueAt(row, 1).toString();
            String number   = tableModel.getValueAt(row, 2).toString();
            String customer = tableModel.getValueAt(row, 3).toString();

            int confirm = JOptionPane.showConfirmDialog(frame,
                    String.format("Seri: %s\nNumara: %s\nMüşteri: %s\n\nSilmek istediğinize emin misiniz?",
                            series, number, customer),
                    "Onay",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                if (database.deleteInvoice(id)) {
                    JOptionPane.showMessageDialog(frame,
                            "Fatura başarıyla silindi.",
                            "Başarılı",
                            JOptionPane.INFORMATION_MESSAGE);
                    refreshInvoiceList();
                } else {
                    JOptionPane.showMessageDialog(frame,
                            "Silme işlemi sırasında hata oluştu.",
                            "Hata",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Geçersiz fatura ID'si.",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancel() {
        frame.dispose();
        returnToMain.run();
    }
}
