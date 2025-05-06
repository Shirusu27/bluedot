import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginForm {
    private JFrame frame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton, registerButton;

    public LoginForm() {
        frame = new JFrame("Login");
        frame.setSize(400, 250);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setBounds(50, 30, 300, 150);
        panel.setLayout(null);
        frame.add(panel);

        JLabel lblTitle = new JLabel("Login");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitle.setBounds(120, 0, 100, 30);
        panel.add(lblTitle);

        JLabel lblUser = new JLabel("Username:");
        lblUser.setBounds(20, 40, 80, 25);
        panel.add(lblUser);

        usernameField = new JTextField();
        usernameField.setBounds(100, 40, 180, 25);
        panel.add(usernameField);

        JLabel lblPass = new JLabel("Password:");
        lblPass.setBounds(20, 80, 80, 25);
        panel.add(lblPass);

        passwordField = new JPasswordField();
        passwordField.setBounds(100, 80, 180, 25);
        panel.add(passwordField);

        loginButton = new JButton("Login");
        loginButton.setBounds(50, 120, 100, 30);
        panel.add(loginButton);

        registerButton = new JButton("Register");
        registerButton.setBounds(160, 120, 100, 30);
        panel.add(registerButton);
        loginButton.addActionListener(e -> login());

        registerButton.addActionListener(e -> {
            frame.dispose();
            new AdminPrompt();
        });

        frame.setVisible(true);
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "All fields must be filled!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.connect()) {
            String query = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                JOptionPane.showMessageDialog(frame, "Login Successful!");
                frame.dispose();
                SwingUtilities.invokeLater(dashboardForm::new);
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid username or password.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new LoginForm();
    }
}
