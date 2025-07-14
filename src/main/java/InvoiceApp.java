// InvoiceApp.java
// Main GUI for billing: selects company from DB, picks date range, adds invoice lines,
// and exports to a PDF using iText.
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// iText imports
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

public class InvoiceApp {
    // Swing components at class scope for easy access
    private static JTable table;
    private static DefaultTableModel model;
    private static JTextField prestationField, tarifField, qtyField;
    private static JComboBox<String> companyComboBox;
    private static JSpinner fromDateSpinner, toDateSpinner;

    public static void main(String[] args) {
        // Always launch Swing GUIs on the Event Dispatch Thread
        SwingUtilities.invokeLater(InvoiceApp::createAndShowGUI);
    }

    /**
     * Builds and displays the main application window.
     */
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Billing Software");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        // --- Top: filter panel with company selector and date range pickers ---
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.add(new JLabel("Company:"));
        companyComboBox = createCompanyComboBox();
        filterPanel.add(companyComboBox);

        filterPanel.add(new JLabel("From:"));
        fromDateSpinner = makeDateSpinner();
        filterPanel.add(fromDateSpinner);

        filterPanel.add(new JLabel("To:"));
        toDateSpinner = makeDateSpinner();
        filterPanel.add(toDateSpinner);

        frame.add(filterPanel, BorderLayout.NORTH);

        // --- Center: invoice table ---
        model = new DefaultTableModel(
                new Object[]{"Prestation", "Tarif (€)", "Quantité", "Total (€)"}, 0
        );
        table = new JTable(model);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Bottom: input fields and action buttons ---
        prestationField = new JTextField();
        tarifField       = new JTextField();
        qtyField         = new JTextField();

        JPanel inputPanel = new JPanel(new GridLayout(2, 4, 10, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        inputPanel.add(new JLabel("Prestation"));
        inputPanel.add(prestationField);
        inputPanel.add(new JLabel("Tarif (€)"));
        inputPanel.add(tarifField);
        inputPanel.add(new JLabel("Quantité"));
        inputPanel.add(qtyField);

        JButton addButton    = new JButton("Add Row");
        JButton exportButton = new JButton("Export to PDF");
        inputPanel.add(addButton);
        inputPanel.add(exportButton);

        frame.add(inputPanel, BorderLayout.SOUTH);

        // Add row action: validate inputs, calculate total, add to table
        addButton.addActionListener(e -> {
            String prestation = prestationField.getText().trim();
            String tarifText   = tarifField.getText().trim();
            String qtyText     = qtyField.getText().trim();
            if (prestation.isEmpty() || tarifText.isEmpty() || qtyText.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "Please fill in all fields before adding.");
                return;
            }
            try {
                double tarif = Double.parseDouble(tarifText);
                int qty      = Integer.parseInt(qtyText);
                model.addRow(new Object[]{prestation, tarif, qty, tarif * qty});
                prestationField.setText("");
                tarifField.setText("");
                qtyField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Enter valid numeric values for Tarif and Quantité.");
            }
        });

        // Export to PDF action: opens save dialog, then writes PDF invoice
        exportButton.addActionListener(e -> exportPDF(frame));

        frame.setVisible(true);
    }

    /**
     * Queries client_main for active client names, builds a JComboBox.
     */
    private static JComboBox<String> createCompanyComboBox() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT client_name FROM client_main "
                + "WHERE soft_delete IS NULL OR soft_delete = 0 "
                + "ORDER BY client_name";
        try (Connection conn = MySQLConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                names.add(rs.getString("client_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            names.add("<< error loading companies >>");
        }
        if (names.isEmpty()) {
            names.add("<< no companies found >>");
        }
        return new JComboBox<>(names.toArray(new String[0]));
    }

    /**
     * Creates a JSpinner configured as a date picker (calendar pop-up).
     */
    private static JSpinner makeDateSpinner() {
        SpinnerDateModel dateModel = new SpinnerDateModel(
                new Date(), null, null, java.util.Calendar.DAY_OF_MONTH
        );
        JSpinner spinner = new JSpinner(dateModel);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
        spinner.setEditor(editor);
        return spinner;
    }

    /**
     * Opens a file-save dialog, then writes the invoice (with selected company
     * and date range) into a PDF file using iText.
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

        // Gather header info
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
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)
            );
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // Company & date range
            document.add(new Paragraph("Entreprise : " + company));
            document.add(new Paragraph(
                    "Période : " + sdf.format(fromDate) + " – " + sdf.format(toDate)
            ));
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
                    "PDF exported successfully to:\n" + path
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Error exporting PDF: " + ex.getMessage()
            );
        }
    }
}
