package ui;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * A Swing tab for inserting new billing entries into bill_main.
 * Follows the same pattern as DataLoader but in reverse.
 */
public class InputPanel extends JPanel {
    public InputPanel() {
        setLayout(new GridLayout(6, 2, 8, 8));
        JTextField clientIdField = new JTextField();
        JTextField serviceField  = new JTextField();
        JTextField unitDayField  = new JTextField();
        JTextField qtyField      = new JTextField();
        JTextField dateField     = new JTextField("YYYY-MM-DD");
        JTextField langField     = new JTextField();

        add(new JLabel("Client ID:"));    add(clientIdField);
        add(new JLabel("Service:"));      add(serviceField);
        add(new JLabel("UnitDay (0=hour/1=day):")); add(unitDayField);
        add(new JLabel("Qty or Hours:"));  add(qtyField);
        add(new JLabel("Date Worked:"));  add(dateField);
        add(new JLabel("Language:"));     add(langField);

        JButton saveBtn = new JButton("Save Entry");
        add(new JLabel()); // spacer
        add(saveBtn);

        saveBtn.addActionListener(e -> {
            String sql = "INSERT INTO bill_main"
                    + " (client_id, service_rendered, UnitDay, workedDayOrHours, date_worked, language)"
                    + " VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = MySQLConnector.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(clientIdField.getText()));
                ps.setString(2, serviceField.getText());
                ps.setInt(3, Integer.parseInt(unitDayField.getText()));
                ps.setInt(4, Integer.parseInt(qtyField.getText()));
                ps.setDate(5, java.sql.Date.valueOf(dateField.getText()));
                ps.setString(6, langField.getText());
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Entry saved!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });
    }
}