
// Import essential Swing components for GUI
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

// Import iText classes for PDF creation
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;

// This declares your class in the default package.
// If you want to use a package, make sure your folders match.
public class InvoiceApp {

    // 🗂️ GUI components declared at the class level so they're accessible across methods
    private static JTable table;
    private static DefaultTableModel model;
    private static JTextField prestationField, tarifField, qtyField;

    public static void main(String[] args) {
        // 🧵 Launch GUI safely on the Event Dispatch Thread
        javax.swing.SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    // 🖼️ Builds and displays the GUI window
    private static void createAndShowGUI() {
        // 🌟 Main window frame
        javax.swing.JFrame frame = new javax.swing.JFrame("Billing Software");
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
        frame.setLocationRelativeTo(null); // center on screen

        // 📋 Table model setup
        model = new javax.swing.table.DefaultTableModel(new Object[]{"Prestation", "Tarif (€)", "Quantité", "Total (€)"}, 0);
        table = new javax.swing.JTable(model);
        frame.add(new javax.swing.JScrollPane(table), java.awt.BorderLayout.CENTER);

        // 🧮 Input fields for invoice entries
        prestationField = new javax.swing.JTextField();
        tarifField = new javax.swing.JTextField();
        qtyField = new javax.swing.JTextField();

        javax.swing.JPanel inputPanel = new javax.swing.JPanel(new java.awt.GridLayout(2, 4));
        inputPanel.add(new javax.swing.JLabel("Prestation"));
        inputPanel.add(prestationField);
        inputPanel.add(new javax.swing.JLabel("Tarif (€)"));
        inputPanel.add(tarifField);
        inputPanel.add(new javax.swing.JLabel("Quantité"));
        inputPanel.add(qtyField);

        javax.swing.JButton addButton = new javax.swing.JButton("Add Row");
        javax.swing.JButton exportButton = new javax.swing.JButton("Export to PDF");
        inputPanel.add(addButton);
        inputPanel.add(exportButton);

        frame.add(inputPanel, java.awt.BorderLayout.SOUTH);

        // ➕ Add row to the table with validation
        addButton.addActionListener(e -> {
            String prestation = prestationField.getText().trim();
            String tarifText = tarifField.getText().trim();
            String qtyText = qtyField.getText().trim();

            if (prestation.isEmpty() || tarifText.isEmpty() || qtyText.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(null, "Please fill in all fields before adding.");
                return;
            }

            try {
                double tarif = Double.parseDouble(tarifText);
                int qty = Integer.parseInt(qtyText);
                double total = tarif * qty;

                model.addRow(new Object[]{prestation, tarif, qty, total});
                prestationField.setText("");
                tarifField.setText("");
                qtyField.setText("");
            } catch (NumberFormatException ex) {
                javax.swing.JOptionPane.showMessageDialog(null, "Enter valid numeric values for Tarif and Quantité.");
            }
        });

        // 📤 Export table data to PDF
        exportButton.addActionListener(e -> exportPDF());

        frame.setVisible(true);
    }

    // 📄 PDF creation using iText library
    public static void exportPDF() {
        try {
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream("invoice.pdf"));
            document.open();

            com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("Facture",
                    com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(title);
            document.add(new com.itextpdf.text.Paragraph(" "));

            com.itextpdf.text.pdf.PdfPTable pdfTable = new com.itextpdf.text.pdf.PdfPTable(4);
            pdfTable.setWidths(new int[]{3, 2, 2, 2});
            pdfTable.addCell("Prestation");
            pdfTable.addCell("Tarif (€)");
            pdfTable.addCell("Quantité");
            pdfTable.addCell("Total (€)");

            double subTotal = 0;
            for (int i = 0; i < model.getRowCount(); i++) {
                pdfTable.addCell(model.getValueAt(i, 0).toString());
                pdfTable.addCell(model.getValueAt(i, 1).toString());
                pdfTable.addCell(model.getValueAt(i, 2).toString());
                pdfTable.addCell(model.getValueAt(i, 3).toString());

                subTotal += Double.parseDouble(model.getValueAt(i, 3).toString());
            }

            document.add(pdfTable);
            document.add(new com.itextpdf.text.Paragraph("Sous Total: " + subTotal + " €"));
            document.add(new com.itextpdf.text.Paragraph("TVA non applicable (article 293B du CGI)"));
            document.add(new com.itextpdf.text.Paragraph("Fait à Balma - " + java.time.LocalDate.now()));

            document.close();
            javax.swing.JOptionPane.showMessageDialog(null, "PDF exported successfully!");
        } catch (Exception ex) {
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }
}
