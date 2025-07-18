import DOA.InvoiceData;
import DOA.InvoiceRow;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * InvoiceApp is our Swing GUI:
 * 1) Lets user pick a client and date range.
 * 2) Loads invoice rows from the database.
 * 3) Allows manual row addition.
 * 4) Exports everything to PDF via PDFCreator.
 *
 * GUI fields used by PDFCreator are declared public static.
 */
public class InvoiceApp {
    // Public so PDFCreator can read them directly
    public static JComboBox<String> companyComboBox;
    public static JSpinner fromDateSpinner, toDateSpinner;
    public static DefaultTableModel model;

    // Internal fields for data storage
    private static JTable table;
    private static JCheckBox ignoreDateCheckbox, ignorePaidCheckbox;
    private static JTextField prestationField, tarifField, qtyField;
    private static double bill_no;
    private static String clientAdd;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(InvoiceApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        // Main window setup
        JFrame frame = new JFrame("Billing Software");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLayout(new BorderLayout(8, 8));

        // ── Top: Company & Date Filters ──
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        filter.add(new JLabel("Company:"));
        companyComboBox   = loadCompanyNames();
        filter.add(companyComboBox);

        filter.add(new JLabel("From:"));
        fromDateSpinner   = makeDateSpinner();
        filter.add(fromDateSpinner);

        filter.add(new JLabel("To:"));
        toDateSpinner     = makeDateSpinner();
        filter.add(toDateSpinner);

        ignoreDateCheckbox= new JCheckBox("Ignore Date");
        filter.add(ignoreDateCheckbox);

        ignorePaidCheckbox= new JCheckBox("Ignore Paid");
        filter.add(ignorePaidCheckbox);

        frame.add(filter, BorderLayout.NORTH);

        // ── Center: Invoice Table ──
        model = new DefaultTableModel(
                new Object[]{"Prestation","Tarif (€)","Quantité","Total (€)","Date","Langue"}, 0
        );
        table = new JTable(model);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // ── Bottom: Manual Input & Buttons ──
        JPanel input = new JPanel(new GridLayout(1, 6, 4, 4));
        prestationField = new JTextField();
        tarifField       = new JTextField();
        qtyField         = new JTextField();
        input.add(new JLabel("Prestation"));  input.add(prestationField);
        input.add(new JLabel("Tarif (€)"));   input.add(tarifField);
        input.add(new JLabel("Quantité"));    input.add(qtyField);

        JButton addBtn    = new JButton("Add Row");
        JButton loadBtn   = new JButton("Load from DB");
        JButton exportBtn = new JButton("Export to PDF");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        buttons.add(addBtn);
        buttons.add(loadBtn);
        buttons.add(exportBtn);

        frame.add(input,   BorderLayout.SOUTH);
        frame.add(buttons, BorderLayout.PAGE_END);

        // ➕ Add manual row handler
        addBtn.addActionListener(e -> {
            try {
                String svc = prestationField.getText().trim();
                double pr  = Double.parseDouble(tarifField.getText().trim());
                int q      = Integer.parseInt(qtyField.getText().trim());
                model.addRow(new Object[]{svc, pr, q, pr * q, "", ""});
                prestationField.setText("");
                tarifField.setText("");
                qtyField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter valid numbers.");
            }
        });

        // 🔄 Load from database handler
        loadBtn.addActionListener(e -> {
            model.setRowCount(0);
            String company = (String) companyComboBox.getSelectedItem();
            Date from      = ((SpinnerDateModel) fromDateSpinner.getModel()).getDate();
            Date to        = ((SpinnerDateModel) toDateSpinner.getModel()).getDate();

            try {
                InvoiceData data = InvoiceDataLoader.load(
                        company, from, to,
                        ignoreDateCheckbox.isSelected(),
                        ignorePaidCheckbox.isSelected()
                );

                // Store results for export
                bill_no   = data.getBillNo();
                clientAdd = data.getClientAddress();

                // Populate table
                for (InvoiceRow row : data.getRows()) {
                    model.addRow(new Object[]{
                            row.getService(),
                            row.getTarif(),
                            row.getQty(),
                            row.getTotal(),
                            row.getDateWorked(),
                            row.getLanguage()
                    });
                }

                if (data.getRows().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "No billing entries found.");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        frame,
                        "Error loading data: " + ex.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        // 📤 Export to PDF handler (pass today's date as billedOn)
        exportBtn.addActionListener(e ->
                PDFCreator.exportPDF(
                        frame,
                        bill_no + 1,
                        clientAdd,
                        "Your Company Address",
                        new Date()
                )
        );

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Queries client_main for all active client names,
     * returns them in a JComboBox.
     */
    private static JComboBox<String> loadCompanyNames() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT client_name FROM client_main "
                + "WHERE soft_delete IS NULL OR soft_delete = 0 "
                + "ORDER BY client_name";

        try (
                Connection conn = MySQLConnector.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                list.add(rs.getString("client_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            list.add("<< error loading companies >>");
        }

        if (list.isEmpty()) {
            list.add("<< no companies found >>");
        }
        return new JComboBox<>(list.toArray(new String[0]));
    }

    /**
     * Creates a JSpinner for dates using yyyy-MM-dd format.
     */
    private static JSpinner makeDateSpinner() {
        SpinnerDateModel m = new SpinnerDateModel(
                new Date(), null, null, java.util.Calendar.DAY_OF_MONTH
        );
        JSpinner spinner = new JSpinner(m);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        return spinner;
    }
}
