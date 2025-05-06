import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class RegisterForm {
    private JFrame frame;
    private JTextField nameField, usernameField;
    private JPasswordField passwordField;
    private JButton registerButton, backButton;

    public RegisterForm() {
        frame = new JFrame("Register");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setBounds(50, 20, 300, 200);
        panel.setLayout(null);
        frame.add(panel);

        JLabel lblTitle = new JLabel("Register");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitle.setBounds(110, 0, 100, 30);
        panel.add(lblTitle);

        JLabel lblName = new JLabel("Name:");
        lblName.setBounds(20, 40, 80, 25);
        panel.add(lblName);

        nameField = new JTextField();
        nameField.setBounds(100, 40, 180, 25);
        panel.add(nameField);

        JLabel lblUser = new JLabel("Username:");
        lblUser.setBounds(20, 80, 80, 25);
        panel.add(lblUser);

        usernameField = new JTextField();
        usernameField.setBounds(100, 80, 180, 25);
        panel.add(usernameField);

        JLabel lblPass = new JLabel("Password:");
        lblPass.setBounds(20, 120, 80, 25);
        panel.add(lblPass);

        passwordField = new JPasswordField();
        passwordField.setBounds(100, 120, 180, 25);
        panel.add(passwordField);

        registerButton = new JButton("Register");
        registerButton.setBounds(50, 160, 100, 30);
        panel.add(registerButton);

        backButton = new JButton("Back");
        backButton.setBounds(160, 160, 100, 30);
        panel.add(backButton);

        registerButton.addActionListener(e -> register());

        backButton.addActionListener(e -> {
            frame.dispose();
            new LoginForm();
        });

        frame.setVisible(true);
    }

    private void register() {
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "All fields must be filled!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.connect()) {
            String query = "INSERT INTO users (name, username, password) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, name);
            stmt.setString(2, username);
            stmt.setString(3, password);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(frame, "Registration Successful!");
            frame.dispose();
            new LoginForm();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new RegisterForm();
    }
}
