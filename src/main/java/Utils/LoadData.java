package Utils;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class LoadData {
    /*
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

     */
}
