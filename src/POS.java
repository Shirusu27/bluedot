import javax.swing.*;
import javax.swing.event.ChangeEvent;
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

public class POS {

    private JTextField txtProductCode, txtProductName, txtPrice, txtTotal, txtPay, txtBalance;
    private JSpinner spinnerQty;
    private JTable table, historyTable;
    private DefaultTableModel tableModel, historyTableModel;
    private JTextArea receiptArea;
    private JPanel mainPanel;
    private JComboBox<String> categoryDropdown;

    private static final String DB_URL = "jdbc:ucanaccess://C://Users//ADMIN//IdeaProjects//bluedot//bluedotDatabase.accdb";

    public POS() {
        mainPanel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(2, 7, 5, 5));
        inputPanel.setBackground(new Color(0, 0, 245));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Sales"));

        txtProductCode = new JTextField();
        txtProductName = new JTextField();
        spinnerQty = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        txtPrice = new JTextField();
        txtPrice.setEditable(false);
        txtPrice.setBackground(Color.LIGHT_GRAY);
        JTextField txtItemTotal = new JTextField();
        txtItemTotal.setEditable(false);
        txtItemTotal.setBackground(Color.LIGHT_GRAY);

        JButton btnAdd = new JButton("Add");
        JButton btnPrint = new JButton("Print");

