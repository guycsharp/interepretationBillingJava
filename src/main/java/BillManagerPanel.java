import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 🧾 BillManagerPanel is a UI module for managing entries in your `bill_main` table.
 * It includes:
 * - A table to display all entries
 * - A form to insert new bills
 * - Options to update and delete existing bills
 *
 * All fields directly match columns in your MySQL schema.
 */
public class BillManagerPanel extends JPanel {
    // Table and its model (for viewing records)
    private JTable table;
    private DefaultTableModel model;

    // Input fields for every column in the table
    private JTextField serviceField, unitDayField, workedField, cityField;
    private JTextField languageField, billNoField, durationField;

    // Date/time pickers (spinners)
    private JSpinner startTimeSpinner, endTimeSpinner, dateWorkedSpinner;

    // Checkbox for 'paid' status and dropdown for clients
    private JCheckBox paidCheck;
    private JComboBox<String> clientCombo;
    private List<Integer> clientIds = new ArrayList<>(); // holds client IDs that match names

    // Buttons for user actions
    private JButton addBtn, updateBtn, deleteBtn, refreshBtn;

    // ───────────────────────────────────────────────────────────────
    // 🏗️ Constructor builds the form layout and sets behavior
    public BillManagerPanel() {
        setLayout(new BorderLayout(5,5));  // give spacing between regions



        // ── Top section: table ──
        model = new DefaultTableModel(new String[]{
                "ID", "Service", "UnitDay", "Worked", "City",
                "StartTime", "EndTime", "Duration", "DateWorked",
                "Paid", "Lang", "BillNo", "ClientID"
        }, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.NORTH);

        // ── Center section: entry form ──
        JPanel form = new JPanel(new GridLayout(13, 2, 5, 5));  // 13 rows, 2 columns
        serviceField  = new JTextField();
        unitDayField  = new JTextField();
        workedField   = new JTextField();
        cityField     = new JTextField();
        startTimeSpinner   = createSpinner("yyyy-MM-dd HH:mm:ss");
        endTimeSpinner     = createSpinner("yyyy-MM-dd HH:mm:ss");
        durationField = new JTextField();
        dateWorkedSpinner  = createSpinner("yyyy-MM-dd");
        paidCheck     = new JCheckBox("Paid");
        languageField = new JTextField();
        billNoField   = new JTextField();
        clientCombo   = new JComboBox<>();

        // Add all form rows
        form.add(new JLabel("Service:"));         form.add(serviceField);
        form.add(new JLabel("UnitDay (0/1):"));   form.add(unitDayField);
        form.add(new JLabel("Worked Hours/Days:")); form.add(workedField);
        form.add(new JLabel("City Serviced:"));   form.add(cityField);
        form.add(new JLabel("Start Time:"));      form.add(startTimeSpinner);
        form.add(new JLabel("End Time:"));        form.add(endTimeSpinner);
        form.add(new JLabel("Duration (min):"));  form.add(durationField);
        form.add(new JLabel("Date Worked:"));     form.add(dateWorkedSpinner);
        form.add(new JLabel("Paid:"));            form.add(paidCheck);
        form.add(new JLabel("Language:"));        form.add(languageField);
        form.add(new JLabel("Bill No:"));         form.add(billNoField);
        form.add(new JLabel("Client:"));          form.add(clientCombo);

        add(form, BorderLayout.CENTER); // Add form to bottom

        // ── Top section: action buttons ──
        JPanel topBar = new JPanel();
        addBtn     = new JButton("Add");
        updateBtn  = new JButton("Update");
        deleteBtn  = new JButton("Delete");
        refreshBtn = new JButton("Refresh");

        topBar.add(addBtn);
        topBar.add(updateBtn);
        topBar.add(deleteBtn);
        topBar.add(refreshBtn);
        add(topBar, BorderLayout.SOUTH);

        // ── Wire up button behavior ──
        refreshBtn.addActionListener(e -> loadAll());
        addBtn    .addActionListener(e -> insertBill());
        updateBtn .addActionListener(e -> updateBill());
        deleteBtn .addActionListener(e -> deleteBill());

        // Table selection: when user clicks a row, fill form
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) { fillForm(); }
        });

        // Initial data load
        loadAll();
    }

    // Creates a date/time spinner with the given format
    private JSpinner createSpinner(String pattern) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        spinner.setEditor(new JSpinner.DateEditor(spinner, pattern));
        return spinner;
    }

    // Loads both client names and table data
    private void loadAll() {
        loadClients();    // load combo box
        refreshTable();   // load bill_main table
    }

    // Populates clientCombo and maps each name to its ID
    private void loadClients() {
        clientIds.clear();
        clientCombo.removeAllItems();
        String sql = "SELECT idclient_main, client_name FROM client_main WHERE COALESCE(soft_delete,0)=0";
        try (Connection c = MySQLConnector.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while(rs.next()) {
                clientIds.add(rs.getInt(1));           // store client ID
                clientCombo.addItem(rs.getString(2));  // display name
            }
        } catch (SQLException ex){ ex.printStackTrace(); }
    }

    // Refreshes the table from the database
    private void refreshTable() {
        model.setRowCount(0);  // clear current table
        String sql = "SELECT idbill_main, service_rendered, UnitDay, duration_in_minutes, CityServiced, " +
                "startTime, endTime, duration_in_minutes, date_worked, paid, language, bill_no, client_id FROM bill_main";
        try (Connection c = MySQLConnector.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while(rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt(1),         // ID
                        rs.getString(2),      // Service
                        rs.getInt(3),         // UnitDay
                        rs.getDouble(4),      // Worked
                        rs.getString(5),      // City
                        rs.getTimestamp(6),   // StartTime
                        rs.getTimestamp(7),   // EndTime
                        rs.getDouble(8),      // Duration
                        rs.getTimestamp(9),   // DateWorked
                        rs.getInt(10) == 1,   // Paid (as boolean)
                        rs.getString(11),     // Language
                        rs.getBigDecimal(12),// Bill No
                        rs.getInt(13)         // Client ID
                });
            }
        } catch (SQLException ex){ ex.printStackTrace(); }
    }

    // ➕ Inserts a new record into the bill_main table
    private void insertBill() {
        String sql = "INSERT INTO bill_main " +
                "(service_rendered, UnitDay, workedDayOrHours, CityServiced, " +
                "startTime, endTime, duration_in_minutes, date_worked, paid, language, bill_no, client_id, insert_date) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,NOW())";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, serviceField.getText().trim());
            ps.setInt(2, Integer.parseInt(unitDayField.getText().trim()));
            ps.setDouble(3, Double.parseDouble(workedField.getText().trim()));
            ps.setString(4, cityField.getText().trim());
            ps.setTimestamp(5, new Timestamp(((Date) startTimeSpinner.getValue()).getTime()));
            ps.setTimestamp(6, new Timestamp(((Date) endTimeSpinner.getValue()).getTime()));
            ps.setDouble(7, Double.parseDouble(durationField.getText().trim()));
            ps.setTimestamp(8, new Timestamp(((Date) dateWorkedSpinner.getValue()).getTime()));
            ps.setInt(9, paidCheck.isSelected() ? 1 : 0);
            ps.setString(10, languageField.getText().trim());
            ps.setBigDecimal(11, new java.math.BigDecimal(billNoField.getText().trim()));
            ps.setInt(12, clientIds.get(clientCombo.getSelectedIndex()));

            ps.executeUpdate();  // 🚀 Insert into database
            refreshTable();      // 🔄 Reload table
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Insert failed: " + ex.getMessage());
        }
    }

    // ✏️ Updates the selected record in bill_main
    private void updateBill() {
        int row = table.getSelectedRow();
        if (row < 0) return; // No row selected

        int id = (int) model.getValueAt(row, 0); // ID of selected row

        String sql = "UPDATE bill_main SET service_rendered=?, UnitDay=?, workedDayOrHours=?, " +
                "CityServiced=?, startTime=?, endTime=?, duration_in_minutes=?, date_worked=?, " +
                "paid=?, language=?, bill_no=?, client_id=?, updated_date=NOW() WHERE idbill_main=?";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, serviceField.getText().trim());
            ps.setInt(2, Integer.parseInt(unitDayField.getText().trim()));
            ps.setDouble(3, Double.parseDouble(workedField.getText().trim()));
            ps.setString(4, cityField.getText().trim());
            ps.setTimestamp(5, new Timestamp(((Date) startTimeSpinner.getValue()).getTime()));
            ps.setTimestamp(6, new Timestamp(((Date) endTimeSpinner.getValue()).getTime()));
            ps.setDouble(7, Double.parseDouble(durationField.getText().trim()));
            ps.setTimestamp(8, new Timestamp(((Date) dateWorkedSpinner.getValue()).getTime()));
            ps.setInt(9, paidCheck.isSelected() ? 1 : 0);
            ps.setString(10, languageField.getText().trim());
            ps.setBigDecimal(11, new java.math.BigDecimal(billNoField.getText().trim()));
            ps.setInt(12, clientIds.get(clientCombo.getSelectedIndex()));
            ps.setInt(13, id); // Target ID for update

            ps.executeUpdate();  // ✔ Update record
            refreshTable();      // 🔄 Refresh table view
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Update failed: " + ex.getMessage());
        }
    }

    // ❌ Deletes the selected record from bill_main
    private void deleteBill() {
        int row = table.getSelectedRow();
        if (row < 0) return; // nothing selected
        int id = (int) model.getValueAt(row, 0); // ID to delete

        String sql = "DELETE FROM bill_main WHERE idbill_main=?";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();  // 🗑 Delete from database
            refreshTable();      // 🔄 Refresh view
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage());
        }
    }

    // 🪄 When a row is selected, prefill the form with its data
    private void fillForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        serviceField      .setText(model.getValueAt(row, 1).toString());
        unitDayField      .setText(model.getValueAt(row, 2).toString());
        workedField       .setText(model.getValueAt(row, 3).toString());
        cityField         .setText(model.getValueAt(row, 4).toString());
        startTimeSpinner  .setValue(model.getValueAt(row, 5));
        endTimeSpinner    .setValue(model.getValueAt(row, 6));
        durationField     .setText(model.getValueAt(row, 7).toString());
        dateWorkedSpinner .setValue(model.getValueAt(row, 8));
        paidCheck         .setSelected((boolean) model.getValueAt(row, 9));
        languageField     .setText(model.getValueAt(row, 10).toString());
        billNoField       .setText(model.getValueAt(row, 11).toString());

        // 🧠 Reverse lookup: match clientId to combo index
        int clientId = (int) model.getValueAt(row, 12);
        clientCombo.setSelectedIndex(clientIds.indexOf(clientId));
    }
}
