// InvoiceApp.java
// GUI-based billing application with PDF export using iText and MySQL integration

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

// iText PDF imports
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

public class InvoiceApp {
    // 👇 Declare table and form components as static for shared access
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

        // 🔝 Top panel: company selector and date pickers
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.add(new JLabel("Company:"));
        companyComboBox = createCompanyComboBox(); // 🏢 Dropdown populated from DB
        filterPanel.add(companyComboBox);

        filterPanel.add(new JLabel("From:"));
        fromDateSpinner = makeDateSpinner();       // 📅 JSpinner for start date
        filterPanel.add(fromDateSpinner);

        filterPanel.add(new JLabel("To:"));
        toDateSpinner = makeDateSpinner();         // 📅 JSpinner for end date
        filterPanel.add(toDateSpinner);

        frame.add(filterPanel, BorderLayout.NORTH);

        // 📊 Main invoice table with four columns
        model = new DefaultTableModel(
                new Object[]{"Prestation", "Tarif (€)", "Quantité", "Total (€)"}, 0
        );
        table = new JTable(model);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // 🧮 Bottom panel: manual entry + control buttons
        prestationField = new JTextField();
        tarifField       = new JTextField();
        qtyField         = new JTextField();

        JPanel inputPanel = new JPanel(new GridLayout(2, 5, 10, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Row 1: input labels and fields
        inputPanel.add(new JLabel("Prestation"));
        inputPanel.add(prestationField);
        inputPanel.add(new JLabel("Tarif (€)"));
        inputPanel.add(tarifField);
        inputPanel.add(new JLabel("Quantité"));

        // Row 2: fields and buttons
        inputPanel.add(qtyField);
        JButton addButton    = new JButton("Add Row");      // ➕ Manual add
        JButton loadButton   = new JButton("Load");         // 🔄 Load from DB
        JButton exportButton = new JButton("Export to PDF");// 📤 Export to PDF
        inputPanel.add(addButton);
        inputPanel.add(loadButton);
        inputPanel.add(exportButton);
        inputPanel.add(new JLabel()); // filler

        frame.add(inputPanel, BorderLayout.SOUTH);

        // ➕ Manual row add logic
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

        // 🔄 Load data from database based on selected company and date
        loadButton.addActionListener(e -> loadData());

        // 📤 Export table contents to PDF
        exportButton.addActionListener(e -> exportPDF(frame));

        frame.setVisible(true);
    }

    /**
     * Create a combo box populated with client names from the database.
     */
    private static JComboBox<String> createCompanyComboBox() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT client_name FROM client_main WHERE soft_delete IS NULL OR soft_delete = 0 ORDER BY client_name";
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
        if (names.isEmpty()) names.add("<< no companies found >>");
        return new JComboBox<>(names.toArray(new String[0]));
    }

    /**
     * Create a date picker using JSpinner.
     */
    private static JSpinner makeDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
        spinner.setEditor(editor);
        return spinner;
    }

    /**
     * Load data from bill_main for the selected company and date range.
     */


    private static void loadData() {
        // Clear previous table data
        model.setRowCount(0);

        // Get selected company and date range
        String company = (String) companyComboBox.getSelectedItem();
        Date fromDate  = ((SpinnerDateModel) fromDateSpinner.getModel()).getDate();
        Date toDate    = ((SpinnerDateModel) toDateSpinner.getModel()).getDate();

        // Queries
        String rateSql = "SELECT client_rate, client_rate_per_day, idclient_main FROM client_main WHERE client_name = ?";
        String billSql = "SELECT service_rendered, UnitDay, workedDayOrHours FROM bill_main WHERE client_id = ? AND date_worked BETWEEN ? AND ?";

        try (Connection conn = MySQLConnector.getConnection();
             PreparedStatement psRate = conn.prepareStatement(rateSql)) {

            // Step 1: Get rate info and client ID
            psRate.setString(1, company);
            double rate, ratePerDay;
            int clientId;

            try (ResultSet rs = psRate.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(null, "No rate info for " + company);
                    return;
                }
                rate        = rs.getDouble("client_rate");
                ratePerDay  = rs.getDouble("client_rate_per_day");
                clientId    = rs.getInt("idclient_main");
            }

            // Step 2: Get bill entries in date range
            try (PreparedStatement psBill = conn.prepareStatement(billSql)) {
                psBill.setInt(1, clientId);
                psBill.setTimestamp(2, new Timestamp(fromDate.getTime()));
                psBill.setTimestamp(3, new Timestamp(toDate.getTime()));

                try (ResultSet rs2 = psBill.executeQuery()) {
                    boolean anyRows = false;

                    while (rs2.next()) {
                        anyRows = true;

                        String service = rs2.getString("service_rendered");
                        int unitDay    = rs2.getInt("UnitDay");
                        int qty        = rs2.getInt("workedDayOrHours");
                        double tarif   = (unitDay == 1) ? ratePerDay : rate;
                        double total   = tarif * qty;

                        // Show debug popup
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null,
                                    "Service: " + service +
                                            "\nTarif: " + tarif +
                                            "\nQuantité: " + qty +
                                            "\nTotal: " + total,
                                    "Loaded Row",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        });

                        // Add to invoice table
                        model.addRow(new Object[]{service, tarif, qty, total});
                    }

                    if (!anyRows) {
                        JOptionPane.showMessageDialog(null,
                                "No billing data found for this company in the selected date range.");
                    }
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Database error: " + ex.getMessage(),
                    "SQL Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }


    /**
     * Exports the invoice table and header info to PDF.
     */
    private static void exportPDF(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Invoice as PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        String path = chooser.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".pdf")) path += ".pdf";

        String company = (String) companyComboBox.getSelectedItem();
        Date fromDate  = ((SpinnerDateModel) fromDateSpinner.getModel()).getDate();
        Date toDate    = ((SpinnerDateModel) toDateSpinner.getModel()).getDate();
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
            for (int i = 0; i < model.getRowCount(); i++) {
                pdfTable.addCell(model.getValueAt(i, 0).toString());
                pdfTable.addCell(model.getValueAt(i, 1).toString());
                pdfTable.addCell(model.getValueAt(i, 2).toString());
                pdfTable.addCell(model.getValueAt(i, 3).toString());
                subTotal += Double.parseDouble(model.getValueAt(i, 3).toString());
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
