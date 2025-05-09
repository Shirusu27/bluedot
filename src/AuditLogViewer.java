import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.*;

public class AuditLogViewer extends JFrame {
    private static final Color COLOR_NAVY_DARK_BG = new Color(0x00, 0x1F, 0x3F);
    private static final Color COLOR_STEEL_BLUE_PANEL = new Color(0x3A, 0x6D, 0x8C);
    private static final Color COLOR_CADET_BLUE_ACCENT = new Color(0x6A, 0x9A, 0xB0);
    private static final Color COLOR_TEXT_LIGHT = new Color(0xE0, 0xE7, 0xEF);

    private JTable table;
    private DefaultTableModel tableModel;

    public AuditLogViewer() {
        setTitle("Login Audit Log");
        setSize(700, 400);
        setLocationRelativeTo(null);

        tableModel = new DefaultTableModel(new Object[]{"Username", "Date/Time", "Status", "Details"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBackground(COLOR_NAVY_DARK_BG);
        scrollPane.getViewport().setBackground(COLOR_NAVY_DARK_BG);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setBackground(COLOR_NAVY_DARK_BG);

        JLabel lblTitle = new JLabel("User Login Audit Log", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(COLOR_CADET_BLUE_ACCENT);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0,0,16,0));
        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        add(panel);

        // Load data
        loadAuditLogs();
    }

    private void styleTable(JTable tbl) {
        tbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tbl.setBackground(COLOR_NAVY_DARK_BG);
        tbl.setForeground(COLOR_TEXT_LIGHT);
        tbl.setRowHeight(30);

        JTableHeader header = tbl.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.setBackground(COLOR_STEEL_BLUE_PANEL);
        header.setForeground(COLOR_TEXT_LIGHT);

        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setForeground(value != null && value.toString().equalsIgnoreCase("Success") ? new Color(0x5CB85C) : new Color(0xD9534F));
                c.setBackground(isSelected ? COLOR_CADET_BLUE_ACCENT : COLOR_NAVY_DARK_BG);
                return c;
            }
        };
        tbl.getColumnModel().getColumn(2).setCellRenderer(statusRenderer);
    }

    private void loadAuditLogs() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username, datetime, status, details FROM AuditLog ORDER BY datetime DESC")) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("username"),
                        rs.getString("datetime"),
                        rs.getString("status"),
                        rs.getString("details")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load audit log: " + e.getMessage());
        }
    }

    // For standalone testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AuditLogViewer().setVisible(true));
    }
}