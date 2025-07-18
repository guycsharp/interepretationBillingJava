package DOA;

import java.util.List;

/**
 * Bundles together all data needed for an invoice:
 * 1) A list of InvoiceRow entries.
 * 2) The client’s address (for header).
 * 3) The latest bill number, so we can increment for the next invoice.
 *
 * This DTO allows the GUI to ask the loader for everything in one call.
 */
public class InvoiceData {
    private final List<InvoiceRow> rows;
    private final String clientAddress;
    private final double billNo;

    public InvoiceData(List<InvoiceRow> rows, String clientAddress, double billNo) {
        this.rows          = rows;
        this.clientAddress = clientAddress;
        this.billNo        = billNo;
    }

    public List<InvoiceRow> getRows()         { return rows; }
    public String getClientAddress()          { return clientAddress; }
    public double getBillNo()                 { return billNo; }
}
