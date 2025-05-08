import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import com.toedter.calendar.JDateChooser;
import javax.imageio.ImageIO;

public class InventoryManagement {
    private JPanel mainPanel;
    private JTable itemTable;
    private JButton insertButton, editButton, removeButton, clearButton, searchButton, chooseFileButton;
    private JButton filterAllBtn, filterFirearmBtn, filterAmmoBtn, filterAccessoryBtn, restoreButton;
    private JDateChooser dateChooser, filterFromDate, filterToDate;
    private JTextField itemIDField, categoryField, modelField, priceField, quantityField, searchField;
    private JLabel imageLabel, totalAccessoryLabel, totalFirearmLabel, totalAmmunitionLabel;
    private File selectedImageFile = null;
    private DefaultTableModel tableModel;
    private Connection conn;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

    public InventoryManagement() {
        initialize();
        connectToDatabase();
        createImageDirectory();
        loadTableData("", "", "", "");

        insertButton.addActionListener(e -> insertItem());
        editButton.addActionListener(e -> updateItem());
        removeButton.addActionListener(e -> {
            if (itemIDField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(mainPanel, "Select an item to delete.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new AdminPrompt(this::deleteItem);
        });
        clearButton.addActionListener(e -> clearFields());
        chooseFileButton.addActionListener(e -> chooseImageFile());
        searchButton.addActionListener(e -> applyFilters());

        filterAllBtn.addActionListener(e -> loadTableData("", "", "", ""));
        filterFirearmBtn.addActionListener(e -> loadTableData("Firearm", "", "", ""));
        filterAmmoBtn.addActionListener(e -> loadTableData("Ammunition", "", "", ""));
        filterAccessoryBtn.addActionListener(e -> loadTableData("Accessory", "", "", ""));
        restoreButton.addActionListener(e -> {
            searchField.setText("");
            filterFromDate.setDate(null);
            filterToDate.setDate(null);
            loadTableData("", "", "", "");
        });

        itemTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && itemTable.getSelectedRow() != -1) {
                int row = itemTable.getSelectedRow();
                itemIDField.setText(itemTable.getValueAt(row, 0).toString());
                categoryField.setText(itemTable.getValueAt(row, 1).toString());
                modelField.setText(itemTable.getValueAt(row, 2).toString());
                try {
                    dateChooser.setDate(dateFormat.parse(itemTable.getValueAt(row, 3).toString()));
                } catch (Exception ex) {
                    dateChooser.setDate(null);
                }
                priceField.setText(itemTable.getValueAt(row, 4).toString());
                quantityField.setText(itemTable.getValueAt(row, 5).toString());
                imageLabel.setIcon((ImageIcon) itemTable.getValueAt(row, 6));
            }
        });
    }

    private void initialize() {
        mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("📦 Inventory Management", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel[] labels = {
                new JLabel("Item ID:"), new JLabel("Category:"), new JLabel("Model:"),
                new JLabel("Date Stored:"), new JLabel("Price:"), new JLabel("Quantity:")
        };

        itemIDField = new JTextField(15);
        categoryField = new JTextField(15);
        modelField = new JTextField(15);
        priceField = new JTextField(15);
        quantityField = new JTextField(15);
        dateChooser = new JDateChooser();
        dateChooser.setDateFormatString("MM/dd/yyyy");
        dateChooser.setPreferredSize(new Dimension(150, 25));

        JTextField[] fields = { itemIDField, categoryField, modelField, priceField, quantityField };
        int fieldIndex = 0;
        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            inputPanel.add(labels[i], gbc);
            gbc.gridx = 1;
            if (i == 3) inputPanel.add(dateChooser, gbc);
            else inputPanel.add(fields[fieldIndex++], gbc);
        }

        chooseFileButton = createStyledButton("Choose Image", new Color(50, 150, 150));
        gbc.gridy++;
        inputPanel.add(chooseFileButton, gbc);
        imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(80, 80));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        gbc.gridy++;
        inputPanel.add(imageLabel, gbc);
        clearButton = createStyledButton("Clear", new Color(100, 100, 100));
        gbc.gridy++;
        inputPanel.add(clearButton, gbc);

        tableModel = new DefaultTableModel(new Object[]{"Product Code", "Category", "Product Name", "Date", "Price", "Qty", "Image"}, 0) {
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 6 ? ImageIcon.class : String.class;
            }
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        itemTable = new JTable(tableModel);
        itemTable.setRowHeight(80);
        itemTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        itemTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        JScrollPane tableScrollPane = new JScrollPane(itemTable);

        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchField = new JTextField(15);
        filterFromDate = new JDateChooser();
        filterFromDate.setDateFormatString("MM/dd/yyyy");
        filterToDate = new JDateChooser();
        filterToDate.setDateFormatString("MM/dd/yyyy");
        searchButton = createStyledButton("Search", new Color(100, 100, 255));
        restoreButton = createStyledButton("Restore", new Color(150, 100, 0));

        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("From: "));
        searchPanel.add(filterFromDate);
        searchPanel.add(new JLabel("To: "));
        searchPanel.add(filterToDate);
        searchPanel.add(searchButton);
        searchPanel.add(restoreButton);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        filterAllBtn = createStyledButton("All", Color.GRAY);
        filterFirearmBtn = createStyledButton("Firearm", new Color(255, 99, 71));
        filterAmmoBtn = createStyledButton("Ammunition", new Color(70, 130, 180));
        filterAccessoryBtn = createStyledButton("Accessory", new Color(34, 139, 34));
        filterPanel.add(filterAllBtn);
        filterPanel.add(filterFirearmBtn);
        filterPanel.add(filterAmmoBtn);
        filterPanel.add(filterAccessoryBtn);

        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        totalAccessoryLabel = new JLabel("Accessories: 0", JLabel.CENTER);
        totalFirearmLabel = new JLabel("Firearms: 0", JLabel.CENTER);
        totalAmmunitionLabel = new JLabel("Ammunition: 0", JLabel.CENTER);
        Font summaryFont = new Font("Segoe UI", Font.BOLD, 16);
        totalAccessoryLabel.setFont(summaryFont);
        totalFirearmLabel.setFont(summaryFont);
        totalAmmunitionLabel.setFont(summaryFont);
        summaryPanel.add(totalAccessoryLabel);
        summaryPanel.add(totalFirearmLabel);
        summaryPanel.add(totalAmmunitionLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        insertButton = createStyledButton("Add", new Color(0, 102, 255));
        editButton = createStyledButton("Update", new Color(0, 153, 0));
        removeButton = createStyledButton("Delete", Color.RED);
        buttonPanel.add(insertButton);
        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);

        topPanel.add(summaryPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.add(filterPanel, BorderLayout.SOUTH);

        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(inputPanel, BorderLayout.WEST);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(topPanel, BorderLayout.PAGE_START);
    }

    private void createImageDirectory() {
        File dir = new File("images");
        if (!dir.exists()) dir.mkdir();
    }

    private void chooseImageFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image files", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = chooser.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(selectedImageFile);
                imageLabel.setIcon(new ImageIcon(img.getScaledInstance(120, 120, Image.SCALE_SMOOTH)));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(mainPanel, "Failed to load image!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refreshTable() {
        loadTableData("", "", "", "");
        updateCategoryTotals();
    }
    public void reconnect(String dbUrl) {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (Exception ignore) {}
        DatabaseConnection.setUrl(dbUrl);
        connectToDatabase();
        loadTableData("", "", "", "");
    }


    private void connectToDatabase() {
        try {
            conn = DatabaseConnection.connect();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel, "Database connect failed!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void applyFilters() {
        String keyword = searchField.getText().trim();
        String fromDate = (filterFromDate.getDate() != null) ? dateFormat.format(filterFromDate.getDate()) : "";
        String toDate = (filterToDate.getDate() != null) ? dateFormat.format(filterToDate.getDate()) : "";
        loadTableData("", keyword, fromDate, toDate);
    }

    private void loadTableData(String category, String keyword, String fromDate, String toDate) {
        try {
            String sql = "SELECT * FROM Item WHERE 1=1";
            if (!category.isEmpty()) sql += " AND Category=?";
            if (!keyword.isEmpty()) sql += " AND (Model LIKE ? OR Category LIKE ? OR ID LIKE ?)";
            if (!fromDate.isEmpty() && !toDate.isEmpty()) sql += " AND Item_Date_Stored BETWEEN ? AND ?";

            PreparedStatement stmt = conn.prepareStatement(sql);

            int index = 1;
            if (!category.isEmpty()) stmt.setString(index++, category);
            if (!keyword.isEmpty()) {
                String kw = "%" + keyword + "%";
                stmt.setString(index++, kw);
                stmt.setString(index++, kw);
                stmt.setString(index++, kw);
            }
            if (!fromDate.isEmpty() && !toDate.isEmpty()) {
                stmt.setDate(index++, new java.sql.Date(dateFormat.parse(fromDate).getTime()));
                stmt.setDate(index, new java.sql.Date(dateFormat.parse(toDate).getTime()));
            }

            ResultSet rs = stmt.executeQuery();
            tableModel.setRowCount(0);
            while (rs.next()) {
                ImageIcon imageIcon = null;
                String path = rs.getString("ImagePath");
                if (path != null && !path.isEmpty()) {
                    File imgFile = new File("images", path);
                    if (imgFile.exists()) {
                        Image img = new ImageIcon(imgFile.getAbsolutePath()).getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                        imageIcon = new ImageIcon(img);
                    }
                }
                tableModel.addRow(new Object[]{
                        rs.getInt("ID"),
                        rs.getString("Category"),
                        rs.getString("Model"),
                        dateFormat.format(rs.getDate("Item_Date_Stored")),
                        String.format("%.2f", rs.getDouble("Unit_Price")),
                        rs.getInt("Quantity_in_Stock"),
                        imageIcon
                });
            }

            updateCategoryTotals();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel, "Error loading data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCategoryTotals() {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT Category, SUM(Quantity_in_Stock) AS TotalQty FROM Item GROUP BY Category");
            totalAccessoryLabel.setText("Accessories: 0");
            totalFirearmLabel.setText("Firearm: 0");
            totalAmmunitionLabel.setText("Ammunition: 0");
            while (rs.next()) {
                String cat = rs.getString("Category");
                int total = rs.getInt("TotalQty");
                switch (cat) {
                    case "Accessory" -> totalAccessoryLabel.setText("Accessories: " + total);
                    case "Firearm" -> totalFirearmLabel.setText("Firearms: " + total);
                    case "Ammunition" -> totalAmmunitionLabel.setText("Ammunition: " + total);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(mainPanel, "Error loading totals", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        return button;
    }

    private void clearFields() {
        itemIDField.setText("");
        categoryField.setText("");
        modelField.setText("");
        dateChooser.setDate(null);
        priceField.setText("");
        quantityField.setText("");
        selectedImageFile = null;
        imageLabel.setIcon(null);
    }

    private void insertItem() {
        try {
            String imageFilename = null;
            if (selectedImageFile != null) {
                imageFilename = System.currentTimeMillis() + "_" + selectedImageFile.getName();
                Files.copy(selectedImageFile.toPath(), new File("images", imageFilename).toPath());
            }

            PreparedStatement stmt = conn.prepareStatement("INSERT INTO Item (ID, Category, Model, Item_Date_Stored, Unit_Price, Quantity_in_Stock, ImagePath) VALUES (?, ?, ?, ?, ?, ?, ?)");
            stmt.setInt(1, Integer.parseInt(itemIDField.getText()));
            stmt.setString(2, categoryField.getText());
            stmt.setString(3, modelField.getText());
            stmt.setDate(4, new java.sql.Date(dateChooser.getDate().getTime()));
            stmt.setDouble(5, Double.parseDouble(priceField.getText()));
            stmt.setInt(6, Integer.parseInt(quantityField.getText()));
            stmt.setString(7, imageFilename);

            stmt.executeUpdate();
            loadTableData("", "", "", "");
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel, "Insert error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateItem() {
        try {
            String imageFilename = null;
            if (selectedImageFile != null) {
                imageFilename = System.currentTimeMillis() + "_" + selectedImageFile.getName();
                Files.copy(selectedImageFile.toPath(), new File("images", imageFilename).toPath());
            } else {
                PreparedStatement getPathStmt = conn.prepareStatement("SELECT ImagePath FROM Item WHERE ID=?");
                getPathStmt.setInt(1, Integer.parseInt(itemIDField.getText()));
                ResultSet rs = getPathStmt.executeQuery();
                if (rs.next()) imageFilename = rs.getString("ImagePath");
            }

            PreparedStatement stmt = conn.prepareStatement("UPDATE Item SET Category=?, Model=?, Item_Date_Stored=?, Unit_Price=?, Quantity_in_Stock=?, ImagePath=? WHERE ID=?");
            stmt.setString(1, categoryField.getText());
            stmt.setString(2, modelField.getText());
            stmt.setDate(3, new java.sql.Date(dateChooser.getDate().getTime()));
            stmt.setDouble(4, Double.parseDouble(priceField.getText()));
            stmt.setInt(5, Integer.parseInt(quantityField.getText()));
            stmt.setString(6, imageFilename);
            stmt.setInt(7, Integer.parseInt(itemIDField.getText()));

            stmt.executeUpdate();
            loadTableData("", "", "", "");
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel, "Update error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteItem() {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Item WHERE ID=?")) {
            stmt.setInt(1, Integer.parseInt(itemIDField.getText()));
            stmt.executeUpdate();
            loadTableData("", "", "", "");
            clearFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(mainPanel, "Delete error!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public static void main(String[] args) {
        String defaultDbUrl = "jdbc:ucanaccess://C://Users//ADMIN//IdeaProjects//bluedot//bluedotDatabase.accdb";
        JFrame frame = new JFrame("Inventory Management");
        ImageIcon icon = new ImageIcon("bluedotlogotrans.png"); // Adjust the path if needed
        frame.setIconImage(icon.getImage());
        frame.setContentPane(new InventoryManagement().getMainPanel());
        frame.setSize(1280, 720);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}


