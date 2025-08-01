

import UI.CreateComponents;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RateManagerPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    private JComboBox<String> clientCombo;
    private List<Integer> clientIds = new ArrayList<>();

    private JComboBox<String> languageField;
    private JTextField rateHourField;
    private JTextField rateDayField;
    private JTextField offsetByField;
    private JTextField weekendField;
    private JTextField offsetUnitField;
    private JSpinner rateAppliedDate;

    private JButton addBtn, updateBtn, deleteBtn, refreshBtn;

    public RateManagerPanel() {
        setLayout(new BorderLayout(5, 5));

        // ── Table ──
        model = new DefaultTableModel(new String[]{
                "ClientID", "Client Name", "Language",
                "Rate/Hour", "Rate/Day", "OffsetBy", "Weekend",
                "OffsetUnit", "InsertDate", "UpdateDate", "Rate Applied Date"
        }, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.NORTH);

        // ── Form ──
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        clientCombo = new JComboBox<>();
        languageField = new JComboBox<>();
        rateHourField = new JTextField();
        rateDayField = new JTextField();
        offsetByField = new JTextField();
        weekendField = new JTextField();
        offsetUnitField = new JTextField();
        rateAppliedDate = CreateComponents.makeDateSpinner();

        form.add(new JLabel("Client:"));
        form.add(clientCombo);
        form.add(new JLabel("Language:"));
        form.add(languageField);
        form.add(new JLabel("Rate per Hour:"));
        form.add(rateHourField);
        form.add(new JLabel("Rate per Day:"));
        form.add(rateDayField);
        form.add(new JLabel("Offset By (int):"));
        form.add(offsetByField);
        form.add(new JLabel("Weekend (text):"));
        form.add(weekendField);
        form.add(new JLabel("OffsetUnit (int):"));
        form.add(offsetUnitField);
        form.add(new JLabel("Rate Applied Date:"));
        form.add(rateAppliedDate);

        add(form, BorderLayout.CENTER);

        // ── Buttons ──
        JPanel buttons = new JPanel();
        addBtn = new JButton("Add");
        updateBtn = new JButton("Update");
        deleteBtn = new JButton("Delete");
        refreshBtn = new JButton("Refresh");
        buttons.add(addBtn);
        buttons.add(updateBtn);
        buttons.add(deleteBtn);
        buttons.add(refreshBtn);
        add(buttons, BorderLayout.SOUTH);

        // ── Wire actions ──
        refreshBtn.addActionListener(e -> loadAll());
        addBtn.addActionListener(e -> insertRate());
        updateBtn.addActionListener(e -> updateRate());
        deleteBtn.addActionListener(e -> deleteRate());

        table.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> fillForm());

        loadAll();
    }

    private void loadAll() {
        loadClients();
        loadLanguages();
        refreshTable();
    }

    private void loadClients() {
        clientIds.clear();
        clientCombo.removeAllItems();
        String sql = "SELECT idclient_main, client_name FROM client_main WHERE COALESCE(soft_delete,0)=0";
        try (Connection c = MySQLConnector.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                clientIds.add(rs.getInt(1));
                clientCombo.addItem(rs.getString(2));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadLanguages() {
        languageField.removeAllItems();
        String sql = "SELECT " +
                "    lang " +
                "FROM language_main";
        try (Connection c = MySQLConnector.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                languageField.addItem(rs.getString(1));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void refreshTable() {
        model.setRowCount(0);
        String sql =
                "SELECT r.client_id, c.client_name, r.language, r.rate_per_hour, r.rate_per_day, " +
                        "r.offsetBy, r.weekend, r.offsetUnit, " +
                        " r.insert_date, r.update_date, r.rate_apply_date_from " +
                        " FROM rate_main r " +
                        " JOIN client_main c ON r.client_id=c.idclient_main";
        try (Connection c = MySQLConnector.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getDouble(4),
                        rs.getDouble(5),
                        rs.getInt(6),
                        rs.getString(7),
                        rs.getInt(8),
                        rs.getTimestamp(9),
                        rs.getTimestamp(10),
                        rs.getDate(11)
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void insertRate() {
        String sql =
                "INSERT INTO rate_main " +
                        "(client_id, language, rate_per_hour, rate_per_day, offsetBy, weekend, offsetUnit, rate_apply_date_from, insert_date) " +
                        "VALUES (?,?,?,?,?,?,?,?,NOW())";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, clientIds.get(clientCombo.getSelectedIndex()));
            ps.setString(2, languageField.getItemAt(languageField.getSelectedIndex()));
            ps.setDouble(3, Double.parseDouble(rateHourField.getText().trim()));
            ps.setDouble(4, Double.parseDouble(rateDayField.getText().trim()));
            ps.setInt(5, Integer.parseInt(offsetByField.getText().trim()));
            ps.setString(6, weekendField.getText().trim());
            ps.setInt(7, Integer.parseInt(offsetUnitField.getText().trim()));
            ps.setDate(8, new java.sql.Date(((Date) rateAppliedDate.getValue()).getTime()));

            ps.executeUpdate();
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Insert failed: " + ex.getMessage());
        }
    }

    private void updateRate() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int clientId = (int) model.getValueAt(row, 0);
        String lang = model.getValueAt(row, 2).toString();
        String rateApplyDate = model.getValueAt(row, 10).toString();
        String sql =
                "UPDATE rate_main SET " +
                        " rate_per_hour=?, rate_per_day=?, offsetBy=?, weekend=?, offsetUnit=?," +
                        " rate_apply_date_from=?, " +
                        " update_date=NOW() " +
                        " WHERE client_id=? AND language=? AND rate_apply_date_from='" + rateApplyDate + "'";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDouble(1, Double.parseDouble(rateHourField.getText().trim()));
            ps.setDouble(2, Double.parseDouble(rateDayField.getText().trim()));
            ps.setInt(3, Integer.parseInt(offsetByField.getText().trim()));
            ps.setString(4, weekendField.getText().trim());
            ps.setInt(5, Integer.parseInt(offsetUnitField.getText().trim()));
            ps.setDate(6, new java.sql.Date(((Date) rateAppliedDate.getValue()).getTime()));
            ps.setInt(7, clientId);
            ps.setString(8, lang);

            ps.executeUpdate();
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Update failed: " + ex.getMessage());
        }
    }

    private void deleteRate() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int clientId = (int) model.getValueAt(row, 0);
        String lang = model.getValueAt(row, 2).toString();

        String sql = "DELETE FROM rate_main WHERE client_id=? AND language=? AND rate_apply_date_from=?";

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
            ps.setInt(1, clientId);
            ps.setString(2, lang);
            ps.setDate(3, new java.sql.Date(((Date) rateAppliedDate.getValue()).getTime()));
            ps.executeUpdate();
            refreshTable();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage());
        }
    }

    private void fillForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        Object dateObj = model.getValueAt(row, 10);
        int clientId = (int) model.getValueAt(row, 0);
        clientCombo.setSelectedIndex(clientIds.indexOf(clientId));
        languageField.setSelectedItem(model.getValueAt(row, 2).toString());
        rateHourField.setText(model.getValueAt(row, 3).toString());
        rateDayField.setText(model.getValueAt(row, 4).toString());
        offsetByField.setText(model.getValueAt(row, 5).toString());
        weekendField.setText(model.getValueAt(row, 6).toString());
        offsetUnitField.setText(model.getValueAt(row, 7).toString());
        rateAppliedDate.setValue(CreateComponents.setDateSpinnerValue(rateAppliedDate, dateObj));
    }
}
