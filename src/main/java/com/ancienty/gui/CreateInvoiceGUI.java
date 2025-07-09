package com.ancienty.gui;

import com.ancienty.database.Database;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 1 Invoice Creation Form - Enhanced with better item management
 */
public class CreateInvoiceGUI {

    private final Database database;
    private final Runnable returnToMain;
    private final JFrame frame;

    private JTextField txtSeries;
    private JTextField txtNumber;
    private JLabel     lblSelectedCustomer;
    private JLabel     lblCustomerInfo;
    private JTextField txtDiscount;
    private JLabel     lblTotalBefore;
    private JLabel     lblTotalAfter;
    private DefaultTableModel tableModel;
    private JTable            itemTable;

    private Database.Customer selectedCustomer = null;
    private Map<String, Double> selectedItems   = new HashMap<>();

    public CreateInvoiceGUI(Database database, Runnable returnToMain) {
        this.database     = database;
        this.returnToMain = returnToMain;
        this.frame        = new JFrame("Fatura Oluştur");
        initializeGUI();
    }

    // Add at top of class
    private static final int BTN_W        = 100;
    private static final int BTN_H        = 28;
    private static final Dimension FIELD_SIZE      = new Dimension(120, 24);
    private static final Dimension ITEM_TABLE_SIZE = new Dimension(400, 150);

    private void initializeGUI() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        JLabel titleLabel = new JLabel("Fatura Oluştur", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        frame.add(titlePanel, BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(4, 4, 4, 4);
        gbc.anchor  = GridBagConstraints.WEST;

        // Invoice Details
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        mainPanel.add(createInvoiceDetailsPanel(), gbc);

        // Customer Section
        gbc.gridy = 1; gbc.gridwidth = 4;
        mainPanel.add(createCustomerPanel(), gbc);

        // Items Section
        gbc.gridy = 2; gbc.gridwidth = 4;
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        mainPanel.add(createItemsPanel(), gbc);

        // Calculation Section
        gbc.gridy = 3; gbc.gridwidth = 4;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; gbc.weighty = 0;
        mainPanel.add(createCalculationPanel(), gbc);

        frame.add(mainPanel, BorderLayout.CENTER);

        // Bottom buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        buttonPanel.setBorder(new EmptyBorder(4, 0, 4, 0));
        JButton btnSave   = createStyledButton("Kaydet", BTN_W, BTN_H);
        btnSave.addActionListener(e -> saveInvoice());
        JButton btnCancel = createStyledButton("Vazgeç", BTN_W, BTN_H);
        btnCancel.addActionListener(e -> cancel());
        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    private JPanel createInvoiceDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Fatura Bilgileri"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Seri
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Seri:"), gbc);
        gbc.gridx = 1;
        txtSeries = new JTextField();
        txtSeries.setPreferredSize(FIELD_SIZE);
        styleTextField(txtSeries);
        panel.add(txtSeries, gbc);

        // Numara
        gbc.gridx = 2;
        panel.add(new JLabel("Numara:"), gbc);
        gbc.gridx = 3;
        txtNumber = new JTextField();
        txtNumber.setPreferredSize(FIELD_SIZE);
        styleTextField(txtNumber);
        panel.add(txtNumber, gbc);

        return panel;
    }

    private JPanel createCustomerPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Müşteri Bilgileri"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        JButton btnSelect = createStyledButton("Müşteri Seç", BTN_W, BTN_H);
        btnSelect.addActionListener(e -> selectCustomer());
        panel.add(btnSelect, gbc);

        gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        lblSelectedCustomer = new JLabel("Seçilen müşteri: (hiçbiri)");
        panel.add(lblSelectedCustomer, gbc);

        gbc.gridy = 2; gbc.gridwidth = 2;
        lblCustomerInfo = new JLabel(" ");
        panel.add(lblCustomerInfo, gbc);

        return panel;
    }

