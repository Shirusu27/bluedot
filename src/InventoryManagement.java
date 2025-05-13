import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.toedter.calendar.JDateChooser;
import javax.imageio.ImageIO;
import javax.swing.text.*;

public class InventoryManagement {
    // --- Color Palette (From POSDesign) ---
    public static final Color COLOR_NAVY_DARK_BG = new Color(0x00, 0x1F, 0x3F);
    public static final Color COLOR_STEEL_BLUE_PANEL = new Color(0x3A, 0x6D, 0x8C);
    public static final Color COLOR_CADET_BLUE_ACCENT = new Color(0x6A, 0x9A, 0xB0);
    public static final Color COLOR_LIGHT_BLUE_HOVER = new Color(0x8A, 0xB8, 0xC8);
    public static final Color COLOR_TEXT_LIGHT = new Color(0xE0, 0xE7, 0xEF);
    public static final Color COLOR_BORDER_SUBTLE = new Color(0x2C, 0x50, 0x6F);
    public static final Color COLOR_INPUT_BG = new Color(0x10, 0x30, 0x50);
    public static final Color COLOR_ACCENT_FIREARM = new Color(0xD9, 0x53, 0x4F); // Using POSDesign accent warn color for firearm
    public static final Color COLOR_ACCENT_AMMO = new Color(0x5C, 0xB8, 0x5C);    // Using POSDesign accent success for ammo
    public static final Color COLOR_ACCENT_ACCESSORY = new Color(0x6A, 0x9A, 0xB0); // Using cadet blue accent for accessory
    public static final Color COLOR_TABLE_GRID = new Color(0x2C, 0x50, 0x6F);

    // --- Fonts ---
    public static final Font FONT_COMPANY_NAME = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_VALUE = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONT_SIDEBAR_BUTTON = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_TABLE_CELL = new Font("Segoe UI", Font.PLAIN, 13);

    private JPanel mainPanel;
    private JTable itemTable;
    private JButton insertButton, editButton, removeButton, clearButton, searchButton, chooseFileButton;
    private JButton filterAllBtn, filterFirearmBtn, filterAmmoBtn, filterAccessoryBtn, restoreButton;
    private JDateChooser dateChooser, filterFromDate, filterToDate;
    private JTextField itemIDField, categoryField, modelField, priceField, quantityField, searchField;
    private JLabel imageLabel, totalAccessoryLabel, totalFirearmLabel, totalAmmunitionLabel;
    private File selectedImageFile = null;
    private DefaultTableModel tableModel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

    public InventoryManagement() {
        initialize();
        createImageDirectory();
        loadTableData("", "", "", "");

        ((AbstractDocument) priceField.getDocument()).setDocumentFilter(new NumericDocumentFilter());
        ((AbstractDocument) quantityField.getDocument()).setDocumentFilter(new NumericDocumentFilter());

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

    // NumericDocumentFilter inner class to restrict document input to digits only
    private static class NumericDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string == null) return;
            if (isNumeric(string)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text == null) return;
            if (isNumeric(text)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        private boolean isNumeric(String text) {
            return text.chars().allMatch(Character::isDigit);
        }
    }


    private void initialize() {
        mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(COLOR_NAVY_DARK_BG);

        JLabel titleLabel = new JLabel("📦 Inventory Management", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(COLOR_TEXT_LIGHT);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(COLOR_STEEL_BLUE_PANEL);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel[] labels = {
                new JLabel("Item ID:"), new JLabel("Category:"), new JLabel("Model:"),
                new JLabel("Date Stored:"), new JLabel("Price:"), new JLabel("Quantity:")
        };

        itemIDField = createStyledTextField();
        categoryField = createStyledTextField();
        modelField = createStyledTextField();
        priceField = createStyledTextField();
        quantityField = createStyledTextField();

        dateChooser = new JDateChooser();
        dateChooser.setDateFormatString("MM/dd/yyyy");
        dateChooser.setPreferredSize(new Dimension(150, 25));
        styleDateChooser(dateChooser);

        JTextField[] fields = { itemIDField, categoryField, modelField, priceField, quantityField };
        int fieldIndex = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i].setForeground(COLOR_TEXT_LIGHT);
            labels[i].setFont(FONT_LABEL);
            gbc.gridx = 0;
            gbc.gridy = i;
            inputPanel.add(labels[i], gbc);
            gbc.gridx = 1;
            if (i == 3) inputPanel.add(dateChooser, gbc);
            else inputPanel.add(fields[fieldIndex++], gbc);
        }

        chooseFileButton = createStyledButton("Choose Image", new Color(60, 150, 150));
        gbc.gridy++;
        inputPanel.add(chooseFileButton, gbc);

        imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(80, 80));
        imageLabel.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE));
        gbc.gridy++;
        inputPanel.add(imageLabel, gbc);

        clearButton = createStyledButton("Clear", COLOR_ACCENT_FIREARM);
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
        itemTable.setFont(FONT_TABLE_CELL);
        itemTable.getTableHeader().setFont(FONT_TABLE_HEADER);
        itemTable.setBackground(COLOR_INPUT_BG);
        itemTable.setForeground(COLOR_TEXT_LIGHT);
        itemTable.setGridColor(COLOR_BORDER_SUBTLE);
        itemTable.setSelectionBackground(COLOR_CADET_BLUE_ACCENT);
        itemTable.setSelectionForeground(COLOR_NAVY_DARK_BG);
        itemTable.getTableHeader().setBackground(COLOR_STEEL_BLUE_PANEL);
        itemTable.getTableHeader().setForeground(COLOR_TEXT_LIGHT);
        itemTable.getTableHeader().setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,0, COLOR_BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(10,8,10,8)
        ));

        JScrollPane tableScrollPane = new JScrollPane(itemTable);
        styleStyledScrollPane(tableScrollPane);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchPanel.setBackground(COLOR_STEEL_BLUE_PANEL);
        searchField = createStyledTextField();

        filterFromDate = new JDateChooser();
        filterFromDate.setDateFormatString("MM/dd/yyyy");
        filterFromDate.setPreferredSize(new Dimension(120, 25));
        styleDateChooser(filterFromDate);

        filterToDate = new JDateChooser();
        filterToDate.setDateFormatString("MM/dd/yyyy");
        filterToDate.setPreferredSize(new Dimension(120, 25));
        styleDateChooser(filterToDate);

        searchButton = createStyledButton("Search", COLOR_CADET_BLUE_ACCENT);
        restoreButton = createStyledButton("Restore", COLOR_ACCENT_FIREARM);

        JLabel searchLabel = new JLabel("Search: ");
        searchLabel.setForeground(COLOR_TEXT_LIGHT);
        JLabel fromLabel = new JLabel("From: ");
        fromLabel.setForeground(COLOR_TEXT_LIGHT);
        JLabel toLabel = new JLabel("To: ");
        toLabel.setForeground(COLOR_TEXT_LIGHT);

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(fromLabel);
        searchPanel.add(filterFromDate);
        searchPanel.add(toLabel);
        searchPanel.add(filterToDate);
        searchPanel.add(searchButton);
        searchPanel.add(restoreButton);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        filterPanel.setBackground(COLOR_STEEL_BLUE_PANEL);
        filterAllBtn = createStyledButton("All", COLOR_BORDER_SUBTLE);
        filterFirearmBtn = createStyledButton("Firearm", COLOR_ACCENT_FIREARM);
        filterAmmoBtn = createStyledButton("Ammunition", COLOR_ACCENT_AMMO);
        filterAccessoryBtn = createStyledButton("Accessory", COLOR_ACCENT_ACCESSORY);

        filterPanel.add(filterAllBtn);
        filterPanel.add(filterFirearmBtn);
        filterPanel.add(filterAmmoBtn);
        filterPanel.add(filterAccessoryBtn);

        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        summaryPanel.setBackground(COLOR_STEEL_BLUE_PANEL);
        totalAccessoryLabel = new JLabel("Accessories: 0", JLabel.CENTER);
        totalFirearmLabel = new JLabel("Firearms: 0", JLabel.CENTER);
        totalAmmunitionLabel = new JLabel("Ammunition: 0", JLabel.CENTER);
        Font summaryFont = new Font("Segoe UI", Font.BOLD, 16);
        totalAccessoryLabel.setFont(summaryFont);
        totalAccessoryLabel.setForeground(COLOR_TEXT_LIGHT);
        totalFirearmLabel.setFont(summaryFont);
        totalFirearmLabel.setForeground(COLOR_TEXT_LIGHT);
        totalAmmunitionLabel.setFont(summaryFont);
        totalAmmunitionLabel.setForeground(COLOR_TEXT_LIGHT);
        summaryPanel.add(totalAccessoryLabel);
        summaryPanel.add(totalFirearmLabel);
        summaryPanel.add(totalAmmunitionLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(COLOR_STEEL_BLUE_PANEL);
        insertButton = createStyledButton("Add", COLOR_CADET_BLUE_ACCENT);
        editButton = createStyledButton("Update", COLOR_ACCENT_AMMO);
        removeButton = createStyledButton("Delete", COLOR_ACCENT_FIREARM);
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

    private void styleDateChooser(JDateChooser chooser) {
        JFormattedTextField dateEditor = chooser.getDateEditor().getUiComponent() instanceof JFormattedTextField
                ? (JFormattedTextField) chooser.getDateEditor().getUiComponent() : null;
        if (dateEditor != null) {
            dateEditor.setBackground(COLOR_INPUT_BG);
            dateEditor.setForeground(COLOR_TEXT_LIGHT); // White text color
            dateEditor.setCaretColor(COLOR_CADET_BLUE_ACCENT);
            dateEditor.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                    BorderFactory.createEmptyBorder(5, 8, 5, 8)
            ));
            dateEditor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            dateEditor.setOpaque(true);

            // Add property change listener to ensure foreground remains white after date selection
            chooser.addPropertyChangeListener("date", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    SwingUtilities.invokeLater(() -> {
                        dateEditor.setForeground(COLOR_TEXT_LIGHT);
                        dateEditor.repaint();
                    });
                }
            });
        }
        chooser.setBackground(COLOR_INPUT_BG);

        // Style calendar popup button
        Component[] comps = chooser.getComponents();
        for (Component c : comps) {
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                btn.setBackground(COLOR_STEEL_BLUE_PANEL);
                btn.setForeground(COLOR_TEXT_LIGHT); // White button text
                btn.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE));
            }
        }
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

    private void applyFilters() {
        String keyword = searchField.getText().trim();
        String fromDate = (filterFromDate.getDate() != null) ? dateFormat.format(filterFromDate.getDate()) : "";
        String toDate = (filterToDate.getDate() != null) ? dateFormat.format(filterToDate.getDate()) : "";
        loadTableData("", keyword, fromDate, toDate);
    }

    private void loadTableData(String category, String keyword, String fromDate, String toDate) {
        try (Connection conn = DatabaseConnection.connect()) {
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
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT Category, SUM(Quantity_in_Stock) AS TotalQty FROM Item GROUP BY Category");
            totalAccessoryLabel.setText("Accessories: 0");
            totalFirearmLabel.setText("Firearms: 0");
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
        button.setForeground(COLOR_TEXT_LIGHT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Color hoverColor = COLOR_LIGHT_BLUE_HOVER;
        if (color.equals(COLOR_ACCENT_FIREARM))
            hoverColor = color.brighter();
        else if (color.equals(COLOR_ACCENT_AMMO) || color.equals(COLOR_ACCENT_ACCESSORY))
            hoverColor = color.brighter();

        Color pressedColor = color.darker();

        Color finalHoverColor = hoverColor;
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(finalHoverColor);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                button.setBackground(pressedColor);
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (button.getBounds().contains(evt.getPoint())) button.setBackground(finalHoverColor);
                else button.setBackground(color);
            }
        });
        return button;
    }

    private JTextField createStyledTextField() {
        JTextField textField = new JTextField(15);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBackground(COLOR_INPUT_BG);
        textField.setForeground(COLOR_TEXT_LIGHT);
        textField.setCaretColor(COLOR_CADET_BLUE_ACCENT);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return textField;
    }

    private void styleStyledScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1));
        scrollPane.getViewport().setBackground(COLOR_INPUT_BG);
        scrollPane.setBackground(COLOR_INPUT_BG);

        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setBackground(COLOR_STEEL_BLUE_PANEL);
        verticalScrollBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = COLOR_CADET_BLUE_ACCENT;
                this.trackColor = COLOR_STEEL_BLUE_PANEL;
                this.thumbDarkShadowColor = this.thumbColor.darker();
                this.thumbHighlightColor = this.thumbColor.brighter();
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton jbutton = new JButton();
                jbutton.setPreferredSize(new Dimension(0, 0));
                jbutton.setMinimumSize(new Dimension(0, 0));
                jbutton.setMaximumSize(new Dimension(0, 0));
                return jbutton;
            }
        });

        JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
        horizontalScrollBar.setBackground(COLOR_STEEL_BLUE_PANEL);
        horizontalScrollBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = COLOR_CADET_BLUE_ACCENT;
                this.trackColor = COLOR_STEEL_BLUE_PANEL;
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton jbutton = new JButton();
                jbutton.setPreferredSize(new Dimension(0, 0));
                jbutton.setMinimumSize(new Dimension(0, 0));
                jbutton.setMaximumSize(new Dimension(0, 0));
                return jbutton;
            }
        });
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
        try (Connection conn = DatabaseConnection.connect()) {
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
        try (Connection conn = DatabaseConnection.connect()){
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
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM Item WHERE ID=?")) {
            stmt.setInt(1, Integer.parseInt(itemIDField.getText()));
            stmt.executeUpdate();
            loadTableData("", "", "", "");
            clearFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(mainPanel, "Delete error! " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public static void main(String[] args) {
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
