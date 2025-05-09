import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class DashboardDesign {

    // --- POS Color Palette ---
    public static final Color COLOR_NAVY_DARK_BG = new Color(0x00, 0x1F, 0x3F);
    public static final Color COLOR_STEEL_BLUE_PANEL = new Color(0x3A, 0x6D, 0x8C);
    public static final Color COLOR_CADET_BLUE_ACCENT = new Color(0x6A, 0x9A, 0xB0);
    public static final Color COLOR_LIGHT_BLUE_HOVER = new Color(0x8A, 0xB8, 0xC8);
    public static final Color COLOR_TEXT_LIGHT = new Color(0xE0, 0xE7, 0xEF);
    public static final Color COLOR_INPUT_BG = new Color(0x10, 0x30, 0x50);
    public static final Color COLOR_BORDER_SUBTLE = new Color(0x2C, 0x50, 0x6F);

    public static final Color COLOR_ACCENT_FIREARM = new Color(255, 99, 71);      // Red
    public static final Color COLOR_ACCENT_AMMO = new Color(70, 130, 180);        // Blue
    public static final Color COLOR_ACCENT_ACCESSORY = new Color(34, 139, 34);    // Green
    public static final Color COLOR_TABLE_GRID = new Color(0x2A, 0x5D, 0x7C);

    // --- Fonts ---
    public static final Font FONT_COMPANY_NAME = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_VALUE = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONT_SIDEBAR_BUTTON = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_TABLE_HEADER_LARGE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_TABLE_CELL_LARGE = new Font("Segoe UI", Font.PLAIN, 14);

    // --- Main UI components (public for Dashboard logic to access) ---
    public JFrame frame;
    public JPanel sidebar;
    public JButton dashboardButton, itemsButton, salesButton, logoutButton, btnClearSalesHistory, btnExportSales, btnImportSales;
    public JLabel companyLogo, companyName;
    public JPanel mainPanel;
    public JPanel dashboardPanel;
    public JPanel inventoryPanel;
    public JPanel salesPanel;

    // Dashboard main areas:
    public JLabel firearmLabel, ammunitionLabel, accessoriesLabel;
    public JPanel chartHolderPanel;
    public JComboBox<Integer> yearComboBox;
    public JPanel topSellingPanel;
    public JPanel contentPanel;
    public JButton loginHistoryButton;
    public JPanel loginHistoryPanel;

    public DashboardDesign() {
        initializeFrame();
        createSidebar(); // Corrected sidebar colors will be applied here
        createDashboardPanel();
        mainPanel = new JPanel(new CardLayout());
        mainPanel.add(dashboardPanel, "Dashboard");

        frame.add(sidebar, BorderLayout.WEST);
        frame.add(mainPanel, BorderLayout.CENTER);
    }

    private void initializeFrame() {
        frame = new JFrame("Bluedot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLayout(new BorderLayout());
        frame.setBackground(COLOR_NAVY_DARK_BG);
        frame.setLocationRelativeTo(null);
    }

    private void createSidebar() {
        sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(200, frame.getHeight()));
        sidebar.setBackground(COLOR_NAVY_DARK_BG); // Entire sidebar background

        // --- Logo and company name Panel (White Background) ---
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(COLOR_NAVY_DARK_BG); // Match sidebar background
        logoPanel.setBorder(new EmptyBorder(15, 10, 15, 10));

        loginHistoryPanel = new JPanel(new BorderLayout());
        loginHistoryPanel.setBackground(COLOR_NAVY_DARK_BG);
        loginHistoryPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        ImageIcon logoIcon = new ImageIcon("bluedotnewlogo.png");
        Image img = logoIcon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
        companyLogo = new JLabel(new ImageIcon(img), SwingConstants.CENTER);

        companyName = new JLabel("BLUEDOT", SwingConstants.CENTER);
        companyName.setFont(FONT_COMPANY_NAME);
        companyName.setForeground(COLOR_NAVY_DARK_BG); // Dark text on white background

        logoPanel.add(companyLogo, BorderLayout.NORTH);
        logoPanel.add(companyName, BorderLayout.CENTER);

        // --- Buttons Panel (FlowLayout like original) ---
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonsPanel.setBackground(COLOR_NAVY_DARK_BG); // Match sidebar background

        dashboardButton = createSidebarButton("Dashboard");
        itemsButton = createSidebarButton("Inventory");
        salesButton = createSidebarButton("Sales");
        loginHistoryButton = createSidebarButton("Login History");
        logoutButton = createSidebarButton("Logout");

        buttonsPanel.add(dashboardButton);
        buttonsPanel.add(itemsButton);
        buttonsPanel.add(salesButton);
        buttonsPanel.add(loginHistoryButton);
        buttonsPanel.add(logoutButton);

        sidebar.add(logoPanel, BorderLayout.NORTH);
        sidebar.add(buttonsPanel, BorderLayout.CENTER);
    }

    private JButton createSidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_SIDEBAR_BUTTON);
        btn.setPreferredSize(new Dimension(180, 45));
        btn.setMaximumSize(new Dimension(180, 45));
        btn.setBackground(COLOR_STEEL_BLUE_PANEL); // CORRECTED: Normal background
        btn.setForeground(COLOR_TEXT_LIGHT);       // CORRECTED: Text color
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(COLOR_CADET_BLUE_ACCENT); // Hover
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(COLOR_STEEL_BLUE_PANEL); // CORRECTED: Normal on exit
            }

            public void mousePressed(java.awt. event.MouseEvent evt) {
                btn.setBackground(COLOR_LIGHT_BLUE_HOVER); // Pressed
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (btn.getBounds().contains(evt.getPoint())) {
                    btn.setBackground(COLOR_CADET_BLUE_ACCENT); // Back to hover if mouse still over
                } else {
                    btn.setBackground(COLOR_STEEL_BLUE_PANEL); // CORRECTED: Back to normal if mouse moved out
                }
            }
        });
        return btn;
    }

    private void createDashboardPanel() {
        dashboardPanel = new JPanel(new BorderLayout());
        dashboardPanel.setBackground(COLOR_NAVY_DARK_BG);

        // --- Top Stat Panel ---
        JPanel topPanel = new JPanel(new GridLayout(1, 3, 24, 12));
        topPanel.setBackground(COLOR_NAVY_DARK_BG);
        topPanel.setBorder(BorderFactory.createEmptyBorder(22, 22, 8, 22));

        firearmLabel = new JLabel("...");
        ammunitionLabel = new JLabel("...");
        accessoriesLabel = new JLabel("...");

        topPanel.add(createStatPanel("TOTAL FIREARMS", firearmLabel, COLOR_ACCENT_FIREARM));
        topPanel.add(createStatPanel("TOTAL AMMUNITION", ammunitionLabel, COLOR_ACCENT_AMMO));
        topPanel.add(createStatPanel("TOTAL ACCESSORIES", accessoriesLabel, COLOR_ACCENT_ACCESSORY));

        dashboardPanel.add(topPanel, BorderLayout.NORTH);

        // --- Content Panel (holds top selling and charts wrapper) ---
        contentPanel = new JPanel(new BorderLayout(0, 15));
        contentPanel.setBackground(COLOR_STEEL_BLUE_PANEL); // CORRECTED: Main content area background
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 22, 22, 22));

        // --- Top Selling Panel (Container for the JTable) ---
        topSellingPanel = new JPanel(new BorderLayout());
        topSellingPanel.setBackground(COLOR_STEEL_BLUE_PANEL); // CORRECTED: Match content panel
        TitledBorder topSellingTitle = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_CADET_BLUE_ACCENT, 1),
                " Top Selling Items ",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                FONT_LABEL,
                COLOR_TEXT_LIGHT
        );
        topSellingPanel.setBorder(BorderFactory.createCompoundBorder(
                topSellingTitle,
                new EmptyBorder(5, 5, 5, 5)
        ));
        topSellingPanel.setPreferredSize(new Dimension(0, 200));

        // --- Charts Wrapper (holds filter and chartHolderPanel) ---
        JPanel chartsWrapper = new JPanel(new BorderLayout());
        chartsWrapper.setBackground(COLOR_STEEL_BLUE_PANEL); // CORRECTED: Match content panel
        chartsWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_CADET_BLUE_ACCENT, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        // --- Year Filter Panel ---
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 22, 8));
        filterPanel.setBackground(COLOR_STEEL_BLUE_PANEL); // CORRECTED: Match parent (chartsWrapper)
        JLabel yearLabel = new JLabel("Select Year For Monthly Revenue:");
        yearLabel.setFont(FONT_LABEL);
        yearLabel.setForeground(COLOR_TEXT_LIGHT);

        yearComboBox = new JComboBox<>();
        yearComboBox.setFont(FONT_LABEL);
        yearComboBox.setPreferredSize(new Dimension(110, 28));
        yearComboBox.setBackground(COLOR_INPUT_BG); // Dark input background
        yearComboBox.setForeground(COLOR_TEXT_LIGHT);

        filterPanel.add(yearLabel);
        filterPanel.add(yearComboBox);

        // --- Chart Holder Panel (actual charts go here) ---
        chartHolderPanel = new JPanel(new BorderLayout());
        chartHolderPanel.setBackground(COLOR_STEEL_BLUE_PANEL); // CORRECTED: Match parent

        chartsWrapper.add(filterPanel, BorderLayout.NORTH);
        chartsWrapper.add(chartHolderPanel, BorderLayout.CENTER);

        contentPanel.add(topSellingPanel, BorderLayout.NORTH);
        contentPanel.add(chartsWrapper, BorderLayout.CENTER);

        dashboardPanel.add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createStatPanel(String title, JLabel valueLabel, Color accentColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(230, 90));
        panel.setMaximumSize(new Dimension(230, 90));
        panel.setMinimumSize(new Dimension(180, 80));
        panel.setBackground(accentColor);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(6, 8, 6, 8),
                BorderFactory.createLineBorder(COLOR_NAVY_DARK_BG, 2)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(FONT_LABEL);
        titleLabel.setForeground(Color.WHITE);

        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setFont(FONT_VALUE);
        valueLabel.setForeground(Color.WHITE);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }
}
