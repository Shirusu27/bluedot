import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminPrompt {
    private JFrame frame;
    private JPasswordField adminPasswordField;
    private JButton submitButton;
    private Runnable onSuccess;

    public AdminPrompt(Runnable onSuccess) {
        this.onSuccess = onSuccess;

        frame = new JFrame("Admin Verification");
        ImageIcon icon = new ImageIcon("bluedotlogotrans.png");
        frame.setIconImage(icon.getImage());
        frame.setSize(300, 150);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(null);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        JLabel lblPass = new JLabel("Admin Password:");
        lblPass.setBounds(20, 20, 120, 25);
        frame.add(lblPass);

        adminPasswordField = new JPasswordField();
        adminPasswordField.setBounds(140, 20, 120, 25);
        frame.add(adminPasswordField);

        submitButton = new JButton("Submit");
        submitButton.setBounds(100, 60, 100, 30);
        frame.add(submitButton);

        submitButton.addActionListener(e -> verifyAdminPassword());

        frame.setVisible(true);
    }

    private void verifyAdminPassword() {
        String enteredPassword = new String(adminPasswordField.getPassword()).trim();

        if (enteredPassword.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter the admin password!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.connect()) {
            String query = "SELECT * FROM admin WHERE admin_password=?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, enteredPassword);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                JOptionPane.showMessageDialog(frame, "Access Granted!");
                frame.dispose();
                if (onSuccess != null) {
                    onSuccess.run(); // ✅ Run the success callback
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Incorrect Password!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
