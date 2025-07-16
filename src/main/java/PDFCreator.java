// PDFCreator.java

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class PDFCreator {

    /**
     * Helper: replaces every instance of replaceChar in sourceText with replaceBy.
     * We’ll use this to turn "\n" into commas (or any other delimiter) before laying out paragraphs.
     */
    public static String textNewLineReplaceDelimiter(String sourceText, char replaceBy, char replaceChar) {
        return sourceText.replace(replaceChar, replaceBy);
    }

    /**
     * Helper: wraps a line of text in an iText Paragraph, applies alignment, and returns it.
     * alignment is one of Element.ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT.
     */
    public static Paragraph addParagraph(String text, int alignment) {
        // First, normalize line breaks (so iText doesn’t start a brand-new line).
        String normalized = textNewLineReplaceDelimiter(text, '\n', ',');
        Paragraph p = new Paragraph(normalized);
        p.setAlignment(alignment);
        return p;
    }

    /**
     * Creates the actual PDF file.
     *
     * @param parent     the Swing Component to anchor file-chooser dialogs
     * @param billNo     the invoice number to print in the title
     * @param address    the client’s address block (may contain "\n")
     * @param myaddress  our own address block (may contain "\n")
     */
    static void exportPDF(Component parent, double billNo, String address, String myaddress,  Date billedOn) {
        // STEP 1: Ask the user where to save the PDF
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Invoice as PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            // User hit Cancel
            return;
        }

        // Build the final path, ensure it ends in .pdf
        String path = chooser.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".pdf")) {
            path += ".pdf";
        }

        // Pull in the rest of our invoice info from InvoiceApp
        String company = (String) InvoiceApp.companyComboBox.getSelectedItem();
        Date fromDate  = ((SpinnerDateModel) InvoiceApp.fromDateSpinner.getModel()).getDate();
        Date toDate    = ((SpinnerDateModel) InvoiceApp.toDateSpinner.getModel()).getDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            // STEP 2: Initialize the PDF document and writer
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(path));
            doc.open();

            // STEP 3: Add the centered title with invoice number
            String billNum = String.valueOf((int)billNo);  // drop decimal
            Paragraph title = new Paragraph(
                    "Facture : " + billNum,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)
            );
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            // A blank line for spacing
            doc.add(new Paragraph(" "));

            // STEP 4: Create a 2-column, full-width table for address blocks
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);           // span full page
            headerTable.setWidths(new float[]{1, 1});      // equal columns

            // Left cell: our address (no border)
            PdfPCell leftCell = new PdfPCell(addParagraph(myaddress, Element.ALIGN_LEFT));
            leftCell.setBorder(PdfPCell.NO_BORDER);
            headerTable.addCell(leftCell);

            // Right cell: company name + client address (no border)
            String rightText = company + "\n" + address;
            PdfPCell rightCell = new PdfPCell(addParagraph(rightText, Element.ALIGN_RIGHT));
            rightCell.setBorder(PdfPCell.NO_BORDER);
            headerTable.addCell(rightCell);

            // Add that header row to the document
            doc.add(headerTable);
            doc.add(new Paragraph(" "));

            // STEP 5: Add the period line centered under the header
            String period = "Période : " + sdf.format(fromDate) + " – " + sdf.format(toDate);
            doc.add(addParagraph(period, Element.ALIGN_CENTER));
            doc.add(new Paragraph(" "));

            // STEP 6: Build the invoice table (4 columns)
            PdfPTable pdfTable = new PdfPTable(4);
            pdfTable.setWidths(new int[]{3, 2, 2, 2});
            pdfTable.addCell("Prestation");
            pdfTable.addCell("Tarif (€)");
            pdfTable.addCell("Quantité");
            pdfTable.addCell("Total (€)");

            // STEP 7: Populate rows from the Swing table model
            double subTotal = 0;
            for (int i = 0; i < InvoiceApp.model.getRowCount(); i++) {
                // 1st column: service text + date + extra info
                String serviceText =
                        InvoiceApp.model.getValueAt(i, 0).toString() +
                                " en " + InvoiceApp.model.getValueAt(i, 5).toString() +
                                "\n" + InvoiceApp.model.getValueAt(i, 4).toString();

                pdfTable.addCell(serviceText);

                // 2nd column: unit price
                pdfTable.addCell(InvoiceApp.model.getValueAt(i, 1).toString());

                // 3rd column: quantity (days/hours)
                pdfTable.addCell(InvoiceApp.model.getValueAt(i, 2).toString());

                // 4th column: total price for this line
                String lineTotal = InvoiceApp.model.getValueAt(i, 3).toString();
                pdfTable.addCell(lineTotal);

                // Accumulate for the sub-total
                subTotal += Double.parseDouble(lineTotal);
            }


            // STEP 8: Add a subtotal row under "Total (€)"
            PdfPCell blank1 = new PdfPCell(new Paragraph(""));
            blank1.setBorder(PdfPCell.NO_BORDER);
            pdfTable.addCell(blank1);

            PdfPCell blank2 = new PdfPCell(new Paragraph(""));
            blank2.setBorder(PdfPCell.NO_BORDER);
            pdfTable.addCell(blank2);

            PdfPCell labelCell = new PdfPCell(new Paragraph("Sous Total"));
            labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pdfTable.addCell(labelCell);

            PdfPCell valueCell = new PdfPCell(new Paragraph(subTotal + " €"));
            valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pdfTable.addCell(valueCell);

            doc.add(pdfTable);

            // STEP 9: Footer notes (centered)
            doc.add(new Paragraph(" "));
            Paragraph vatNote = new Paragraph("TVA non applicable (article 293B du CGI)");
            vatNote.setAlignment(Element.ALIGN_CENTER);
            doc.add(vatNote);

            Paragraph placeDate = new Paragraph("Fait à Balma - " + (new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)).format(billedOn));
            placeDate.setAlignment(Element.ALIGN_CENTER);
            doc.add(placeDate);

            // STEP 10: Append a signature image at the bottom-left
            try {
                Image signature = Image.getInstance("resources/RojiSig.png");
                signature.scaleToFit(100, 50);
                signature.setAlignment(Element.ALIGN_LEFT);
                doc.add(signature);
            } catch (Exception imgEx) {
                // If the image fails to load, we still continue
                imgEx.printStackTrace();
                doc.add(new Paragraph("⚠ Failed to load signature image."));
            }

            // STEP 11: Finalize and inform the user
            doc.close();
            JOptionPane.showMessageDialog(parent,
                    "PDF exported successfully to:\n" + path
            );

        } catch (Exception ex) {
            // Any exception along the way pops up an error dialog
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Error exporting PDF: " + ex.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
