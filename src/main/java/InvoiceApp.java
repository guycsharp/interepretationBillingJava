// File: src/main/java/InvoiceApp.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * InvoiceApp is the main entry point of the billing application.
 * It sets up the GUI, handles database connection, and delegates
 * data loading to DataLoader and data entry to InputPanel.
 */
public class InvoiceApp {
    // These static fields are updated by DataLoader when fetching data
    public static String clientAdd;
    public static double bill_no;

    // Database connector utility; adjust connection details there
    public static void main(String[] args) {
        // Ensure UI updates run on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Create and show the GUI, passing a live DB connection
                Connection conn = MySQLConnector.getConnection();
                createAndShowGUI(conn);
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Unable to start application: " + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Builds and displays the main window with two tabs:
     *  - Billing: filter panel + results table
     *  - Input Data: an InputPanel for inserting new rows
     */
    private static void createAndShowGUI(Connection conn) {
        // 1. Main window setup
        JFrame frame = new JFrame("Billing Software");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        // 2. Filter panel (north of Billing tab)
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JComboBox<String> companyComboBox = new JComboBox<>(
                new String[]{"All", "Company A", "Company B"} // populate dynamically as needed
        );
        JSpinner fromDateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner toDateSpinner   = new JSpinner(new SpinnerDateModel());
        JCheckBox ignoreDateCheckbox = new JCheckBox("Ignore Date");
        JCheckBox ignorePaidCheckbox = new JCheckBox("Ignore Paid");
        JButton loadButton = new JButton("Load Data");

        // Add filter controls to panel
        filterPanel.add(new JLabel("Company:"));
        filterPanel.add(companyComboBox);
        filterPanel.add(new JLabel("From:"));
        filterPanel.add(fromDateSpinner);
        filterPanel.add(new JLabel("To:"));
        filterPanel.add(toDateSpinner);
        filterPanel.add(ignoreDateCheckbox);
        filterPanel.add(ignorePaidCheckbox);
        filterPanel.add(loadButton);

        // 3. Table for billing results
        String[] columns = {"Service", "Rate", "Qty", "Total", "Date", "Language"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);

        // 4. Input panel for adding new entries
        //    Defined in ui/InputPanel.java
        JPanel inputDataPanel = new ui.InputPanel();

        // 5. Wire load button to DataLoader
        loadButton.addActionListener(e ->
                DataLoader.loadData(model,
                        companyComboBox,
                        fromDateSpinner,
                        toDateSpinner,
                        ignoreDateCheckbox,
                        ignorePaidCheckbox)
        );

        // 6. Tabbed pane containing Billing view and Input Data view
        JTabbedPane tabs = new JTabbedPane();

        // Billing tab content
        JPanel billingPanel = new JPanel(new BorderLayout(10, 10));
        billingPanel.add(filterPanel, BorderLayout.NORTH);
        billingPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        tabs.addTab("Billing", billingPanel);

        // Input Data tab
        tabs.addTab("Input Data", inputDataPanel);

        // 7. Final window setup
        frame.add(tabs, BorderLayout.CENTER);
        frame.setSize(900, 600);              // Set an appropriate window size
        frame.setLocationRelativeTo(null);    // Center on screen
        frame.setVisible(true);               // Show the window
    }
}