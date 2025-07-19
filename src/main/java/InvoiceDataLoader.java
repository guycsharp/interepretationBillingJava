import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.Date;

public class InvoiceDataLoader {

    public static void loadInvoiceData(String company, Date fromDate, Date toDate,
                                       boolean ignoreDate, boolean ignorePaid,
                                       DefaultTableModel model) {
        model.setRowCount(0);

        String rateSql = "SELECT client_rate, client_rate_per_day, idclient_main, client_address FROM client_main WHERE client_name = ?";
        String maxBillSql = "SELECT MAX(bill_no) as max_bill FROM bill_main";
        StringBuilder billSqlBuilder = new StringBuilder(
                "SELECT service_rendered, UnitDay, duration_in_minutes, date_worked, language FROM bill_main WHERE 1=1"
        );

        if (!"All".equals(company)) {
            billSqlBuilder.append(" AND client_id = ?");
        }
        if (!ignoreDate) {
            billSqlBuilder.append(" AND date_worked >= ? AND date_worked <= ?");
        }
        if (!ignorePaid) {
            billSqlBuilder.append(" AND paid != 1");
        }

        String billSql = billSqlBuilder.toString();

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
                InvoiceApp.clientAdd = rs.getString("client_address");
            }

            try (PreparedStatement psMaxBill = conn.prepareStatement(maxBillSql);
                 ResultSet rsMax = psMaxBill.executeQuery()) {

                InvoiceApp.bill_no = rsMax.next() ? rsMax.getDouble("max_bill") : 1;
            }

            try (PreparedStatement psBill = conn.prepareStatement(billSql)) {
                int paramIndex = 1;
                if (!"All".equals(company)) {
                    psBill.setInt(paramIndex++, clientId);
                }
                if (!ignoreDate) {
                    psBill.setDate(paramIndex++, new java.sql.Date(fromDate.getTime()));
                    psBill.setDate(paramIndex++, new java.sql.Date(toDate.getTime()));
                }

                try (ResultSet rs2 = psBill.executeQuery()) {
                    boolean anyRows = false;

                    while (rs2.next()) {
                        anyRows = true;
                        String service = rs2.getString("service_rendered");
                        int unitDay = rs2.getInt("UnitDay");
                        double qty = (unitDay == 1) ? 1 : rs2.getDouble("duration_in_minutes");
                        double tarif = (unitDay == 1) ? ratePerDay : rate;
                        double total = tarif * qty;
                        InvoiceApp.date_worked = rs2.getString("date_worked");
                        InvoiceApp.languageInterpret = rs2.getString("language");

                        model.addRow(new Object[]{service, tarif, qty, total, InvoiceApp.date_worked, InvoiceApp.languageInterpret});
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

