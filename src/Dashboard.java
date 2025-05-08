import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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
    private TimeSeries seriesFirearms = new TimeSeries("Firearms");
    private TimeSeries seriesAmmunition = new TimeSeries("Ammunition");
    private TimeSeries seriesAccessory = new TimeSeries("Accessory");
    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    private JComboBox<Integer> yearComboBox;

    public Dashboard() {
        connectToDatabase();
        createUI();
        updateDashboardStats();
        refreshInventorySummary();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        ImageIcon icon = new ImageIcon("bluedotlogotrans.png");
        frame.setIconImage(icon.getImage());
    }

    private void connectToDatabase() {
        try {
            String url = "jdbc:ucanaccess://C:/Files/bluedot/bluedotDatabase.accdb";
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
        if (rs.next()) return rs.getInt(1);
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
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT SUM(Quantity_in_Stock) FROM Item WHERE Category = ?")) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    private void createUI() {
        frame = new JFrame("Bluedot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 700);
        frame.setLayout(new BorderLayout());

        createSidebar();
        createDashboard();

        inventoryManagement = new InventoryManagement();
        inventoryPanel = inventoryManagement.getMainPanel();
        POS pos = new POS();

        mainPanel = new JPanel(new CardLayout());
        mainPanel.add(dashboardPanel, "Dashboard");
        mainPanel.add(inventoryPanel, "Inventory");
        mainPanel.add(pos.getMainPanel(), "Sales");

        cardLayout = (CardLayout) mainPanel.getLayout();
        frame.add(sidebar, BorderLayout.WEST);
        frame.add(mainPanel, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private void createSidebar() {
        sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(240, 240, 240)); // Lighten background
        sidebar.setBackground(new Color(80, 80, 80));
        sidebar.setPreferredSize(new Dimension(200, frame.getHeight()));

        // Logo
        ImageIcon logoIcon = new ImageIcon("bluedotlogotrans.png");
        Image img = logoIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
        companyLogo = new JLabel(new ImageIcon(img), SwingConstants.CENTER);

        companyName = new JLabel("BLUEDOT", SwingConstants.CENTER);
        companyName.setForeground(Color.WHITE);
        companyName.setFont(new Font("Arial", Font.BOLD, 18));

        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(new Color(80, 80, 80));
        logoPanel.add(companyLogo, BorderLayout.NORTH);
        logoPanel.add(companyName, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonsPanel.setBackground(new Color(80, 80, 80));

        dashboardButton = createSidebarButton("Dashboard");
        itemsButton = createSidebarButton("Inventory");
        salesButton = createSidebarButton("Sales");
        logoutButton = createSidebarButton("Logout");

        dashboardButton.setBackground(new Color(30, 144, 255)); // Brighter blue
        dashboardButton.setForeground(Color.WHITE);

        itemsButton.setBackground(new Color(30, 144, 255));
        itemsButton.setForeground(Color.WHITE);

        salesButton.setBackground(new Color(30, 144, 255));
        salesButton.setForeground(Color.WHITE);

        logoutButton.setBackground(new Color(30, 144, 255));
        logoutButton.setForeground(Color.WHITE);

        buttonsPanel.add(dashboardButton);
        buttonsPanel.add(itemsButton);
        buttonsPanel.add(salesButton);
        buttonsPanel.add(logoutButton);

        sidebar.add(logoPanel, BorderLayout.NORTH);
        sidebar.add(buttonsPanel, BorderLayout.CENTER);

        dashboardButton.addActionListener(e -> {
            refreshInventorySummary();
            refreshTopSellingPanel();
            cardLayout.show(mainPanel, "Dashboard");
        });

        itemsButton.addActionListener(e -> {
            inventoryManagement.refreshTable();
            cardLayout.show(mainPanel, "Inventory");
        });

        salesButton.addActionListener(e -> cardLayout.show(mainPanel, "Sales"));
        logoutButton.addActionListener(e -> System.exit(0));
    }

    private void createDashboard() {
        dashboardPanel = new JPanel(new BorderLayout());

        // Top Stat Panel
        topPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        firearmLabel = new JLabel("...");
        ammunitionLabel = new JLabel("...");
        accessoriesLabel = new JLabel("...");

        topPanel.add(createStatPanel("TOTAL FIREARMS", firearmLabel));
        topPanel.add(createStatPanel("TOTAL AMMUNITION", ammunitionLabel));
        topPanel.add(createStatPanel("TOTAL ACCESSORIES", accessoriesLabel));

        JPanel topWrapperPanel = new JPanel(new BorderLayout());
        topWrapperPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topWrapperPanel.add(topPanel, BorderLayout.CENTER);

        dashboardPanel.add(topWrapperPanel, BorderLayout.NORTH);

        // Chart Panel
        chartHolderPanel = new JPanel(new BorderLayout());
        chartHolderPanel.add(createChartPanel(), BorderLayout.CENTER);

        yearComboBox = new JComboBox<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int year = 2000; year <= currentYear; year++) {
            yearComboBox.addItem(year);
        }
        yearComboBox.setSelectedItem(selectedYear);

        yearComboBox.setFont(new Font("Arial", Font.BOLD, 16));  // Set larger font
        yearComboBox.setPreferredSize(new Dimension(120, 40));   // Set a larger size for the combo box

        // Add some padding/margins
        yearComboBox.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));  // Adds padding around the combo box

        yearComboBox.addActionListener(e -> {
            selectedYear = (int) yearComboBox.getSelectedItem();
            refreshChart(seriesFirearms, seriesAmmunition, seriesAccessory, selectedYear);
        });

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 30, 10));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        filterPanel.add(new JLabel("Select Year For Monthly Revenue:"));
        filterPanel.setFont(new Font("Arial", Font.BOLD, 16));
        filterPanel.add(yearComboBox);

        JPanel chartWrapper = new JPanel(new BorderLayout());
        chartWrapper.add(filterPanel, BorderLayout.NORTH);
        chartWrapper.add(chartHolderPanel, BorderLayout.CENTER);

        // Top selling panel
        topSellingPanel = createTopSellingPanel();

        // Content
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(topSellingPanel, BorderLayout.NORTH);
        contentPanel.add(chartWrapper, BorderLayout.CENTER);

        dashboardPanel.add(contentPanel, BorderLayout.CENTER);
    }

    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(0, 102, 204));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(180, 45));
        return button;
    }

    private static final Color FIREARM_COLOR = new Color(255, 99, 71);  // Red (for Firearm)
    private static final Color AMMUNITION_COLOR = new Color(70, 130, 180); // Blue (for Ammunition)
    private static final Color ACCESSORY_COLOR = new Color(34, 139, 34);  // Green (for Accessory)

    private JPanel createStatPanel(String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(250, 80)); // Make stats bigger

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));

        if (title.equals("TOTAL FIREARMS")) {
            panel.setBackground(FIREARM_COLOR); // Red for Firearm
        } else if (title.equals("TOTAL AMMUNITION")) {
            panel.setBackground(AMMUNITION_COLOR); // Blue for Ammunition
        } else if (title.equals("TOTAL ACCESSORIES")) {
            panel.setBackground(ACCESSORY_COLOR); // Green for Accessory
        }

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createChartPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));

        seriesFirearms = new TimeSeries("Firearms");
        seriesAmmunition = new TimeSeries("Ammunition");
        seriesAccessory = new TimeSeries("Accessory");

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(seriesFirearms);
        dataset.addSeries(seriesAmmunition);
        dataset.addSeries(seriesAccessory);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Monthly Revenue Trend", "Date", "Total Revenue", dataset, true, false, false);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));

        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        domainAxis.setDateFormatOverride(new SimpleDateFormat("MMM yyyy"));

        // Set line colors based on category
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesStroke(0, new BasicStroke(2f));
        renderer.setSeriesPaint(0, FIREARM_COLOR); // Red for Firearm
        renderer.setSeriesPaint(1, AMMUNITION_COLOR); // Blue for Ammunition
        renderer.setSeriesPaint(2, ACCESSORY_COLOR); // Green for Accessory
        plot.setRenderer(renderer);

        refreshChart(seriesFirearms, seriesAmmunition, seriesAccessory, selectedYear);
        panel.add(new ChartPanel(chart));

        // Pie chart
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT Category, SUM(Quantity_in_Stock) as Total FROM Item GROUP BY Category")) {
            while (rs.next()) {
                pieDataset.setValue(rs.getString("Category"), rs.getInt("Total"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading inventory pie chart!", "Error", JOptionPane.ERROR_MESSAGE);
        }

        JFreeChart pieChart = ChartFactory.createPieChart(
                "Inventory Distribution", pieDataset, true, true, false);
        pieChart.setBackgroundPaint(Color.WHITE);
        pieChart.getTitle().setFont(new Font("Arial", Font.BOLD, 16));
        pieChart.getLegend().setItemFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(new ChartPanel(pieChart));

        return panel;
    }

    private void refreshChart(TimeSeries firearm, TimeSeries ammo, TimeSeries accessory, int year) {
        firearm.clear();
        ammo.clear();
        accessory.clear();

        for (int month = 1; month <= 12; month++) {
            Month m = new Month(month, year);
            firearm.addOrUpdate(m, null);
            ammo.addOrUpdate(m, null);
            accessory.addOrUpdate(m, null);
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT Month(Date) AS Month, " +
                     "SUM(CASE WHEN Category = 'Firearm' THEN Total ELSE 0 END) AS FirearmRevenue, " +
                     "SUM(CASE WHEN Category = 'Ammunition' THEN Total ELSE 0 END) AS AmmoRevenue, " +
                     "SUM(CASE WHEN Category = 'Accessory' THEN Total ELSE 0 END) AS AccessoryRevenue " +
                     "FROM Sales WHERE Year(Date) = " + year + " GROUP BY Month(Date)")) {
            while (rs.next()) {
                Month m = new Month(rs.getInt("Month"), year);
                firearm.addOrUpdate(m, rs.getDouble("FirearmRevenue"));
                ammo.addOrUpdate(m, rs.getDouble("AmmoRevenue"));
                accessory.addOrUpdate(m, rs.getDouble("AccessoryRevenue"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error refreshing revenue chart!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createTopSellingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Top Selling Items"));

        JPanel itemsPanel = new JPanel(new GridLayout(1, 5, 10, 10));

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT TOP 5 Item.Model, SUM(Sales.Quantity) AS TotalSold, Item.Category " +
                             "FROM Sales " +
                             "INNER JOIN Item ON Sales.Product_ID = Item.ID " +
                             "GROUP BY Item.Model, Item.Category " +
                             "ORDER BY TotalSold DESC")) {

            while (rs.next()) {
                String productName = rs.getString("Model");
                int totalSold = rs.getInt("TotalSold");
                String category = rs.getString("Category");

                // Determine color based on the category
                Color itemColor;
                switch (category) {
                    case "Firearm":
                        itemColor = new Color(255, 99, 71);  // Tomato Red for Firearms
                        break;
                    case "Ammunition":
                        itemColor = new Color(70, 130, 180);  // Steel Blue for Ammunition
                        break;
                    case "Accessory":
                        itemColor = new Color(34, 139, 34);  // Forest Green for Accessories
                        break;
                    default:
                        itemColor = Color.GRAY;  // Default if category is unknown
                        break;
                }

                itemsPanel.add(createItemCard(productName, totalSold + " sold", itemColor));
            }
        } catch (SQLException e) {
            e.printStackTrace();  // Add this line
            JOptionPane.showMessageDialog(frame, "Error loading top selling items!\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        panel.add(new JScrollPane(itemsPanel), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createItemCard(String itemName, String sold, Color cardColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(200, 100));  // Bigger cards
        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        panel.setBackground(cardColor);

        JLabel nameLabel = new JLabel(itemName, SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(Color.BLACK);

        JLabel soldLabel = new JLabel(sold, SwingConstants.CENTER);
        soldLabel.setFont(new Font("Arial", Font.BOLD, 14));
        soldLabel.setForeground(Color.BLACK);

        panel.add(nameLabel, BorderLayout.CENTER);
        panel.add(soldLabel, BorderLayout.SOUTH);
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