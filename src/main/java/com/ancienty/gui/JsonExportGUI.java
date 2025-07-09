package com.ancienty.gui;

import com.ancienty.database.Database;
import com.ancienty.server.model.InvoiceUploadData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class JsonExportGUI {

    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 35;
    private static final Dimension TABLE_SCROLL_SIZE = new Dimension(480, 380);
    private static final Dimension PREVIEW_SCROLL_SIZE = new Dimension(420, 380);

    private final Database database;
    private final Runnable returnToMain;
    private final JFrame frame;

    private DefaultTableModel tableModel;
    private JTable invoiceTable;
    private JTextArea txtPreview;
    private JButton btnExport;

    public JsonExportGUI(Database database, Runnable returnToMain) {
        this.database = database;
        this.returnToMain = returnToMain;
        this.frame = new JFrame("JSON Dışa Aktarma");
        initializeGUI();
    }

    private static final int BTN_W = 100;
    private static final int BTN_H = 28;

    private void initializeGUI() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        JLabel titleLabel = new JLabel("JSON Dışa Aktarma", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        frame.add(titlePanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(4);
        splitPane.setLeftComponent(createInvoiceSelectionPanel());
        splitPane.setRightComponent(createPreviewPanel());
        frame.add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        buttonPanel.setBorder(new EmptyBorder(5, 0, 5, 0));

        btnExport = createStyledButton("Kaydet", BTN_W, BTN_H);
        btnExport.setEnabled(false);
        btnExport.addActionListener(e -> exportToJson());

        JButton btnRefresh = createStyledButton("Yenile", BTN_W, BTN_H);
        btnRefresh.addActionListener(e -> refreshInvoiceList());

        JButton btnCancel = createStyledButton("Geri", BTN_W, BTN_H);
        btnCancel.addActionListener(e -> cancel());

        buttonPanel.add(btnExport);
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnCancel);

        frame.add(buttonPanel, BorderLayout.SOUTH);

        refreshInvoiceList();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    private JPanel createInvoiceSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Fatura Seçimi"));

        String[] cols = {"ID", "Seri", "Numara", "Müşteri", "Toplam", "Tarih"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        invoiceTable = new JTable(tableModel);
        invoiceTable.setRowHeight(22);
        for (int i = 0; i < cols.length; i++) {
            invoiceTable.getColumnModel().getColumn(i).setPreferredWidth((i == 3) ? 120 : (i == 5) ? 80 : 50);
        }
        invoiceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updatePreview();
        });
        invoiceTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && btnExport.isEnabled()) exportToJson();
            }
        });

        JScrollPane scroll = new JScrollPane(invoiceTable);
        scroll.setPreferredSize(new Dimension(400, 200));
        panel.add(scroll, BorderLayout.CENTER);

        JLabel tip = new JLabel("<html><i>Çift tıklayarak kaydet, seçmek için tıklayın</i></html>");
        tip.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(tip, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("JSON Önizleme"));

        txtPreview = new JTextArea();
        txtPreview.setEditable(false);
        txtPreview.setText("Fatura seçerek JSON önizleme görebilirsiniz…");
        txtPreview.setBorder(new EmptyBorder(4, 4, 4, 4));

        JScrollPane scroll = new JScrollPane(txtPreview);
        scroll.setPreferredSize(new Dimension(300, 200));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JButton createStyledButton(String text, int w, int h) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(w, h));
        return b;
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
        txtPreview.setText("Fatura seçerek JSON önizleme görebilirsiniz...");
        txtPreview.setForeground(Color.DARK_GRAY);
        btnExport.setEnabled(false);
    }

    private void updatePreview() {
        int row = invoiceTable.getSelectedRow();
        if (row < 0 || "Kayıt yok".equals(tableModel.getValueAt(row, 0))) {
            txtPreview.setText("Geçerli fatura seçilmedi.");
            txtPreview.setForeground(Color.RED);
            btnExport.setEnabled(false);
            return;
        }
        try {
            int invId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
            Database.Invoice inv = database.getInvoiceById(invId);
            if (inv == null) throw new RuntimeException("Detay bulunamadı");
            InvoiceUploadData.UploadSystem jsonData = createJsonInvoiceData(inv);
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            String jsonString = mapper.writeValueAsString(jsonData);
            txtPreview.setText(jsonString);
            txtPreview.setForeground(Color.BLACK);
            btnExport.setEnabled(true);
        } catch (Exception ex) {
            txtPreview.setText("Önizleme oluşturulurken hata: " + ex.getMessage());
            txtPreview.setForeground(Color.RED);
            btnExport.setEnabled(false);
        }
    }

    private InvoiceUploadData.UploadSystem createJsonInvoiceData(Database.Invoice invoice) {
        InvoiceUploadData.UploadSystem data = new InvoiceUploadData.UploadSystem();

        InvoiceUploadData.Customer customer = new InvoiceUploadData.Customer();
        customer.name = invoice.customerName;
        customer.ssn = invoice.customerSsn;
        customer.type = invoice.isCompany ? "COMPANY" : "INDIVIDUAL";
        data.customer = customer;

        InvoiceUploadData.InvoiceData invoiceData = new InvoiceUploadData.InvoiceData();
        invoiceData.seri = invoice.series;
        invoiceData.number = invoice.number;
        invoiceData.totalAmount = invoice.totalBefore;
        invoiceData.discount = invoice.discount;
        invoiceData.amountToPay = invoice.totalAfter;

        List<Database.InvoiceItem> items = database.getInvoiceItems(invoice.id);
        invoiceData.items = new java.util.ArrayList<>();
        for (Database.InvoiceItem item : items) {
            InvoiceUploadData.Item jsonItem = new InvoiceUploadData.Item();
            jsonItem.name = item.name;
            jsonItem.unitPrice = item.unitPrice;
            jsonItem.quantity = item.quantity;
            jsonItem.lineTotal = item.lineTotal;
            invoiceData.items.add(jsonItem);
        }
        data.invoiceData = invoiceData;

        return data;
    }

    private void exportToJson() {
        int row = invoiceTable.getSelectedRow();
        if (row < 0 || !btnExport.isEnabled()) {
            JOptionPane.showMessageDialog(frame,
                    "Lütfen dışa aktarmak için bir fatura seçin.",
                    "Fatura Seçilmedi",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int invId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
            Database.Invoice invoice = database.getInvoiceById(invId);
            if (invoice == null) {
                JOptionPane.showMessageDialog(frame,
                        "Fatura detayları alınamadı.",
                        "Hata",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("JSON Dosyasını Kaydet");
            String name = String.format("fatura_%s_%s_%s.json",
                    invoice.series,
                    invoice.number,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            chooser.setSelectedFile(new File(name));
            chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".json");
                }

                @Override
                public String getDescription() {
                    return "JSON Dosyaları (*.json)";
                }
            });

            if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".json")) {
                    file = new File(file.getAbsolutePath() + ".json");
                }
                InvoiceUploadData.UploadSystem jsonData = createJsonInvoiceData(invoice);
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                try (FileWriter writer = new FileWriter(file)) {
                    mapper.writeValue(writer, jsonData);
                }
                JOptionPane.showMessageDialog(frame,
                        "JSON dosyası başarıyla kaydedildi:\n" + file.getAbsolutePath(),
                        "Başarılı",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                    "JSON dışa aktarılırken hata oluştu:\n" + e.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancel() {
        frame.dispose();
        returnToMain.run();
    }
}
