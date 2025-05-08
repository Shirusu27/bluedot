import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.*;
import java.text.NumberFormat; // Import for currency formatting
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Dashboard {
   // private Connection conn;
    private DashboardDesign ui;

    private InventoryManagement inventoryManagement;
    private POS pos;

    private TimeSeries seriesFirearms;
    private TimeSeries seriesAmmunition;
    private TimeSeries seriesAccessory;
    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);

    public Dashboard() {
        ui = new DashboardDesign();

        inventoryManagement = new InventoryManagement();
        ui.inventoryPanel = inventoryManagement.getMainPanel();
        pos = new POS();
        ui.salesPanel = pos.getMainPanel();

        ui.mainPanel.add(ui.inventoryPanel, "Inventory");
        ui.mainPanel.add(ui.salesPanel, "Sales");

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int year = 2020; year <= currentYear; year++) { // Adjusted start year for relevance
            ui.yearComboBox.addItem(year);
        }
        ui.yearComboBox.setSelectedItem(selectedYear);

        setupEventListeners();

        refreshAllDashboardData(); // Initial data load

        ui.frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        ImageIcon icon = new ImageIcon("bluedotlogotrans.png");
        ui.frame.setIconImage(icon.getImage());
        ui.frame.setVisible(true);
    }

    private void setupEventListeners() {
        ui.dashboardButton.addActionListener(e -> {
            refreshAllDashboardData();
            switchCard("Dashboard");
        });
        ui.itemsButton.addActionListener(e -> {
            if (inventoryManagement != null) {
                inventoryManagement.refreshTable();
            }
            switchCard("Inventory");
        });
        ui.salesButton.addActionListener(e -> switchCard("Sales"));
        ui.logoutButton.addActionListener(e -> System.exit(0));

        ui.yearComboBox.addActionListener(e -> {
            if (ui.yearComboBox.getSelectedItem() != null) {
                selectedYear = (int) ui.yearComboBox.getSelectedItem();
                refreshInventorySummary(); // This re-creates and refreshes charts
            }
        });
    }

    public void refreshAllDashboardData() {
        refreshInventorySummary(); // Loads stats and creates/refreshes charts
        refreshTopSellingPanel();  // Loads top selling items
    }


    private void switchCard(String cardName) {
        CardLayout cl = (CardLayout) ui.mainPanel.getLayout();
        cl.show(ui.mainPanel, cardName);
    }
    private void refreshInventorySummary() {
        // Potential lines around 136 where NPE could occur if ui.component is null:
        ui.firearmLabel.setText(String.valueOf(getItemCountByCategory("Firearm")));
        ui.ammunitionLabel.setText(String.valueOf(getItemCountByCategory("Ammunition")));
        ui.accessoriesLabel.setText(String.valueOf(getItemCountByCategory("Accessory")));

        ui.chartHolderPanel.removeAll(); // Or this line
        ui.chartHolderPanel.add(createChartPanel(), BorderLayout.CENTER);
        // ...
    }


    private int getItemCountByCategory(String category) {
        String sql =  "SELECT SUM(Quantity_in_Stock) FROM Item WHERE Category = ?" ;
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            System.err.println("Error getting item count for category "  + category + ": " + ex.getMessage());
        }
        return 0;
    }


    private JPanel createChartPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 15));
        panel.setBackground(DashboardDesign.COLOR_STEEL_BLUE_PANEL);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // --- TimeSeries Chart (Monthly Revenue) ---
        seriesFirearms = new TimeSeries("Firearms");
        seriesAmmunition = new TimeSeries("Ammunition");
        seriesAccessory = new TimeSeries("Accessory");

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(seriesFirearms);
        dataset.addSeries(seriesAmmunition);
        dataset.addSeries(seriesAccessory);

        JFreeChart timeSeriesChart = ChartFactory.createTimeSeriesChart(
                "Monthly Revenue Trend",
                "Month",
                "Total Revenue",
                dataset,
                true, true, false);

        // Style TimeSeries Chart
        timeSeriesChart.setBackgroundPaint(DashboardDesign.COLOR_STEEL_BLUE_PANEL);
        timeSeriesChart.getTitle().setPaint(DashboardDesign.COLOR_TEXT_LIGHT);
        timeSeriesChart.getTitle().setFont(DashboardDesign.FONT_TITLE);

        XYPlot plot = timeSeriesChart.getXYPlot();
        plot.setBackgroundPaint(DashboardDesign.COLOR_INPUT_BG);
        plot.setDomainGridlinePaint(DashboardDesign.COLOR_BORDER_SUBTLE);
        plot.setRangeGridlinePaint(DashboardDesign.COLOR_BORDER_SUBTLE);
        plot.setOutlineVisible(false);

        // Style Domain Axis (X-axis - Date)
        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        domainAxis.setDateFormatOverride(new SimpleDateFormat("MMM yyyy"));
        domainAxis.setTickLabelPaint(DashboardDesign.COLOR_TEXT_LIGHT);
        domainAxis.setLabelPaint(DashboardDesign.COLOR_TEXT_LIGHT);
        domainAxis.setTickLabelFont(DashboardDesign.FONT_BUTTON);
        domainAxis.setLabelFont(DashboardDesign.FONT_LABEL);

        // Style Range Axis (Y-axis - Revenue)
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelPaint(DashboardDesign.COLOR_TEXT_LIGHT);
        rangeAxis.setLabelPaint(DashboardDesign.COLOR_TEXT_LIGHT);
        rangeAxis.setTickLabelFont(DashboardDesign.FONT_BUTTON);
        rangeAxis.setLabelFont(DashboardDesign.FONT_LABEL);
        rangeAxis.setAutoRangeIncludesZero(true); // Ensure Y-axis starts at 0
        rangeAxis.setNumberFormatOverride(NumberFormat.getCurrencyInstance()); // Format as currency

        // Renderer for series colors and style
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, DashboardDesign.COLOR_ACCENT_FIREARM);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));

        renderer.setSeriesPaint(1, DashboardDesign.COLOR_ACCENT_AMMO);
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));

        renderer.setSeriesPaint(2, DashboardDesign.COLOR_ACCENT_ACCESSORY);
        renderer.setSeriesStroke(2, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(2, true);
        renderer.setSeriesShape(2, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));
        plot.setRenderer(renderer);

        // Style Legend
        LegendTitle legend = timeSeriesChart.getLegend();
        if (legend != null) {
            legend.setBackgroundPaint(DashboardDesign.COLOR_STEEL_BLUE_PANEL);
            legend.setItemPaint(DashboardDesign.COLOR_TEXT_LIGHT);
            legend.setItemFont(DashboardDesign.FONT_BUTTON);
        }

        refreshChartData(seriesFirearms, seriesAmmunition, seriesAccessory, selectedYear); // Populate with data
        ChartPanel timeSeriesChartPanel = new ChartPanel(timeSeriesChart);
        timeSeriesChartPanel.setBorder(BorderFactory.createEmptyBorder());
        panel.add(timeSeriesChartPanel);


        // --- Pie Chart (Inventory Distribution) ---
        DefaultPieDataset pieDataset = new DefaultPieDataset();

            String pieSql = "SELECT Category, SUM(Quantity_in_Stock) as Total FROM Item GROUP BY Category";
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(pieSql)) {
                while (rs.next()) {
                    String category = rs.getString("Category");
                    int total = rs.getInt("Total" );
                    if (category != null && total > 0) {
                        pieDataset.setValue(category, total);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error loading inventory pie chart data: " + e.getMessage());
            }


        JFreeChart pieChart = ChartFactory.createPieChart(
                "Inventory Distribution", pieDataset, true, true, false);

        // Style Pie Chart
        pieChart.setBackgroundPaint(DashboardDesign.COLOR_STEEL_BLUE_PANEL);
        pieChart.getTitle().setPaint(DashboardDesign.COLOR_TEXT_LIGHT);
        pieChart.getTitle().setFont(DashboardDesign.FONT_TITLE);

        PiePlot piePlot = (PiePlot) pieChart.getPlot();
        piePlot.setBackgroundPaint(DashboardDesign.COLOR_STEEL_BLUE_PANEL);
        piePlot.setLabelGenerator(null);
        piePlot.setOutlineVisible(false);
        piePlot.setShadowPaint(null);

        for (Object key : pieDataset.getKeys()) {
            String category = (String) key;
            if ("Firearm".equalsIgnoreCase(category)) {
                piePlot.setSectionPaint(category, DashboardDesign.COLOR_ACCENT_FIREARM);
            } else if ("Ammunition".equalsIgnoreCase(category)) {
                piePlot.setSectionPaint(category, DashboardDesign.COLOR_ACCENT_AMMO);
            } else if ("Accessory".equalsIgnoreCase(category)) {
                piePlot.setSectionPaint(category, DashboardDesign.COLOR_ACCENT_ACCESSORY);
            } else {
                piePlot.setSectionPaint(category, Color.DARK_GRAY);
            }
        }
        piePlot.setDefaultSectionOutlinePaint(DashboardDesign.COLOR_NAVY_DARK_BG);
        piePlot.setDefaultSectionOutlineStroke(new BasicStroke(1.5f));
        piePlot.setSectionOutlinesVisible(true);

        LegendTitle pieLegend = pieChart.getLegend();
        if (pieLegend != null) {
            pieLegend.setBackgroundPaint(DashboardDesign.COLOR_STEEL_BLUE_PANEL);
            pieLegend.setItemPaint(DashboardDesign.COLOR_TEXT_LIGHT);
            pieLegend.setItemFont(DashboardDesign.FONT_BUTTON);
        }
        ChartPanel pieChartPanel = new ChartPanel(pieChart);
        pieChartPanel.setBorder(BorderFactory.createEmptyBorder());
        panel.add(pieChartPanel);

        return panel;
    }

    private void refreshChartData(TimeSeries firearmSeries, TimeSeries ammoSeries, TimeSeries accessorySeries, int year) {


        firearmSeries.clear();
        ammoSeries.clear();
        accessorySeries.clear();

        for (int month = 1; month <= 12; month++) {
            Month m = new Month(month, year);
            firearmSeries.addOrUpdate(m, 0.0);
            ammoSeries.addOrUpdate(m, 0.0); // Corrected this line, was cut off
            accessorySeries.addOrUpdate(m, 0.0);
        }

        // Query to get monthly revenue per category by joining Sales with Item
        // CORRECTED THE COLUMN NAME FROM s.S_Date TO s.Date
        String sql = "SELECT Month(s.Date) AS SaleMonth, i.Category, SUM(s.Total) AS MonthlyRevenue " +
                "FROM Sales s " +
                "INNER JOIN Item i ON s.Product_ID = i.ID " +
                "WHERE Year(s.Date) = ? " + // CORRECTED HERE
                "GROUP BY Month(s.Date), i.Category " + // CORRECTED HERE
                "ORDER BY SaleMonth, i.Category";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, year);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int saleMonth = rs.getInt("SaleMonth");
                String category = rs.getString("Category");
                double monthlyRevenue = rs.getDouble("MonthlyRevenue");
                Month m = new Month(saleMonth, year);

                if ("Firearm".equalsIgnoreCase(category)) {
                    firearmSeries.addOrUpdate(m, monthlyRevenue);
                } else if ("Ammunition".equalsIgnoreCase(category)) {
                    ammoSeries.addOrUpdate(m, monthlyRevenue);
                } else if ("Accessory".equalsIgnoreCase(category)) {
                    accessorySeries.addOrUpdate(m, monthlyRevenue);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error refreshing revenue chart data: " + e.getMessage());
            // It's good practice to show the user a more friendly error message as well
            JOptionPane.showMessageDialog(ui.frame, "Error refreshing revenue chart data!\n" + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void refreshTopSellingPanel() {
        ui.topSellingPanel.removeAll();
        JScrollPane topItemsScrollPane = createTopSellingTableScrollPane();
        ui.topSellingPanel.add(topItemsScrollPane, BorderLayout.CENTER);
        ui.topSellingPanel.revalidate();
        ui.topSellingPanel.repaint();
    }

    private JScrollPane createTopSellingTableScrollPane() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Rank", "Item Name", "Category", "Quantity Sold"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

            String sql = "SELECT TOP 5 i.Model, SUM(s.Quantity) AS TotalSold, i.Category " +
                    "FROM Sales s " +
                    "INNER JOIN Item i ON s.Product_ID = i.ID " +
                    "GROUP BY i.Model, i.Category " +
                    "ORDER BY TotalSold DESC";
        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                int rank = 1;
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rank++,
                            rs.getString("Model"),
                            rs.getString("Category"),
                            rs.getInt("TotalSold")
                    });
                }
            } catch (SQLException e) {
                System.err.println("Error loading top selling items: " + e.getMessage());
            }


        JTable topItemsTable = new JTable(model);
        styleTopItemsTable(topItemsTable);

        JScrollPane scrollPane = new JScrollPane(topItemsTable);
        scrollPane.getViewport().setBackground(DashboardDesign.COLOR_INPUT_BG);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = DashboardDesign.COLOR_CADET_BLUE_ACCENT;
                this.trackColor = DashboardDesign.COLOR_STEEL_BLUE_PANEL;
                this.thumbDarkShadowColor = this.thumbColor.darker();
                this.thumbLightShadowColor = this.thumbColor.brighter();
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton jbutton = new JButton();
                jbutton.setPreferredSize(new Dimension(0, 0));
                jbutton.setMinimumSize(new Dimension(0, 0));
                jbutton.setMaximumSize(new Dimension(0, 0));
                return jbutton;
            }
        });
        verticalScrollBar.setPreferredSize(new Dimension(10, 0));

        return scrollPane;
    }

    private void styleTopItemsTable(JTable table) {
        table.setFont(DashboardDesign.FONT_TABLE_CELL_LARGE);
        table.setRowHeight(38);
        table.setGridColor(DashboardDesign.COLOR_TABLE_GRID);
        table.setBackground(DashboardDesign.COLOR_INPUT_BG);
        table.setForeground(DashboardDesign.COLOR_TEXT_LIGHT);
        table.setSelectionBackground(DashboardDesign.COLOR_CADET_BLUE_ACCENT);
        table.setSelectionForeground(DashboardDesign.COLOR_NAVY_DARK_BG);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader header = table.getTableHeader();
        header.setFont(DashboardDesign.FONT_TABLE_HEADER_LARGE);
        header.setBackground(DashboardDesign.COLOR_STEEL_BLUE_PANEL);
        header.setForeground(DashboardDesign.COLOR_TEXT_LIGHT);
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, DashboardDesign.COLOR_CADET_BLUE_ACCENT));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(isSelected ? DashboardDesign.COLOR_CADET_BLUE_ACCENT : DashboardDesign.COLOR_INPUT_BG);
                c.setForeground(isSelected ? DashboardDesign.COLOR_NAVY_DARK_BG : DashboardDesign.COLOR_TEXT_LIGHT);
                c.setBorder(new EmptyBorder(5, 10, 5, 10));

                if (column == 0 || column == 3) {
                    c.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    c.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(60);
            table.getColumnModel().getColumn(0).setMaxWidth(80);
        }
        if (table.getColumnModel().getColumnCount() > 1) {
            table.getColumnModel().getColumn(1).setPreferredWidth(250);
        }
        if (table.getColumnModel().getColumnCount() > 2) {
            table.getColumnModel().getColumn(2).setPreferredWidth(150);
        }
        if (table.getColumnModel().getColumnCount() > 3) {
            table.getColumnModel().getColumn(3).setPreferredWidth(120);
            table.getColumnModel().getColumn(3).setMaxWidth(150);
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(Dashboard::new);
    }
}

