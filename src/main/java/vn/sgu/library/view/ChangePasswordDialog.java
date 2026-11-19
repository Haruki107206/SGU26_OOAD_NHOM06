package vn.sgu.library.view;

import vn.sgu.library.LibraryApp;
import vn.sgu.library.controller.AuthController;
import vn.sgu.library.model.AuthenticationException;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;

public final class ChangePasswordDialog {
    private final AuthController controller = new AuthController();

    private ChangePasswordDialog() { }

    public static void show(Window owner) {
        new ChangePasswordDialog().open(owner);
    }

    private void open(Window owner) {
        JPasswordField current = new JPasswordField(20);
        JPasswordField next = new JPasswordField(20);
        JPasswordField confirmation = new JPasswordField(20);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Mật khẩu hiện tại"));
        form.add(current);
        form.add(new JLabel("Mật khẩu mới"));
        form.add(next);
        form.add(new JLabel("Xác nhận mật khẩu mới"));
        form.add(confirmation);
        if (JOptionPane.showConfirmDialog(owner, form, "Đổi mật khẩu",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }

        char[] oldPassword = current.getPassword();
        char[] newPassword = next.getPassword();
        char[] confirmedPassword = confirmation.getPassword();
        try {
            controller.changePassword(oldPassword, newPassword, confirmedPassword);
            JOptionPane.showMessageDialog(owner, "Đã đổi mật khẩu.");
        } catch (SQLException | AuthenticationException | RuntimeException ex) {
            LibraryApp.showError(owner, ex);
        } finally {
            Arrays.fill(oldPassword, '\0');
            Arrays.fill(newPassword, '\0');
            Arrays.fill(confirmedPassword, '\0');
            current.setText("");
            next.setText("");
            confirmation.setText("");
        }
    }
}
