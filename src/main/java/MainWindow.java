import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainWindow {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Business Tools");

//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Prevent immediate closing
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    int result = JOptionPane.showConfirmDialog(
                            frame,
                            "Are you sure you want to quit?",
                            "Confirm Exit",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );

                    if (result == JOptionPane.YES_OPTION) {
                        frame.dispose(); // close window
                        System.exit(0);  // terminate app
                    }
                }
            });

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
