

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class LanguageManagerPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    private JTextField languageField;
    private JButton addBtn, updateBtn, deleteBtn, refreshBtn;

    public LanguageManagerPanel() {
        setLayout(new BorderLayout(5,5));

        // ── Table Setup ──
        model = new DefaultTableModel(new String[]{"ID", "Language"}, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ── Entry Form ──
        JPanel form = new JPanel(new GridLayout(1, 2, 5, 5));
        form.add(new JLabel("Language:"));
        languageField = new JTextField();
        form.add(languageField);
        add(form, BorderLayout.NORTH);

        // ── Action Buttons ──
        JPanel btnPanel = new JPanel();
        addBtn     = new JButton("Add");
        updateBtn  = new JButton("Update");
        deleteBtn  = new JButton("Delete");
        refreshBtn = new JButton("Refresh");
        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(refreshBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // ── Wire Actions ──
        refreshBtn.addActionListener(e -> refreshTable());
        addBtn    .addActionListener(e -> insertLanguage());
        updateBtn .addActionListener(e -> updateLanguage());
        deleteBtn .addActionListener(e -> deleteLanguage());

        table.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) fillForm();
        });

        // Initial load
        refreshTable();
    }

    // Reloads all rows from language_main
    private void refreshTable() {
        model.setRowCount(0);
        String sql = "SELECT lang_id, lang FROM language_main";
        try (Connection c = MySQLConnector.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("lang_id"),
                        rs.getString("lang")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Refresh failed: " + ex.getMessage());
        }
    }

    // Inserts a new language
    private void insertLanguage() {
        String sql = "INSERT INTO language_main (lang) VALUES (?)";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, languageField.getText().trim());
            ps.executeUpdate();
            refreshTable();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Insert failed: " + ex.getMessage());
        }
    }

    // Updates the selected language
    private void updateLanguage() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        int id = (int) model.getValueAt(row, 0);
        String sql = "UPDATE language_main SET lang = ? WHERE lang_id = ?";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, languageField.getText().trim());
            ps.setInt(2, id);
            ps.executeUpdate();
            refreshTable();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Update failed: " + ex.getMessage());
        }
    }

    // Deletes the selected language
    private void deleteLanguage() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        int id = (int) model.getValueAt(row, 0);
        String sql = "DELETE FROM language_main WHERE lang_id = ?";
        try (Connection c = MySQLConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
            refreshTable();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage());
        }
    }

    // Prefills the form when a row is selected
    private void fillForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        languageField.setText(model.getValueAt(row, 1).toString());
    }
}
