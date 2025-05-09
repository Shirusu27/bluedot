import javax.swing.*;
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

    private POSDesign design;
    private String lastReceiptText = "";
    public InventoryManagement inventoryManagement;
    public Dashboard dashboard;

    public POS() {

        design = new POSDesign();
        // Product Name live search (KeyListener)
        design.txtProductName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (design.searchPopupMenu.isVisible()) {
                    int selectedIndex = design.searchSuggestionList.getSelectedIndex();
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DOWN:
                            if (selectedIndex < design.searchListModel.getSize() - 1) {
                                design.searchSuggestionList.setSelectedIndex(selectedIndex + 1);
                                design.searchSuggestionList.ensureIndexIsVisible(selectedIndex + 1);
                            }
                            e.consume();
                            break;
                        case KeyEvent.VK_UP:
                            if (selectedIndex > 0) {
                                design.searchSuggestionList.setSelectedIndex(selectedIndex - 1);
                                design.searchSuggestionList.ensureIndexIsVisible(selectedIndex - 1);
                            }
                            e.consume();
                            break;
                        case KeyEvent.VK_ENTER:
                            if (selectedIndex >= 0) {
                                String selectedItem = design.searchSuggestionList.getSelectedValue();
                                selectProductFromSuggestion(selectedItem);
                                design.searchPopupMenu.setVisible(false);
                                design.txtProductName.requestFocusInWindow();
                            }
                            e.consume();
                            break;
                        case KeyEvent.VK_ESCAPE:
                            design.searchPopupMenu.setVisible(false);
                            design.txtProductName.requestFocusInWindow();
                            e.consume();
                            break;
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN ||
                        e.getKeyCode() == KeyEvent.VK_UP ||
                        e.getKeyCode() == KeyEvent.VK_ENTER ||
                        e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    return;
                }
                String searchText = design.txtProductName.getText().trim();
                if (!searchText.isEmpty()) {
                    updateSearchSuggestions(searchText);
                    if (design.searchListModel.getSize() > 0) {
                        if (!design.searchPopupMenu.isVisible()) {
                            design.searchSuggestionList.setSelectedIndex(0);
                            design.searchPopupMenu.show(design.txtProductName, 0, design.txtProductName.getHeight());
                            design.txtProductName.requestFocusInWindow();
                        }
                    } else {
                        design.searchPopupMenu.setVisible(false);
                    }
                } else {
                    design.searchPopupMenu.setVisible(false);
                    design.searchListModel.clear();
                }
            }
        });

        // Suggestion list mouse click
        design.searchSuggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    String selectedItem = design.searchSuggestionList.getSelectedValue();
                    if (selectedItem != null) {
                        selectProductFromSuggestion(selectedItem);
                        design.searchPopupMenu.setVisible(false);
                        design.txtProductName.requestFocusInWindow();
                    }
                }
            }
        });

        // Product Code lookup by key
        design.txtProductCode.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String code = design.txtProductCode.getText().trim();
                if (!code.isEmpty()) {
                    try (Connection conn = DatabaseConnection.connect();
                         PreparedStatement ps = conn.prepareStatement("SELECT * FROM Item WHERE ID = ?")) {
                        ps.setString(1, code);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            design.txtProductName.setText(rs.getString("Model"));
                            design.txtPrice.setText(String.format("%.2f", rs.getDouble("Unit_Price")));
                            String categoryFromDB = rs.getString("Category");
                            design.txtCategory.setText(categoryFromDB);
                        }
                    } catch (SQLException ignored) {}
                }
            }
        });

        // Qty spinner and price field update total per row
        design.spinnerQty.addChangeListener(e -> updateItemTotal());
        design.txtPrice.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { updateItemTotal(); }
        });

        // Pay field updates balance
        design.txtPay.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                try {
                    double pay = Double.parseDouble(design.txtPay.getText());
                    double total = Double.parseDouble(design.txtTotal.getText());
                    if (pay >= total) {
                        design.txtBalance.setText(String.format("%.2f", pay - total));
                    } else if (!design.txtPay.getText().isEmpty()) {
                        design.txtBalance.setText("0.00");
                    } else {
                        design.txtBalance.setText("");
                    }
                } catch (NumberFormatException ex) {
                    design.txtBalance.setText("");
                }
            }
        });

        // Add button
        design.btnAdd.addActionListener(e -> addToCart());

        // Print button
        design.btnPrint.addActionListener(e -> {
            if (lastReceiptText == null || lastReceiptText.trim().isEmpty()) {
                JOptionPane.showMessageDialog(getMainPanel(),
                        "There is no receipt to print. Please finalize a sale first.",
                        "Print Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            printBill(lastReceiptText);
        });

        // Finalize sale
        design.btnFinalize.addActionListener(e -> finalizeSale());

        // Clear Orders
        design.btnClear.addActionListener(e -> {
            design.tableModel.setRowCount(0);
            design.txtTotal.setText("0.00");
            design.txtPay.setText("0.00");
            design.txtBalance.setText("0.00");
            design.receiptArea.setText("");
        });

        // Delete row
        design.btnDeleteRow.addActionListener(e -> {
            int selectedRow = design.table.getSelectedRow();
            if (selectedRow != -1) {
                design.tableModel.removeRow(selectedRow);
                updateTotal();
            } else {
                JOptionPane.showMessageDialog(getMainPanel(), "Please select a row to delete.");
            }
        });

        // Transaction History
        design.btnShowHistory.addActionListener(e -> showTransactionHistoryDialog());

        // Load transaction history at startup
        loadTransactionHistory();
    }

    // --- Logic methods below ---
    private void addToCart() {
        String code = design.txtProductCode.getText().trim();
        String name = design.txtProductName.getText().trim();
        String priceText = design.txtPrice.getText().trim();

        if (design.tableModel.getRowCount() == 0) { // cart is empty before adding
            design.receiptArea.setText("");
            lastReceiptText = "";
        }

        for (int i = 0; i < design.tableModel.getRowCount(); i++) {
            String existingCode = (String) design.tableModel.getValueAt(i, 0);
            if (existingCode.equals(code)) {
                JOptionPane.showMessageDialog(getMainPanel(), "Product already added to the cart.");
                return;
            }
        }

        if (code.isEmpty() || name.isEmpty() || priceText.isEmpty()) {
            JOptionPane.showMessageDialog(getMainPanel(), "Please fill in all product fields.");
            return;
        }

        try {
            int qty = (int) design.spinnerQty.getValue();
            double price = Double.parseDouble(priceText);
            double total = qty * price;

            String category = "";
            try (Connection conn = DatabaseConnection.connect();
                 PreparedStatement ps = conn.prepareStatement("SELECT Category FROM Item WHERE ID = ?")) {
                ps.setString(1, code);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    category = rs.getString("Category");
                }
            } catch (SQLException ex2) {
                ex2.printStackTrace();
            }

            design.tableModel.addRow(new Object[]{code, name, category, qty, price, total});
            updateTotal();

            design.txtProductCode.setText("");
            design.txtProductName.setText("");
            design.spinnerQty.setValue(1);
            design.txtPrice.setText("");
            design.txtItemTotal.setText("");
            design.spinnerQty.setValue(0);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(getMainPanel(), "Invalid price input.");
        }
    }

    private void updateItemTotal() {
        try {
            int qty = (int) design.spinnerQty.getValue();
            double price = Double.parseDouble(design.txtPrice.getText());
            design.txtItemTotal.setText(String.format("%.2f", qty * price));
        } catch (NumberFormatException ignored) {}
    }

    private void updateTotal() {
        double sum = 0;
        for (int i = 0; i < design.tableModel.getRowCount(); i++) {
            sum += (double) design.tableModel.getValueAt(i, 5);
        }
        design.txtTotal.setText(String.format("%.2f", sum));
    }

    private void showTransactionHistoryDialog() {
        JDialog historyDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(getMainPanel()), "Transaction History", true);
        historyDialog.setSize(1000, 600);
        historyDialog.setLocationRelativeTo(getMainPanel());
        historyDialog.getContentPane().setBackground(POSDesign.COLOR_NAVY_DARK_BG); // Set dialog background color

        // Create JScrollPane for history table
        JScrollPane historyScroll = new JScrollPane(design.historyTable);
        historyScroll.setBorder(BorderFactory.createLineBorder(new Color(255, 193, 7), 2, true));
        historyScroll.setBackground(POSDesign.COLOR_NAVY_DARK_BG); // Set scroll pane background
        historyScroll.getViewport().setBackground(POSDesign.COLOR_NAVY_DARK_BG); // Set viewport background

        // Set the history table background and foreground colors
        design.historyTable.setBackground(POSDesign.COLOR_NAVY_DARK_BG);
        design.historyTable.setForeground(POSDesign.COLOR_TEXT_LIGHT);

        // Create filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        filterPanel.setBackground(POSDesign.COLOR_STEEL_BLUE_PANEL); // Set filter panel background color

        JLabel lblFrom = new JLabel("From:");
        lblFrom.setForeground(POSDesign.COLOR_TEXT_LIGHT);
        JLabel lblTo = new JLabel("To:");
        lblTo.setForeground(POSDesign.COLOR_TEXT_LIGHT);

        JButton btnFilter = new JButton("Filter");
        btnFilter.setBackground(new Color(76, 175, 80));
        btnFilter.setForeground(Color.WHITE);
        btnFilter.addActionListener(e -> filterTransactionHistory());

        JButton btnReset = new JButton("Reset");
        btnReset.setBackground(new Color(244, 67, 54));
        btnReset.setForeground(Color.WHITE);
        btnReset.addActionListener(e -> loadTransactionHistory());

        filterPanel.add(lblFrom);
        filterPanel.add(design.dateFromSpinner);
        filterPanel.add(lblTo);
        filterPanel.add(design.dateToSpinner);
        filterPanel.add(btnFilter);
        filterPanel.add(btnReset);

        // Add clear, export, and import buttons
        if (design.btnClearSalesHistory == null) {
            design.btnClearSalesHistory = new JButton("Clear Sales History");
            design.btnExportSales = new JButton("Export Sales DB");
            design.btnImportSales = new JButton("Import Sales DB");
            // Style as needed
            design.btnClearSalesHistory.setBackground(new Color(233, 30, 99));
            design.btnClearSalesHistory.setForeground(Color.WHITE);
            design.btnExportSales.setBackground(new Color(33, 150, 243));
            design.btnExportSales.setForeground(Color.WHITE);
            design.btnImportSales.setBackground(new Color(33, 150, 243));
            design.btnImportSales.setForeground(Color.WHITE);
        }

        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        dbPanel.setBackground(POSDesign.COLOR_STEEL_BLUE_PANEL); // Set background color
        dbPanel.add(design.btnClearSalesHistory);
        dbPanel.add(design.btnExportSales);
        dbPanel.add(design.btnImportSales);

        Box verticalBox = Box.createVerticalBox();
        verticalBox.add(filterPanel);
        verticalBox.add(Box.createVerticalStrut(10));
        verticalBox.add(dbPanel);

        historyDialog.add(verticalBox, BorderLayout.NORTH);
        historyDialog.add(historyScroll, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.setBackground(new Color(255, 193, 7));
        closeButton.setForeground(Color.WHITE);
        closeButton.addActionListener(e -> historyDialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(POSDesign.COLOR_STEEL_BLUE_PANEL); // Set button panel background color
        buttonPanel.add(closeButton);
        historyDialog.add(buttonPanel, BorderLayout.SOUTH);

        historyDialog.setVisible(true);
    }

    private void filterTransactionHistory() {
        design.historyTableModel.setRowCount(0);
        java.util.Date fromDate = (java.util.Date) design.dateFromSpinner.getValue();
        java.util.Date toDate = (java.util.Date) design.dateToSpinner.getValue();

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(toDate);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
        cal.set(java.util.Calendar.MINUTE, 59);
        cal.set(java.util.Calendar.SECOND, 59);
        toDate = cal.getTime();

        try (Connection conn = DatabaseConnection.connect();
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

                design.historyTableModel.addRow(new Object[]{
                        id, productId, productName, category, qty, unitPrice, total, formattedDate
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(getMainPanel(), "Failed to filter transaction history:\n" + ex.getMessage());
        }
    }

    private void loadTransactionHistory() {
        design.historyTableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.connect();
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

                design.historyTableModel.addRow(new Object[]{
                        id, productId, productName, category, qty, unitPrice, total, formattedDate
                });
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(getMainPanel(), "Failed to load transaction history:\n" + ex.getMessage());
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
                JOptionPane.showMessageDialog(getMainPanel(), "Print Error: " + e.getMessage());
            }
        }
    }

    private void generateReceipt() {
        StringBuilder receipt = new StringBuilder();

        receipt.append("************************************\n");
        receipt.append("        Bluedot Gunshop\n");
        receipt.append("************************************\n");
        receipt.append("Date: ").append(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date())).append("\n");
        receipt.append("------------------------------------\n");
        receipt.append(String.format("%-12s %-12s %5s %8s\n", "Product", "Category", "Qty", "Total"));
        receipt.append("------------------------------------\n");

        double grandTotal = 0.0;
        int rowCount = design.tableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            String productName = (String) design.tableModel.getValueAt(i, 1);
            String category = (String) design.tableModel.getValueAt(i, 2);
            int qty = Integer.parseInt(design.tableModel.getValueAt(i, 3).toString());
            double total = ((Number) design.tableModel.getValueAt(i, 5)).doubleValue();
            grandTotal += total;

            receipt.append(String.format("%-12s %-12s %5d %8.2f\n",
                    productName.length() > 12 ? productName.substring(0, 12) : productName,
                    category.length() > 12 ? category.substring(0, 12) : category,
                    qty, total
            ));
        }

        receipt.append("------------------------------------\n");
        receipt.append("------------------------------------\n");
        receipt.append(String.format("GRAND TOTAL: %26.2f\n", grandTotal));
        receipt.append("------------------------------------\n");

        String pay = design.txtPay.getText().trim();
        String balance = design.txtBalance.getText().trim();
        if (!pay.isEmpty() && !pay.equals("0.00")) {
            receipt.append(String.format("PAY: %33s\n", pay));
        }
        if (!balance.isEmpty() && !balance.equals("0.00")) {
            receipt.append(String.format("CHANGE: %30s\n", balance));
        }

        receipt.append("********* Thank you! ***************\n");

        lastReceiptText = receipt.toString();
        design.receiptArea.setText(lastReceiptText);
    }

    private void updateSearchSuggestions(String searchText) {
        design.searchListModel.clear();
        List<Product> matchingProducts = searchProducts(searchText);

        if (!matchingProducts.isEmpty()) {
            matchingProducts.sort((p1, p2) -> p1.model.compareToIgnoreCase(p2.model));
            for (Product product : matchingProducts) {
                design.searchListModel.addElement(product.model + " (ID: " + product.id + ")");
            }
        }
    }

    private void selectProductFromSuggestion(String selectedItem) {
        if (selectedItem == null || selectedItem.isEmpty()) return;

        List<Product> products = searchProducts(selectedItem.split(" \\(ID: ")[0]);
        for (Product product : products) {
            if ((product.model + " (ID: " + product.id + ")").equals(selectedItem)) {
                design.txtProductName.setText(product.model);
                design.txtProductCode.setText(product.id);
                design.txtCategory.setText(product.category);
                design.txtPrice.setText(String.format("%.2f", product.price));
                break;
            }
        }
    }

    private List<Product> searchProducts(String searchText) {
        List<Product> products = new ArrayList<>();
        String query = "SELECT ID, Model, Category, Unit_Price FROM Item WHERE Model LIKE ?";
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, "%" + searchText + "%");
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next() && count < 10) {
                String id = rs.getString("ID");
                String model = rs.getString("Model");
                String category = rs.getString("Category");
                double price = rs.getDouble("Unit_Price");
                products.add(new Product(id, model, category, price));
                count++;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(getMainPanel(), "Error searching products: " + ex.getMessage(),
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
        if (design.tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(getMainPanel(),
                    "There are no items to finalize.",
                    "Finalize Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        double total = 0.0;
        for (int i = 0; i < design.tableModel.getRowCount(); i++) {
            total += ((Number) design.tableModel.getValueAt(i, 5)).doubleValue();
        }
        double pay;
        try {
            pay = Double.parseDouble(design.txtPay.getText().trim());
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
        design.txtBalance.setText(String.format("%.2f", pay - total));

        try (Connection conn = DatabaseConnection.connect()) {
            conn.setAutoCommit(false);

            // Check all stock first
            for (int i = 0; i < design.tableModel.getRowCount(); i++) {
                String code = (String) design.tableModel.getValueAt(i, 0);
                int qty = Integer.parseInt(design.tableModel.getValueAt(i, 3).toString());

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
                for (int i = 0; i < design.tableModel.getRowCount(); i++) {
                    String code = (String) design.tableModel.getValueAt(i, 0);
                    String name = (String) design.tableModel.getValueAt(i, 1);
                    String category = (String) design.tableModel.getValueAt(i, 2);
                    int qty = Integer.parseInt(design.tableModel.getValueAt(i, 3).toString());
                    double price = (double) design.tableModel.getValueAt(i, 4);
                    total = ((Number) design.tableModel.getValueAt(i, 5)).doubleValue();

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

                        design.historyTableModel.insertRow(0, new Object[]{
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
                    printBill(lastReceiptText);
                }

                // Now clear the cart and fields!
                design.tableModel.setRowCount(0);
                design.txtTotal.setText("0.00");
                design.txtPay.setText("0.00");
                design.txtBalance.setText("0.00");

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

    private void clearSalesHistory() {
        // Ask for admin password
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(getMainPanel(), panel, "Admin Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            try (Connection conn = DatabaseConnection.connect()) {
                String sql = "SELECT * FROM users WHERE username=? AND password=?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(getMainPanel(), "Incorrect username or password.", "Access Denied", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(getMainPanel(), "Error checking admin password:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(getMainPanel(),
                "Are you sure you want to delete ALL sales transactions? This cannot be undone!",
                "Confirm Clear Sales History", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.connect();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM Sales");
                JOptionPane.showMessageDialog(getMainPanel(), "All sales transactions have been deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadTransactionHistory();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(getMainPanel(), "Error clearing sales history:\n" + ex.getMessage());
            }
        }
    }

    private void exportDatabase() {
        // Ask for admin password
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(getMainPanel(), panel, "Admin Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            try (Connection conn = DatabaseConnection.connect()) {
                String sql = "SELECT * FROM users WHERE username=? AND password=?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(getMainPanel(), "Incorrect username or password.", "Access Denied", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(getMainPanel(), "Error checking admin password:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export (Backup) Sales Database");
        fc.setSelectedFile(new java.io.File("bluedotDatabase_backup.accdb"));
        int chooserResult = fc.showSaveDialog(getMainPanel());
        if (chooserResult == JFileChooser.APPROVE_OPTION) {
            java.io.File dest = fc.getSelectedFile();
            try {
                String currentDbPath = DatabaseConnection.getUrl().replace("jdbc:ucanaccess://", "");
                java.nio.file.Files.copy(
                        java.nio.file.Paths.get(currentDbPath),
                        dest.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(getMainPanel(), "Database backup exported:\n" + dest.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(getMainPanel(), "Export failed:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void importDatabase() {
        // Ask for admin password
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(getMainPanel(), panel, "Admin Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            try (Connection conn = DatabaseConnection.connect()) {
                String sql = "SELECT * FROM users WHERE username=? AND password=?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(getMainPanel(), "Incorrect username or password.", "Access Denied", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(getMainPanel(), "Error checking admin password:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import/Switch Sales Database");
        int chooserResult = fc.showOpenDialog(getMainPanel());
        if (chooserResult == JFileChooser.APPROVE_OPTION) {
            java.io.File chosenFile = fc.getSelectedFile();
            String newDbUrl = "jdbc:ucanaccess://" + chosenFile.getAbsolutePath();

            // Test if the DB is accessible and has the correct tables
            try (Connection conn = DriverManager.getConnection(newDbUrl);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM Sales")) {

                // If query succeeds, switch!
                DatabaseConnection.setUrl(newDbUrl);

                // Refresh InventoryManagement so it picks up the new DB
                if (inventoryManagement != null) {
                    inventoryManagement.refreshTable();
                    dashboard.refreshAllDashboardData();// or other method to reload data from DB
                }

                JOptionPane.showMessageDialog(getMainPanel(),
                        "Switched to database:\n" + chosenFile.getAbsolutePath(),
                        "Database Switched", JOptionPane.INFORMATION_MESSAGE);

                loadTransactionHistory();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(getMainPanel(),
                        "Failed to switch: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    // Management logic for admin operations would go here...
    // (clearSalesHistory, exportDatabase, importDatabase)
    // Make sure all database access uses DatabaseConnection.connect() for consistency.

    public JPanel getMainPanel() {

        return design.getMainPanel();
    }

    public static void main(String[] args) {
        // Set DB URL before app starts
        DatabaseConnection.setUrl("jdbc:ucanaccess://C://Files//bluedot//bluedotDatabase.accdb");
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bluedot POS");
            POS posPanel = new POS();
            ImageIcon icon = new ImageIcon("bluedotlogotrans.png");
            frame.setIconImage(icon.getImage());
            frame.setContentPane(posPanel.getMainPanel());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1300, 800);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}