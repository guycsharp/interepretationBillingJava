import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InvoiceDataLoader {

    /*
    public static void loadInvoiceData(String company, Date fromDate, Date toDate,
                                       boolean ignoreDate, boolean ignorePaid,
                                       DefaultTableModel model) {
        model.setRowCount(0);

        String rateSql =
                "SELECT r.client_id, c.idclient_main," +
                        "       c.client_name, " +
                        "       c.client_address, " +   // ← comma here
                        "       r.language, " +
                        "       r.rate_per_hour, " +
                        "       r.rate_per_day, " +
                        "       r.offsetBy, " +
                        "       r.weekend, " +
                        "       r.offsetUnit " +        // ← no comma here
                        "FROM rate_main r " +
                        "INNER JOIN client_main c " +
                        "  ON r.client_id = c.idclient_main " +
                        " AND r.language  = c.language " +
                        "WHERE c.client_name = ?";

        String maxBillSql = "SELECT MAX(bill_no) as max_bill FROM bill_main";
        StringBuilder billSqlBuilder = new StringBuilder(
                "SELECT service_rendered, UnitDay, duration_in_minutes, date_worked, language, rate_per_hour, rate_per_day " +
                        "FROM bill_main " +
                        " inner join rate_main " +
                        " WHERE 1=1"
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
                rate = rs.getDouble("rate_per_hour");
                ratePerDay = rs.getDouble("rate_per_day");
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


     */

    public static void loadInvoiceData(String company,
                                       Date fromDate, Date toDate,
                                       boolean ignoreDate, boolean ignorePaid,
                                       DefaultTableModel model) {
        model.setRowCount(0);

        // 1) First we look up the client_id once (still fine to keep this)
        int clientId = -1;
        String findClientSql =
                "SELECT idclient_main, client_address " +
                        "FROM client_main " +
                        "WHERE client_name = ?";
        try (Connection conn = MySQLConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(findClientSql)) {
            ps.setString(1, company);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(null, "Unknown client: " + company);
                    return;
                }
                clientId = rs.getInt("idclient_main");
                InvoiceApp.clientAdd = rs.getString("client_address");
            }

            // 2) Build a single SQL that JOINs bill_main → rate_main
            StringBuilder billNRate = new StringBuilder(
                    "SELECT " +
                            "  b.service_rendered, " +
                            "  b.UnitDay, " +
                            "  b.duration_in_minutes, " +
                            "  b.date_worked, " +
                            "  b.language, " +
                            "  r.rate_per_hour, " +
                            "  r.rate_per_day " +
                            "FROM bill_main b " +
                            "INNER JOIN rate_main r " +
                            "  ON b.client_id = r.client_id " +
                            " AND b.language  = r.language " +
                            "WHERE b.client_id = ?"
            );

            if (!ignoreDate) {
                billNRate.append(" AND b.date_worked >= ? AND b.date_worked <= ?");
            }
            if (!ignorePaid) {
                billNRate.append(" AND b.paid = 0");
            }

            try (PreparedStatement psBill = conn.prepareStatement(billNRate.toString())) {
                int idx = 1;
                psBill.setInt(idx++, clientId);
                if (!ignoreDate) {
                    psBill.setDate(idx++, new java.sql.Date(fromDate.getTime()));
                    psBill.setDate(idx++, new java.sql.Date(toDate  .getTime()));
                }

//                JOptionPane.showMessageDialog(
//                        null,
//                         psBill.toString(),
//                        "SQL Statement",
//                        JOptionPane.INFORMATION_MESSAGE);

                try (ResultSet rs2 = psBill.executeQuery()) {
                    boolean any = false;
                    while (rs2.next()) {
                        any = true;
                        String service = rs2.getString("service_rendered");
                        int unitDay    = rs2.getInt("UnitDay");
                        double mins    = rs2.getDouble("duration_in_minutes");
                        double perHour = rs2.getDouble("rate_per_hour");
                        double perDay  = rs2.getDouble("rate_per_day");

                        // if UnitDay == 1 use perDay, otherwise perHour
                        double qty    = (unitDay == 1 ? 1 : mins);
                        double tarif  = (unitDay == 1 ? perDay : perHour);
                        double total  = tarif * qty;
                        java.sql.Date rawDate = rs2.getDate("date_worked");
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                        String date = sdf.format(rawDate);
//                        String date   = rs2.getDate("date_worked");
                        String lang   = rs2.getString("language");

                        model.addRow(new Object[]{
                                service,
                                tarif,
                                qty,
                                total,
                                date,
                                lang
                        });
                    }
                    if (!any) {
                        JOptionPane.showMessageDialog(
                                null,
                                "No billing entries found for “" + company + "”"
                        );
                    }
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Database error: " + ex.getMessage(),
                    "SQL Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

}
