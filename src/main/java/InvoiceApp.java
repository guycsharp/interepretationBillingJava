// InvoiceApp.java

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

// iText imports
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

/**
 * InvoiceApp with:
 *  - Company selector (JComboBox)
 *  - From/To date pickers using JSpinner+DateEditor
 *  - Entry table + Add Row
 *  - Save-as PDF dialog
 */
public class InvoiceApp {
    private static JTable table;
    private static DefaultTableModel model;
    private static JTextField prestationField, tarifField, qtyField;
    private static JComboBox<String> companyComboBox;
    private static JSpinner fromDateSpinner, toDateSpinner;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(InvoiceApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Billing Software");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        // Top panel: Company + Date range
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.add(new JLabel("Company:"));
        companyComboBox = new JComboBox<>(new String[]{"Company A", "Company B", "Company C"});
        top.add(companyComboBox);

        top.add(new JLabel("From:"));
        fromDateSpinner = makeDateSpinner();
        top.add(fromDateSpinner);

        top.add(new JLabel("To:"));
        toDateSpinner = makeDateSpinner();
        top.add(toDateSpinner);

        frame.add(top, BorderLayout.NORTH);

        // Center: Invoice table
        model = new DefaultTableModel(
                new Object[]{"Prestation", "Tarif (€)", "Quantité", "Total (€)"}, 0
        );
        table = new JTable(model);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom: Input fields + buttons
        prestationField = new JTextField();
        tarifField       = new JTextField();
        qtyField         = new JTextField();

        JPanel bottom = new JPanel(new GridLayout(2, 4, 10, 5));
        bottom.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        bottom.add(new JLabel("Prestation"));
        bottom.add(prestationField);
        bottom.add(new JLabel("Tarif (€)"));
        bottom.add(tarifField);
        bottom.add(new JLabel("Quantité"));
        bottom.add(qtyField);

        JButton addBtn    = new JButton("Add Row");
        JButton exportBtn = new JButton("Export to PDF");
        bottom.add(addBtn);
        bottom.add(exportBtn);

        frame.add(bottom, BorderLayout.SOUTH);

        // Add row action
        addBtn.addActionListener(e -> {
            String p = prestationField.getText().trim();
            String t = tarifField.getText().trim();
            String q = qtyField.getText().trim();
            if (p.isEmpty() || t.isEmpty() || q.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please fill in all fields.");
                return;
            }
            try {
                double tarif = Double.parseDouble(t);
                int qty      = Integer.parseInt(q);
                model.addRow(new Object[]{p, tarif, qty, tarif * qty});
                prestationField.setText("");
                tarifField.setText("");
                qtyField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Enter valid numbers for Tarif and Quantité.");
            }
        });

        // Export action
        exportBtn.addActionListener(e -> exportPDF(frame));

        frame.setVisible(true);
    }

    /**
     * Creates a JSpinner configured as a date picker (calendar pop-up).
     */
    private static JSpinner makeDateSpinner() {
        // SpinnerDateModel with current date, no bounds, step = day
        SpinnerDateModel model = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model);
        // Editor that shows yyyy-MM-dd and provides a calendar popup
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
        spinner.setEditor(editor);
        return spinner;
    }

    /**
     * Opens a save dialog, gathers inputs (company, dates, table rows),
     * and writes them into a PDF invoice.
     */
    private static void exportPDF(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Invoice as PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        String path = file.getAbsolutePath();
        if (!path.toLowerCase().endsWith(".pdf")) {
            path += ".pdf";
        }

        // Read header data
        String company = (String) companyComboBox.getSelectedItem();
        Date fromDate  = ((SpinnerDateModel) fromDateSpinner.getModel()).getDate();
        Date toDate    = ((SpinnerDateModel) toDateSpinner.getModel()).getDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(path));
            document.open();

            // Title
            Paragraph title = new Paragraph("Facture",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // Company & date range
            document.add(new Paragraph("Entreprise : " + company));
            document.add(new Paragraph("Période : "
                    + sdf.format(fromDate) + " – " + sdf.format(toDate)));
            document.add(new Paragraph(" "));

            // Build PDF table
            PdfPTable pdfTable = new PdfPTable(4);
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

            // Footer
            document.add(new Paragraph("Sous Total: " + subTotal + " €"));
            document.add(new Paragraph("TVA non applicable (article 293B du CGI)"));
            document.add(new Paragraph("Fait à Balma - " + java.time.LocalDate.now()));

            document.close();

            JOptionPane.showMessageDialog(parent,
                    "PDF exported successfully to:\n" + path);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Error exporting PDF: " + ex.getMessage());
        }
    }
}
