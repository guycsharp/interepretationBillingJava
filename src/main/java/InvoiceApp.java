// Import essential Swing components for GUI and file chooser
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;

// Import iText classes for PDF creation
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.File;
import java.io.FileOutputStream;

/**
 * InvoiceApp is a simple billing software GUI.
 *
 * In this version, when you click "Export to PDF",
 * a file-save dialog appears so you choose where to save your invoice.
 * Every step is commented in a teacher-style to help you learn.
 */
public class InvoiceApp {

    // GUI components declared at class level so all methods can use them
    private static JTable table;
    private static DefaultTableModel model;
    private static JTextField prestationField, tarifField, qtyField;

    public static void main(String[] args) {
        // We launch the GUI on the Event Dispatch Thread for thread safety in Swing
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    /**
     * Builds and shows the main application window.
     * This sets up the table, input fields, and buttons.
     */
    private static void createAndShowGUI() {
        // Create the main window frame
        JFrame frame = new JFrame("Billing Software");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
        frame.setLocationRelativeTo(null); // Center the window on the screen

        // Define the table model with four columns: service, price, quantity, total
        model = new DefaultTableModel(
                new Object[]{"Prestation", "Tarif (€)", "Quantité", "Total (€)"},
                0
        );
        table = new JTable(model);

        // Wrap the table in a scroll pane in case many rows are added
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // Prepare input fields for adding new rows
        prestationField = new JTextField();
        tarifField     = new JTextField();
        qtyField       = new JTextField();

        // Panel with a 2×4 grid: labels and fields in the first row, buttons in the second
        JPanel inputPanel = new JPanel(new GridLayout(2, 4));
        inputPanel.add(new JLabel("Prestation"));
        inputPanel.add(prestationField);
        inputPanel.add(new JLabel("Tarif (€)"));
        inputPanel.add(tarifField);
        inputPanel.add(new JLabel("Quantité"));
        inputPanel.add(qtyField);

        // Buttons to add a row or export the table to PDF
        JButton addButton    = new JButton("Add Row");
        JButton exportButton = new JButton("Export to PDF");
        inputPanel.add(addButton);
        inputPanel.add(exportButton);

        frame.add(inputPanel, BorderLayout.SOUTH);

        // When "Add Row" is clicked, validate inputs and add a new row to the table
        addButton.addActionListener(e -> {
            String prestation = prestationField.getText().trim();
            String tarifText   = tarifField.getText().trim();
            String qtyText     = qtyField.getText().trim();

            // Check for empty fields
            if (prestation.isEmpty() || tarifText.isEmpty() || qtyText.isEmpty()) {
                JOptionPane.showMessageDialog(
                        null,
                        "Please fill in all fields before adding."
                );
                return;
            }

            try {
                // Parse numeric inputs
                double tarif = Double.parseDouble(tarifText);
                int qty      = Integer.parseInt(qtyText);
                double total = tarif * qty;

                // Add the new row to the table model
                model.addRow(new Object[]{prestation, tarif, qty, total});

                // Clear input fields for the next entry
                prestationField.setText("");
                tarifField.setText("");
                qtyField.setText("");
            } catch (NumberFormatException ex) {
                // Show an error if parsing fails
                JOptionPane.showMessageDialog(
                        null,
                        "Enter valid numeric values for Tarif and Quantité."
                );
            }
        });

        // When "Export to PDF" is clicked, open a save dialog and generate the PDF
        exportButton.addActionListener(e -> exportPDF());

        // Finally, display the window
        frame.setVisible(true);
    }

    /**
     * Opens a file-save dialog, lets the user pick a location,
     * and writes the current table data into a PDF invoice.
     */
    public static void exportPDF() {
        // Step 1: Prompt user with a JFileChooser in SAVE mode
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Invoice as PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
        int userSelection = chooser.showSaveDialog(null);

        // If the user cancels, do nothing
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        // Step 2: Determine the selected file and ensure ".pdf" extension
        File file = chooser.getSelectedFile();
        String path = file.getAbsolutePath();
        if (!path.toLowerCase().endsWith(".pdf")) {
            path += ".pdf";
        }

        // Step 3: Build the PDF document using iText
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(path));
            document.open();

            // Add a centered title
            Paragraph title = new Paragraph(
                    "Facture",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)
            );
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ")); // Blank line for spacing

            // Create a table with the same number of columns as our JTable
            PdfPTable pdfTable = new PdfPTable(4);
            pdfTable.setWidths(new int[]{3, 2, 2, 2});

            // Add header cells
            pdfTable.addCell("Prestation");
            pdfTable.addCell("Tarif (€)");
            pdfTable.addCell("Quantité");
            pdfTable.addCell("Total (€)");

            // Populate the PDF table with data from the Swing table model
            double subTotal = 0;
            for (int i = 0; i < model.getRowCount(); i++) {
                pdfTable.addCell(model.getValueAt(i, 0).toString());
                pdfTable.addCell(model.getValueAt(i, 1).toString());
                pdfTable.addCell(model.getValueAt(i, 2).toString());
                pdfTable.addCell(model.getValueAt(i, 3).toString());
                subTotal += Double.parseDouble(model.getValueAt(i, 3).toString());
            }

            document.add(pdfTable);

            // Add footer lines for subtotal and legal note
            document.add(new Paragraph("Sous Total: " + subTotal + " €"));
            document.add(new Paragraph("TVA non applicable (article 293B du CGI)"));
            document.add(new Paragraph("Fait à Balma - " + java.time.LocalDate.now()));

            document.close();

            // Inform the user of success
            JOptionPane.showMessageDialog(
                    null,
                    "PDF exported successfully to:\n" + path
            );
        } catch (Exception ex) {
            // If anything goes wrong, show the error
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Error exporting PDF: " + ex.getMessage()
            );
        }
    }
}
