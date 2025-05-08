import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class Dashboard {
    private JFrame frame;
    private Connection conn;
    private JPanel sidebar, mainPanel, topPanel, contentPanel, dashboardPanel, inventoryPanel;
    private JButton dashboardButton, itemsButton, salesButton, logoutButton;
    private JLabel companyLogo, companyName, firearmLabel, ammunitionLabel, accessoriesLabel;
    private InventoryManagement inventoryManagement;
    private JPanel chartHolderPanel;
    private CardLayout cardLayout;
    private JPanel topSellingPanel;

    public Dashboard() {
        connectToDatabase();
        createUI();
        updateDashboardStats();
        refreshInventorySummary();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void connectToDatabase() {
        try {
            String url = "jdbc:ucanaccess://C://Users//ADMIN//IdeaProjects//bluedot//bluedotDatabase.accdb";
            conn = DriverManager.getConnection(url);
            System.out.println("Database connected successfully!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Failed to connect to database!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void updateDashboardStats() {
        try {
            firearmLabel.setText(getCategoryCount("Firearm") + "");
            ammunitionLabel.setText(getCategoryCount("Ammunition") + "");
            accessoriesLabel.setText(getCategoryCount("Accessory") + "");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Failed to update stats!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private int getCategoryCount(String category) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT SUM(Quantity_in_Stock) FROM Item WHERE Category = ?");
        stmt.setString(1, category);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            System.out.println("Category " + category + ": " + rs.getInt(1));
            return rs.getInt(1);
        }
        return 0;
    }

    private void refreshInventorySummary() {
        firearmLabel.setText(String.valueOf(getItemCountByCategory("Firearm")));
        ammunitionLabel.setText(String.valueOf(getItemCountByCategory("Ammunition")));
        accessoriesLabel.setText(String.valueOf(getItemCountByCategory("Accessory")));

        chartHolderPanel.removeAll();
        chartHolderPanel.add(createChartPanel(), BorderLayout.CENTER);
        chartHolderPanel.revalidate();
        chartHolderPanel.repaint();
    }

    private int getItemCountByCategory(String category) {
        int count = 0;
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT SUM(Quantity_in_Stock) FROM Item WHERE Category = ?")) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return count;
    }

    private void createUI() {
        frame = new JFrame("Bluedot");
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 700);
        frame.setLayout(new BorderLayout());

        sidebar = new JPanel();
        sidebar.setLayout(new BorderLayout());
        sidebar.setBackground(new Color(0x001F3F));
        sidebar.setPreferredSize(new Dimension(200, frame.getHeight()));

        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(new Color(255,255,255));

        ImageIcon logoIcon = new ImageIcon("bluedotlogotrans.png");
        Image img = logoIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
        companyLogo = new JLabel(new ImageIcon(img));
        companyLogo.setHorizontalAlignment(SwingConstants.CENTER);

        companyName = new JLabel("BLUEDOT", SwingConstants.CENTER);

        companyName.setForeground(Color.BLACK);
        companyName.setFont(new Font("Arial", Font.BOLD, 18));

        JPanel namePanel = new JPanel();
        namePanel.setBackground(new Color(255,255,255));
        namePanel.setLayout(new BorderLayout());
        namePanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        namePanel.add(companyName, BorderLayout.CENTER);

        logoPanel.add(companyLogo, BorderLayout.NORTH);
        logoPanel.add(namePanel, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonsPanel.setBackground(new Color(16, 48, 80)); // Steel blue
        buttonsPanel.setForeground(new Color(0xE0E7EF));

        dashboardButton = createSidebarButton("Dashboard");
        itemsButton = createSidebarButton("Inventory");
        salesButton = createSidebarButton("Sales");
        logoutButton = createSidebarButton("Logout");

        buttonsPanel.add(dashboardButton);
        buttonsPanel.add(itemsButton);
        buttonsPanel.add(salesButton);
        buttonsPanel.add(logoutButton);

        sidebar.add(logoPanel, BorderLayout.NORTH);
        sidebar.add(buttonsPanel, BorderLayout.CENTER);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        dashboardPanel = new JPanel(new BorderLayout());
        topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(1, 3, 10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        firearmLabel = new JLabel("...");
        ammunitionLabel = new JLabel("...");
        accessoriesLabel = new JLabel("...");

        topPanel.add(createStatPanel("TOTAL FIREARMS", firearmLabel));
        topPanel.add(createStatPanel("TOTAL AMMUNITION", ammunitionLabel));
        topPanel.add(createStatPanel("TOTAL ACCESSORIES", accessoriesLabel));

        ImageIcon refreshIcon = new ImageIcon("refresh.png");
        Image scaledImage = refreshIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        ImageIcon scaledRefreshIcon = new ImageIcon(scaledImage);

        JButton refreshButton = new JButton(scaledRefreshIcon);
        refreshButton.setPreferredSize(new Dimension(32, 32));
        refreshButton.setContentAreaFilled(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setFocusPainted(false);
        refreshButton.setToolTipText("Refresh Inventory Summary");

        refreshButton.addActionListener(e -> {
            refreshInventorySummary();
            refreshTopSellingPanel();
        });

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        refreshPanel.setOpaque(false);
        JPanel topWrapperPanel = new JPanel(new BorderLayout());
        topWrapperPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        topWrapperPanel.add(topPanel, BorderLayout.CENTER);
        topWrapperPanel.add(refreshPanel, BorderLayout.WEST);

        dashboardPanel.add(topWrapperPanel, BorderLayout.NORTH);

        contentPanel = new JPanel(new BorderLayout());
        topSellingPanel = createTopSellingPanel();
        contentPanel.add(topSellingPanel, BorderLayout.NORTH);
        chartHolderPanel = new JPanel(new BorderLayout());
        chartHolderPanel.add(createChartPanel(), BorderLayout.CENTER);
        contentPanel.add(chartHolderPanel, BorderLayout.CENTER);

        dashboardPanel.add(contentPanel, BorderLayout.CENTER);

        inventoryManagement = new InventoryManagement();
        inventoryPanel = inventoryManagement.getMainPanel();
        POS pos = new POS();

        mainPanel.add(dashboardPanel, "Dashboard");
        mainPanel.add(inventoryPanel, "Inventory");
        mainPanel.add(pos.getMainPanel(), "Sales");

        frame.add(sidebar, BorderLayout.WEST);
        frame.add(mainPanel, BorderLayout.CENTER);

        dashboardButton.addActionListener(e -> {
            refreshInventorySummary();
            refreshTopSellingPanel();
            cardLayout.show(mainPanel, "Dashboard");
        });

        itemsButton.addActionListener(e -> {
            inventoryManagement.refreshTable();
            cardLayout.show(mainPanel, "Inventory");
        });

        salesButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "Sales");
        });
        logoutButton.addActionListener(e -> System.exit(0));

        frame.setVisible(true);
    }

    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(0x6A, 0x9A, 0xB0));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(180, 45));
        return button;
    }

    private JPanel createStatPanel(String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        panel.add(new JLabel(title, SwingConstants.CENTER));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(valueLabel);
        return panel;
    }

    private JPanel createChartPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        double[] revenues = {
                12000, 13500, 9800, 14200, 15700, 13100,
                14900, 16200, 13800, 14400, 15500, 16000
        };

        for (int i = 0; i < monthNames.length; i++) {
            dataset.addValue(revenues[i], "Revenue", monthNames[i]);
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Financial Performance",
                "Month",
                "Revenue",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        panel.add(new ChartPanel(barChart));


        DefaultPieDataset pieDataset = new DefaultPieDataset();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT Category, SUM(Quantity_in_Stock) as Total FROM Item GROUP BY Category");

            while (rs.next()) {
                String category = rs.getString("Category");
                int quantity = rs.getInt("Total");
                pieDataset.setValue(category, quantity);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading inventory pie chart!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        JFreeChart pieChart = ChartFactory.createPieChart(
                "Inventory Distribution",
                pieDataset,
                true,
                true,
                false
        );
        panel.add(new ChartPanel(pieChart));

        return panel;
    }

    private JPanel createTopSellingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Top Selling Items"));

        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new GridLayout(1, 5, 10, 10));

        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT TOP 5 Product_Name, SUM(Quantity) as TotalSold FROM Sales GROUP BY Product_Name ORDER BY SUM(Quantity) DESC";
            ResultSet rs = stmt.executeQuery(sql);

            int count = 0;
            while (rs.next() && count < 5) {
                String productName = rs.getString("Product_Name");
                int totalSold = rs.getInt("TotalSold");
                itemsPanel.add(createItemCard(productName, totalSold + " sold"));
                count++;
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading top selling items!", "Error", JOptionPane.ERROR_MESSAGE);
        }

        JScrollPane scrollPane = new JScrollPane(itemsPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(600, 100));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createItemCard(String itemName, String sold) {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        panel.add(new JLabel(itemName, SwingConstants.CENTER));
        JLabel soldLabel = new JLabel(sold, SwingConstants.CENTER);
        soldLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(soldLabel);
        return panel;
    }

    private void refreshTopSellingPanel() {
        contentPanel.remove(topSellingPanel);
        topSellingPanel = createTopSellingPanel();
        contentPanel.add(topSellingPanel, BorderLayout.NORTH);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Dashboard::new);
    }
}
