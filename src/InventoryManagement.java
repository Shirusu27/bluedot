import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import com.toedter.calendar.JDateChooser;

public class InventoryManagement {
    private JPanel mainPanel;
    private JTable itemTable;
    private JButton insertButton, editButton, removeButton, clearButton, searchButton, chooseFileButton;
    private JDateChooser dateChooser;
    private JTextField itemIDField, categoryField, modelField, priceField, quantityField, searchField;
    private JLabel imageLabel, totalAccessoryLabel, totalFirearmLabel, totalAmmunitionLabel;
    private File selectedImageFile = null;
    private DefaultTableModel tableModel;
    private static Connection conn;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

    public InventoryManagement() {
        initialize();
        connectToDatabase();
        createImageDirectory();
        loadTableData("");
        updateCategoryTotals();

        insertButton.addActionListener(e -> insertItem());
        editButton.addActionListener(e -> updateItem());
        removeButton.addActionListener(e -> {
            if (itemIDField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(mainPanel, "Select an item to delete.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            new AdminPrompt(() -> {
                deleteItem();
            });
        });
        clearButton.addActionListener(e -> clearFields());
        chooseFileButton.addActionListener(e -> chooseImageFile());

        itemTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && itemTable.getSelectedRow() != -1) {
                int selectedRow = itemTable.getSelectedRow();
                itemIDField.setText(itemTable.getValueAt(selectedRow, 0).toString());
                categoryField.setText(itemTable.getValueAt(selectedRow, 1).toString());
                modelField.setText(itemTable.getValueAt(selectedRow, 2).toString());
                try {
                    Date date = dateFormat.parse(itemTable.getValueAt(selectedRow, 3).toString());
                    dateChooser.setDate(date);
                } catch (Exception ex) {
                    dateChooser.setDate(null);
                }
                priceField.setText(itemTable.getValueAt(selectedRow, 4).toString());
                quantityField.setText(itemTable.getValueAt(selectedRow, 5).toString());
                imageLabel.setIcon((ImageIcon) itemTable.getValueAt(selectedRow, 6));
            }
        });

        searchButton.addActionListener(e -> loadTableData(searchField.getText()));
        searchField.addActionListener(e -> loadTableData(searchField.getText()));
    }

    private void createImageDirectory() {
        File imageDir = new File("images");
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }
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

        JTextField[] fields = {
                itemIDField, categoryField, modelField, priceField, quantityField
        };

        int fieldIndex = 0;
        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            inputPanel.add(labels[i], gbc);

            gbc.gridx = 1;
            if (i == 3) {
                inputPanel.add(dateChooser, gbc);
            } else {
                inputPanel.add(fields[fieldIndex], gbc);
                fieldIndex++;
            }
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
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 6 ? ImageIcon.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        itemTable = new JTable(tableModel);
        itemTable.setRowHeight(80);
        itemTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        itemTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));

        JScrollPane tableScrollPane = new JScrollPane(itemTable);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchField = new JTextField(15);
        searchButton = createStyledButton("Search", new Color(100, 100, 255));
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

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

        mainPanel.add(summaryPanel, BorderLayout.BEFORE_FIRST_LINE);


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        insertButton = createStyledButton("Add", new Color(0, 102, 255));
        editButton = createStyledButton("Update", new Color(0, 153, 0));
        removeButton = createStyledButton("Delete", Color.RED);
        buttonPanel.add(insertButton);
        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);

        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(inputPanel, BorderLayout.WEST);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(searchPanel, BorderLayout.PAGE_START);
    }

    private void chooseImageFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image files", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = chooser.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(selectedImageFile);
                ImageIcon icon = new ImageIcon(img.getScaledInstance(120, 120, Image.SCALE_SMOOTH));
                imageLabel.setIcon(icon);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(mainPanel, "Failed to load image!", "Error", JOptionPane.ERROR_MESSAGE);
            }
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

    private void connectToDatabase() {
        try {
            String url = "jdbc:ucanaccess://C://Users//ADMIN//IdeaProjects//bluedot//bluedotDatabase.accdb";
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(mainPanel, "Database connection failed!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshTable() {
        loadTableData("");
        updateCategoryTotals();
    }

    private void loadTableData(String keyword) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Item WHERE Category LIKE ? OR Model LIKE ?")) {
            String search = "%" + keyword + "%";
            stmt.setString(1, search);
            stmt.setString(2, search);
            ResultSet rs = stmt.executeQuery();

            tableModel.setRowCount(0);
            while (rs.next()) {
                ImageIcon imageIcon = null;
                String imagePath = rs.getString("ImagePath");
                if (imagePath != null && !imagePath.isEmpty()) {
                    File imgFile = new File("images", imagePath);
                    if (imgFile.exists()) {
                        Image img = new ImageIcon(imgFile.getAbsolutePath()).getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                        imageIcon = new ImageIcon(img);
                    }
                }

                String formattedDate = new SimpleDateFormat("MM/dd/yyyy").format(rs.getDate("Item_Date_Stored"));
                tableModel.addRow(new Object[]{
                        rs.getInt("ID"),
                        rs.getString("Category"),
                        rs.getString("Model"),
                        formattedDate,
                        String.format("%.2f", rs.getDouble("Unit_Price")),
                        rs.getInt("Quantity_in_Stock"),
                        imageIcon
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(mainPanel, "Error loading data!", "Error", JOptionPane.ERROR_MESSAGE);
        }
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

            Date selectedDate = dateChooser.getDate();
            if (selectedDate == null) {
                JOptionPane.showMessageDialog(mainPanel, "Please select a date.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            stmt.setDate(4, new java.sql.Date(selectedDate.getTime()));
            stmt.setDouble(5, Double.parseDouble(priceField.getText()));
            stmt.setInt(6, Integer.parseInt(quantityField.getText()));
            stmt.setString(7, imageFilename);

            stmt.executeUpdate();
            loadTableData("");
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel, "Insert error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateItem() {
        try {
            String imageFilename;

            if (selectedImageFile != null) {
                imageFilename = System.currentTimeMillis() + "_" + selectedImageFile.getName();
                Files.copy(selectedImageFile.toPath(), new File("images", imageFilename).toPath());
            } else {
                int selectedRow = itemTable.getSelectedRow();
                if (selectedRow != -1) {
                    Object existingImageIcon = itemTable.getValueAt(selectedRow, 6);
                    String query = "SELECT ImagePath FROM Item WHERE ID=?";
                    PreparedStatement getPathStmt = conn.prepareStatement(query);
                    getPathStmt.setInt(1, Integer.parseInt(itemIDField.getText()));
                    ResultSet rs = getPathStmt.executeQuery();
                    if (rs.next()) {
                        imageFilename = rs.getString("ImagePath");
                    } else {
                        imageFilename = null;
                    }
                } else {
                    imageFilename = null;
                }
            }

            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE Item SET Category=?, Model=?, Item_Date_Stored=?, Unit_Price=?, Quantity_in_Stock=?, ImagePath=? WHERE ID=?"
            );

            stmt.setString(1, categoryField.getText());
            stmt.setString(2, modelField.getText());

            Date selectedDate = dateChooser.getDate();
            if (selectedDate == null) {
                JOptionPane.showMessageDialog(mainPanel, "Please select a date.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            stmt.setDate(3, new java.sql.Date(selectedDate.getTime()));
            stmt.setDouble(4, Double.parseDouble(priceField.getText()));
            stmt.setInt(5, Integer.parseInt(quantityField.getText()));
            stmt.setString(6, imageFilename);
            stmt.setInt(7, Integer.parseInt(itemIDField.getText()));

            stmt.executeUpdate();
            loadTableData("");
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel, "Update error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void deleteItem() {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Item WHERE ID=?")) {
            stmt.setInt(1, Integer.parseInt(itemIDField.getText()));
            stmt.executeUpdate();
            loadTableData("");
            clearFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(mainPanel, "Delete error!", "Error", JOptionPane.ERROR_MESSAGE);
        }
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

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void updateCategoryTotals() {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT Category, SUM(Quantity_in_Stock) as TotalQty FROM Item GROUP BY Category"
            );

            totalAccessoryLabel.setText("Accessory: 0");
            totalFirearmLabel.setText("Firearms: 0");
            totalAmmunitionLabel.setText("Ammunition: 0");

            while (rs.next()) {
                String category = rs.getString("Category");
                int totalQty = rs.getInt("TotalQty");

                switch (category) {
                    case "Accessory":
                        totalAccessoryLabel.setText("Accessory: " + totalQty);
                        break;
                    case "Firearms":
                        totalFirearmLabel.setText("Firearms: " + totalQty);
                        break;
                    case "Ammunition":
                        totalAmmunitionLabel.setText("Ammunition: " + totalQty);
                        break;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(mainPanel, "Error loading category totals", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    public static void main(String[] args) {
        JFrame frame = new JFrame("Inventory Management");
        frame.setContentPane(new InventoryManagement().getMainPanel());
        frame.setSize(1280, 720);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
