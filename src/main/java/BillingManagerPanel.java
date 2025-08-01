import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.List;

public class BillingManagerPanel {
    // Public components accessed by PDFCreator
    public static JTable table;
    public static DefaultTableModel model;
    public static JTextField prestationField, tarifField, qtyField, cityWorkedForField;
    public static JComboBox<String> companyComboBox;
    public static JSpinner fromDateSpinner, toDateSpinner, billedOnSpinner;
    public static String clientAdd, languageInterpret, date_worked;
    public static JCheckBox ignoreDateCheckbox;
    public static JCheckBox ignorePaidCheckbox;
    public static final String myaddress = ConfigLoader.get("db.address");
    public static final int width = 900, height = 600;
    public static JCheckBox exportDayBill = new JCheckBox("Per Day Billing");;

//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(InvoiceApp::createAndShowGUI);
//    }

    // public static String billNo = System.currentTimeMillis() + "";
    public static HashMap<Integer, String> billIds;


    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Billing Software");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.add(getInvoicePanel());
        frame.setVisible(true);
    }

    /**
     * Builds and returns the full invoice panel.
     */
    public static JPanel getInvoicePanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        // 🔝 Top panel: company selector and date range
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

        JButton loadButton = new JButton("Load");
        filterPanel.add(loadButton);

        filterPanel.add(new JLabel("Billed On:"));
        billedOnSpinner = makeDateSpinner();
        filterPanel.add(billedOnSpinner);

        mainPanel.add(filterPanel, BorderLayout.NORTH);

        // 📋 Center: invoice table
        model = new DefaultTableModel(
                new Object[]{"Prestation", "Tarif (€)", "Quantité", "Total (€)", "Date Worked", "Language"},
                0
        );
        table = new JTable(model);
        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // 🔘 Bottom panel: manual entry and export controls
        prestationField = new JTextField();
        tarifField = new JTextField();
        qtyField = new JTextField();
        cityWorkedForField = new JTextField();

        JPanel inputPanel = new JPanel(new GridLayout(1, 6, 10, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        inputPanel.add(new JLabel("Prestation"));
        inputPanel.add(prestationField);
        inputPanel.add(new JLabel("Tarif (€)"));
        inputPanel.add(tarifField);
        inputPanel.add(new JLabel("Quantité"));
        inputPanel.add(qtyField);

        JButton addButton = new JButton("Add");
        JButton exportButton = new JButton("Export");
        inputPanel.add(addButton);
        inputPanel.add(exportButton);
        inputPanel.add(exportDayBill); // filler

        inputPanel.add(new JLabel("City Worked For:"));
        inputPanel.add(cityWorkedForField);

        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        // ➕ Add row manually
        addButton.addActionListener(e -> {
            String service = prestationField.getText().trim();
            String tarifText = tarifField.getText().trim();
            String qtyText = qtyField.getText().trim();
            if (service.isEmpty() || tarifText.isEmpty() || qtyText.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill all fields.");
                return;
            }
            try {
                double tarif = Double.parseDouble(tarifText);
                int qty = Integer.parseInt(qtyText);
                model.addRow(new Object[]{service, tarif, qty, tarif * qty});
                prestationField.setText("");
                tarifField.setText("");
                qtyField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Tarif and Quantité must be numeric.");
            }
        });

        // 🔄 Load rows from database
        loadButton.addActionListener(e -> {
            billIds = InvoiceDataLoader.loadInvoiceData(
                    (String) companyComboBox.getSelectedItem(),
                    ((SpinnerDateModel) fromDateSpinner.getModel()).getDate(),
                    ((SpinnerDateModel) toDateSpinner.getModel()).getDate(),
                    ignoreDateCheckbox.isSelected(),
                    ignorePaidCheckbox.isSelected(),
                    model);

        });

        // 📤 Export to PDF
        exportButton.addActionListener(e -> PDFCreator.exportPDF(
                null,
                System.currentTimeMillis() + "",
                clientAdd,
                myaddress,
                ((SpinnerDateModel) billedOnSpinner.getModel()).getDate(), billIds
        ));

        return mainPanel;
    }

    /**
     * Populates company dropdown from client_main table.
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

        if (names.isEmpty()) {
            names.add("<< no companies found >>");
        }
        return new JComboBox<>(names.toArray(new String[0]));
    }

    /**
     * Creates a date spinner using yyyy-MM-dd format.
     */
    private static JSpinner makeDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        return spinner;
    }
}
