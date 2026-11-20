package vn.sgu.library.view;

import vn.sgu.library.LibraryApp;
import vn.sgu.library.controller.AuthController;
import vn.sgu.library.model.AuthenticationException;
import vn.sgu.library.model.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.Arrays;

public final class LoginDialog {
    private final AuthController controller = new AuthController();
    private boolean authenticating;

    private LoginDialog() { }

    public static void showLogin() {
        new LoginDialog().open();
    }

    private void open() {
        JFrame frame = new JFrame("Đăng nhập - Thư viện SGU");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(430, 360));
        frame.setSize(480, 410);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBorder(new EmptyBorder(30, 42, 28, 42));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel icon = new JLabel("▤", SwingConstants.CENTER);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setFont(icon.getFont().deriveFont(Font.BOLD, 34f));
        icon.setForeground(new Color(36, 91, 145));
        JLabel title = new JLabel("THƯ VIỆN SGU", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JLabel subtitle = new JLabel("Đăng nhập để tiếp tục", SwingConstants.CENTER);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(new Color(95, 105, 115));
        heading.add(icon);
        heading.add(Box.createVerticalStrut(5));
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);
        root.add(heading, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 6, 0);
        JLabel usernameLabel = new JLabel("Tên đăng nhập");
        usernameLabel.setFont(usernameLabel.getFont().deriveFont(Font.BOLD));
        constraints.gridy = 0;
        fields.add(usernameLabel, constraints);
        JTextField username = new JTextField();
        username.setFont(username.getFont().deriveFont(15f));
        username.setPreferredSize(new Dimension(320, 38));
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 16, 0);
        fields.add(username, constraints);

        JLabel passwordLabel = new JLabel("Mật khẩu");
        passwordLabel.setFont(passwordLabel.getFont().deriveFont(Font.BOLD));
        constraints.gridy = 2;
        constraints.insets = new Insets(0, 0, 6, 0);
        fields.add(passwordLabel, constraints);
        JPasswordField password = new JPasswordField();
        password.setFont(password.getFont().deriveFont(15f));
        password.setPreferredSize(new Dimension(320, 38));
        constraints.gridy = 3;
        constraints.insets = new Insets(0, 0, 10, 0);
        fields.add(password, constraints);

        JLabel error = new JLabel(" ");
        error.setForeground(new Color(180, 35, 35));
        error.setPreferredSize(new Dimension(320, 20));
        constraints.gridy = 4;
        constraints.insets = new Insets(0, 0, 0, 0);
        fields.add(error, constraints);
        root.add(fields, BorderLayout.CENTER);

        JButton login = new JButton("Đăng nhập");
        login.setFont(login.getFont().deriveFont(Font.BOLD, 14f));
        login.setPreferredSize(new Dimension(130, 40));
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        JLabel hint = new JLabel("Đăng nhập: Admin | Mật khẩu: admin@12345");
        hint.setForeground(new Color(105, 112, 120));
        actions.add(hint, BorderLayout.WEST);
        actions.add(login, BorderLayout.EAST);
        root.add(actions, BorderLayout.SOUTH);
        frame.setContentPane(root);

        Runnable submit = () -> {
            if (authenticating) {
                return;
            }
            String accountName = username.getText().trim();
            char[] suppliedPassword = password.getPassword();
            if (accountName.isEmpty() || suppliedPassword.length == 0) {
                error.setText("Vui lòng nhập tên đăng nhập và mật khẩu.");
                Arrays.fill(suppliedPassword, '\0');
                return;
            }
            authenticating = true;
            login.setEnabled(false);
            username.setEnabled(false);
            password.setEnabled(false);
            error.setText(" ");
            try {
                UserSession session = controller.login(accountName, suppliedPassword);
                frame.dispose();
                new LibraryApp(session).show();
            } catch (SQLException | AuthenticationException | RuntimeException ex) {
                error.setText(ex.getMessage() == null
                        ? "Không thể đăng nhập. Hãy thử lại hoặc liên hệ quản trị viên."
                        : ex.getMessage());
                username.setEnabled(true);
                password.setEnabled(true);
                login.setEnabled(true);
                password.setText("");
                password.requestFocusInWindow();
            } finally {
                Arrays.fill(suppliedPassword, '\0');
                authenticating = false;
            }
        };
        login.addActionListener(event -> submit.run());
        password.addActionListener(event -> login.doClick());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                username.requestFocusInWindow();
            }
        });
        frame.setVisible(true);
    }
}
