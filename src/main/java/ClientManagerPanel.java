import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ClientManagerPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private JTextField nameField, addressField, rateField, ratePerDayField, phoneField;
    private JButton addBtn, updateBtn, deleteBtn, refreshBtn;

    public ClientManagerPanel() {
        setLayout(new BorderLayout(5,5));

        // Top buttons
        JPanel topBar = new JPanel();
        addBtn     = new JButton("Add");
        updateBtn  = new JButton("Update");
        deleteBtn  = new JButton("Delete");
        refreshBtn = new JButton("Refresh");
        topBar.add(addBtn);
        topBar.add(updateBtn);
        topBar.add(deleteBtn);
        topBar.add(refreshBtn);
        add(topBar, BorderLayout.NORTH);

        // Table
        model = new DefaultTableModel(
                new String[]{"ID","Name","Address","Rate","Rate/Day","Phone","Deleted"}, 0
        );
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Form
        JPanel form = new JPanel(new GridLayout(6,2,5,5));
        nameField       = new JTextField();
        addressField    = new JTextField();
        rateField       = new JTextField();
        ratePerDayField = new JTextField();
        phoneField      = new JTextField();

        form.add(new JLabel("Name:"));        form.add(nameField);
        form.add(new JLabel("Address:"));     form.add(addressField);
        form.add(new JLabel("Rate:"));        form.add(rateField);
        form.add(new JLabel("Rate/Day:"));    form.add(ratePerDayField);
        form.add(new JLabel("Phone #:"));     form.add(phoneField);

        add(form, BorderLayout.SOUTH);

        // Wire actions
        refreshBtn.addActionListener(e -> refreshTable());
        addBtn    .addActionListener(e -> createClient());
        updateBtn .addActionListener(e -> updateClient());
        deleteBtn .addActionListener(e -> deleteClient());

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                fillFormFromSelection();
            }
        });

        // Initial load
        refreshTable();
    }

    private void refreshTable() {
        model.setRowCount(0);
        String sql = "SELECT idclient_main, client_name, client_address, client_rate, " +
                "client_rate_per_day, phone_number, COALESCE(soft_delete,0) " +
                "FROM client_main";
        try (Connection c = MySQLConnector.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getInt(4),
                        rs.getInt(5),
                        rs.getString(6),
                        rs.getInt(7)
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void createClient() {
        String sql = "INSERT INTO client_main " +
                "(client_name, client_address, client_rate, client_rate_per_day, phone_number, language, insert_date) " +
                "VALUES (?,?,?,?,?,'na',?)";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nameField.getText());
            ps.setString(2, addressField.getText());
            ps.setInt   (3, Integer.parseInt(rateField.getText()));
            ps.setInt   (4, Integer.parseInt(ratePerDayField.getText()));
            ps.setString(5, phoneField.getText());
            ps.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Insert failed: "+ex.getMessage());
        }
    }

    private void updateClient() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) model.getValueAt(row, 0);

        String sql = "UPDATE client_main SET client_name=?, client_address=?, " +
                "client_rate=?, client_rate_per_day=?, phone_number=?, update_date=? WHERE idclient_main=?";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nameField.getText());
            ps.setString(2, addressField.getText());
            ps.setInt   (3, Integer.parseInt(rateField.getText()));
            ps.setInt   (4, Integer.parseInt(ratePerDayField.getText()));
            ps.setString(5, phoneField.getText());
            ps.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setInt   (7, id);
            ps.executeUpdate();
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Update failed: "+ex.getMessage());
        }
    }

    private void deleteClient() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) model.getValueAt(row, 0);

        String sql = "UPDATE client_main SET soft_delete=1 WHERE idclient_main=?";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            refreshTable();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Delete failed: "+ex.getMessage());
        }
    }

    private void fillFormFromSelection() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        nameField      .setText(model.getValueAt(row, 1).toString());
        addressField   .setText(model.getValueAt(row, 2).toString());
        rateField      .setText(model.getValueAt(row, 3).toString());
        ratePerDayField.setText(model.getValueAt(row, 4).toString());
        phoneField     .setText(model.getValueAt(row, 5).toString());
    }
}
