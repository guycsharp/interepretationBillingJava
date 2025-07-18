import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class PhoneManagerPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> clientCombo;
    private JTextField phoneField;
    private JButton addBtn, updateBtn, deleteBtn, refreshBtn;
    // Holds parallel array of client IDs
    private java.util.List<Integer> clientIds = new java.util.ArrayList<>();

    public PhoneManagerPanel() {
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
                new String[]{"ID","Client","Phone#"}, 0
        );
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Form
        JPanel form = new JPanel(new GridLayout(2,2,5,5));
        clientCombo = new JComboBox<>();
        phoneField  = new JTextField();
        form.add(new JLabel("Client:")); form.add(clientCombo);
        form.add(new JLabel("Phone#:")); form.add(phoneField);
        add(form, BorderLayout.SOUTH);

        // Wire actions
        refreshBtn.addActionListener(e -> refreshAll());
        addBtn    .addActionListener(e -> createPhone());
        updateBtn .addActionListener(e -> updatePhone());
        deleteBtn .addActionListener(e -> deletePhone());

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                fillForm();
            }
        });

        // Initial load
        refreshAll();
    }

    private void refreshAll() {
        loadClients();
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
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void refreshTable() {
        model.setRowCount(0);
        String sql = "SELECT p.idphone_main, c.client_name, p.phone_number " +
                "FROM phone_main p " +
                "LEFT JOIN client_main c ON p.client_id=c.idclient_main " +
                "WHERE COALESCE(p.soft_delete,0)=0";
        try (Connection c = MySQLConnector.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3)
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void createPhone() {
        int idx = clientCombo.getSelectedIndex();
        if (idx < 0) return;
        String sql = "INSERT INTO phone_main(client_id, phone_number) VALUES(?,?)";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt   (1, clientIds.get(idx));
            ps.setString(2, phoneField.getText());
            ps.executeUpdate();
            refreshTable();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Insert failed: "+ex.getMessage());
        }
    }

    private void updatePhone() {
        int row = table.getSelectedRow();
        int idx = clientCombo.getSelectedIndex();
        if (row<0 || idx<0) return;
        int id = (int) model.getValueAt(row, 0);
        String sql = "UPDATE phone_main SET client_id=?, phone_number=? WHERE idphone_main=?";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt   (1, clientIds.get(idx));
            ps.setString(2, phoneField.getText());
            ps.setInt   (3, id);
            ps.executeUpdate();
            refreshTable();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Update failed: "+ex.getMessage());
        }
    }

    private void deletePhone() {
        int row = table.getSelectedRow();
        if (row<0) return;
        int id = (int) model.getValueAt(row, 0);
        String sql = "UPDATE phone_main SET soft_delete=1 WHERE idphone_main=?";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            refreshTable();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Delete failed: "+ex.getMessage());
        }
    }

    private void fillForm() {
        int row = table.getSelectedRow();
        if (row<0) return;
        String clientName = (String)model.getValueAt(row,1);
        phoneField.setText(model.getValueAt(row,2).toString());
        clientCombo.setSelectedItem(clientName);
    }
}
