import DOA.InvoiceData;
import DOA.InvoiceRow;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * InvoiceDataLoader handles ALL database access for invoices.
 *
 * load(...) does three things:
 * 1) Queries client_main for rates, ID, and address.
 * 2) Finds the MAX(bill_no) from bill_main.
 * 3) Queries bill_main for each line item in the date range.
 *
 * Returns an InvoiceData DTO to the caller.
 */
public class InvoiceDataLoader {

    public static InvoiceData load(
            String company,
            java.util.Date fromDate,
            java.util.Date toDate,
            boolean ignoreDate,
            boolean ignorePaid
    ) throws SQLException {
        // 1) SQL to fetch rates and address
        String rateSql    = "SELECT client_rate, client_rate_per_day, idclient_main, client_address "
                + "FROM client_main WHERE client_name = ?";
        // 2) SQL to fetch the latest bill number
        String maxBillSql = "SELECT MAX(bill_no) AS max_bill FROM bill_main";

        // 3) Dynamically build the billing query
        StringBuilder billSql = new StringBuilder(
                "SELECT service_rendered, UnitDay, workedDayOrHours, date_worked, language "
                        + "FROM bill_main WHERE 1=1"
        );
        if (!"All".equals(company)) billSql.append(" AND client_id = ?");
        if (!ignoreDate)           billSql.append(" AND date_worked >= ? AND date_worked <= ?");
        if (!ignorePaid)           billSql.append(" AND paid != 1");

        try (
                Connection conn = MySQLConnector.getConnection();
                PreparedStatement psRate = conn.prepareStatement(rateSql)
        ) {
            // Bind and execute client lookup
            psRate.setString(1, company);
            double rate, ratePerDay;
            int clientId;
            String clientAddress;

            try (ResultSet rs = psRate.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No client found: " + company);
                }
                rate          = rs.getDouble("client_rate");
                ratePerDay    = rs.getDouble("client_rate_per_day");
                clientId      = rs.getInt("idclient_main");
                clientAddress = rs.getString("client_address");
            }

            // Get the highest existing bill number
            double billNo = 1;
            try (
                    PreparedStatement psMax = conn.prepareStatement(maxBillSql);
                    ResultSet rsMax = psMax.executeQuery()
            ) {
                if (rsMax.next()) {
                    billNo = rsMax.getDouble("max_bill");
                }
            }

            // Fetch each invoice line
            List<InvoiceRow> rows = new ArrayList<>();
            try (PreparedStatement psBill = conn.prepareStatement(billSql.toString())) {
                int idx = 1;
                if (!"All".equals(company)) {
                    psBill.setInt(idx++, clientId);
                }
                if (!ignoreDate) {
                    psBill.setDate(idx++, new Date(fromDate.getTime()));
                    psBill.setDate(idx++, new Date(toDate.getTime()));
                }

                try (ResultSet rs2 = psBill.executeQuery()) {
                    while (rs2.next()) {
                        // Read fields from each row
                        String service    = rs2.getString("service_rendered");
                        int unitDay       = rs2.getInt("UnitDay");
                        int qty           = (unitDay == 1)
                                ? 1
                                : rs2.getInt("workedDayOrHours");
                        double tarif      = (unitDay == 1)
                                ? ratePerDay
                                : rate;
                        double total      = tarif * qty;
                        String dateWorked = rs2.getString("date_worked").substring(0, 10);
                        String language   = rs2.getString("language");

                        // Add to our list
                        rows.add(new InvoiceRow(
                                service, tarif, qty, total, dateWorked, language
                        ));
                    }
                }
            }

            // Return a single object containing everything
            return new InvoiceData(rows, clientAddress, billNo);
        }
    }
}
