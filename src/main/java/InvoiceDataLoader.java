import Utils.BillingLogic;
import Utils.CombineDateTime;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class InvoiceDataLoader {
    final static double debugMins = 13.0;

    public static HashMap<Integer, String> loadInvoiceData(String company,
                                                           Date fromDate, Date toDate,
                                                           boolean ignoreDate, boolean ignorePaid,
                                                           DefaultTableModel model) {
        model.setRowCount(0);
        HashMap<Integer, String> billIds = new HashMap<>();
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
                    return null;
                }
                clientId = rs.getInt("idclient_main");
                BillingManagerPanel.clientAdd = rs.getString("client_address");
            }

            StringBuilder lessThan30 = new StringBuilder(
                    " SELECT " +
                            "  rate_per_hour " +
                            " FROM rate_main  " +
                            " WHERE    language = 'LessThan30' " +
                            " AND client_id = ? " +
                            " AND rate_apply_date_from <= ? " +
                            " Order by rate_per_hour desc " +
                            " LIMIT 1 "
            );
            double lessThan30Rate = 0;

            // 2) Build a single SQL that JOINs bill_main → rate_main
            StringBuilder billNRate = new StringBuilder(
                    " SELECT " +
                            "  b.idbill_main, b.service_rendered, " +
                            "  b.UnitDay, " +
                            "  b.duration_in_minutes, " +
                            "  b.date_worked, " +
                            "  b.language, " +
                            "  r.rate_per_hour, " +
                            "  r.rate_per_day, r.offsetby, r.offsetunit " +
                            " FROM bill_main b " +
                            " INNER JOIN rate_main r " +
                            "  ON b.client_id = r.client_id " +
                            " AND b.language  = r.language " +
                            " WHERE b.client_id = ? " +
                            " AND r.rate_apply_date_from = ( " +
                            "     SELECT MAX(r2.rate_apply_date_from) " +
                            "     FROM rate_main AS r2 " +
                            "     WHERE r2.client_id            = b.client_id " +
                            "       AND r2.language             = b.language " +
                            "       AND r2.rate_apply_date_from <= b.date_worked " +
                            "   )"
            );

            if (!ignoreDate) {
                billNRate.append(" AND b.date_worked >= ? AND b.date_worked <= ? ");
            }
            if (!ignorePaid) {
                billNRate.append(" AND b.paid = 0 ");
            }

            billNRate.append("  order by b.date_worked  ");

            try (PreparedStatement psBill = conn.prepareStatement(billNRate.toString())) {
                int idx = 1;
                psBill.setInt(idx++, clientId);
                if (!ignoreDate) {
                    psBill.setDate(idx++, new java.sql.Date(fromDate.getTime()));
                    psBill.setDate(idx++, new java.sql.Date(toDate.getTime()));
                }

                try (ResultSet rs2 = psBill.executeQuery()) {
                    boolean any = false;
                    while (rs2.next()) {
                        any = true;
                        double mins = rs2.getDouble("duration_in_minutes");
                        java.sql.Date rawDate = rs2.getDate("date_worked");
                        String frenchDate = CombineDateTime.DateFormatter(rawDate,"dd-MM-yyyy");

                        try (PreparedStatement psBill1 = conn.prepareStatement(lessThan30.toString())) {

                            psBill1.setInt(1, clientId);
                            psBill1.setString(2, CombineDateTime.DateFormatter(rawDate, "yyyy-MM-dd") );


                            if (mins == debugMins) {
                                System.out.println("debug here");
                            }

                            try (ResultSet rs21 = psBill1.executeQuery()) {
                                while (rs21.next()) {
                                    lessThan30Rate = rs21.getDouble("rate_per_hour");
                                }
                            }
                        }

                        String service = rs2.getString("service_rendered");
                        int unitDay = rs2.getInt("UnitDay");


                        int offsetBy = rs2.getInt("offsetby");
                        int offsetunit = rs2.getInt("offsetunit");



                        double perHour = rs2.getDouble("rate_per_hour");
                        double perDay = rs2.getDouble("rate_per_day");

                        // if UnitDay == 1 use perDay, otherwise perHour
                        double qty = (unitDay == 1 ? 1 : mins);
                        double tarif = (unitDay == 1 ? perDay : perHour);

                        double total = 0;
                        if(unitDay == 1){
                            total = tarif;
                        } else {
                            total = BillingLogic.calculateTotalAmount(offsetBy, offsetunit, tarif, mins, lessThan30Rate);
                        }

                        billIds.put(rs2.getInt("idbill_main"), total + "");

                        String lang = rs2.getString("language");

                        model.addRow(new Object[]{
                                service,
                                tarif,
                                qty,
                                total,
                                frenchDate,
                                lang
                        });
                    }
                    if (!any) {
                        JOptionPane.showMessageDialog(
                                null,
                                "No billing entries found for “" + company + "” for selected date."
                        );
                    }
                }
            }
            return billIds;
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Database error: " + ex.getMessage(),
                    "SQL Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
        return billIds;
    }

    public static void updateBillNumber(HashMap<Integer, String> billNos, String billNum) {
        if (billNos == null || billNos.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "No bills list provided",
                    "SQL Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        int updatedCount = 0;

        for (Integer k : billNos.keySet()) {
            StringBuilder updateSQL = new StringBuilder("UPDATE bill_main SET bill_no = ")
                    .append(billNum)
                    .append(" , total_amt=")
                    .append(billNos.get(k))
                    .append(" WHERE idbill_main = ")
                    .append(k);

            try (Connection conn = MySQLConnector.getConnection();  // ✅ Now includes Connection
                 Statement stmt = conn.createStatement()) {

                if (stmt.executeUpdate(updateSQL.toString()) == 0) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Database error: No rows updated",
                            "SQL Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Database error: " + ex.getMessage(),
                        "SQL Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            updatedCount++;
        }

        JOptionPane.showMessageDialog(
                null,
                "Updated " + updatedCount + " bill(s)",
                "Update Successful",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

}
