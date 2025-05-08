import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSpinnerUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class POSDesign {

    // --- Color Palette ---
    private static final Color COLOR_NAVY_DARK_BG = new Color(0x00, 0x1F, 0x3F);
    private static final Color COLOR_STEEL_BLUE_PANEL = new Color(0x3A, 0x6D, 0x8C);
    private static final Color COLOR_CADET_BLUE_ACCENT = new Color(0x6A, 0x9A, 0xB0);
    private static final Color COLOR_LIGHT_BLUE_HOVER = new Color(0x8A, 0xB8, 0xC8);
    private static final Color COLOR_TEXT_LIGHT = new Color(0xE0, 0xE7, 0xEF);
    private static final Color COLOR_BORDER_SUBTLE = new Color(0x2C, 0x50, 0x6F);
    private static final Color COLOR_INPUT_BG = new Color(0x10, 0x30, 0x50);
    private static final Color COLOR_ACCENT_WARN = new Color(0xD9, 0x53, 0x4F);
    private static final Color COLOR_ACCENT_SUCCESS = new Color(0x5C, 0xB8, 0x5C);

    // --- Fonts ---
    private static final Font FONT_PRIMARY_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_PRIMARY_PLAIN = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_TABLE_CELL = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_RECEIPT = new Font("Consolas", Font.PLAIN, 12);

    // --- Public UI fields ---
    public JPanel mainPanel;
    public JTextField txtProductCode, txtProductName, txtCategory, txtPrice, txtTotal, txtPay, txtBalance, txtItemTotal;
    public JSpinner spinnerQty;
    public JTable table, historyTable;
    public DefaultTableModel tableModel, historyTableModel;
    public JTextArea receiptArea;
    public JButton btnAdd, btnFinalize, btnPrint, btnClear, btnDeleteRow, btnShowHistory;
    public JPopupMenu searchPopupMenu;
    public JList<String> searchSuggestionList;
    public DefaultListModel<String> searchListModel;
    public JSpinner dateFromSpinner, dateToSpinner;

    public POSDesign() {
        mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(COLOR_NAVY_DARK_BG);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- Top Input Panel ---
        JPanel inputPanel = new JPanel(new GridLayout(2, 7, 12, 12));
        inputPanel.setBackground(COLOR_STEEL_BLUE_PANEL);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                new EmptyBorder(15, 15, 15, 15))
        );
        txtProductCode = createStyledTextField();
        txtProductName = createStyledTextField();
        txtCategory = createStyledTextField();
        txtCategory.setEditable(false);
        spinnerQty = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        styleStyledSpinner(spinnerQty);
        txtPrice = createStyledTextField();
        txtPrice.setEditable(false);
        txtItemTotal = createStyledTextField();
        txtItemTotal.setEditable(false);
        btnAdd = createStyledButton("Add Item", COLOR_ACCENT_SUCCESS, new Dimension(120, 40));
        inputPanel.add(createStyledLabel("Product Code:"));
        inputPanel.add(createStyledLabel("Product Name:"));
        inputPanel.add(createStyledLabel("Category:"));
        inputPanel.add(createStyledLabel("Quantity:"));
        inputPanel.add(createStyledLabel("Price:"));
        inputPanel.add(createStyledLabel("Item Total:"));
        inputPanel.add(new JLabel(""));
        inputPanel.add(txtProductCode);
        inputPanel.add(txtProductName);
        inputPanel.add(txtCategory);
        inputPanel.add(spinnerQty);
        inputPanel.add(txtPrice);
        inputPanel.add(txtItemTotal);
        JPanel addBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        addBtnPanel.setOpaque(false);
        addBtnPanel.add(btnAdd);
        inputPanel.add(addBtnPanel);

        // --- Table for Orders ---
        tableModel = new DefaultTableModel(new Object[]{"Product Code", "Product Name", "Category", "Qty", "Price", "Total"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = styleStyledTable(new JTable(tableModel));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setRowHeight(38);
        JScrollPane tableScroll = new JScrollPane(table);
        styleStyledScrollPane(tableScroll);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(COLOR_STEEL_BLUE_PANEL);
        centerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                new EmptyBorder(10, 10, 10, 10))
        );
        JLabel lblOrders = new JLabel("Current Order Items", JLabel.CENTER);
        lblOrders.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblOrders.setForeground(COLOR_TEXT_LIGHT);
        lblOrders.setBorder(new EmptyBorder(5, 5, 10, 5));
        centerPanel.add(lblOrders, BorderLayout.NORTH);
        centerPanel.add(tableScroll, BorderLayout.CENTER);

        // --- RIGHT SECTION: Summary & Actions (modern grid) + Receipt Preview (fills) ---
        int summaryPanelWidth = 480; // wider panel
        int summaryFieldWidth = 140, summaryFieldHeight = 32;
        int buttonWidth = 200, buttonHeight = 38;

        JPanel rightSectionPanel = new JPanel(new BorderLayout(10, 10));
        rightSectionPanel.setOpaque(false);

        // --- Summary & Actions Panel ---
        JPanel summaryAndActionsPanel = new JPanel();
        summaryAndActionsPanel.setLayout(new BorderLayout());
        summaryAndActionsPanel.setBackground(COLOR_STEEL_BLUE_PANEL);
        summaryAndActionsPanel.setPreferredSize(new Dimension(summaryPanelWidth, 205));
        summaryAndActionsPanel.setMaximumSize(new Dimension(summaryPanelWidth, 205));
        summaryAndActionsPanel.setBorder(createStyledPanelBorder("Summary & Actions"));

        // --- Actions Grid (2x3 grid) ---
        JPanel actionsGrid = new JPanel(new GridLayout(2, 3, 12, 10));
        actionsGrid.setOpaque(false);

        btnFinalize = createStyledButton("Finalize Sale", COLOR_CADET_BLUE_ACCENT, new Dimension(buttonWidth, buttonHeight));
        btnPrint = createStyledButton("Print Receipt", COLOR_CADET_BLUE_ACCENT, new Dimension(buttonWidth, buttonHeight));
        btnShowHistory = createStyledButton("History", COLOR_CADET_BLUE_ACCENT, new Dimension(buttonWidth, buttonHeight));
        btnClear = createStyledButton("Clear Order", COLOR_ACCENT_WARN, new Dimension(buttonWidth, buttonHeight));
        btnDeleteRow = createStyledButton("Remove Item", COLOR_ACCENT_WARN, new Dimension(buttonWidth, buttonHeight));

        actionsGrid.add(btnFinalize);
        actionsGrid.add(btnPrint);
        actionsGrid.add(btnShowHistory);
        actionsGrid.add(btnClear);
        actionsGrid.add(btnDeleteRow);
        actionsGrid.add(new JLabel()); // empty cell for grid symmetry

        summaryAndActionsPanel.add(actionsGrid, BorderLayout.NORTH);

        // --- Summary Fields: horizontal row with fields (labels above) ---
        JPanel summaryFieldsPanel = new JPanel();
        summaryFieldsPanel.setOpaque(false);
        summaryFieldsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 6, 0, 6);

        txtTotal = createSummaryFieldWithLabel("Grand Total", summaryFieldWidth, summaryFieldHeight);
        txtTotal.setEditable(false);
        txtPay = createSummaryFieldWithLabel("Payment", summaryFieldWidth, summaryFieldHeight);
        txtBalance = createSummaryFieldWithLabel("Change", summaryFieldWidth, summaryFieldHeight);
        txtBalance.setEditable(false);

        gbc.gridx = 0;
        summaryFieldsPanel.add(wrapSummaryField(txtTotal, "Grand Total"), gbc);
        gbc.gridx = 1;
        summaryFieldsPanel.add(wrapSummaryField(txtPay, "Payment"), gbc);
        gbc.gridx = 2;
        summaryFieldsPanel.add(wrapSummaryField(txtBalance, "Change"), gbc);

        summaryAndActionsPanel.add(summaryFieldsPanel, BorderLayout.SOUTH);

        // --- Receipt Preview Panel (fills remaining height) ---
        receiptArea = new JTextArea();
        receiptArea.setEditable(false);
        receiptArea.setFont(FONT_RECEIPT);
        receiptArea.setBackground(COLOR_INPUT_BG);
        receiptArea.setForeground(COLOR_TEXT_LIGHT);
        receiptArea.setCaretColor(COLOR_CADET_BLUE_ACCENT);
        receiptArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                new EmptyBorder(10, 10, 10, 10))
        );
        JScrollPane receiptScroll = new JScrollPane(receiptArea);
        styleStyledScrollPane(receiptScroll);
        receiptScroll.setBorder(createStyledPanelBorder("Receipt Preview"));
        receiptScroll.setMinimumSize(new Dimension(summaryPanelWidth, 200));
        receiptScroll.setPreferredSize(new Dimension(summaryPanelWidth, 400));
        receiptScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        rightSectionPanel.add(summaryAndActionsPanel, BorderLayout.NORTH);
        rightSectionPanel.add(receiptScroll, BorderLayout.CENTER);

        // --- Suggestion List Popup for Product Name ---
        searchListModel = new DefaultListModel<>();
        searchSuggestionList = new JList<>(searchListModel);
        searchSuggestionList.setFont(FONT_PRIMARY_PLAIN);
        searchSuggestionList.setBackground(COLOR_INPUT_BG);
        searchSuggestionList.setForeground(COLOR_TEXT_LIGHT);
        searchSuggestionList.setSelectionBackground(COLOR_CADET_BLUE_ACCENT);
        searchSuggestionList.setSelectionForeground(COLOR_NAVY_DARK_BG);
        searchSuggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchSuggestionList.setVisibleRowCount(6);
        JScrollPane listScrollPane = new JScrollPane(searchSuggestionList);
        styleStyledScrollPane(listScrollPane);
        listScrollPane.setPreferredSize(new Dimension(320, 190));
        searchPopupMenu = new JPopupMenu();
        searchPopupMenu.setBackground(COLOR_INPUT_BG);
        searchPopupMenu.setBorder(BorderFactory.createLineBorder(COLOR_CADET_BLUE_ACCENT, 1));
        searchPopupMenu.add(listScrollPane);

        // --- History Table (for popup dialog) ---
        historyTableModel = new DefaultTableModel(
                new Object[]{"Transaction ID", "Product Code", "Product Name", "Category", "Qty", "Unit Price", "Total", "Date"}, 0
        ) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        historyTable = styleStyledTable(new JTable(historyTableModel));
        historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        historyTable.setRowHeight(35);

        int[] historyWidths = {120, 120, 220, 130, 80, 120, 120, 150};
        for (int i = 0; i < historyWidths.length; i++) {
            historyTable.getColumnModel().getColumn(i).setPreferredWidth(historyWidths[i]);
            historyTable.getColumnModel().getColumn(i).setResizable(true);
        }

        dateFromSpinner = new JSpinner(new SpinnerDateModel());
        styleStyledSpinner(dateFromSpinner);
        JSpinner.DateEditor fromEditor = new JSpinner.DateEditor(dateFromSpinner, "MM/dd/yyyy");
        fromEditor.getTextField().setForeground(COLOR_TEXT_LIGHT);
        fromEditor.getTextField().setBackground(COLOR_INPUT_BG);
        fromEditor.getTextField().setCaretColor(COLOR_CADET_BLUE_ACCENT);
        dateFromSpinner.setEditor(fromEditor);

        dateToSpinner = new JSpinner(new SpinnerDateModel());
        styleStyledSpinner(dateToSpinner);
        JSpinner.DateEditor toEditor = new JSpinner.DateEditor(dateToSpinner, "MM/dd/yyyy");
        toEditor.getTextField().setForeground(COLOR_TEXT_LIGHT);
        toEditor.getTextField().setBackground(COLOR_INPUT_BG);
        toEditor.getTextField().setCaretColor(COLOR_CADET_BLUE_ACCENT);
        dateToSpinner.setEditor(toEditor);

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(rightSectionPanel, BorderLayout.EAST);
    }

    // --- Summary field creator (label on top, field below) ---
    private JTextField createSummaryFieldWithLabel(String label, int width, int height) {
        JTextField tf = createStyledTextField();
        tf.setHorizontalAlignment(JTextField.RIGHT);
        tf.setFont(FONT_PRIMARY_BOLD);
        tf.setMaximumSize(new Dimension(width, height));
        tf.setPreferredSize(new Dimension(width, height));
        tf.setMinimumSize(new Dimension(width, height));
        return tf;
    }

    private JPanel wrapSummaryField(JTextField tf, String labelText) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(COLOR_TEXT_LIGHT);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        tf.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(2));
        panel.add(tf);
        return panel;
    }

    private JTextField createStyledTextField() {
        JTextField textField = new JTextField();
        textField.setFont(FONT_PRIMARY_PLAIN);
        textField.setBackground(COLOR_INPUT_BG);
        textField.setForeground(COLOR_TEXT_LIGHT);
        textField.setCaretColor(COLOR_CADET_BLUE_ACCENT);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                new EmptyBorder(8, 10, 8, 10)));
        return textField;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_LABEL);
        label.setForeground(COLOR_TEXT_LIGHT);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    private JButton createStyledButton(String text, Color bgColor, Dimension preferredSize) {
        JButton button = new JButton(text);
        button.setFont(FONT_PRIMARY_BOLD);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 15, 10, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(preferredSize);
        button.setMinimumSize(preferredSize);

        Color hoverColor = (bgColor == COLOR_ACCENT_WARN || bgColor == COLOR_ACCENT_SUCCESS) ? bgColor.brighter() : COLOR_LIGHT_BLUE_HOVER;
        Color pressedColor = bgColor.darker();

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                button.setBackground(pressedColor);
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (button.getBounds().contains(evt.getPoint())) button.setBackground(hoverColor);
                else button.setBackground(bgColor);
            }
        });
        return button;
    }

    private void styleStyledSpinner(JSpinner spinner) {
        spinner.setFont(FONT_PRIMARY_PLAIN);
        spinner.setBackground(COLOR_INPUT_BG);
        spinner.setForeground(COLOR_TEXT_LIGHT);
        spinner.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1));

        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setFont(FONT_PRIMARY_PLAIN);
            textField.setBackground(COLOR_INPUT_BG);
            textField.setForeground(COLOR_TEXT_LIGHT);
            textField.setCaretColor(COLOR_CADET_BLUE_ACCENT);
            textField.setBorder(new EmptyBorder(5, 8, 5, 8));
        }

        spinner.setUI(new BasicSpinnerUI() {
            protected Component createNextButton() {
                JButton btn = (JButton) super.createNextButton();
                btn.setBackground(COLOR_STEEL_BLUE_PANEL);
                btn.setForeground(COLOR_TEXT_LIGHT);
                btn.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE));
                btn.setText("▲");
                return btn;
            }

            protected Component createPreviousButton() {
                JButton btn = (JButton) super.createPreviousButton();
                btn.setBackground(COLOR_STEEL_BLUE_PANEL);
                btn.setForeground(COLOR_TEXT_LIGHT);
                btn.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE));
                btn.setText("▼");
                return btn;
            }
        });
    }

    private JTable styleStyledTable(JTable jTable) {
        jTable.setFont(FONT_TABLE_CELL);
        jTable.setBackground(COLOR_INPUT_BG);
        jTable.setForeground(COLOR_TEXT_LIGHT);
        jTable.setGridColor(COLOR_BORDER_SUBTLE);
        jTable.setRowHeight(35);
        jTable.setSelectionBackground(COLOR_CADET_BLUE_ACCENT);
        jTable.setSelectionForeground(COLOR_NAVY_DARK_BG);

        JTableHeader header = jTable.getTableHeader();
        header.setFont(FONT_TABLE_HEADER);
        header.setBackground(COLOR_STEEL_BLUE_PANEL);
        header.setForeground(COLOR_TEXT_LIGHT);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER_SUBTLE),
                new EmptyBorder(10, 8, 10, 8)));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent (JTable table, Object value,boolean isSelected,
            boolean hasFocus, int row, int column){
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(isSelected ? COLOR_CADET_BLUE_ACCENT : COLOR_INPUT_BG);
                if (isSelected) {
                    c.setForeground(COLOR_NAVY_DARK_BG);
                } else {
                    c.setForeground(COLOR_TEXT_LIGHT);
                }
                setBorder(new EmptyBorder(5, 8, 5, 8));
                return c;
            }
        } ;
        jTable.setDefaultRenderer(Object.class, renderer);
        return jTable;
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

    private Border createStyledPanelBorder(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                        " " + title + " ",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        FONT_LABEL,
                        COLOR_TEXT_LIGHT
                ),
                new EmptyBorder(10, 10, 10, 10)
        );
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
