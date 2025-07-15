import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PDFCreator {
    /**
     * Exports the invoice table and header info to PDF.
     */
    static void exportPDF(Component parent, double billNo) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Invoice as PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        String path = chooser.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".pdf")) path += ".pdf";

        // 🏢 Access selected company and date range from InvoiceApp
        String company = (String) InvoiceApp.companyComboBox.getSelectedItem();
        Date fromDate = ((SpinnerDateModel) InvoiceApp.fromDateSpinner.getModel()).getDate();
        Date toDate   = ((SpinnerDateModel) InvoiceApp.toDateSpinner.getModel()).getDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(path));
            doc.open();

            // 📝 PDF Title
            String billNum = billNo+"";
            Paragraph title = new Paragraph("Facture:"+billNum.substring(0,billNum.indexOf('.')), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));

            // 🔖 Company and date range info
            doc.add(new Paragraph("Entreprise : " + company));
            doc.add(new Paragraph("Période : " + sdf.format(fromDate) + " – " + sdf.format(toDate)));
            doc.add(new Paragraph(" "));

            // 📋 Invoice table
            PdfPTable pdfTable = new PdfPTable(4);
            pdfTable.setWidths(new int[]{3, 2, 2, 2});
            pdfTable.addCell("Prestation");
            pdfTable.addCell("Tarif (€)");
            pdfTable.addCell("Quantité");
            pdfTable.addCell("Total (€)");

            double subTotal = 0;

            // ➕ Add invoice rows from the table model
            for (int i = 0; i < InvoiceApp.model.getRowCount(); i++) {
                pdfTable.addCell(InvoiceApp.model.getValueAt(i, 0).toString());
                pdfTable.addCell(InvoiceApp.model.getValueAt(i, 1).toString());
                pdfTable.addCell(InvoiceApp.model.getValueAt(i, 2).toString());
                pdfTable.addCell(InvoiceApp.model.getValueAt(i, 3).toString());
                subTotal += Double.parseDouble(InvoiceApp.model.getValueAt(i, 3).toString());
            }

            // ➕ Subtotal row within the table, aligned under "Total (€)"
            PdfPCell emptyCell1 = new PdfPCell(new Paragraph(""));
            emptyCell1.setBorder(PdfPCell.NO_BORDER);
            pdfTable.addCell(emptyCell1);

            PdfPCell emptyCell2 = new PdfPCell(new Paragraph(""));
            emptyCell2.setBorder(PdfPCell.NO_BORDER);
            pdfTable.addCell(emptyCell2);

            PdfPCell labelCell = new PdfPCell(new Paragraph("Sous Total"));
            labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pdfTable.addCell(labelCell);

            PdfPCell valueCell = new PdfPCell(new Paragraph(subTotal + " €"));
            valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pdfTable.addCell(valueCell);

            doc.add(pdfTable);

            // 📝 Footer notes
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("TVA non applicable (article 293B du CGI)"));
            doc.add(new Paragraph("Fait à Balma - " + java.time.LocalDate.now()));

            // 🔚 Add image at the end
            try {
                com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance("path/to/image.png");
                image.scaleToFit(100, 50);
                image.setAlignment(Element.ALIGN_CENTER);
                doc.add(image);
            } catch (Exception e) {
                e.printStackTrace();
                doc.add(new Paragraph("⚠ Failed to load image."));
            }

            doc.close();
            JOptionPane.showMessageDialog(parent, "PDF exported successfully to:\n" + path);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Error exporting PDF: " + ex.getMessage());
        }
    }
}
