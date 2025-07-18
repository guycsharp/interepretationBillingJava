import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BillManagerPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private JTextField serviceField, unitDayField, workedField, cityField, languageField, billNoField;
    private JSpinner dateWorkedSpinner, insertDateSpinner, updatedDateSpinner;
    private JCheckBox paidCheck;
    private JComboBox<String> clientCombo;
    private List<Integer> clientIds = new ArrayList<>();
    private JButton addBtn, updateBtn, deleteBtn, refreshBtn;

    public BillManagerPanel() {
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
                new String[]{
                        "ID","Service","UnitDay","Worked","City","DateWorked",
                        "Paid","Lang","BillNo","ClientID"
                }, 0
        );
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Form
        JPanel form = new JPanel(new GridLayout(9,2,5,5));
        serviceField        = new JTextField();
        unitDayField        = new JTextField();
        workedField         = new JTextField();
        cityField           = new JTextField();
        languageField       = new JTextField();
        billNoField         = new JTextField();
        dateWorkedSpinner   = new JSpinner(new SpinnerDateModel());
        insertDateSpinner   = new JSpinner(new SpinnerDateModel());
        updatedDateSpinner  = new JSpinner(new SpinnerDateModel());
        paidCheck           = new JCheckBox("Paid");
        clientCombo         = new JComboBox<>();

        form.add(new JLabel("Service:"));       form.add(serviceField);
        form.add(new JLabel("UnitDay:"));       form.add(unitDayField);
        form.add(new JLabel("Worked:"));        form.add(workedField);
        form.add(new JLabel("City:"));          form.add(cityField);
        form.add(new JLabel("Date Worked:"));   form.add(dateWorkedSpinner);
        form.add(new JLabel("Paid:"));          form.add(paidCheck);
        form.add(new JLabel("Language:"));      form.add(languageField);
        form.add(new JLabel("Bill No:"));       form.add(billNoField);
        form.add(new JLabel("Client:"));        form.add(clientCombo);

        add(form, BorderLayout.SOUTH);

        // Handlers
        refreshBtn.addActionListener(e -> loadAll());
        addBtn    .addActionListener(e -> insertBill());
        updateBtn .addActionListener(e -> updateBill());
        deleteBtn .addActionListener(e -> deleteBill());

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) { fillForm(); }
        });

        loadAll();
    }

    private void loadAll() {
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
            while(rs.next()) {
                clientIds.add(rs.getInt(1));
                clientCombo.addItem(rs.getString(2));
            }
        } catch (SQLException ex){ ex.printStackTrace(); }
    }

    private void refreshTable() {
        model.setRowCount(0);
        String sql = "SELECT idbill_main, service_rendered, UnitDay, workedDayOrHours, CityServiced, " +
                "date_worked, paid, language, bill_no, client_id FROM bill_main";
        try (Connection c = MySQLConnector.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while(rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getInt(3),
                        rs.getInt(4),
                        rs.getString(5),
                        rs.getTimestamp(6),
                        rs.getInt(7)==1,
                        rs.getString(8),
                        rs.getBigDecimal(9),
                        rs.getInt(10)
                });
            }
        } catch (SQLException ex){ ex.printStackTrace(); }
    }

    private void insertBill() {
        String sql = "INSERT INTO bill_main " +
                "(service_rendered, UnitDay, workedDayOrHours, CityServiced, date_worked, paid, language, bill_no, client_id) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, serviceField.getText());
            ps.setInt   (2, Integer.parseInt(unitDayField.getText()));
            ps.setInt   (3, Integer.parseInt(workedField.getText()));
            ps.setString(4, cityField.getText());
            ps.setTimestamp(5, new java.sql.Timestamp(((java.util.Date)dateWorkedSpinner.getValue()).getTime()));
            ps.setInt   (6, paidCheck.isSelected()?1:0);
            ps.setString(7, languageField.getText());
            ps.setBigDecimal(8, new java.math.BigDecimal(billNoField.getText()));
            ps.setInt(9, clientIds.get(clientCombo.getSelectedIndex()));
            ps.executeUpdate();
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Insert failed: "+ex.getMessage());
        }
    }

    private void updateBill() {
        int row = table.getSelectedRow();
        if (row<0) return;
        int id = (int) model.getValueAt(row, 0);

        String sql = "UPDATE bill_main SET service_rendered=?, UnitDay=?, workedDayOrHours=?, " +
                "CityServiced=?, date_worked=?, paid=?, language=?, bill_no=?, client_id=? WHERE idbill_main=?";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, serviceField.getText());
            ps.setInt   (2, Integer.parseInt(unitDayField.getText()));
            ps.setInt   (3, Integer.parseInt(workedField.getText()));
            ps.setString(4, cityField.getText());
            ps.setTimestamp(5, new java.sql.Timestamp(((Date)dateWorkedSpinner.getValue()).getTime()));
            ps.setInt   (6, paidCheck.isSelected()?1:0);
            ps.setString(7, languageField.getText());
            ps.setBigDecimal(8, new java.math.BigDecimal(billNoField.getText()));
            ps.setInt   (9, clientIds.get(clientCombo.getSelectedIndex()));
            ps.setInt   (10, id);
            ps.executeUpdate();
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Update failed: "+ex.getMessage());
        }
    }

    private void deleteBill() {
        int row = table.getSelectedRow();
        if (row<0) return;
        int id = (int) model.getValueAt(row, 0);
        String sql = "DELETE FROM bill_main WHERE idbill_main=?";
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
        serviceField       .setText(model.getValueAt(row,1).toString());
        unitDayField       .setText(model.getValueAt(row,2).toString());
        workedField        .setText(model.getValueAt(row,3).toString());
        cityField          .setText(model.getValueAt(row,4).toString());
        dateWorkedSpinner  .setValue(model.getValueAt(row,5));
        paidCheck          .setSelected((boolean)model.getValueAt(row,6));
        languageField      .setText(model.getValueAt(row,7).toString());
        billNoField        .setText(model.getValueAt(row,8).toString());
        int clientId       = (int) model.getValueAt(row,9);
        clientCombo.setSelectedIndex(clientIds.indexOf(clientId));
    }
}
