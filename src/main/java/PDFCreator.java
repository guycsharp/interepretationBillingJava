import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * PDFCreator is your “teacher” for turning the GUI data into a PDF.
 * It:
 * 1) Asks where to save via JFileChooser.
 * 2) Reads invoice details from InvoiceApp.
 * 3) Builds header, tables, footer, and signature.
 * 4) Writes the PDF and shows success/error dialogs.
 */
public class PDFCreator {

    /**
     * Replaces each occurrence of replaceChar in sourceText with replaceBy.
     * Used to normalize line breaks into commas (or any character).
     */
    public static String textNewLineReplaceDelimiter(
            String sourceText,
            char replaceChar,
            char replaceBy
    ) {
        return sourceText.replace(replaceChar, replaceBy);
    }

    /**
     * Wraps text into a Paragraph, applying the given alignment.
     */
    public static Paragraph addParagraph(String text, int alignment) {
        String normalized = textNewLineReplaceDelimiter(text, '\n', ',');
        Paragraph p = new Paragraph(normalized);
        p.setAlignment(alignment);
        return p;
    }

    /**
     * Creates the PDF file from current InvoiceApp data.
     *
     * @param parent     Swing parent for dialogs
     * @param billNo     invoice number to show in title
     * @param address    client address block
     * @param myaddress  your company’s address block
     * @param billedOn   date to print under the footer
     */
    public static void exportPDF(
            Component parent,
            double billNo,
            String address,
            String myaddress,
            Date billedOn
    ) {
        // STEP 1: Ask user where to save
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Invoice as PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return; // user cancelled
        }
        String path = chooser.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".pdf")) {
            path += ".pdf";
        }

        // Pull data from InvoiceApp
        String company = (String) InvoiceApp.companyComboBox.getSelectedItem();
        Date fromDate  = ((SpinnerDateModel) InvoiceApp.fromDateSpinner.getModel()).getDate();
        Date toDate    = ((SpinnerDateModel) InvoiceApp.toDateSpinner.getModel()).getDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            // STEP 2: Initialize document
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(path));
            doc.open();

            // STEP 3: Title
            String billNum = String.valueOf((int) billNo);
            Paragraph title = new Paragraph(
                    "Facture : " + billNum,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)
            );
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));

            // STEP 4: Header table with addresses
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1, 1});

            PdfPCell leftCell = new PdfPCell(addParagraph(myaddress, Element.ALIGN_LEFT));
            leftCell.setBorder(PdfPCell.NO_BORDER);
            headerTable.addCell(leftCell);

            String rightText = company + "\n" + address;
            PdfPCell rightCell = new PdfPCell(addParagraph(rightText, Element.ALIGN_RIGHT));
            rightCell.setBorder(PdfPCell.NO_BORDER);
            headerTable.addCell(rightCell);

            doc.add(headerTable);
            doc.add(new Paragraph(" "));

            // STEP 5: Period line
            String period = "Période : " + sdf.format(fromDate) + " – " + sdf.format(toDate);
            doc.add(addParagraph(period, Element.ALIGN_CENTER));
            doc.add(new Paragraph(" "));

            // STEP 6: Invoice table
            PdfPTable table = new PdfPTable(4);
            table.setWidths(new int[]{3, 2, 2, 2});
            table.addCell("Prestation");
            table.addCell("Tarif (€)");
            table.addCell("Quantité");
            table.addCell("Total (€)");

            // STEP 7: Populate rows and subtotal
            double subTotal = 0;
            for (int i = 0; i < InvoiceApp.model.getRowCount(); i++) {
                String serviceText =
                        InvoiceApp.model.getValueAt(i, 0) + " en " +
                                InvoiceApp.model.getValueAt(i, 5) + "\n" +
                                InvoiceApp.model.getValueAt(i, 4);

                table.addCell(serviceText);
                table.addCell(InvoiceApp.model.getValueAt(i, 1).toString());
                table.addCell(InvoiceApp.model.getValueAt(i, 2).toString());
                String lineTotal = InvoiceApp.model.getValueAt(i, 3).toString();
                table.addCell(lineTotal);
                subTotal += Double.parseDouble(lineTotal);
            }

            // STEP 8: Add subtotal row
            for (int c = 0; c < 2; c++) {
                PdfPCell blank = new PdfPCell(new Paragraph(""));
                blank.setBorder(PdfPCell.NO_BORDER);
                table.addCell(blank);
            }
            PdfPCell labelCell = new PdfPCell(new Paragraph("Sous Total"));
            labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(labelCell);
            PdfPCell valueCell = new PdfPCell(new Paragraph(subTotal + " €"));
            valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(valueCell);

            doc.add(table);

            // STEP 9: Footer notes
            doc.add(new Paragraph(" "));
            Paragraph vatNote = new Paragraph("TVA non applicable (article 293B du CGI)");
            vatNote.setAlignment(Element.ALIGN_CENTER);
            doc.add(vatNote);

            Paragraph placeDate = new Paragraph(
                    "Fait à Balma - " +
                            (new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)).format(billedOn)
            );
            placeDate.setAlignment(Element.ALIGN_CENTER);
            doc.add(placeDate);

            // STEP 10: Signature image
            try {
                com.itextpdf.text.Image sig = com.itextpdf.text.Image.getInstance("resources/RojiSig.png");

                sig.scaleToFit(100, 50);
                sig.setAlignment(Element.ALIGN_LEFT);
                doc.add(sig);
            } catch (Exception imgEx) {
                imgEx.printStackTrace();
                doc.add(new Paragraph("⚠ Failed to load signature image."));
            }

            // STEP 11: Close and notify
            doc.close();
            JOptionPane.showMessageDialog(parent, "PDF exported to:\n" + path);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    parent,
                    "Error exporting PDF: " + ex.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