        btnAdd.addActionListener(e -> {
            String code = txtProductCode.getText().trim();
            String name = txtProductName.getText().trim();
            String priceText = txtPrice.getText().trim();

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

        categoryDropdown = new JComboBox<>();
        categoryDropdown.addItem("Select Category");
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT Category FROM Item")) {
            while (rs.next()) {
                categoryDropdown.addItem(rs.getString("Category"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        btnPrint.addActionListener(e -> {
            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(getMainPanel(),
                        "There are no items to print.",
                        "Print Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                conn.setAutoCommit(false);

                try {
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        String code = (String) tableModel.getValueAt(i, 0);
                        String name = (String) tableModel.getValueAt(i, 1);
                        String category = (String) tableModel.getValueAt(i, 2);
                        int qty = Integer.parseInt(tableModel.getValueAt(i, 3).toString());
                        double price = (double) tableModel.getValueAt(i, 4);
                        Double total = ((Number) tableModel.getValueAt(i, 5)).doubleValue();

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

                        try (PreparedStatement psUpdateStock = conn.prepareStatement(
                                "UPDATE Item SET Quantity_in_Stock = Quantity_in_Stock - ? WHERE ID = ?")) {
                            psUpdateStock.setInt(1, qty);
                            psUpdateStock.setString(2, code);
                            psUpdateStock.executeUpdate();
                        }
                    }

                    conn.commit();
                    JOptionPane.showMessageDialog(getMainPanel(), "Sale finalized and printed!");
                } catch (SQLException ex) {
                    conn.rollback();
                    JOptionPane.showMessageDialog(getMainPanel(), "Error saving sale: " + ex.getMessage());
                    return;
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(getMainPanel(), "DB Error: " + ex.getMessage());
                return;
            }

            generateReceipt();
            printBill();

            tableModel.setRowCount(0);
            txtTotal.setText("0.00");
        });

        inputPanel.add(new JLabel("Product Code", JLabel.CENTER));
        inputPanel.add(new JLabel("Product Name", JLabel.CENTER));
        inputPanel.add(new JLabel("Category", JLabel.CENTER));
        inputPanel.add(new JLabel("Qty", JLabel.CENTER));
        inputPanel.add(new JLabel("Price", JLabel.CENTER));
        inputPanel.add(new JLabel("Total", JLabel.CENTER));
        inputPanel.add(new JLabel(""));

        inputPanel.add(txtProductCode);
        inputPanel.add(txtProductName);
        inputPanel.add(categoryDropdown);
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
                            txtProductName.setText(rs.getString("Product_Name"));
                            txtPrice.setText(String.format("%.2f", rs.getDouble("Unit_Price")));
                            String categoryFromDB = rs.getString("Category");
                            categoryDropdown.setSelectedItem(categoryFromDB);
                        }
                    } catch (SQLException ignored) {}
                }
            }
        });

        categoryDropdown.addActionListener(e -> {
            String code = txtProductCode.getText().trim();
            String selectedCategory = (String) categoryDropdown.getSelectedItem();

            if (!code.isEmpty() && selectedCategory != null && !selectedCategory.equals("Select Category")) {
                try (Connection conn = DriverManager.getConnection(DB_URL);
                     PreparedStatement ps = conn.prepareStatement("SELECT Unit_Price FROM Item WHERE ID = ? AND Category = ?")) {
                    ps.setString(1, code);
                    ps.setString(2, selectedCategory);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        double price = rs.getDouble("Unit_Price");
                        txtPrice.setText(String.format("%.2f", price));
                    } else {
                        txtPrice.setText("");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
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
        table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(30);

        int[] widths = {150, 335, 160, 135, 135, 135};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setResizable(false);
        }

        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));

        txtTotal = createSummaryField(summaryPanel, "Total");
        txtTotal.setBackground(Color.LIGHT_GRAY);
        txtTotal.setEditable(false);
        txtPay = createSummaryField(summaryPanel, "Pay");
        txtBalance = createSummaryField(summaryPanel, "Balance");
        txtBalance.setBackground(Color.LIGHT_GRAY);
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
        historyTable = new JTable(historyTableModel);
        historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        historyTable.setRowHeight(25);

        int[] historyWidths = {100, 100, 150, 100, 80, 150, 150, 200};
        for (int i = 0; i < historyWidths.length; i++) {
            historyTable.getColumnModel().getColumn(i).setPreferredWidth(historyWidths[i]);
            historyTable.getColumnModel().getColumn(i).setResizable(false);
        }

        JButton btnClear = new JButton("Clear Current Orders");
        btnClear.addActionListener(e -> {
            tableModel.setRowCount(0);
            txtTotal.setText("0.00");
            txtPay.setText("0.00");
            txtBalance.setText("0.00");
            receiptArea.setText("");
        });

        JButton btnDeleteRow = new JButton("Delete Selected Row");
        btnDeleteRow.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                tableModel.removeRow(selectedRow);
                updateTotal();
            } else {
                JOptionPane.showMessageDialog(mainPanel, "Please select a row to delete.");
            }
        });

        JButton btnShowHistory = new JButton("Show Transaction History");
        btnShowHistory.addActionListener(e -> showTransactionHistoryDialog());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        buttonPanel.add(btnClear);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(btnDeleteRow);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(btnPrint);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(btnShowHistory);

        summaryPanel.add(buttonPanel, BorderLayout.WEST);

        receiptArea = new JTextArea(20, 40);
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane receiptScroll = new JScrollPane(receiptArea);

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.add(summaryPanel, BorderLayout.NORTH);
        rightPanel.add(receiptScroll, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        JLabel lblOrders = new JLabel("Current Orders", JLabel.CENTER);
        lblOrders.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblOrders.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(lblOrders);
        centerPanel.add(tableScroll);

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        loadTransactionHistory();
    }

    private void showTransactionHistoryDialog() {
        JDialog historyDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel), "Transaction History", true);
        historyDialog.setSize(1000, 600);
        historyDialog.setLocationRelativeTo(mainPanel);

        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyDialog.add(historyScroll, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> historyDialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        historyDialog.add(buttonPanel, BorderLayout.SOUTH);

        historyDialog.setVisible(true);
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
        JTextField tf = new JTextField("0.00");
        tf.setHorizontalAlignment(JTextField.RIGHT);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.add(lbl);
        panel.add(tf);
        return tf;
    }

    private void updateTotal() {
        double sum = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            sum += (double) tableModel.getValueAt(i, 4);
        }
        txtTotal.setText(String.format("%.2f", sum));
    }

    private void loadExistingSales() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Sales")) {

            while (rs.next()) {
                String code = rs.getString("Product_ID");
                String name = rs.getString("Product_Name");
                int qty = rs.getInt("Quantity");
                double price = rs.getDouble("Unit_Price");
                double total = rs.getDouble("Total");
                tableModel.addRow(new Object[]{code, name, qty, price, total});
            }
            updateTotal();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(mainPanel, "Failed to load sales:\n" + ex.getMessage());
        }
    }

    private void loadTransactionHistory() {
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

    private void printBill() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("POS Receipt");

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            receiptArea.printAll(g2d);
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
        receipt.append("********** Bluedot Gunstore **********\n");

        int rowCount = tableModel.getRowCount();
        receipt.append("Number of Items: ").append(rowCount).append("\n");
        receipt.append("----------------------------\n");

        for (int i = 0; i < rowCount; i++) {
            String code = (String) tableModel.getValueAt(i, 0);
            String name = (String) tableModel.getValueAt(i, 1);
            String category = (String) tableModel.getValueAt(i, 2);
            int qty = Integer.parseInt(tableModel.getValueAt(i, 3).toString());
            double price = (double) tableModel.getValueAt(i, 4);
            Double total = ((Number) tableModel.getValueAt(i, 5)).doubleValue();

            receipt.append("Item: ").append(name).append("\n");
            receipt.append("Unit Code: ").append(code).append("\n");
            receipt.append("Category: ").append(category).append("\n");
            receipt.append("Quantity: ").append(qty).append("\n");
            receipt.append("Unit Price: ").append(price).append("\n");
            receipt.append("Total: ").append(total).append("\n");
            receipt.append("----------------------------\n");
        }

        receipt.append("Total Amount: ").append(txtTotal.getText()).append("\n");
        receipt.append("******************************\n");

        receiptArea.setText(receipt.toString());
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
