import DOA.InvoiceData;
import DOA.InvoiceRow;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InvoiceApp {
    // Made public so PDFCreator can read them directly
    public static JTable table;
    public static DefaultTableModel model;
    public static JComboBox<String> companyComboBox;
    public static JSpinner fromDateSpinner, toDateSpinner;
    public static JTextField prestationField, tarifField, qtyField;
    public static JCheckBox ignoreDateCheckbox, ignorePaidCheckbox;
    public static double bill_no;
    public static String clientAdd;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(InvoiceApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Billing Software");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        // Top filter panel
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

        ignoreDateCheckbox = new JCheckBox("Ignore Date");
        filterPanel.add(ignoreDateCheckbox);

        ignorePaidCheckbox = new JCheckBox("Ignore Paid");
        filterPanel.add(ignorePaidCheckbox);

        frame.add(filterPanel, BorderLayout.NORTH);

        // Center table
        model = new DefaultTableModel(
                new Object[]{"Prestation", "Tarif (€)", "Quantité", "Total (€)", "Date", "Langue"},
                0
        );
        table = new JTable(model);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom input fields + buttons
        prestationField = new JTextField();
        tarifField      = new JTextField();
        qtyField        = new JTextField();

        JPanel inputPanel = new JPanel(new GridLayout(1, 6, 10, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        inputPanel.add(new JLabel("Prestation"));
        inputPanel.add(prestationField);
        inputPanel.add(new JLabel("Tarif (€)"));
        inputPanel.add(tarifField);
        inputPanel.add(new JLabel("Quantité"));
        inputPanel.add(qtyField);

        JButton addButton    = new JButton("Add Row");
        JButton loadButton   = new JButton("Load from DB");
        JButton exportButton = new JButton("Export to PDF");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.add(addButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(exportButton);

        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.add(buttonPanel, BorderLayout.PAGE_END);

        // Add row handler
        addButton.addActionListener(e -> {
            try {
                String service = prestationField.getText().trim();
                double tarif   = Double.parseDouble(tarifField.getText().trim());
                int qty        = Integer.parseInt(qtyField.getText().trim());
                model.addRow(new Object[]{service, tarif, qty, tarif * qty, "", ""});
                prestationField.setText("");
                tarifField.setText("");
                qtyField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter valid numbers.");
            }
        });

        // Load from DB handler
        loadButton.addActionListener(e -> {
            model.setRowCount(0);
            String company = (String) companyComboBox.getSelectedItem();
            Date fromDate  = ((SpinnerDateModel) fromDateSpinner.getModel()).getDate();
            Date toDate    = ((SpinnerDateModel) toDateSpinner.getModel()).getDate();

            try {
                InvoiceData data = InvoiceDataLoader.load(
                        company, fromDate, toDate,
                        ignoreDateCheckbox.isSelected(),
                        ignorePaidCheckbox.isSelected()
                );
                bill_no   = data.getBillNo();
                clientAdd = data.getClientAddress();

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

        // Export to PDF handler (pass today's date as billedOn)
        exportButton.addActionListener(e ->
                PDFCreator.exportPDF(
                        frame,
                        bill_no + 1,
                        clientAdd,
                        "Your Company Address",
                        new Date()  // fifth argument to match PDFCreator signature
                )
        );

        frame.setVisible(true);
    }

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

    private static JSpinner makeDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel(
                new Date(), null, null, java.util.Calendar.DAY_OF_MONTH
        );
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        return spinner;
    }
}
