package vn.sgu.library.controller;

import vn.sgu.library.dao.AuthDAO;
import vn.sgu.library.model.AuthenticationException;
import vn.sgu.library.model.UserSession;
import vn.sgu.library.security.SessionManager;

import java.sql.SQLException;

public final class AuthController {
    private final AuthDAO dao = new AuthDAO();

    public UserSession login(String username, char[] password)
            throws SQLException, AuthenticationException {
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            throw new AuthenticationException("Nhập tên đăng nhập và mật khẩu.");
        }
        UserSession session = dao.authenticate(username, password);
        SessionManager.start(session);
        return session;
    }

    public void logout() {
        SessionManager.end();
    }

    public void changePassword(char[] currentPassword, char[] newPassword, char[] confirmation)
            throws SQLException, AuthenticationException {
        UserSession session = AccessControl.require();
        if (currentPassword == null || currentPassword.length == 0
                || newPassword == null || newPassword.length < 8) {
            throw new AuthenticationException("Mật khẩu mới phải có ít nhất 8 ký tự.");
        }
        if (!java.util.Arrays.equals(newPassword, confirmation)) {
            throw new AuthenticationException("Mật khẩu mới và phần xác nhận không khớp.");
        }
        dao.changePassword(session.accountId(), currentPassword, newPassword);
    }
}
