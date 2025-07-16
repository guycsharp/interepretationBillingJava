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
    public static String clientAdd, languageInterpret, date_worked;
    private static JCheckBox ignoreDateCheckbox;
    private static JCheckBox ignorePaidCheckbox;
    private static final String myaddress = ConfigLoader.get("db.address");
    private static final int width = 800, height = 600;


    public static void main(String[] args) {
        SwingUtilities.invokeLater(InvoiceApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Billing Software");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
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

        ignoreDateCheckbox = new JCheckBox("Ignore Date");
        filterPanel.add(ignoreDateCheckbox);

        ignorePaidCheckbox = new JCheckBox("Ignore Paid");
        filterPanel.add(ignorePaidCheckbox);


        frame.add(filterPanel, BorderLayout.NORTH);

        // 📋 Center: invoice table
        model = new DefaultTableModel(new Object[]{"Prestation", "Tarif (€)", "Quantité", "Total (€)", "Date Worked", "Language"}, 0);
        table = new JTable(model);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // 🔘 Bottom panel: fields and buttons
        prestationField = new JTextField();
        tarifField = new JTextField();
        qtyField = new JTextField();

        // JPanel inputPanel = new JPanel(new GridLayout(2, 5, 10, 5));
        JPanel inputPanel = new JPanel(new GridLayout(1, 6, 10, 5));

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
        exportButton.addActionListener(e -> PDFCreator.exportPDF(frame, bill_no + 1, clientAdd, myaddress));

        frame.setVisible(true);
    }

    /**
     * Populates company dropdown from client_main table.
     */
    private static JComboBox<String> createCompanyComboBox() {
        List<String> names = new ArrayList<>();
        // names.add("All");
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
        // 🧹 Step 1: Clear any existing rows in the table before loading new data
        model.setRowCount(0);

        // 🏢 Step 2: Get selected company and date range from UI
        String company = (String) companyComboBox.getSelectedItem();
        Date fromDate = ((SpinnerDateModel) fromDateSpinner.getModel()).getDate();
        Date toDate = ((SpinnerDateModel) toDateSpinner.getModel()).getDate();

        // 🧾 Step 3: SQL queries
        String rateSql = "SELECT client_rate, client_rate_per_day, idclient_main, client_address FROM client_main WHERE client_name = ?";
        String maxBillSql = "SELECT MAX(bill_no) as max_bill FROM bill_main ";
        // String billSql    = "SELECT service_rendered, UnitDay, workedDayOrHours FROM bill_main WHERE client_id = ? AND date_worked >= ? AND date_worked <= ?";
        StringBuilder billSqlBuilder = new StringBuilder(
                "SELECT service_rendered, UnitDay, workedDayOrHours, date_worked, language FROM bill_main WHERE 1=1"
        );

        if (!"All".equals(company)) {
            billSqlBuilder.append(" AND client_id = ?");
        }
        if (!ignoreDateCheckbox.isSelected()) {
            billSqlBuilder.append(" AND date_worked >= ? AND date_worked <= ?");
        }

        if (!ignorePaidCheckbox.isSelected()) {
            billSqlBuilder.append(" AND paid != 1"); // adjust logic to match your schema
        }

        String billSql = billSqlBuilder.toString();

        try (
                Connection conn = MySQLConnector.getConnection();

                // 🌐 Query client rates and ID
                PreparedStatement psRate = conn.prepareStatement(rateSql)
        ) {
            // Step 4: Bind company name to client lookup query
            psRate.setString(1, company);

            double rate;
            double ratePerDay;
            int clientId;


            // Step 5: Execute rate query and extract values
            try (ResultSet rs = psRate.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(null, "No rate info for " + company);
                    return; // Stop if company not found
                }
                rate = rs.getDouble("client_rate");
                ratePerDay = rs.getDouble("client_rate_per_day");
                clientId = rs.getInt("idclient_main");
                clientAdd = rs.getString("client_address");
            }

            // Step 6: Get latest bill number for this client
            try (
                    PreparedStatement psMaxBill = conn.prepareStatement(maxBillSql)
            ) {

                try (ResultSet rsMax = psMaxBill.executeQuery()) {
                    if (rsMax.next()) {
                        bill_no = rsMax.getDouble("max_bill"); // If no rows exist, will default to 0
                    } else {
                        bill_no = 1;
                    }
                }
            }

            // Step 7: Load billing entries between selected dates
            try (
                    PreparedStatement psBill = conn.prepareStatement(billSql)
            ) {
                psBill.setInt(1, clientId);
                if (!ignoreDateCheckbox.isSelected()) {
                    psBill.setDate(2, new java.sql.Date(fromDate.getTime())); // Start date
                    psBill.setDate(3, new java.sql.Date(toDate.getTime()));   // End date
                }

                try (ResultSet rs2 = psBill.executeQuery()) {
                    boolean anyRows = false;

                    while (rs2.next()) {
                        anyRows = true;

                        String service = rs2.getString("service_rendered"); //0
                        int unitDay = rs2.getInt("UnitDay");              // 1 = day rate, 0 = hour rate
                        int qty = (unitDay == 1) ? 1 : rs2.getInt("workedDayOrHours"); //2
                        double tarif = (unitDay == 1) ? ratePerDay : rate; // Choose rate based on unit 3
                        double total = tarif * qty; //4
                        date_worked = rs2.getString("date_worked").substring(0,11); //5
                        languageInterpret = rs2.getString("language"); //6

                        // Add row to table
                        model.addRow(new Object[]{service, tarif, qty, total, date_worked, languageInterpret});
                    }

                    // 🗨 If no matching billing data, notify user
                    if (!anyRows) {
                        JOptionPane.showMessageDialog(null,
                                "No billing entries found for this company in the selected date range. ");
                        // + psBill.toString())
                        ;
                    }
                }
            }

        } catch (SQLException ex) {
            // 💥 Handle database errors gracefully
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Database error: " + ex.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE
            );
        }
    }

}
