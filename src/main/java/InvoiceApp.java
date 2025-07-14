// InvoiceApp.java
// Main GUI: select a company & date range, Load rows from DB, add manual rows,
// then Export everything to a PDF invoice.

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

        // --- Top panel: company selector + date range pickers ---
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

        // --- Bottom: inputs + Add, Load, Export buttons ---
        prestationField = new JTextField();
        tarifField       = new JTextField();
        qtyField         = new JTextField();

        // use 2 rows × 5 cols so we can fit three buttons
        JPanel inputPanel = new JPanel(new GridLayout(2, 5, 10, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Row 1: labels & text fields
        inputPanel.add(new JLabel("Prestation"));
        inputPanel.add(prestationField);
        inputPanel.add(new JLabel("Tarif (€)"));
        inputPanel.add(tarifField);
        inputPanel.add(new JLabel("Quantité"));

        // Row 2: qty field + 3 buttons + filler
        inputPanel.add(qtyField);
        JButton addButton  = new JButton("Add Row");
        JButton loadButton = new JButton("Load");
        JButton exportButton = new JButton("Export to PDF");
        inputPanel.add(addButton);
        inputPanel.add(loadButton);
        inputPanel.add(exportButton);
        inputPanel.add(new JLabel()); // filler cell

        frame.add(inputPanel, BorderLayout.SOUTH);

        // — Add Row logic —
        addButton.addActionListener(e -> {
            String p = prestationField.getText().trim();
            String t = tarifField.getText().trim();
            String q = qtyField.getText().trim();
            if (p.isEmpty() || t.isEmpty() || q.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please fill in all fields.");
                return;
            }
            try {
                double tarif = Double.parseDouble(t);
                int qty = Integer.parseInt(q);
                model.addRow(new Object[]{p, tarif, qty, tarif * qty});
                prestationField.setText("");
                tarifField.setText("");
                qtyField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Enter valid numbers for Tarif and Quantité.");
            }
        });

        // — Load from DB logic —
        loadButton.addActionListener(e -> loadData());

        // — Export to PDF logic —
        exportButton.addActionListener(e -> exportPDF(frame));

        frame.setVisible(true);
    }

    /**
     * Fetches active client names from client_main and
     * builds a JComboBox.
     */
    private static JComboBox<String> createCompanyComboBox() {
        List<String> names = new ArrayList<>();
        String sql =
                "SELECT client_name FROM client_main " +
                        "WHERE soft_delete IS NULL OR soft_delete = 0 " +
                        "ORDER BY client_name";
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
     * Creates a JSpinner as a date picker (calendar pop-up).
     */
    private static JSpinner makeDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel(
                new Date(), null, null, java.util.Calendar.DAY_OF_MONTH
        );
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
        spinner.setEditor(editor);
        return spinner;
    }

    /**
     * Clears the table and loads rows from bill_main filtered by
     * the selected company as CityServiced and the date range.
     * Computes the correct tarif (per hour vs. per day) using client_main.
     */
    private static void loadData() {
        model.setRowCount(0);  // clear existing rows

        String company = (String) companyComboBox.getSelectedItem();
        Date fromDate  = ((SpinnerDateModel) fromDateSpinner.getModel()).getDate();
        Date toDate    = ((SpinnerDateModel) toDateSpinner.getModel()).getDate();

        String rateSql =
                "SELECT client_rate, client_rate_per_day " +
                        "FROM client_main WHERE client_name = ?";
        String billSql =
                "SELECT service_rendered, UnitDay, workedDayOrHours " +
                        "FROM bill_main " +
                        "WHERE CityServiced = ? " +
                        "  AND date_worked BETWEEN ? AND ?";

        try (Connection conn = MySQLConnector.getConnection();
             PreparedStatement psRate = conn.prepareStatement(rateSql)) {

            // 1) fetch rates
            psRate.setString(1, company);
            double rate, ratePerDay;
            try (ResultSet rs = psRate.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(null,
                            "No rate info for " + company);
                    return;
                }
                rate        = rs.getDouble("client_rate");
                ratePerDay  = rs.getDouble("client_rate_per_day");
            }

            // 2) fetch bills
            try (PreparedStatement psBill = conn.prepareStatement(billSql)) {
                psBill.setString(1, company);
                psBill.setTimestamp(2, new Timestamp(fromDate.getTime()));
                psBill.setTimestamp(3, new Timestamp(toDate.getTime()));

                try (ResultSet rs2 = psBill.executeQuery()) {
                    while (rs2.next()) {
                        String service = rs2.getString("service_rendered");
                        int unitDay    = rs2.getInt("UnitDay");
                        int qty        = rs2.getInt("workedDayOrHours");

                        double tarif = (unitDay == 1) ? ratePerDay : rate;
                        model.addRow(new Object[]{
                                service, tarif, qty, tarif * qty
                        });
                    }
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error loading data: " + ex.getMessage());
        }
    }

    /**
     * Opens a save dialog and dumps the table + header info into a PDF.
     */
    private static void exportPDF(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Invoice as PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String path = chooser.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".pdf")) {
            path += ".pdf";
        }

        // Header info
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
            document.add(new Paragraph(
                    "Période : " + sdf.format(fromDate) + " – " + sdf.format(toDate)
            ));
            document.add(new Paragraph(" "));

            // Build PDF table
            PdfPTable pdfTable = new PdfPTable(4);
            pdfTable.setWidths(new int[]{3,2,2,2});
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
