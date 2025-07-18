import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import ui.InputPanel;
import mysqlConnection.MySQLConnector;    // import your connector

/**
 * InvoiceApp is the main entry point of the billing application.
 */
public class InvoiceApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
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

    private static void createAndShowGUI(Connection conn) {
        JFrame frame = new JFrame("Billing Software");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JComboBox<String> companyComboBox = new JComboBox<>(new String[]{"All"});
        JSpinner fromDateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner toDateSpinner   = new JSpinner(new SpinnerDateModel());
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

        // Results table
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Service","Rate","Qty","Total","Date","Language"}, 0
        );
        JTable table = new JTable(model);

        // Input Data tab
        JPanel inputDataPanel = new InputPanel();

        // Wire loader
        loadButton.addActionListener(e -> DataLoader.loadData(
                model, companyComboBox,
                fromDateSpinner, toDateSpinner,
                ignoreDateCheckbox, ignorePaidCheckbox
        ));

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        JPanel billingPanel = new JPanel(new BorderLayout(10,10));
        billingPanel.add(filterPanel, BorderLayout.NORTH);
        billingPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        tabs.addTab("Billing", billingPanel);
        tabs.addTab("Input Data", inputDataPanel);

        frame.add(tabs, BorderLayout.CENTER);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}