package com.ancienty.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Phase 2 Service Operations GUI
 * Provides client interface for HTTP and TCP API operations
 */
public class ServiceOperationsGUI {

    private static final int OP_BUTTON_WIDTH        = 200;
    private static final int OP_BUTTON_HEIGHT       = 40;
    private static final int CONTROL_BUTTON_WIDTH   = 100;
    private static final int CONTROL_BUTTON_HEIGHT  = 30;
    private static final int CLOSE_BUTTON_WIDTH     = 120;
    private static final int CLOSE_BUTTON_HEIGHT    = 35;
    private static final Dimension TEXTAREA_SIZE    = new Dimension(700, 240);

    private final Runnable returnToMain;
    private final JFrame   frame;

    private JTextField txtServerHost;
    private JTextField txtHttpPort;
    private JTextField txtTcpPort;
    private JTextArea  txtResults;

    public ServiceOperationsGUI(Runnable returnToMain) {
        this.returnToMain = returnToMain;
        this.frame        = new JFrame("Servis İşlemleri");
        initializeGUI();
    }

    private void initializeGUI() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Main title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(15, 20, 10, 20));
        JLabel titleLabel = new JLabel("Hugin Servis İşlemleri - Phase 2", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        frame.add(titlePanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        mainPanel.add(createServerConfigPanel(), BorderLayout.NORTH);
        mainPanel.add(createOperationsPanel(), BorderLayout.CENTER);
        mainPanel.add(createResultsPanel(), BorderLayout.SOUTH);
        frame.add(mainPanel, BorderLayout.CENTER);

        // Bottom buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setBorder(new EmptyBorder(10, 0, 15, 0));
        JButton btnClose = createStyledButton("Kapat", CLOSE_BUTTON_WIDTH, CLOSE_BUTTON_HEIGHT);
        btnClose.addActionListener(e -> close());
        buttonPanel.add(btnClose);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        // Pack and show
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setMinimumSize(frame.getSize());
        frame.setVisible(true);
    }

    private JPanel createServerConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Sunucu Ayarları"),
                new EmptyBorder(10, 10, 10, 10)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Server Host
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Sunucu Adresi:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtServerHost = new JTextField("localhost", 15);
        panel.add(txtServerHost, gbc);

        // HTTP Port
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("HTTP Port:"), gbc);
        gbc.gridx = 3;
        txtHttpPort = new JTextField("8080", 8);
        panel.add(txtHttpPort, gbc);

        // TCP Port
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("TCP Port:"), gbc);
        gbc.gridx = 1;
        txtTcpPort = new JTextField("8888", 8);
        panel.add(txtTcpPort, gbc);

        // Status info
        gbc.gridx = 2; gbc.gridwidth = 2;
        panel.add(new JLabel("Tip: Sunucunun çalıştığından emin olun"), gbc);

        return panel;
    }

    private JPanel createOperationsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Servis İşlemleri"),
                new EmptyBorder(10, 10, 10, 10)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // HTTP API İşlemleri
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel httpLabel = new JLabel("HTTP API İşlemleri", SwingConstants.CENTER);
        panel.add(httpLabel, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1;
        JButton btnHttpUploadXml = createStyledButton("XML Fatura Gönder", OP_BUTTON_WIDTH, OP_BUTTON_HEIGHT);
        btnHttpUploadXml.addActionListener(e -> httpUploadInvoice("xml"));
        panel.add(btnHttpUploadXml, gbc);

        gbc.gridx = 1;
        JButton btnHttpUploadJson = createStyledButton("JSON Fatura Gönder", OP_BUTTON_WIDTH, OP_BUTTON_HEIGHT);
        btnHttpUploadJson.addActionListener(e -> httpUploadInvoice("json"));
        panel.add(btnHttpUploadJson, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JButton btnHttpQuerySeries = createStyledButton("Fatura Sorgula", OP_BUTTON_WIDTH, OP_BUTTON_HEIGHT);
        btnHttpQuerySeries.addActionListener(e -> httpQueryInvoice());
        panel.add(btnHttpQuerySeries, gbc);

        gbc.gridx = 1;
        JButton btnHttpQueryList = createStyledButton("Fatura Listesi", OP_BUTTON_WIDTH, OP_BUTTON_HEIGHT);
        btnHttpQueryList.addActionListener(e -> httpQueryList());
        panel.add(btnHttpQueryList, gbc);

        // Separator
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        // TCP API İşlemleri
        gbc.gridy = 4;
        JLabel tcpLabel = new JLabel("TCP API İşlemleri", SwingConstants.CENTER);
        panel.add(tcpLabel, gbc);

        gbc.gridy = 5; gbc.gridwidth = 1;
        JButton btnTcpUploadXml = createStyledButton("XML Fatura Gönder", OP_BUTTON_WIDTH, OP_BUTTON_HEIGHT);
        btnTcpUploadXml.addActionListener(e -> tcpUploadInvoice("xml"));
        panel.add(btnTcpUploadXml, gbc);

        gbc.gridx = 1;
        JButton btnTcpUploadJson = createStyledButton("JSON Fatura Gönder", OP_BUTTON_WIDTH, OP_BUTTON_HEIGHT);
        btnTcpUploadJson.addActionListener(e -> tcpUploadInvoice("json"));
        panel.add(btnTcpUploadJson, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        JButton btnTcpQuerySeries = createStyledButton("Fatura Sorgula", OP_BUTTON_WIDTH, OP_BUTTON_HEIGHT);
        btnTcpQuerySeries.addActionListener(e -> tcpQueryInvoice());
        panel.add(btnTcpQuerySeries, gbc);

        gbc.gridx = 1;
        JButton btnTcpQueryList = createStyledButton("Fatura Listesi", OP_BUTTON_WIDTH, OP_BUTTON_HEIGHT);
        btnTcpQueryList.addActionListener(e -> tcpQueryList());
        panel.add(btnTcpQueryList, gbc);

        return panel;
    }

    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Sonuçlar"),
                new EmptyBorder(10, 10, 10, 10)
        ));

        txtResults = new JTextArea(12, 40);
        txtResults.setEditable(false);
        txtResults.setLineWrap(true);
        txtResults.setWrapStyleWord(true);
        JScrollPane resultsScrollPane = new JScrollPane(txtResults);
        resultsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        resultsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(resultsScrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnSaveResults = createStyledButton("Kaydet", CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT);
        btnSaveResults.addActionListener(e -> saveResultsToFile());
        JButton btnClear = createStyledButton("Temizle", CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT);
        btnClear.addActionListener(e -> txtResults.setText(""));
        controlPanel.add(btnSaveResults);
        controlPanel.add(btnClear);
        panel.add(controlPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JButton createStyledButton(String text, int width, int height) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(width, height));
        return button;
    }

    private void saveResultsToFile() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File("service_results.txt"));
            fileChooser.setDialogTitle("Sonuçları Kaydet");
            int result = fileChooser.showSaveDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                Files.write(fileChooser.getSelectedFile().toPath(), txtResults.getText().getBytes());
                appendResult("Sonuçlar başarıyla kaydedildi: " +
                        fileChooser.getSelectedFile().getAbsolutePath() + "\n\n");
            }
        } catch (Exception e) {
            appendResult("Dosya kaydetme hatası: " + e.getMessage() + "\n\n");
        }
    }

    // HTTP API Methods

    private void httpUploadInvoice(String type) {
        try {
            appendInfo("HTTP " + type.toUpperCase() + " Upload Başlıyor", "Dosya seçimi bekleniyor...");
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Fatura Dosyası Seç (" + type.toUpperCase() + ")");
            fileChooser.setFileFilter(new FileNameExtensionFilter(type.toUpperCase() + " Dosyaları", type));
            int result = fileChooser.showOpenDialog(frame);
            if (result != JFileChooser.APPROVE_OPTION) {
                appendInfo("HTTP " + type.toUpperCase() + " Upload", "İşlem kullanıcı tarafından iptal edildi");
                return;
            }
            String fileName = fileChooser.getSelectedFile().getName();
            appendInfo("HTTP " + type.toUpperCase() + " Upload", "Dosya seçildi: " + fileName);
            String invoiceData = new String(Files.readAllBytes(fileChooser.getSelectedFile().toPath()));
            String serverUrl = "http://" + txtServerHost.getText() + ":" + txtHttpPort.getText() + "/UploadInvoice";
            appendInfo("HTTP " + type.toUpperCase() + " Upload", "Sunucuya bağlanılıyor: " + serverUrl);
            URL url = new URL(serverUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(5000); // 5s
            conn.setReadTimeout(10000);   // 10s
            conn.setDoOutput(true);
            String formData = "tür=" + URLEncoder.encode(type, "UTF-8") +
                    "&fatura=" + URLEncoder.encode(invoiceData, "UTF-8");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(formData.getBytes("UTF-8"));
            }
            int responseCode = conn.getResponseCode();
            String response = readHttpResponse(conn);
            if (responseCode == 200 && response.contains("Kaydedildi")) {
                appendSuccess("HTTP " + type.toUpperCase() + " Upload",
                        "Dosya: " + fileName + ", Sunucu Yanıtı: " + response);
            } else {
                appendError("HTTP " + type.toUpperCase() + " Upload",
                        "HTTP " + responseCode + " - " + response);
            }
        } catch (java.net.ConnectException e) {
            appendError("HTTP Upload", "Sunucuya bağlanılamadı. Sunucunun çalıştığından emin olun.");
        } catch (java.net.SocketTimeoutException e) {
            appendError("HTTP Upload", "Bağlantı zaman aşımına uğradı. Sunucu yanıt vermiyor.");
        } catch (Exception e) {
            appendError("HTTP Upload", e.getMessage());
        }
    }

    private void httpQueryInvoice() {
        try {
            String[] options = {"Seri numarasına göre", "İsme göre"};
            int choice = JOptionPane.showOptionDialog(frame,
                    "Sorgu türünü seçin:",
                    "Fatura Sorgula",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            if (choice == -1) return;
            String serverUrl;
            if (choice == 0) {
                String series = JOptionPane.showInputDialog(frame, "Fatura Serisi:");
                if (series == null) return;
                String number = JOptionPane.showInputDialog(frame, "Fatura Numarası:");
                if (number == null) return;
                serverUrl = "http://" + txtServerHost.getText() + ":" + txtHttpPort.getText() +
                        "/QueryInvoice?tur=seri&seri=" + URLEncoder.encode(series, "UTF-8") +
                        "&no=" + URLEncoder.encode(number, "UTF-8");
            } else {
                String customerName = JOptionPane.showInputDialog(frame, "Müşteri Adı:");
                if (customerName == null) return;
                serverUrl = "http://" + txtServerHost.getText() + ":" + txtHttpPort.getText() +
                        "/QueryInvoice?tur=name&name=" + URLEncoder.encode(customerName, "UTF-8");
            }
            URL url = new URL(serverUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            String response = readHttpResponse(conn);
            appendResult("HTTP Query Invoice:\n" +
                    "Response Code: " + responseCode + "\n" +
                    "Response: " + response + "\n\n");
        } catch (Exception e) {
            appendResult("HTTP Query Error: " + e.getMessage() + "\n\n");
        }
    }

    private void httpQueryList() {
        try {
            String serverUrl = "http://" + txtServerHost.getText() + ":" + txtHttpPort.getText() +
                    "/QueryInvoice?tur=liste";
            URL url = new URL(serverUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            String response = readHttpResponse(conn);
            appendResult("HTTP Query List:\n" +
                    "Response Code: " + responseCode + "\n" +
                    "Response: " + response + "\n\n");
        } catch (Exception e) {
            appendResult("HTTP Query List Error: " + e.getMessage() + "\n\n");
        }
    }

    // TCP API Methods

    private void tcpUploadInvoice(String type) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Fatura Dosyası Seç (" + type.toUpperCase() + ")");
            int result = fileChooser.showOpenDialog(frame);
            if (result != JFileChooser.APPROVE_OPTION) return;
            String fileName = fileChooser.getSelectedFile().getName();
            appendInfo("TCP " + type.toUpperCase() + " Upload", "Dosya seçildi: " + fileName);
            String invoiceData = new String(Files.readAllBytes(fileChooser.getSelectedFile().toPath()));
            try (Socket socket = new Socket(txtServerHost.getText(), Integer.parseInt(txtTcpPort.getText()));
                 DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                 DataInputStream input   = new DataInputStream(socket.getInputStream())) {
                appendInfo("TCP " + type.toUpperCase() + " Upload", "Sunucuya bağlanılıyor...");
                sendBinaryTcpMessage(output, 1, type.equals("xml") ? 1 : 2, invoiceData);
                BinaryTcpResponse response = readBinaryTcpResponse(input);
                if (response != null) {
                    if (response.content.contains("Kaydedildi")) {
                        appendSuccess("TCP " + type.toUpperCase() + " Upload",
                                "Dosya: " + fileName + ", Sunucu Yanıtı: " + response.content);
                    } else {
                        appendError("TCP " + type.toUpperCase() + " Upload",
                                "Sunucu Yanıtı: " + response.content);
                    }
                } else {
                    appendError("TCP Upload", "Sunucudan yanıt alınamadı");
                }
            }
        } catch (java.net.ConnectException e) {
            appendError("TCP Upload", "Sunucuya bağlanılamadı. Sunucunun çalıştığından emin olun.");
        } catch (java.net.SocketTimeoutException e) {
            appendError("TCP Upload", "Bağlantı zaman aşımına uğradı. Sunucu yanıt vermiyor.");
        } catch (Exception e) {
            appendError("TCP Upload", e.getMessage());
        }
    }

    private void tcpQueryInvoice() {
        try {
            String[] options = {"Seri numarasına göre", "İsme göre"};
            int choice = JOptionPane.showOptionDialog(frame,
                    "Sorgu türünü seçin:",
                    "Fatura Sorgula",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            if (choice == -1) return;
            String queryData;
            int queryType;
            if (choice == 0) {
                String series = JOptionPane.showInputDialog(frame, "Fatura Serisi:");
                if (series == null) return;
                String number = JOptionPane.showInputDialog(frame, "Fatura Numarası:");
                if (number == null) return;
                queryData = series + " " + number;
                queryType = 1;
                appendInfo("TCP Query Invoice", "Seri-numara sorgusu: " + queryData);
            } else {
                String customerName = JOptionPane.showInputDialog(frame, "Müşteri Adı:");
                if (customerName == null) return;
                queryData = customerName;
                queryType = 2;
                appendInfo("TCP Query Invoice", "İsim sorgusu: " + queryData);
            }
            try (Socket socket = new Socket(txtServerHost.getText(), Integer.parseInt(txtTcpPort.getText()));
                 DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                 DataInputStream input   = new DataInputStream(socket.getInputStream())) {
                sendBinaryTcpMessage(output, 2, queryType, queryData);
                BinaryTcpResponse response = readBinaryTcpResponse(input);
                if (response != null) {
                    if ("Kayıt bulunamadı".equals(response.content)) {
                        appendInfo("TCP Query Invoice", "Sonuç: " + response.content);
                    } else {
                        appendSuccess("TCP Query Invoice", "Toplam tutar: " + response.content + " TL");
                    }
                } else {
                    appendError("TCP Query Invoice", "Sunucudan yanıt alınamadı");
                }
            }
        } catch (java.net.ConnectException e) {
            appendError("TCP Query Invoice", "Sunucuya bağlanılamadı. Sunucunun çalıştığından emin olun.");
        } catch (java.net.SocketTimeoutException e) {
            appendError("TCP Query Invoice", "Bağlantı zaman aşımına uğradı. Sunucu yanıt vermiyor.");
        } catch (Exception e) {
            appendError("TCP Query Invoice", e.getMessage());
        }
    }

    private void tcpQueryList() {
        try (Socket socket = new Socket(txtServerHost.getText(), Integer.parseInt(txtTcpPort.getText()));
             DataOutputStream output = new DataOutputStream(socket.getOutputStream());
             DataInputStream input   = new DataInputStream(socket.getInputStream())) {
            appendInfo("TCP Query List", "Fatura listesi isteniyor...");
            sendBinaryTcpMessage(output, 2, 2, "ALL_INVOICES");
            BinaryTcpResponse response = readBinaryTcpResponse(input);
            if (response != null) {
                appendResult("TCP Query List:\nResponse: " + response.content + "\n\n");
            } else {
                appendError("TCP Query List", "Sunucudan yanıt alınamadı");
            }
        } catch (java.net.ConnectException e) {
            appendError("TCP Query List", "Sunucuya bağlanılamadı. Sunucunun çalıştığından emin olun.");
        } catch (java.net.SocketTimeoutException e) {
            appendError("TCP Query List", "Bağlantı zaman aşımına uğradı. Sunucu yanıt vermiyor.");
        } catch (Exception e) {
            appendError("TCP Query List", e.getMessage());
        }
    }

    /**
     * Send binary TCP message according to Phase 2 protocol:
     * - 2 bytes: Message Length (command+type+content length)
     * - 1 byte: Command (1=UploadInvoice, 2=QueryInvoice)
     * - 1 byte: Type (1=XML or series upload/query, 2=JSON or name upload/query)
     * - Remaining bytes: UTF-8 content
     */
    private void sendBinaryTcpMessage(DataOutputStream output, int command, int type, String content) throws IOException {
        byte[] contentBytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int messageLength = 1 + contentBytes.length;
        appendInfo("TCP Binary Protocol",
                String.format("Gönderiliyor - Komut: %d, Tür: %d, İçerik uzunluğu: %d",
                        command, type, contentBytes.length));
        output.writeShort(messageLength);
        output.writeByte(command);
        output.writeByte(type);
        output.write(contentBytes);
        output.flush();
    }

    /**
     * Read binary TCP response according to Phase 2 protocol
     */
    private BinaryTcpResponse readBinaryTcpResponse(DataInputStream input) throws IOException {
        try {
            int messageLength = input.readUnsignedShort();
            int command       = input.readUnsignedByte();
            int type          = input.readUnsignedByte();
            int contentLength = messageLength - 1;
            if (contentLength < 0) {
                appendError("TCP Binary Protocol", "Geçersiz mesaj uzunluğu: " + messageLength);
                return null;
            }
            byte[] contentBytes = new byte[contentLength];
            input.readFully(contentBytes);
            String content = new String(contentBytes, java.nio.charset.StandardCharsets.UTF_8);
            appendInfo("TCP Binary Protocol",
                    String.format("Alındı - Komut: %d, Tür: %d, Yanıt: %s", command, type, content));
            return new BinaryTcpResponse(messageLength, command, type, content);
        } catch (IOException e) {
            appendError("TCP Binary Protocol", "Yanıt okuma hatası: " + e.getMessage());
            return null;
        }
    }

    private static class BinaryTcpResponse {
        final int    messageLength;
        final int    command;
        final int    type;
        final String content;
        BinaryTcpResponse(int messageLength, int command, int type, String content) {
            this.messageLength = messageLength;
            this.command       = command;
            this.type          = type;
            this.content       = content;
        }
    }

    private String readHttpResponse(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        }
    }

    private void appendResult(String text) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (txtResults.getText().contains("Servis işlemleri sonuçları burada")) {
                txtResults.setText("");
            }
            txtResults.append("[" + timestamp + "] " + text);
            txtResults.setCaretPosition(txtResults.getDocument().getLength());
        });
    }

    private void appendSuccess(String operation, String details) {
        appendResult("[BAŞARILI] " + operation + "\n");
        if (!details.isEmpty()) {
            appendResult("   Detaylar: " + details + "\n");
        }
        appendResult("   ─────────────────────────────────────\n\n");
    }

    private void appendError(String operation, String error) {
        appendResult("[HATA] " + operation + "\n");
        appendResult("   Hata: " + error + "\n");
        appendResult("   ─────────────────────────────────────\n\n");
    }

    private void appendInfo(String operation, String info) {
        appendResult("[BİLGİ] " + operation + "\n");
        appendResult("   " + info + "\n");
        appendResult("   ─────────────────────────────────────\n\n");
    }

    private void close() {
        frame.dispose();
        returnToMain.run();
    }
}
