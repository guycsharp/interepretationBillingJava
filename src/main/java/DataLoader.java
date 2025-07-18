
import javax.swing.*;

import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.Date;

/**
 * DataLoader handles loading billing rows from the database.
 * We moved the logic here so InvoiceApp stays focused on UI.
 */
public class DataLoader {
    /**
     * Populates the table model with rows from bill_main,
     * using the UI selections passed in.
     */
    public static void loadData(
            DefaultTableModel model,
            JComboBox<String> companyComboBox,
            JSpinner fromDateSpinner,
            JSpinner toDateSpinner,
            JCheckBox ignoreDateCheckbox,
            JCheckBox ignorePaidCheckbox
    ) {
        // 1. Clear old rows
        model.setRowCount(0);

        // 2. Read filters
        String company = (String) companyComboBox.getSelectedItem();
        Date fromDate = ((SpinnerDateModel) fromDateSpinner.getModel()).getDate();
        Date toDate = ((SpinnerDateModel) toDateSpinner.getModel()).getDate();

        // 3. Prepare SQL fragments
        String rateSql = "SELECT client_rate, client_rate_per_day, idclient_main, client_address"
                + " FROM client_main WHERE client_name = ?";
        StringBuilder billSql = new StringBuilder(
                "SELECT service_rendered, UnitDay, workedDayOrHours, date_worked, language"
                        + " FROM bill_main WHERE 1=1"
        );
        if (!"All".equals(company)) billSql.append(" AND client_id = ?");
        if (!ignoreDateCheckbox.isSelected()) billSql.append(" AND date_worked >= ? AND date_worked <= ?");
        if (!ignorePaidCheckbox.isSelected()) billSql.append(" AND paid != 1");

        // 4. Execute queries and fill model
        try (Connection conn = MySQLConnector.getConnection()) {
            // 4a. Lookup client rate and ID
            int clientId;
            double rate, ratePerDay;
            String clientAddress;
            try (PreparedStatement ps = conn.prepareStatement(rateSql)) {
                ps.setString(1, company);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(null, "No rate info for " + company);
                        return;
                    }
                    rate = rs.getDouble("client_rate");
                    ratePerDay = rs.getDouble("client_rate_per_day");
                    clientId = rs.getInt("idclient_main");
                    clientAddress = rs.getString("client_address");
                    InvoiceApp.clientAdd = clientAddress;
                }
            }

            // 4b. Find max bill_no
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT MAX(bill_no) AS max_bill FROM bill_main")) {
                InvoiceApp.bill_no = rs.next() ? rs.getDouble("max_bill") : 0;
            }

            // 4c. Load billing rows
            try (PreparedStatement ps = conn.prepareStatement(billSql.toString())) {
                int idx = 1;
                if (!"All".equals(company)) ps.setInt(idx++, clientId);
                if (!ignoreDateCheckbox.isSelected()) {
                    ps.setDate(idx++, new java.sql.Date(fromDate.getTime()));
                    ps.setDate(idx++, new java.sql.Date(toDate.getTime()));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    boolean any = false;
                    while (rs.next()) {
                        any = true;
                        String service = rs.getString("service_rendered");
                        int unitDay = rs.getInt("UnitDay");
                        int qty = (unitDay == 1) ? 1 : rs.getInt("workedDayOrHours");
                        double tarif = (unitDay == 1) ? ratePerDay : rate;
                        double total = tarif * qty;
                        String dateWorked = rs.getString("date_worked").substring(0,10);
                        String language = rs.getString("language");
                        model.addRow(new Object[]{service, tarif, qty, total, dateWorked, language});
                    }
                    if (!any) JOptionPane.showMessageDialog(null, "No entries found.");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "DB error: " + ex.getMessage());
        }
    }
}
