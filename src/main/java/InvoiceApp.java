// File: src/main/java/InvoiceApp.java
// Ensure this class is in the default package (no "package" line), or adjust to match your project's package structure.

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

// Import DataLoader (assumed in default package) and InputPanel (in ui package)
// Adjust these imports if your classes reside in different packages
import ui.InputPanel;
import mysqlConnection.MySQLConnector;

/**
 * InvoiceApp is the main entry point of the billing application.
 * It sets up the GUI, handles database connection, and delegates
 * data loading to DataLoader and data entry to InputPanel.
 */
public class InvoiceApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Establish a connection to the database
                Connection conn = MySQLConnector.getConnection();
                // Build and show the GUI
                createAndShowGUI(conn);
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Unable to start application: " + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void createAndShowGUI(Connection conn) {
        // Main window
        JFrame frame = new JFrame("Billing Software");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // === 1. Filter panel (for billing tab) ===
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JComboBox<String> companyComboBox = new JComboBox<>(new String[]{"All", "Company A", "Company B"});
        JSpinner fromDateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner toDateSpinner = new JSpinner(new SpinnerDateModel());
        JCheckBox ignoreDateCheckbox = new JCheckBox("Ignore Date");
        JCheckBox ignorePaidCheckbox = new JCheckBox("Ignore Paid");
        JButton loadButton = new JButton("Load Data");

        filterPanel.add(new JLabel("Company:"));
        filterPanel.add(companyComboBox);
        filterPanel.add(new JLabel("From:"));
        filterPanel.add(fromDateSpinner);
        filterPanel.add(new JLabel("To:"));
        filterPanel.add(toDateSpinner);
        filterPanel.add(ignoreDateCheckbox);
        filterPanel.add(ignorePaidCheckbox);
        filterPanel.add(loadButton);

        // === 2. Table model & table for billing results ===
        String[] columns = {"Service", "Rate", "Qty", "Total", "Date", "Language"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);

        // === 3. Input data panel ===
        JPanel inputDataPanel = new InputPanel();

        // === 4. Load button action invokes DataLoader ===
        loadButton.addActionListener(e -> DataLoader.loadData(
                model,
                companyComboBox,
                fromDateSpinner,
                toDateSpinner,
                ignoreDateCheckbox,
                ignorePaidCheckbox
        ));

        // === 5. Tabbed pane setup ===
        JTabbedPane tabs = new JTabbedPane();

        // Billing tab layout
        JPanel billingPanel = new JPanel(new BorderLayout(10, 10));
        billingPanel.add(filterPanel, BorderLayout.NORTH);
        billingPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        tabs.addTab("Billing", billingPanel);

        // Input Data tab
        tabs.addTab("Input Data", inputDataPanel);

        // === 6. Finalize frame ===
        frame.add(tabs, BorderLayout.CENTER);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}