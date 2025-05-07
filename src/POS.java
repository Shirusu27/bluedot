import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class POS {

    private JTextField txtProductCode, txtProductName,txtCategory, txtPrice, txtTotal, txtPay, txtBalance;
    private JSpinner spinnerQty;
    private JTable table, historyTable;
    private DefaultTableModel tableModel, historyTableModel;
    private JTextArea receiptArea;
    private JPanel mainPanel;
    private JComboBox<String> categoryDropdown;

    private JSpinner dateFromSpinner, dateToSpinner;
    private JPopupMenu searchPopupMenu; // For search results popup
    private JList<String> searchSuggestionList; // JList for suggestions
    private DefaultListModel<String> searchListModel; // Model for JList
    private String lastReceiptText = ""; //String for last Receipt


    private static final String DB_URL = "jdbc:ucanaccess://C://Users//ADMIN//IdeaProjects//bluedot//bluedotDatabase.accdb";

    public POS() {
        // Set up the main frame with a modern flat background color (no transparency)
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(236, 239, 241)); // Light modern gray

        // Input Panel (solid background, rounded border)
        JPanel inputPanel = new JPanel(new GridLayout(2, 7, 10, 10));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 150, 243), 2, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        txtProductCode = createModernTextField();
        txtProductName = createModernTextField();
        spinnerQty = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        styleSpinner(spinnerQty);
        txtPrice = createModernTextField();
        txtPrice.setEditable(false);
        txtPrice.setBackground(new Color(245, 245, 245)); // light gray
        JTextField txtItemTotal = createModernTextField();
        txtItemTotal.setEditable(false);
        txtItemTotal.setBackground(new Color(245, 245, 245));

        // Initialize the search popup menu with a JList for product name search
        searchListModel = new DefaultListModel<>();
        searchSuggestionList = new JList<>(searchListModel);
        searchSuggestionList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchSuggestionList.setBackground(Color.WHITE);
        searchSuggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchSuggestionList.setVisibleRowCount(5);
        JScrollPane listScrollPane = new JScrollPane(searchSuggestionList);
        listScrollPane.setPreferredSize(new Dimension(300, 150));

        searchPopupMenu = new JPopupMenu();
        searchPopupMenu.add(listScrollPane);

        // Add KeyListener to txtProductName for live search
        txtProductName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (searchPopupMenu.isVisible()) {
                    int selectedIndex = searchSuggestionList.getSelectedIndex();
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DOWN:
                            if (selectedIndex < searchListModel.getSize() - 1) {
                                searchSuggestionList.setSelectedIndex(selectedIndex + 1);
                                searchSuggestionList.ensureIndexIsVisible(selectedIndex + 1);
                            }
                            e.consume();
                            break;
                        case KeyEvent.VK_UP:
                            if (selectedIndex > 0) {
                                searchSuggestionList.setSelectedIndex(selectedIndex - 1);
                                searchSuggestionList.ensureIndexIsVisible(selectedIndex - 1);
                            }
                            e.consume();
                            break;
                        case KeyEvent.VK_ENTER:
                            if (selectedIndex >= 0) {
                                String selectedItem = searchSuggestionList.getSelectedValue();
                                selectProductFromSuggestion(selectedItem);
                                searchPopupMenu.setVisible(false);
                                // Return focus so user can keep typing
                                txtProductName.requestFocusInWindow();
                            }
                            e.consume();
                            break;
                        case KeyEvent.VK_ESCAPE:
                            searchPopupMenu.setVisible(false);
                            txtProductName.requestFocusInWindow();
                            e.consume();
                            break;
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // Only handle text-changing keys for suggestions
                if (e.getKeyCode() == KeyEvent.VK_DOWN ||
                        e.getKeyCode() == KeyEvent.VK_UP ||
                        e.getKeyCode() == KeyEvent.VK_ENTER ||
                        e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    return;
                }

                String searchText = txtProductName.getText().trim();
                if (!searchText.isEmpty()) {
                    updateSearchSuggestions(searchText);

                    if (searchListModel.getSize() > 0) {
                        if (!searchPopupMenu.isVisible()) {
                            searchSuggestionList.setSelectedIndex(0);
                            searchPopupMenu.show(txtProductName, 0, txtProductName.getHeight());
                            // Ensure focus returns to text field!
                            txtProductName.requestFocusInWindow();
                        }
                    } else {
                        searchPopupMenu.setVisible(false);
                    }
                } else {
                    searchPopupMenu.setVisible(false);
                    searchListModel.clear();
                }
            }
        });





        // Add MouseListener to JList for clicking suggestions
        searchSuggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    String selectedItem = searchSuggestionList.getSelectedValue();
                    if (selectedItem != null) {
                        selectProductFromSuggestion(selectedItem);
                        searchPopupMenu.setVisible(false);
                        txtProductName.requestFocusInWindow(); // <-- ensure focus returns to text field
                    }
                }
            }
        });


        // Add a PopupMenuListener to prevent hiding when interacting with the popup
        searchPopupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if (txtProductName.hasFocus()) {
                    String searchText = txtProductName.getText().trim();
                    if (!searchText.isEmpty() && searchListModel.getSize() > 0) {
                        searchPopupMenu.show(txtProductName, 0, txtProductName.getHeight());
                    }
                }
            }
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        JButton btnAdd = createModernButton("Add", new Color(76, 175, 80));
        JButton btnFinalize = createModernButton("Finalize Sale", new Color(255,152,0));
        btnFinalize.addActionListener(e -> finalizeSale());
        JButton btnPrint = createModernButton("Print", new Color(33, 150, 243));

        btnAdd.addActionListener(e -> {
            String code = txtProductCode.getText().trim();
            String name = txtProductName.getText().trim();
            String priceText = txtPrice.getText().trim();

            if (tableModel.getRowCount() == 0) { // cart is empty before adding
                receiptArea.setText("");
                lastReceiptText = "";
            }

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String existingCode = (String) tableModel.getValueAt(i, 0);
                if (existingCode.equals(code)) {
                    JOptionPane.showMessageDialog(mainPanel, "Product already added to the cart.");
                    return;
                }
            }

            if (code.isEmpty() || name.isEmpty() || priceText.isEmpty()) {
                JOptionPane.showMessageDialog(mainPanel, "Please fill in all product fields.");
                return;
            }

            try {
                int qty = (int) spinnerQty.getValue();
                double price = Double.parseDouble(priceText);
                double total = qty * price;

                String category = "";
                try (Connection conn = DriverManager.getConnection(DB_URL);
                     PreparedStatement ps = conn.prepareStatement("SELECT Category FROM Item WHERE ID = ?")) {
                    ps.setString(1, code);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        category = rs.getString("Category");
                    }
                } catch (SQLException ex2) {
                    ex2.printStackTrace();
                }

                tableModel.addRow(new Object[]{code, name, category, qty, price, total});
                updateTotal();

                txtProductCode.setText("");
                txtProductName.setText("");
                spinnerQty.setValue(1);
                txtPrice.setText("");
                txtItemTotal.setText("");
                spinnerQty.setValue(0);
                categoryDropdown.setSelectedIndex(0);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(mainPanel, "Invalid price input.");
            }
        });

        btnPrint.addActionListener(e -> {
            if (lastReceiptText == null || lastReceiptText.trim().isEmpty()) {
                JOptionPane.showMessageDialog(getMainPanel(),
                        "There is no receipt to print. Please finalize a sale first.",
                        "Print Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            printBill(lastReceiptText);
        });



        txtCategory = createModernTextField();
        txtCategory.setEditable(false); // Read only!


        inputPanel.add(createModernLabel("Product Code"));
        inputPanel.add(createModernLabel("Product Name"));
        inputPanel.add(createModernLabel("Category"));
        inputPanel.add(createModernLabel("Qty"));
        inputPanel.add(createModernLabel("Price"));
        inputPanel.add(createModernLabel("Total"));
        inputPanel.add(new JLabel(""));

        inputPanel.add(txtProductCode);
        inputPanel.add(txtProductName);
        inputPanel.add(txtCategory);
        inputPanel.add(spinnerQty);
        inputPanel.add(txtPrice);
        inputPanel.add(txtItemTotal);
        inputPanel.add(btnAdd);

        txtProductCode.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String code = txtProductCode.getText().trim();
                if (!code.isEmpty()) {
                    try (Connection conn = DriverManager.getConnection(DB_URL);
                         PreparedStatement ps = conn.prepareStatement("SELECT * FROM Item WHERE ID = ?")) {
                        ps.setString(1, code);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            txtProductName.setText(rs.getString("Model"));
                            txtPrice.setText(String.format("%.2f", rs.getDouble("Unit_Price")));
                            String categoryFromDB = rs.getString("Category");
                            txtCategory.setText(categoryFromDB);
                        }
                    } catch (SQLException ignored) {}
                }
            }
        });



        spinnerQty.addChangeListener((ChangeEvent e) -> updateItemTotal(txtItemTotal));
        txtPrice.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                updateItemTotal(txtItemTotal);
            }
        });

        tableModel = new DefaultTableModel(new Object[]{"Product Code", "Product Name", "Category", "Qty", "Price", "Total"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = styleTable(new JTable(tableModel));
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(33, 150, 243), 2, true));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(30);

        int[] widths = {150, 335, 160, 135, 135, 135};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setResizable(false);
        }

        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBackground(Color.WHITE);

        txtTotal = createSummaryField(summaryPanel, "Total");
        txtTotal.setBackground(new Color(245, 245, 245));
        txtTotal.setEditable(false);
        txtPay = createSummaryField(summaryPanel, "Pay");
        txtBalance = createSummaryField(summaryPanel, "Balance");
        txtBalance.setBackground(new Color(245, 245, 245));
        txtBalance.setEditable(false);

        txtPay.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                try {
                    double pay = Double.parseDouble(txtPay.getText());
                    double total = Double.parseDouble(txtTotal.getText());
                    txtBalance.setText(String.format("%.2f", pay - total));
                } catch (NumberFormatException ex) {
                    txtBalance.setText("0.00");
                }
            }
        });

        historyTableModel = new DefaultTableModel(
                new Object[]{"Transaction ID", "Product Code", "Product Name", "Category", "Qty", "Unit Price", "Total", "Date"}, 0
        ) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        historyTable = styleTable(new JTable(historyTableModel));
        historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        historyTable.setRowHeight(25);

        int[] historyWidths = {100, 100, 150, 100, 80, 150, 150, 200};
        for (int i = 0; i < historyWidths.length; i++) {
            historyTable.getColumnModel().getColumn(i).setPreferredWidth(historyWidths[i]);
            historyTable.getColumnModel().getColumn(i).setResizable(false);
        }

        JButton btnClear = createModernButton("Clear Orders", new Color(244, 67, 54));
        btnClear.addActionListener(e -> {
            tableModel.setRowCount(0);
            txtTotal.setText("0.00");
            txtPay.setText("0.00");
            txtBalance.setText("0.00");
            receiptArea.setText("");
        });

        JButton btnDeleteRow = createModernButton("Delete Row", new Color(244, 67, 54));
        btnDeleteRow.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                tableModel.removeRow(selectedRow);
                updateTotal();
            } else {
                JOptionPane.showMessageDialog(mainPanel, "Please select a row to delete.");
            }
        });

        JButton btnShowHistory = createModernButton("Transaction History", new Color(255, 193, 7));
        btnShowHistory.addActionListener(e -> showTransactionHistoryDialog());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.setBackground(Color.WHITE);

        buttonPanel.add(btnClear);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(btnDeleteRow);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(btnFinalize);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(btnPrint);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(btnShowHistory);


        summaryPanel.add(buttonPanel, BorderLayout.WEST);

        receiptArea = new JTextArea(20, 40);
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        receiptArea.setBackground(Color.WHITE);
        receiptArea.setBorder(BorderFactory.createLineBorder(new Color(33, 150, 243), 2, true));
        JScrollPane receiptScroll = new JScrollPane(receiptArea);

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.add(summaryPanel, BorderLayout.NORTH);
        rightPanel.add(receiptScroll, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        JLabel lblOrders = new JLabel("Current Orders", JLabel.CENTER);
        lblOrders.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblOrders.setForeground(new Color(33, 150, 243));
        centerPanel.add(lblOrders, BorderLayout.NORTH);
        centerPanel.add(tableScroll, BorderLayout.CENTER);

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        loadTransactionHistory();
    }

    private void showTransactionHistoryDialog() {
        JDialog historyDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel), "Transaction History", true);
        historyDialog.setSize(1000, 600);
        historyDialog.setLocationRelativeTo(mainPanel);
        historyDialog.getContentPane().setBackground(Color.WHITE);

        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyScroll.setBorder(BorderFactory.createLineBorder(new Color(255, 193, 7), 2, true));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        filterPanel.setBackground(Color.WHITE);

        JLabel lblFrom = createModernLabel("From:");
        lblFrom.setForeground(new Color(33, 150, 243));
        dateFromSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor fromEditor = new JSpinner.DateEditor(dateFromSpinner, "MM/dd/yyyy");
        dateFromSpinner.setEditor(fromEditor);
        styleSpinner(dateFromSpinner);

        JLabel lblTo = createModernLabel("To:");
        lblTo.setForeground(new Color(33, 150, 243));
        dateToSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor toEditor = new JSpinner.DateEditor(dateToSpinner, "MM/dd/yyyy");
        dateToSpinner.setEditor(toEditor);
        styleSpinner(dateToSpinner);

        JButton btnFilter = createModernButton("Filter", new Color(76, 175, 80));
        btnFilter.addActionListener(e -> filterTransactionHistory());

        JButton btnReset = createModernButton("Reset", new Color(244, 67, 54));
        btnReset.addActionListener(e -> loadTransactionHistory());

        filterPanel.add(lblFrom);
        filterPanel.add(dateFromSpinner);
        filterPanel.add(lblTo);
        filterPanel.add(dateToSpinner);
        filterPanel.add(btnFilter);
        filterPanel.add(btnReset);

        historyDialog.add(filterPanel, BorderLayout.NORTH);
        historyDialog.add(historyScroll, BorderLayout.CENTER);

        JButton closeButton = createModernButton("Close", new Color(255, 193, 7));
        closeButton.addActionListener(e -> historyDialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(closeButton);
        historyDialog.add(buttonPanel, BorderLayout.SOUTH);

        historyDialog.setVisible(true);
    }

    private void filterTransactionHistory() {
        historyTableModel.setRowCount(0);
        java.util.Date fromDate = (java.util.Date) dateFromSpinner.getValue();
        java.util.Date toDate = (java.util.Date) dateToSpinner.getValue();

        // Adjust toDate to include the full day
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(toDate);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
        cal.set(java.util.Calendar.MINUTE, 59);
        cal.set(java.util.Calendar.SECOND, 59);
        toDate = cal.getTime();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM Sales WHERE Date BETWEEN ? AND ? ORDER BY ID DESC")) {
            stmt.setTimestamp(1, new Timestamp(fromDate.getTime()));
            stmt.setTimestamp(2, new Timestamp(toDate.getTime()));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("ID");
                String productId = rs.getString("Product_ID");
                String productName = rs.getString("Product_Name");
                int qty = rs.getInt("Quantity");
                double unitPrice = rs.getDouble("Unit_Price");
                double total = rs.getDouble("Total");
                Timestamp timestamp = rs.getTimestamp("Date");
                String formattedDate = new SimpleDateFormat("MM/dd/yyyy").format(timestamp);

                String category = "";
                try (PreparedStatement psCategory = conn.prepareStatement("SELECT Category FROM Item WHERE ID = ?")) {
                    psCategory.setString(1, productId);
                    ResultSet rsCategory = psCategory.executeQuery();
                    if (rsCategory.next()) {
                        category = rsCategory.getString("Category");
                    }
                }

                historyTableModel.addRow(new Object[]{
                        id, productId, productName, category, qty, unitPrice, total, formattedDate
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(mainPanel, "Failed to filter transaction history:\n" + ex.getMessage());
        }
    }

    private JTextField createModernTextField() {
        JTextField textField = new JTextField();
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 150, 243), 1, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        textField.setBackground(Color.WHITE);
        textField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return textField;
    }

    private JLabel createModernLabel(String text) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(new Color(33, 150, 243));
        return label;
    }

    private JButton createModernButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(150, 40));
        button.setBorder(BorderFactory.createLineBorder(color.darker(), 2, true));
        return button;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBorder(BorderFactory.createLineBorder(new Color(33, 150, 243), 1, true));
        spinner.setFont(new Font("SansSerif", Font.PLAIN, 14));
        spinner.setBackground(Color.WHITE);
    }

    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setBorder(BorderFactory.createLineBorder(new Color(33, 150, 243), 1, true));
        comboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        comboBox.setBackground(Color.WHITE);
    }

    private JTable styleTable(JTable table) {
        table.setBorder(BorderFactory.createLineBorder(new Color(33, 150, 243), 1, true));
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setBackground(Color.WHITE);
        table.setGridColor(new Color(33, 150, 243));
        table.getTableHeader().setBackground(new Color(33, 150, 243));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        return table;
    }

    private void updateItemTotal(JTextField txtItemTotal) {
        try {
            int qty = (int) spinnerQty.getValue();
            double price = Double.parseDouble(txtPrice.getText());
            txtItemTotal.setText(String.format("%.2f", qty * price));
        } catch (NumberFormatException ignored) {}
    }

    private JTextField createSummaryField(JPanel panel, String label) {
        JLabel lbl = new JLabel(label);
        lbl.setForeground(new Color(33, 150, 243));
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        JTextField tf = new JTextField("0.00");
        tf.setHorizontalAlignment(JTextField.RIGHT);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 150, 243), 1, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        tf.setBackground(Color.WHITE);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(lbl);
        panel.add(tf);
        return tf;
    }

    private void updateTotal() {
        double sum = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            sum += (double) tableModel.getValueAt(i, 5);
        }
        txtTotal.setText(String.format("%.2f", sum));
    }

    private void loadTransactionHistory() {
        historyTableModel.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Sales ORDER BY ID DESC")) {

            while (rs.next()) {
                int id = rs.getInt("ID");
                String productId = rs.getString("Product_ID");
                String productName = rs.getString("Product_Name");
                int qty = rs.getInt("Quantity");
                double unitPrice = rs.getDouble("Unit_Price");
                double total = rs.getDouble("Total");
                Timestamp timestamp = rs.getTimestamp("Date");
                String formattedDate = new SimpleDateFormat("MM/dd/yyyy").format(timestamp);

                String category = "";
                try (PreparedStatement psCategory = conn.prepareStatement("SELECT Category FROM Item WHERE ID = ?")) {
                    psCategory.setString(1, productId);
                    ResultSet rsCategory = psCategory.executeQuery();
                    if (rsCategory.next()) {
                        category = rsCategory.getString("Category");
                    }
                }

                historyTableModel.addRow(new Object[]{
                        id, productId, productName, category, qty, unitPrice, total, formattedDate
                });
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(mainPanel, "Failed to load transaction history:\n" + ex.getMessage());
        }
    }

    private void printBill(String receiptText) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("POS Receipt");

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));
            int y = g2d.getFontMetrics().getHeight();
            for (String line : receiptText.split("\n")) {
                g2d.drawString(line, 0, y);
                y += g2d.getFontMetrics().getHeight();
            }
            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(mainPanel, "Print Error: " + e.getMessage());
            }
        }
    }



    private void generateReceipt() {
        StringBuilder receipt = new StringBuilder();

        // Business name and header
        receipt.append("************************************\n");
        receipt.append("        Bluedot Gunshop\n");
        receipt.append("************************************\n");
        receipt.append("Date: ").append(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date())).append("\n");
        receipt.append("------------------------------------\n");
        receipt.append(String.format("%-12s %-12s %5s %8s\n", "Product", "Category", "Qty", "Total"));
        receipt.append("------------------------------------\n");

        double grandTotal = 0.0;
        int rowCount = tableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            String productName = (String) tableModel.getValueAt(i, 1);
            String category = (String) tableModel.getValueAt(i, 2);
            int qty = Integer.parseInt(tableModel.getValueAt(i, 3).toString());
            double total = ((Number) tableModel.getValueAt(i, 5)).doubleValue();
            grandTotal += total;

            receipt.append(String.format("%-12s %-12s %5d %8.2f\n",
                    productName.length() > 12 ? productName.substring(0, 12) : productName,
                    category.length() > 12 ? category.substring(0, 12) : category,
                    qty, total
            ));
        }

        receipt.append("------------------------------------\n");
        receipt.append(String.format("GRAND TOTAL: %26.2f\n", grandTotal));
        receipt.append("------------------------------------\n");

        String pay = txtPay.getText().trim();
        String balance = txtBalance.getText().trim();
        if (!pay.isEmpty() && !pay.equals("0.00")) {
            receipt.append(String.format("PAY: %33s\n", pay));
        }
        if (!balance.isEmpty() && !balance.equals("0.00")) {
            receipt.append(String.format("CHANGE: %30s\n", balance));
        }

        receipt.append("********* Thank you! ***************\n");

        lastReceiptText = receipt.toString();            // <--- Store for print
        receiptArea.setText(lastReceiptText);            // <--- Display in textarea
    }



    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void updateSearchSuggestions(String searchText) {
        searchListModel.clear();
        List<Product> matchingProducts = searchProducts(searchText);

        if (!matchingProducts.isEmpty()) {
            matchingProducts.sort((p1, p2) -> p1.model.compareToIgnoreCase(p2.model));
            for (Product product : matchingProducts) {
                searchListModel.addElement(product.model + " (ID: " + product.id + ")");
            }
            // Do NOT setSelectedIndex here!
        }
    }



    private void selectProductFromSuggestion(String selectedItem) {
        if (selectedItem == null || selectedItem.isEmpty()) return;

        List<Product> products = searchProducts(selectedItem.split(" \\(ID: ")[0]);
        for (Product product : products) {
            if ((product.model + " (ID: " + product.id + ")").equals(selectedItem)) {
                txtProductName.setText(product.model);
                txtProductCode.setText(product.id);
                categoryDropdown.setSelectedItem(product.category);
                txtPrice.setText(String.format("%.2f", product.price));
                break;
            }
        }
    }

    private List<Product> searchProducts(String searchText) {
        List<Product> products = new ArrayList<>();
        String query = "SELECT ID, Model, Category, Unit_Price FROM Item WHERE Model LIKE ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, "%" + searchText + "%");
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next() && count < 10) { // Limit to 10 results
                String id = rs.getString("ID");
                String model = rs.getString("Model");
                String category = rs.getString("Category");
                double price = rs.getDouble("Unit_Price");
                products.add(new Product(id, model, category, price));
                count++;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(mainPanel, "Error searching products: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        return products;
    }

    private static class Product {
        String id;
        String model;
        String category;
        double price;

        Product(String id, String model, String category, double price) {
            this.id = id;
            this.model = model;
            this.category = category;
            this.price = price;
        }
    }

    private void finalizeSale() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(getMainPanel(),
                    "There are no items to finalize.",
                    "Finalize Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        double total = 0.0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            total += ((Number) tableModel.getValueAt(i, 5)).doubleValue();
        }
        double pay;
        try {
            pay = Double.parseDouble(txtPay.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(getMainPanel(),
                    "Please enter a valid payment amount.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (pay < total) {
            JOptionPane.showMessageDialog(getMainPanel(),
                    "Insufficient payment. Please enter an amount equal to or greater than the total.",
                    "Payment Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        txtBalance.setText(String.format("%.2f", pay - total));


        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);

            // Check all stock first
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String code = (String) tableModel.getValueAt(i, 0);
                int qty = Integer.parseInt(tableModel.getValueAt(i, 3).toString());

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT Quantity_in_Stock FROM Item WHERE ID = ?")) {
                    ps.setString(1, code);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        int stock = rs.getInt("Quantity_in_Stock");
                        if (qty > stock) {
                            JOptionPane.showMessageDialog(getMainPanel(),
                                    "Not enough inventory for Product Code: " + code + ". In stock: " + stock + ", Required: " + qty,
                                    "Insufficient Inventory",
                                    JOptionPane.ERROR_MESSAGE);
                            conn.rollback();
                            return;
                        }
                    } else {
                        JOptionPane.showMessageDialog(getMainPanel(),
                                "Product code " + code + " not found in inventory.",
                                "Inventory Error",
                                JOptionPane.ERROR_MESSAGE);
                        conn.rollback();
                        return;
                    }
                }
            }

            // All checks passed, perform the sale
            try {
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    String code = (String) tableModel.getValueAt(i, 0);
                    String name = (String) tableModel.getValueAt(i, 1);
                    String category = (String) tableModel.getValueAt(i, 2);
                    int qty = Integer.parseInt(tableModel.getValueAt(i, 3).toString());
                    double price = (double) tableModel.getValueAt(i, 4);
                     total = ((Number) tableModel.getValueAt(i, 5)).doubleValue();


                    // Insert sale record
                    try (PreparedStatement psInsertSale = conn.prepareStatement(
                            "INSERT INTO Sales (Product_ID, Product_Name, Category, Quantity, Unit_Price, Total, Date) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS)) {

                        psInsertSale.setString(1, code);
                        psInsertSale.setString(2, name);
                        psInsertSale.setString(3, category);
                        psInsertSale.setInt(4, qty);
                        psInsertSale.setDouble(5, price);
                        psInsertSale.setDouble(6, total);
                        psInsertSale.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                        psInsertSale.executeUpdate();

                        ResultSet generatedKeys = psInsertSale.getGeneratedKeys();
                        int generatedId = -1;
                        if (generatedKeys.next()) {
                            generatedId = generatedKeys.getInt(1);
                        }

                        historyTableModel.insertRow(0, new Object[]{
                                generatedId, code, name, category, qty, price, total,
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy"))
                        });
                    }

                    // Update item stock
                    try (PreparedStatement psUpdateStock = conn.prepareStatement(
                            "UPDATE Item SET Quantity_in_Stock = Quantity_in_Stock - ? WHERE ID = ?")) {
                        psUpdateStock.setInt(1, qty);
                        psUpdateStock.setString(2, code);
                        psUpdateStock.executeUpdate();
                    }
                }

                conn.commit();

                // Generate and display receipt while cart still has data
                generateReceipt();

                // Ask if want to print now
                int result = JOptionPane.showConfirmDialog(getMainPanel(),
                        "Sale finalized!\nWould you like to print a receipt now?",
                        "Print Receipt?", JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.YES_OPTION) {
                    printBill(lastReceiptText); // <-- send stored receipt text to printer
                }

                // Now clear the cart and fields!
                tableModel.setRowCount(0);
                txtTotal.setText("0.00");
                txtPay.setText("0.00");
                txtBalance.setText("0.00");

            } catch (SQLException ex) {
                conn.rollback();
                JOptionPane.showMessageDialog(getMainPanel(), "Error saving sale: " + ex.getMessage());
                return;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(getMainPanel(), "DB Error: " + ex.getMessage());
            return;
        }
    }



    // For testing standalone
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bluedot POS");
            POS posPanel = new POS();
            frame.setContentPane(posPanel.getMainPanel());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1300, 800);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
