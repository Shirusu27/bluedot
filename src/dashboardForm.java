import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class dashboardForm {
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel border;
    private JPanel Sidebar;
    private JButton recButton;
    private JButton posButton;
    private JButton stockButton;
    private JButton CRUDButton;
    private JButton logoutButton;

    private JPanel contentPanel;
    private JPanel graphPanel;
    private JPanel inventoryPanel;

    public dashboardForm() {
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        frame = new JFrame("Firearm Inventory Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new Dimension(800, 500));
        frame.setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout());

        Sidebar = new JPanel();
        Sidebar.setLayout(new GridLayout(5, 1, 3, 3));
        Sidebar.setBackground(Color.DARK_GRAY);


        Sidebar.add(recButton);
        Sidebar.add(posButton);
        Sidebar.add(stockButton);
        Sidebar.add(CRUDButton);
        Sidebar.add(logoutButton);

        contentPanel = new JPanel(new BorderLayout());
        graphPanel = createGraphPanel();

        contentPanel.add(graphPanel, BorderLayout.CENTER);

        frame.add(Sidebar, BorderLayout.WEST);
        frame.add(contentPanel, BorderLayout.CENTER);

        frame.setVisible(true);

        CRUDButton.addActionListener(e -> switchToInventory());
        recButton.addActionListener(e -> switchToGraphs());
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int choice = JOptionPane.showConfirmDialog(frame, "Are you sure you want to log out?", "Logout Confirmation", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    frame.dispose();
                    new LoginForm();
                }
            }
        });
    }

    private JPanel createGraphPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(createFirearmStockGraph());
        panel.add(createAmmoSalesGraph());
        panel.add(createFirearmCategoryGraph());
        panel.add(createFirearmRevenueGraph());

        return panel;
    }

    private JPanel createFirearmStockGraph() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(35, "Stock", "Handguns");
        dataset.addValue(25, "Stock", "Rifles");
        dataset.addValue(15, "Stock", "Shotguns");
        dataset.addValue(20, "Stock", "SMGs");

        JFreeChart chart = ChartFactory.createBarChart(
                "Current Firearm Stock",
                "Firearm Type",
                "Quantity",
                dataset
        );

        return new ChartPanel(chart);
    }

    private JPanel createAmmoSalesGraph() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(500, "Ammunition Sales", "Week 1");
        dataset.addValue(650, "Ammunition Sales", "Week 2");
        dataset.addValue(800, "Ammunition Sales", "Week 3");
        dataset.addValue(750, "Ammunition Sales", "Week 4");

        JFreeChart chart = ChartFactory.createLineChart(
                "Ammunition Sales Trends",
                "Week",
                "Rounds Sold",
                dataset
        );

        return new ChartPanel(chart);
    }

    private JPanel createFirearmCategoryGraph() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Handguns", 40);
        dataset.setValue("Rifles", 30);
        dataset.setValue("Shotguns", 15);
        dataset.setValue("SMGs", 15);

        JFreeChart chart = ChartFactory.createPieChart(
                "Firearm Inventory Distribution",
                dataset
        );

        return new ChartPanel(chart);
    }

    private JPanel createFirearmRevenueGraph() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(15000, "Revenue", "January");
        dataset.addValue(18000, "Revenue", "February");
        dataset.addValue(22000, "Revenue", "March");
        dataset.addValue(25000, "Revenue", "April");

        JFreeChart chart = ChartFactory.createBarChart(
                "Monthly Firearm Sales Revenue",
                "Month",
                "Revenue (PHP)",
                dataset
        );

        return new ChartPanel(chart);
    }

    private void switchToInventory() {
        contentPanel.removeAll();
        contentPanel.add(inventoryPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void switchToGraphs() {
        contentPanel.removeAll();
        contentPanel.add(graphPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public static void main(String[] args) {
        new dashboardForm();
    }
}
