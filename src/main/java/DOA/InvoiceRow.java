package DOA;

/**
 * A single line item on an invoice.
 *
 * Fields:
 *  - service     : description of the work performed
 *  - tarif       : unit price (per day or per hour)
 *  - qty         : number of units (days or hours)
 *  - total       : tarif * qty
 *  - dateWorked  : the date on which the work was done
 *  - language    : any additional language note
 *
 * This is a pure Data Transfer Object (DTO) with no business logic.
 */
public class InvoiceRow {
    private final String service;
    private final double tarif;
    private final int qty;
    private final double total;
    private final String dateWorked;
    private final String language;

    public InvoiceRow(
            String service,
            double tarif,
            int qty,
            double total,
            String dateWorked,
            String language
    ) {
        this.service    = service;
        this.tarif      = tarif;
        this.qty        = qty;
        this.total      = total;
        this.dateWorked = dateWorked;
        this.language   = language;
    }

    public String getService()    { return service; }
    public double getTarif()      { return tarif; }
    public int getQty()           { return qty; }
    public double getTotal()      { return total; }
    public String getDateWorked() { return dateWorked; }
    public String getLanguage()   { return language; }
}
