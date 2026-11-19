package vn.sgu.library.view;

import vn.sgu.library.LibraryApp;
import vn.sgu.library.controller.AuthController;
import vn.sgu.library.model.AuthenticationException;
import vn.sgu.library.model.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;

public final class LoginDialog {
    private final AuthController controller = new AuthController();

    private LoginDialog() { }

    public static void showLogin() {
        new LoginDialog().open();
    }

    private void open() {
        JFrame frame = new JFrame("Đăng nhập - Thư viện SGU");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(420, 230);
        frame.setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(new EmptyBorder(20, 20, 12, 20));
        JTextField username = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        form.add(new JLabel("Tên đăng nhập"));
        form.add(username);
        form.add(new JLabel("Mật khẩu"));
        form.add(password);
        JButton login = new JButton("Đăng nhập");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(login);
        frame.add(form, BorderLayout.CENTER);
        frame.add(actions, BorderLayout.SOUTH);
        login.addActionListener(event -> {
            char[] suppliedPassword = password.getPassword();
            try {
                UserSession session = controller.login(username.getText(), suppliedPassword);
                frame.dispose();
                new LibraryApp(session).show();
            } catch (SQLException | AuthenticationException | RuntimeException ex) {
                LibraryApp.showError(frame, ex);
            } finally {
                Arrays.fill(suppliedPassword, '\0');
                password.setText("");
            }
        });
        password.addActionListener(event -> login.doClick());
        frame.setVisible(true);
        username.requestFocusInWindow();
    }
}
