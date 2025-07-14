import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
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
    static void exportPDF(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Invoice as PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        String path = chooser.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".pdf")) path += ".pdf";

        String company = (String) InvoiceApp.companyComboBox.getSelectedItem();
        Date fromDate = ((SpinnerDateModel) InvoiceApp.fromDateSpinner.getModel()).getDate();
        Date toDate = ((SpinnerDateModel) InvoiceApp.toDateSpinner.getModel()).getDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(path));
            doc.open();

            // 📝 Title
            Paragraph title = new Paragraph("Facture",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));

            // 🏢 Company and date range
            doc.add(new Paragraph("Entreprise : " + company));
            doc.add(new Paragraph("Période : " + sdf.format(fromDate) + " – " + sdf.format(toDate)));
            doc.add(new Paragraph(" "));

            // 📋 Invoice table in PDF
            PdfPTable pdfTable = new PdfPTable(4);
            pdfTable.setWidths(new int[]{3, 2, 2, 2});
            pdfTable.addCell("Prestation");
            pdfTable.addCell("Tarif (€)");
            pdfTable.addCell("Quantité");
            pdfTable.addCell("Total (€)");

            double subTotal = 0;
            for (int i = 0; i < InvoiceApp.model.getRowCount(); i++) {
                pdfTable.addCell(InvoiceApp.model.getValueAt(i, 0).toString());
                pdfTable.addCell(InvoiceApp.model.getValueAt(i, 1).toString());
                pdfTable.addCell(InvoiceApp.model.getValueAt(i, 2).toString());
                pdfTable.addCell(InvoiceApp.model.getValueAt(i, 3).toString());
                subTotal += Double.parseDouble(InvoiceApp.model.getValueAt(i, 3).toString());
            }
            doc.add(pdfTable);

            // 🧾 Footer notes
            doc.add(new Paragraph("Sous Total: " + subTotal + " €"));
            doc.add(new Paragraph("TVA non applicable (article 293B du CGI)"));
            doc.add(new Paragraph("Fait à Balma - " + java.time.LocalDate.now()));

            doc.close();
            JOptionPane.showMessageDialog(parent, "PDF exported successfully to:\n" + path);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Error exporting PDF: " + ex.getMessage());
        }
    }
}
