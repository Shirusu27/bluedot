import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;

public class InventoryManagement {
    private JPanel mainPanel;
    private JTable inventoryTable;
    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private DefaultTableModel tableModel;
    private static Connection conn;

    public InventoryManagement() {
        initialize();
        connectToDatabase();
        loadTableData();

        addButton.addActionListener(e -> addItem());
        updateButton.addActionListener(e -> updateItem());
        deleteButton.addActionListener(e -> deleteItem());
    }

    private void initialize() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new Object[]{"Item ID", "Category", "Model", "Date Stored", "Price", "Quantity"}, 0);
        inventoryTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(inventoryTable);

        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add Item");
        updateButton = new JButton("Update Item");
        deleteButton = new JButton("Delete Item");
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);

        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void connectToDatabase() {
        try {
            String url = "jdbc:ucanaccess://C:/Files/bluedot/bluedotDatabase.accdb";
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(mainPanel, "Database connection failed!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void loadTableData() {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT Item_ID, Category, Model, Item_Date_Stored, Item_Price, Quantity_in_Stock FROM Item")) {

            tableModel.setRowCount(0);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            while (rs.next()) {
                Object[] row = {
                        rs.getInt("Item_ID"),
                        rs.getString("Category"),
                        rs.getString("Model"),
                        sdf.format(rs.getDate("Item_Date_Stored")),
                        rs.getDouble("Item_Price"),
                        rs.getInt("Quantity_in_Stock")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(mainPanel, "Error loading table data!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void addItem() {
        JPanel inputPanel = createInputPanel();
        int option = JOptionPane.showConfirmDialog(mainPanel, inputPanel, "Add Item", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                String category = ((JTextField) inputPanel.getComponent(1)).getText();
                String model = ((JTextField) inputPanel.getComponent(3)).getText();
                String dateStored = ((JTextField) inputPanel.getComponent(5)).getText();
                double price = Double.parseDouble(((JTextField) inputPanel.getComponent(7)).getText());
                int quantity = Integer.parseInt(((JTextField) inputPanel.getComponent(9)).getText());

                if (!dateStored.matches("\\d{2}/\\d{2}/\\d{4}")) {
                    JOptionPane.showMessageDialog(mainPanel, "Invalid date format. Please use dd/MM/yyyy.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String[] parts = dateStored.split("/");
                String sqlFormattedDate = parts[2] + "-" + parts[1] + "-" + parts[0];

                String sql = "INSERT INTO Item (Category, Model, Item_Date_Stored, Item_Price, Quantity_in_Stock) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, category);
                    stmt.setString(2, model);
                    stmt.setDate(3, Date.valueOf(sqlFormattedDate));
                    stmt.setDouble(4, price);
                    stmt.setInt(5, quantity);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(mainPanel, "Item added successfully!");
                loadTableData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainPanel, "Error adding item: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateItem() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(mainPanel, "Please select an item to update.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int itemId = (int) tableModel.getValueAt(selectedRow, 0);
        JPanel inputPanel = createInputPanel();

        ((JTextField) inputPanel.getComponent(1)).setText((String) tableModel.getValueAt(selectedRow, 1));
        ((JTextField) inputPanel.getComponent(3)).setText((String) tableModel.getValueAt(selectedRow, 2));
        ((JTextField) inputPanel.getComponent(5)).setText((String) tableModel.getValueAt(selectedRow, 3));
        ((JTextField) inputPanel.getComponent(7)).setText(String.valueOf(tableModel.getValueAt(selectedRow, 4)));
        ((JTextField) inputPanel.getComponent(9)).setText(String.valueOf(tableModel.getValueAt(selectedRow, 5)));

        int option = JOptionPane.showConfirmDialog(mainPanel, inputPanel, "Update Item", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                String category = ((JTextField) inputPanel.getComponent(1)).getText();
                String model = ((JTextField) inputPanel.getComponent(3)).getText();
                String dateStored = ((JTextField) inputPanel.getComponent(5)).getText();
                double price = Double.parseDouble(((JTextField) inputPanel.getComponent(7)).getText());
                int quantity = Integer.parseInt(((JTextField) inputPanel.getComponent(9)).getText());

                if (!dateStored.matches("\\d{2}/\\d{2}/\\d{4}")) {
                    JOptionPane.showMessageDialog(mainPanel, "Invalid date format. Please use dd/MM/yyyy.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String[] parts = dateStored.split("/");
                String sqlFormattedDate = parts[2] + "-" + parts[1] + "-" + parts[0];

                String sql = "UPDATE Item SET Category=?, Model=?, Item_Date_Stored=?, Item_Price=?, Quantity_in_Stock=? WHERE Item_ID=?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, category);
                    stmt.setString(2, model);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(mainPanel, "Item updated successfully!");
                loadTableData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainPanel, "Error updating item: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteItem() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(mainPanel, "Please select an item to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int itemId = (int) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(mainPanel, "Are you sure you want to delete this item?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String sql = "DELETE FROM Item WHERE Item_ID=?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, itemId);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(mainPanel, "Item deleted successfully!");
                loadTableData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(mainPanel, "Error deleting item: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private JPanel createInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField categoryField = new JTextField();
        JTextField modelField = new JTextField();
        JTextField dateField = new JTextField("dd/MM/yyyy");
        JTextField priceField = new JTextField();
        JTextField quantityField = new JTextField();

        panel.add(new JLabel("Category:"));
        panel.add(categoryField);
        panel.add(new JLabel("Model:"));
        panel.add(modelField);
        panel.add(new JLabel("Date Stored:"));
        panel.add(dateField);
        panel.add(new JLabel("Price:"));
        panel.add(priceField);
        panel.add(new JLabel("Quantity:"));
        panel.add(quantityField);

        return panel;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Inventory Management System");
        frame.setContentPane(new InventoryManagement().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    public JPanel getMainPanel() {
        return mainPanel;
    }
}
