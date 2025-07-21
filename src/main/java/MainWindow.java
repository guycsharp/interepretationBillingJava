import javax.swing.*;

public class MainWindow {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Business Tools");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);

            JTabbedPane tabbedPane = new JTabbedPane();

            // 🧾 Tab 1: Invoice App
            tabbedPane.addTab("Invoice Generator", BillingManagerPanel.getInvoicePanel());

            // ➕ Tab 2: Placeholder for future tools
            JPanel placeholderPanel = new JPanel();
            placeholderPanel.add(new JLabel("More tools coming soon..."));

            tabbedPane.addTab("Clients", new ClientManagerPanel());
            tabbedPane.addTab("Phones",  new PhoneManagerPanel());
            tabbedPane.addTab("Bills",   new BillManagerPanel());
            tabbedPane.addTab("Rates",   new RateManagerPanel());
            tabbedPane.addTab("Languages",   new LanguageManagerPanel());
            frame.add(tabbedPane);
            frame.setVisible(true);
        });
    }
}