    private JPanel createItemsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Ürünler"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Buttons
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createStyledButton("Ekle", BTN_W, BTN_H), gbc);
        gbc.gridx = 1;
        panel.add(createStyledButton("Düzenle", BTN_W, BTN_H), gbc);
        gbc.gridx = 2;
        panel.add(createStyledButton("Sil", BTN_W, BTN_H), gbc);

        // Table
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3;
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        tableModel = new DefaultTableModel(new String[]{"Ad","Fiyat","Miktar","Toplam"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        itemTable = new JTable(tableModel);
        itemTable.setRowHeight(22);
        JScrollPane scroll = new JScrollPane(itemTable);
        scroll.setPreferredSize(ITEM_TABLE_SIZE);
        panel.add(scroll, gbc);

        // Tip
        gbc.gridy = 2; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.NONE;
        JLabel tip = new JLabel("<html><i>Ürünleri düzenlemek için çift tıklayın</i></html>");
        panel.add(tip, gbc);

        return panel;
    }

    private JPanel createCalculationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("İndirim ve Toplam"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Discount
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("İndirim:"), gbc);
        gbc.gridx = 1;
        txtDiscount = new JTextField("0");
        txtDiscount.setPreferredSize(FIELD_SIZE);
        styleTextField(txtDiscount);
        panel.add(txtDiscount, gbc);

        // Calculate
        gbc.gridx = 2; gbc.gridwidth = 2;
        panel.add(createStyledButton("Hesapla", BTN_W, BTN_H), gbc);

        // Totals
        gbc.gridy = 1; gbc.gridx = 0; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel totals = new JPanel(new GridBagLayout());
        totals.setBorder(new EmptyBorder(4,0,0,0));
        GridBagConstraints tGbc = new GridBagConstraints();
        tGbc.insets = new Insets(4,4,4,4);
        totals.add(new JLabel("Önce:"), tGbc);
        tGbc.gridx = 1; lblTotalBefore = new JLabel("0.00"); totals.add(lblTotalBefore, tGbc);
        tGbc.gridx = 2; totals.add(new JLabel("Sonra:"), tGbc);
        tGbc.gridx = 3; lblTotalAfter = new JLabel("0.00"); totals.add(lblTotalAfter, tGbc);
        panel.add(totals, gbc);

        return panel;
    }

    private JButton createStyledButton(String text, int w, int h) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(w, h));
        return b;
    }

    private void styleTextField(JTextField tf) {
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }

    private void selectCustomer() {
        String[] customers = database.getCustomers();
        String selected = (String) JOptionPane.showInputDialog(
                frame, "Müşteri seçin:", "Müşteri Seç",
                JOptionPane.PLAIN_MESSAGE, null, customers, null
        );
        if (selected == null) return;
        if (selected.equals("Yeni Müşteri")) {
            showAddCustomerDialog();
        } else {
            int id = Integer.parseInt(selected.substring(1, selected.indexOf(")")));
            selectedCustomer = database.getCustomer(id);
            if (selectedCustomer != null) {
                lblSelectedCustomer.setText("Seçilen müşteri: " + selectedCustomer.name);
                lblCustomerInfo.setText("TCKN/Vergi No: " + selectedCustomer.ssn +
                        " | Tip: " + (selectedCustomer.isCompany ? "Şirket" : "Şahıs"));
            }
        }
    }

    private void showAddCustomerDialog() {
        JDialog dialog = new JDialog(frame, "Yeni Müşteri", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(5,5,5,5);

        gbc.gridx=0; gbc.gridy=0; dialog.add(new JLabel("Ad:"), gbc);
        gbc.gridx=1; JTextField txtName = new JTextField(15); dialog.add(txtName, gbc);
        gbc.gridx=0; gbc.gridy=1; dialog.add(new JLabel("TCKN/Vergi No:"), gbc);
        gbc.gridx=1; JTextField txtSsn  = new JTextField(15); dialog.add(txtSsn, gbc);
        gbc.gridx=0; gbc.gridy=2; gbc.gridwidth=2; JCheckBox chkComp = new JCheckBox("Şirket");
        dialog.add(chkComp, gbc);

        gbc.gridy=3; gbc.gridwidth=1;
        JButton btnSave = new JButton("Kaydet");
        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim(), ssn = txtSsn.getText().trim();
            if (name.isEmpty()||ssn.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,"Tüm alanları doldurun.","Hata",JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (database.addCustomer(name, ssn, chkComp.isSelected())) {
                JOptionPane.showMessageDialog(dialog,"Müşteri eklendi.","Başarılı",JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose(); selectCustomer();
            } else {
                JOptionPane.showMessageDialog(dialog,"Hata oluştu.","Hata",JOptionPane.ERROR_MESSAGE);
            }
        });
        gbc.gridx=1; dialog.add(btnSave, gbc);

        gbc.gridx=0; JButton btnCancel = new JButton("İptal");
        btnCancel.addActionListener(e -> dialog.dispose());
        dialog.add(btnCancel, gbc);

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void addProduct() {
        Map<String, Double> items = database.getItems();
        if (items.isEmpty()) {
            if (JOptionPane.showConfirmDialog(frame,
                    "Ürün yok. Yeni eklemek ister misiniz?",
                    "Ürün Yok", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                showAddItemDialog();
            }
            return;
        }
        JDialog dialog = new JDialog(frame, "Ürün Ekle", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets=new Insets(5,5,5,5);

        gbc.gridx=0; gbc.gridy=0; dialog.add(new JLabel("Ürün:"), gbc);
        gbc.gridx=1;
        JComboBox<String> cmb = new JComboBox<>(items.keySet().toArray(new String[0]));
        cmb.setPreferredSize(new Dimension(200,25)); dialog.add(cmb,gbc);

        gbc.gridx=0; gbc.gridy=1; dialog.add(new JLabel("Miktar:"), gbc);
        gbc.gridx=1; JTextField txtQty = new JTextField("1",10); dialog.add(txtQty,gbc);

        gbc.gridx=0; gbc.gridy=2; dialog.add(new JLabel("Birim Fiyat:"), gbc);
        gbc.gridx=1; JLabel lblPrice = new JLabel();
        updatePriceLabel(lblPrice, cmb, items);
        dialog.add(lblPrice, gbc);

        cmb.addActionListener(e -> updatePriceLabel(lblPrice, cmb, items));

        gbc.gridy=3; gbc.gridwidth=1;
        JButton btnAdd = new JButton("Ekle");
        btnAdd.addActionListener(e -> {
            try {
                String item = (String)cmb.getSelectedItem();
                double qty = Double.parseDouble(txtQty.getText().trim());
                if (qty<=0) throw new NumberFormatException();
                selectedItems.merge(item, qty, Double::sum);
                updateItemTable(); calculateInvoice(); dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,"Geçerli miktar girin.","Hata",JOptionPane.ERROR_MESSAGE);
            }
        });
        gbc.gridx=1; dialog.add(btnAdd,gbc);

        gbc.gridx=0; JButton btnDlgCancel = new JButton("İptal");
        btnDlgCancel.addActionListener(e -> dialog.dispose());
        dialog.add(btnDlgCancel,gbc);

        gbc.gridx=0; gbc.gridy=4; gbc.gridwidth=2;
        JButton btnNew = new JButton("Yeni Ürün Ekle");
        btnNew.addActionListener(e -> { dialog.dispose(); showAddItemDialog(); });
        dialog.add(btnNew,gbc);

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void updatePriceLabel(JLabel lblPrice, JComboBox<String> cmbItems, Map<String, Double> items) {
        Object sel = cmbItems.getSelectedItem();
        if (sel != null) {
            double price = items.get(sel.toString());
            lblPrice.setText(String.format("%.2f TL", price));
        }
    }

    private void editSelectedItem() {
        int r = itemTable.getSelectedRow();
        if (r<0) {
            JOptionPane.showMessageDialog(frame,"Ürün seçin.","Bilgi",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String name = (String)tableModel.getValueAt(r,0);
        double cur = selectedItems.get(name);
        String in = JOptionPane.showInputDialog(frame,"Yeni miktar ("+name+"):",String.valueOf(cur));
        if (in==null) return;
        try {
            double nq = Double.parseDouble(in);
            if (nq<=0) throw new NumberFormatException();
            selectedItems.put(name, nq);
            updateItemTable(); calculateInvoice();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame,"Geçerli miktar girin.","Hata",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedItem() {
        int r = itemTable.getSelectedRow();
        if (r<0) {
            JOptionPane.showMessageDialog(frame,"Ürün seçin.","Bilgi",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String name = (String)tableModel.getValueAt(r,0);
        if (JOptionPane.showConfirmDialog(frame,
                "'" + name + "' silinsin mi?","Ürün Sil",
                JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
            selectedItems.remove(name);
            updateItemTable(); calculateInvoice();
        }
    }

    private void showAddItemDialog() {
        JDialog d = new JDialog(frame,"Yeni Ürün",true);
        d.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets=new Insets(5,5,5,5);

        gbc.gridx=0; gbc.gridy=0; d.add(new JLabel("Ürün Adı:"),gbc);
        gbc.gridx=1; JTextField txtName = new JTextField(15); d.add(txtName,gbc);
        gbc.gridx=0; gbc.gridy=1; d.add(new JLabel("Birim Fiyat:"),gbc);
        gbc.gridx=1; JTextField txtPrice = new JTextField(15); d.add(txtPrice,gbc);

        gbc.gridy=2; gbc.gridwidth=1;
        JButton btnSave = new JButton("Kaydet");
        btnSave.addActionListener(e -> {
            String nm = txtName.getText().trim(), pr=txtPrice.getText().trim();
            if (nm.isEmpty()||pr.isEmpty()) {
                JOptionPane.showMessageDialog(d,"Tüm alanları doldurun.","Hata",JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                double p = Double.parseDouble(pr);
                if (p<=0) throw new NumberFormatException();
                if (database.addItem(nm,p)) {
                    JOptionPane.showMessageDialog(d,"Ürün eklendi.","Başarılı",JOptionPane.INFORMATION_MESSAGE);
                    d.dispose(); addProduct();
                } else {
                    JOptionPane.showMessageDialog(d,"Hata oluştu.","Hata",JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(d,"Geçerli fiyat girin.","Hata",JOptionPane.ERROR_MESSAGE);
            }
        });
        gbc.gridx=1; d.add(btnSave,gbc);

        gbc.gridx=0; JButton btnCancel = new JButton("İptal");
        btnCancel.addActionListener(e -> d.dispose());
        d.add(btnCancel,gbc);

        d.pack(); d.setLocationRelativeTo(frame); d.setVisible(true);
    }

    private void updateItemTable() {
        tableModel.setRowCount(0);
        var items = database.getItems();
        for (var e : selectedItems.entrySet()) {
            String name = e.getKey();
            double qty = e.getValue();
            double unit = items.get(name);
            tableModel.addRow(new Object[]{
                    name,
                    String.format("%.2f", unit),
                    String.format("%.2f", qty),
                    String.format("%.2f", unit * qty)
            });
        }
    }

    private void calculateInvoice() {
        var items = database.getItems();
        double before = selectedItems.entrySet().stream()
                .mapToDouble(en -> items.get(en.getKey()) * en.getValue()).sum();
        double discount = 0;
        try {
            discount = Double.parseDouble(txtDiscount.getText().trim());
            if (discount < 0) discount = 0;
            if (discount > before) discount = before;
        } catch (NumberFormatException ignored) {
            discount = 0;
            txtDiscount.setText("0");
        }
        lblTotalBefore.setText(String.format("%.2f", before));
        lblTotalAfter.setText(String.format("%.2f", before - discount));
    }

    private void saveInvoice() {
        String series = txtSeries.getText().trim();
        String number = txtNumber.getText().trim();
        if (series.isEmpty() || number.isEmpty()) {
            JOptionPane.showMessageDialog(frame,"Seri ve numara zorunlu.","Hata",JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedCustomer == null) {
            JOptionPane.showMessageDialog(frame,"Müşteri seçin.","Hata",JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedItems.isEmpty()) {
            JOptionPane.showMessageDialog(frame,"En az bir ürün ekleyin.","Hata",JOptionPane.ERROR_MESSAGE);
            return;
        }
        double discount;
        try {
            discount = Double.parseDouble(txtDiscount.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame,"Geçerli indirim girin.","Hata",JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (database.saveInvoice(series, number, selectedCustomer.id, discount, selectedItems)) {
            JOptionPane.showMessageDialog(frame,"Fatura kaydedildi.","Başarılı",JOptionPane.INFORMATION_MESSAGE);
            frame.dispose();
            returnToMain.run();
        } else {
            JOptionPane.showMessageDialog(frame,"Kaydetme hatası.","Hata",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancel() {
        frame.dispose();
        returnToMain.run();
    }
}
