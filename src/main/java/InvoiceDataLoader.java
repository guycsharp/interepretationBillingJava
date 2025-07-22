import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InvoiceDataLoader {
    final static double debugMins = 122.0;

    public static List<Integer> loadInvoiceData(String company,
                                                Date fromDate, Date toDate,
                                                boolean ignoreDate, boolean ignorePaid,
                                                DefaultTableModel model) {
        model.setRowCount(0);
        List<Integer> billIds = new ArrayList<>();
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
                    "SELECT " +

                            "  rate_per_hour " +
                            " FROM rate_main  " +
                            " WHERE    language = 'LessThan30' " +
                            " AND client_id = ?"
            );
            double lessThan30Rate = 0;
            try (PreparedStatement psBill = conn.prepareStatement(lessThan30.toString())) {
                int idx = 1;
                psBill.setInt(idx++, clientId);


                try (ResultSet rs2 = psBill.executeQuery()) {
                    while (rs2.next()) {
                        lessThan30Rate = rs2.getDouble("rate_per_hour");
                    }
                }
            }
            // 2) Build a single SQL that JOINs bill_main → rate_main
            StringBuilder billNRate = new StringBuilder(
                    "SELECT " +
                            "  b.idbill_main, b.service_rendered, " +
                            "  b.UnitDay, " +
                            "  b.duration_in_minutes, " +
                            "  b.date_worked, " +
                            "  b.language, " +
                            "  r.rate_per_hour, " +
                            "  r.rate_per_day, r.offsetby, r.offsetunit " +
                            "FROM bill_main b " +
                            "INNER JOIN rate_main r " +
                            "  ON b.client_id = r.client_id " +
                            " AND b.language  = r.language " +
                            "WHERE b.client_id = ? "
            );

            if (!ignoreDate) {
                billNRate.append(" AND b.date_worked >= ? AND b.date_worked <= ?");
            }
            if (!ignorePaid) {
                billNRate.append(" AND b.paid = 0");
            }

            billNRate.append("  order by b.date_worked  ");

            try (PreparedStatement psBill = conn.prepareStatement(billNRate.toString())) {
                int idx = 1;
                psBill.setInt(idx++, clientId);
                if (!ignoreDate) {
                    psBill.setDate(idx++, new java.sql.Date(fromDate.getTime()));
                    psBill.setDate(idx++, new java.sql.Date(toDate.getTime()));
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
                        int unitDay = rs2.getInt("UnitDay");
                        double mins = rs2.getDouble("duration_in_minutes");

                        billIds.add(rs2.getInt("idbill_main"));

                        int offsetBy = rs2.getInt("offsetby");
                        int offsetunit = rs2.getInt("offsetunit");
                        double isOffset = mins % offsetunit;
                        double adjustedMin = mins;
                        int count = 0;
                        if (mins == debugMins) {
                            System.out.println("debug here");
                        }
                        while (mins > offsetunit && isOffset > offsetBy) {
                            adjustedMin = mins - isOffset + offsetunit;
                            isOffset = adjustedMin % offsetunit;
                            count++;
                            if (count > 1) {
                                JOptionPane.showMessageDialog(
                                        null,
                                        "Minute adjustment error has occurred",
                                        "Adjust Minute",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                        if (isOffset <= offsetBy) {
                            adjustedMin = adjustedMin - isOffset;
                        }
                        double perHour = rs2.getDouble("rate_per_hour");
                        double perDay = rs2.getDouble("rate_per_day");

                        // if UnitDay == 1 use perDay, otherwise perHour
                        double qty = (unitDay == 1 ? 1 : mins);
                        double tarif = (unitDay == 1 ? perDay : perHour);
                        double total = tarif * (adjustedMin / 60);
                        // until it is 32 minutes lessthan30 rate applies
                        if (mins <= offsetunit + offsetBy) {
                            total = lessThan30Rate;
                        }
                        java.sql.Date rawDate = rs2.getDate("date_worked");
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                        String date = sdf.format(rawDate);
//                        String date   = rs2.getDate("date_worked");
                        String lang = rs2.getString("language");

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

    public static void updateBillNumber(List<Integer> billNos, String billNum) {
        if (billNos == null || billNos.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "No bills list provided",
                    "SQL Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        StringBuilder updateSQL = new StringBuilder("UPDATE bill_main SET bill_no = ")
                .append(billNum)
                .append(" WHERE idbill_main IN (");

        for (int i = 0; i < billNos.size(); i++) {
            updateSQL.append(billNos.get(i));
            if (i < billNos.size() - 1) updateSQL.append(", ");
        }
        updateSQL.append(")");

        try (Connection conn = MySQLConnector.getConnection();  // ✅ Now includes Connection
             Statement stmt = conn.createStatement()) {

            int updatedCount = stmt.executeUpdate(updateSQL.toString());

            if (updatedCount == 0) {
                JOptionPane.showMessageDialog(
                        null,
                        "Database error: No rows updated",
                        "SQL Error",
                        JOptionPane.ERROR_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        null,
                        "Updated " + updatedCount + " bill(s)",
                        "Update Successful",
                        JOptionPane.INFORMATION_MESSAGE
                );
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
