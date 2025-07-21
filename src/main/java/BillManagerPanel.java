import Utils.CombineDateTime;

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
    private JTextField serviceField, workedField, cityField;  // unitDayField;
    private JTextField languageField, billNoField, durationField;

    // Date/time pickers (spinners)
    private JSpinner startTimeSpinner, endTimeSpinner, dateWorkedSpinner;

    // Checkbox for 'paid' status and dropdown for clients
    private JCheckBox paidCheck, unitDayField;
    private JComboBox<String> clientCombo;
    private List<Integer> clientIds = new ArrayList<>(); // holds client IDs that match names

    // Buttons for user actions
    private JButton addBtn, updateBtn, deleteBtn, refreshBtn;

    // ───────────────────────────────────────────────────────────────
    // 🏗️ Constructor builds the form layout and sets behavior
    public BillManagerPanel() {
        setLayout(new BorderLayout(5, 5));  // give spacing between regions


        // ── Top section: table ──
        model = new DefaultTableModel(new String[]{
                "ID", "Service", "UnitDay", "City",
                "StartTime", "EndTime", "Duration", "DateWorked",
                "Paid", "Lang", "BillNo", "ClientID"
        }, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.NORTH);

        // ── Center section: entry form ──
        JPanel form = new JPanel(new GridLayout(13, 2, 5, 5));  // 13 rows, 2 columns
        serviceField = new JTextField();
        unitDayField = new JCheckBox("Per Day"); //new JTextField();
//        workedField   = new JTextField();
        cityField = new JTextField();
        startTimeSpinner = createSpinner("HH:mm");
        endTimeSpinner = createSpinner("HH:mm");
        durationField = new JTextField();
        dateWorkedSpinner = createSpinner("yyyy-MM-dd");
        paidCheck = new JCheckBox("Paid");
        languageField = new JTextField();
        billNoField = new JTextField();
        clientCombo = new JComboBox<>();

        // Add all form rows
        form.add(new JLabel("Service:"));
        form.add(serviceField);
        form.add(new JLabel("UnitDay (0/1):"));
        form.add(unitDayField);
//        form.add(new JLabel("Worked Hours/Days:")); form.add(workedField);
        form.add(new JLabel("City Serviced:"));
        form.add(cityField);
        form.add(new JLabel("Start Time:"));
        form.add(startTimeSpinner);
        form.add(new JLabel("End Time:"));
        form.add(endTimeSpinner);
        form.add(new JLabel("Duration (min):"));
        form.add(durationField);
        form.add(new JLabel("Date Worked:"));
        form.add(dateWorkedSpinner);
        form.add(new JLabel("Paid:"));
        form.add(paidCheck);
        form.add(new JLabel("Language:"));
        form.add(languageField);
        form.add(new JLabel("Bill No:"));
        form.add(billNoField);
        form.add(new JLabel("Client:"));
        form.add(clientCombo);

        add(form, BorderLayout.CENTER); // Add form to bottom

        // ── Top section: action buttons ──
        JPanel topBar = new JPanel();
        addBtn = new JButton("Add");
        updateBtn = new JButton("Update");
        deleteBtn = new JButton("Delete");
        refreshBtn = new JButton("Refresh");

        topBar.add(addBtn);
        topBar.add(updateBtn);
        topBar.add(deleteBtn);
        topBar.add(refreshBtn);
        add(topBar, BorderLayout.SOUTH);

        // ── Wire up button behavior ──
        refreshBtn.addActionListener(e -> loadAll());
        addBtn.addActionListener(e -> insertBill());
        updateBtn.addActionListener(e -> updateBill());
        deleteBtn.addActionListener(e -> deleteBill());

        // Table selection: when user clicks a row, fill form
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                fillForm();
            }
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
            while (rs.next()) {
                clientIds.add(rs.getInt(1));           // store client ID
                clientCombo.addItem(rs.getString(2));  // display name
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Refreshes the table from the database
    private void refreshTable() {
        model.setRowCount(0);  // clear current table
        String sql = "SELECT idbill_main, service_rendered, UnitDay, duration_in_minutes, CityServiced, " +
                "startTime, endTime, duration_in_minutes, date_worked, paid, language, bill_no, client_id FROM bill_main order by date_worked";
        try (Connection c = MySQLConnector.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt(1),         // ID
                        rs.getString(2),      // Service
                        rs.getInt(3),         // UnitDay
//                        rs.getDouble(4),      // Worked
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
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ➕ Inserts a new record into the bill_main table
    private void insertBill() {
        String sql = "INSERT INTO bill_main " +
                "(service_rendered, UnitDay, CityServiced, " +
                " startTime, endTime, duration_in_minutes, date_worked," +
                " paid, language, bill_no, client_id, insert_date, dayOfTheWeek) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, serviceField.getText().trim());
            ps.setInt(2, unitDayField.isSelected() ? 1 : 0);
            ps.setString(3, cityField.getText().trim());

            Timestamp startTime, endTime;
            startTime = new Timestamp(
                    CombineDateTime.mergeDateAndTime(
                            (Date) dateWorkedSpinner.getValue(),
                            (Date) startTimeSpinner.getValue()
                    ).getTime());
            endTime = new Timestamp(
                    CombineDateTime.mergeDateAndTime(
                            (Date) dateWorkedSpinner.getValue(),
                            (Date) endTimeSpinner.getValue()
                    ).getTime());
            ps.setTimestamp(4, startTime);
            ps.setTimestamp(5, endTime);

            double dur = CombineDateTime.calcDuration(startTime, endTime, durationField.getText().trim());

            java.sql.Date sqldate = new java.sql.Date(
                    ((Date) dateWorkedSpinner.getValue()).getTime()
            );
            ps.setDouble(6, dur);
            ps.setDate(7, sqldate);
            ps.setInt(8, paidCheck.isSelected() ? 1 : 0);
            ps.setString(9, languageField.getText().trim());
            ps.setBigDecimal(10, new java.math.BigDecimal(billNoField.getText().trim()));
            ps.setInt(11, clientIds.get(clientCombo.getSelectedIndex()));
            // Param 12: current timestamp
            ps.setTimestamp(12, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setString(13, CombineDateTime.getDayOfWeek(sqldate));

            ps.executeUpdate();
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Insert failed: " + ex.getMessage());
        }
    }


    // ✏️ Updates the selected record in bill_main
    // ✏️ This method updates the selected bill entry in the database
    private void updateBill() {
        // Step 1: Get the selected row from the table
        int row = table.getSelectedRow();

        // Step 2: If no row is selected, just return (nothing to update)
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a record to update.");
            return;
        }

        // Step 3: Extract the ID of the selected row — this is our target for the WHERE clause
        int id = (int) model.getValueAt(row, 0); // ID is in column 0 of the table

        // Step 4: Write the SQL update query
        // Note: It must exactly match the real table columns — no missing or extra columns!
        String sql = "UPDATE bill_main SET " +
                "service_rendered=?, UnitDay=?, CityServiced=?, " +
                "startTime=?, endTime=?, duration_in_minutes=?, date_worked=?, " +
                "paid=?, language=?, bill_no=?, client_id=?, updated_date=NOW() " +
                "WHERE idbill_main=?";

        // Step 5: Use JDBC to send the data to MySQL
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            // Step 6: Fill in each ? in the SQL query with values from the form
            ps.setString(1, serviceField.getText().trim());  // service_rendered
            ps.setInt(2, unitDayField.isSelected() ? 1 : 0); // UnitDay (checkbox, 1 for true, 0 for false)
            ps.setString(3, cityField.getText().trim());     // CityServiced

            // Combine date and time using your custom utility method
            // This merges the selected date with the time from the spinner
//            ps.setTimestamp(4, new Timestamp(
//                    CombineDateTime.mergeDateAndTime(
//                            (Date) dateWorkedSpinner.getValue(),    // date part
//                            (Date) startTimeSpinner.getValue()      // time part
//                    ).getTime()
//            ));
//            ps.setTimestamp(5, new Timestamp(
//                    CombineDateTime.mergeDateAndTime(
//                            (Date) dateWorkedSpinner.getValue(),    // date part
//                            (Date) endTimeSpinner.getValue()        // time part
//                    ).getTime()
//            ));

            Timestamp startTime, endTime;
            startTime = new Timestamp(
                    CombineDateTime.mergeDateAndTime(
                            (Date) dateWorkedSpinner.getValue(),
                            (Date) startTimeSpinner.getValue()
                    ).getTime());
            endTime = new Timestamp(
                    CombineDateTime.mergeDateAndTime(
                            (Date) dateWorkedSpinner.getValue(),
                            (Date) endTimeSpinner.getValue()
                    ).getTime());
            ps.setTimestamp(4, startTime);
            ps.setTimestamp(5, endTime);

            // Duration in minutes — parsed from the text field
            // ps.setDouble(6, Double.parseDouble(durationField.getText().trim()));
            double dur = CombineDateTime.calcDuration(startTime, endTime, durationField.getText().trim());

            java.sql.Date sqldate = new java.sql.Date(
                    ((Date) dateWorkedSpinner.getValue()).getTime()
            );

            // Duration in minutes — parsed from the text field
            ps.setDouble(6, dur);
            // Date only — stored separately in the database
            ps.setDate(7, sqldate);

            // Paid status (checkbox)
            ps.setInt(8, paidCheck.isSelected() ? 1 : 0);

            // Language field
            ps.setString(9, languageField.getText().trim());

            // Bill number (BigDecimal is used for precise numbers)
            ps.setBigDecimal(10, new java.math.BigDecimal(billNoField.getText().trim()));

            // Get selected client ID from the combo box
            ps.setInt(11, clientIds.get(clientCombo.getSelectedIndex()));

            // Set the WHERE clause target ID — very important!
            ps.setInt(12, id);

            // Step 7: Execute the update
            ps.executeUpdate();

            // Step 8: Refresh the table to show the new data
            refreshTable();

            // Optional: Show a confirmation
            // JOptionPane.showMessageDialog(this, "Record updated successfully!");

        } catch (Exception ex) {
            // If anything goes wrong, show the error to the user
            JOptionPane.showMessageDialog(this, "Update failed: " + ex.getMessage());
        }
    }


    // ❌ Deletes the selected record from bill_main
    private void deleteBill() {
        int row = table.getSelectedRow();
        if (row < 0) return; // nothing selected
        int id = (int) model.getValueAt(row, 0); // ID to delete

        String sql = "DELETE FROM bill_main WHERE idbill_main=?";

        // make sure user wants to delete and not clicked by mistake
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this record?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            // user clicked “No” or closed dialog
            return;
        }

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
    // 🪄 This method fills the input form with data from the selected table row
    private void fillForm() {
        // Step 1: Get the selected row index
        int row = table.getSelectedRow();

        // Step 2: If no row is selected, we do nothing
        if (row < 0) return;

        // Step 3: Fill each input field with the corresponding column from the table model

        // Service name (String)
        serviceField.setText(model.getValueAt(row, 1).toString());

        // UnitDay checkbox (0 or 1 in table)
        unitDayField.setSelected((int) model.getValueAt(row, 2) == 1);

        // City serviced (String)
        cityField.setText(model.getValueAt(row, 3).toString());

        // ─────────────────────────────────────────────────────────────
        // 🕒 Time/date fields need special care because they must be java.util.Date
        // Otherwise JSpinner throws an IllegalArgumentException
        Object startObj = model.getValueAt(row, 4); // startTime
        Object endObj = model.getValueAt(row, 5); // endTime
        Object dateObj = model.getValueAt(row, 7); // date_worked

        // Start time: convert Timestamp to Date
        if (startObj instanceof Timestamp) {
            startTimeSpinner.setValue(new Date(((Timestamp) startObj).getTime()));
        } else if (startObj instanceof Date) {
            startTimeSpinner.setValue(startObj);
        }

        // End time: same
        if (endObj instanceof Timestamp) {
            endTimeSpinner.setValue(new Date(((Timestamp) endObj).getTime()));
        } else if (endObj instanceof Date) {
            endTimeSpinner.setValue(endObj);
        }

        // Date worked: same logic
        if (dateObj instanceof Timestamp) {
            dateWorkedSpinner.setValue(new Date(((Timestamp) dateObj).getTime()));
        } else if (dateObj instanceof Date) {
            dateWorkedSpinner.setValue(dateObj);
        }

        // Duration (minutes), shown as text
        durationField.setText(model.getValueAt(row, 6).toString());

        // Paid checkbox (true or false as String or Boolean)
        paidCheck.setSelected(Boolean.parseBoolean(model.getValueAt(row, 8).toString()));

        // Language (String)
        languageField.setText(model.getValueAt(row, 9).toString());

        // Bill number (BigDecimal → shown as string)
        billNoField.setText(model.getValueAt(row, 10).toString());

        // Client: match the ID from the table back to the combo box selection
        int clientId = (int) model.getValueAt(row, 11);  // client_id column
        int index = clientIds.indexOf(clientId);         // find matching index in list
        if (index >= 0) {
            clientCombo.setSelectedIndex(index);
        }
    }

}
