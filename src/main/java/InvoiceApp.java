// InvoiceApp.java
// GUI-based billing tool: selects company, picks date range,
// loads invoice rows from DB, manually adds rows, and exports to PDF via PDFCreator.

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InvoiceApp {
    // Public components accessed by PDFCreator
    public static JTable table;
    public static DefaultTableModel model;
    public static JTextField prestationField, tarifField, qtyField;
    public static JComboBox<String> companyComboBox;
    public static JSpinner fromDateSpinner, toDateSpinner;
    public static double bill_no;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(InvoiceApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Billing Software");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        // 🔝 Top panel: company selector and date range
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.add(new JLabel("Company:"));
        companyComboBox = createCompanyComboBox();
        filterPanel.add(companyComboBox);

        filterPanel.add(new JLabel("From:"));
        fromDateSpinner = makeDateSpinner();
        filterPanel.add(fromDateSpinner);

        filterPanel.add(new JLabel("To:"));
        toDateSpinner = makeDateSpinner();
        filterPanel.add(toDateSpinner);

        frame.add(filterPanel, BorderLayout.NORTH);

        // 📋 Center: invoice table
        model = new DefaultTableModel(new Object[]{"Prestation", "Tarif (€)", "Quantité", "Total (€)"}, 0);
        table = new JTable(model);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // 🔘 Bottom panel: fields and buttons
        prestationField = new JTextField();
        tarifField = new JTextField();
        qtyField = new JTextField();

        JPanel inputPanel = new JPanel(new GridLayout(2, 5, 10, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        inputPanel.add(new JLabel("Prestation"));
        inputPanel.add(prestationField);
        inputPanel.add(new JLabel("Tarif (€)"));
        inputPanel.add(tarifField);
        inputPanel.add(new JLabel("Quantité"));
        inputPanel.add(qtyField);

        JButton addButton = new JButton("Add Row");
        JButton loadButton = new JButton("Load from DB");
        JButton exportButton = new JButton("Export to PDF");

        inputPanel.add(addButton);
        filterPanel.add(loadButton);
        inputPanel.add(exportButton);
        inputPanel.add(new JLabel()); // filler

        frame.add(inputPanel, BorderLayout.SOUTH);

        // ➕ Add row manually
        addButton.addActionListener(e -> {
            String service = prestationField.getText().trim();
            String tarifText = tarifField.getText().trim();
            String qtyText = qtyField.getText().trim();
            if (service.isEmpty() || tarifText.isEmpty() || qtyText.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please fill all fields.");
                return;
            }
            try {
                double tarif = Double.parseDouble(tarifText);
                int qty = Integer.parseInt(qtyText);
                model.addRow(new Object[]{service, tarif, qty, tarif * qty});
                prestationField.setText("");
                tarifField.setText("");
                qtyField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Tarif and Quantité must be numeric.");
            }
        });

        // 🔄 Load rows from database
        loadButton.addActionListener(e -> loadData());

        // 📤 Export to PDF using separate class
        exportButton.addActionListener(e -> PDFCreator.exportPDF(frame,bill_no+1));

        frame.setVisible(true);
    }

    /**
     * Populates company dropdown from client_main table.
     */
    private static JComboBox<String> createCompanyComboBox() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT client_name FROM client_main WHERE soft_delete IS NULL OR soft_delete = 0 ORDER BY client_name";

        try (Connection conn = MySQLConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                names.add(rs.getString("client_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            names.add("<< error loading companies >>");
        }

        if (names.isEmpty()) names.add("<< no companies found >>");

        return new JComboBox<>(names.toArray(new String[0]));
    }

    /**
     * Creates a date spinner using yyyy-MM-dd format.
     */
    private static JSpinner makeDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        return spinner;
    }

    /**
     * Loads rows from bill_main using selected client and date range.
     */
    private static void loadData() {
        model.setRowCount(0);

        String company = (String) companyComboBox.getSelectedItem();
        Date fromDate = ((SpinnerDateModel) fromDateSpinner.getModel()).getDate();
        Date toDate = ((SpinnerDateModel) toDateSpinner.getModel()).getDate();

        String rateSql = "SELECT client_rate, client_rate_per_day, idclient_main FROM client_main WHERE client_name = ?";
        String billSql = "SELECT service_rendered, UnitDay, workedDayOrHours, idbill_main, bill_no FROM bill_main WHERE client_id = ? AND date_worked >= ? AND date_worked <= ?";

        try (Connection conn = MySQLConnector.getConnection();
             PreparedStatement psRate = conn.prepareStatement(rateSql)) {

            psRate.setString(1, company);
            double rate, ratePerDay;
            int clientId;


            try (ResultSet rs = psRate.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(null, "No rate info for " + company);
                    return;
                }
                rate = rs.getDouble("client_rate");
                ratePerDay = rs.getDouble("client_rate_per_day");
                clientId = rs.getInt("idclient_main");

            }

            try (PreparedStatement psBill = conn.prepareStatement(billSql)) {
                psBill.setInt(1, clientId);
                psBill.setDate(2, new java.sql.Date(fromDate.getTime()));
                psBill.setDate(3, new java.sql.Date(toDate.getTime()));

                try (ResultSet rs2 = psBill.executeQuery()) {
                    boolean anyRows = false;

                    while (rs2.next()) {
                        anyRows = true;
                        String service = rs2.getString("service_rendered");
                        int unitDay = rs2.getInt("UnitDay");
                        int qty = rs2.getInt("workedDayOrHours");
                        double tarif = (unitDay == 1) ? ratePerDay : rate;
                        double total = tarif * qty;
                        bill_no = rs2.getDouble("bill_no");
                        model.addRow(new Object[]{service, tarif, qty, total});
                    }

                    if (!anyRows) {
                        JOptionPane.showMessageDialog(null,
                                "No billing entries found for this company in the selected date range.");
                    }
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Database error: " + ex.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
