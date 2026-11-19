package vn.sgu.library.controller;

import vn.sgu.library.dao.AccountDAO;
import vn.sgu.library.model.Account;
import vn.sgu.library.model.ReaderOption;
import vn.sgu.library.model.UserRole;
import vn.sgu.library.model.UserSession;
import vn.sgu.library.security.PasswordHasher;

import java.sql.SQLException;
import java.util.List;

public final class AccountController {
    private final AccountDAO dao = new AccountDAO();

    public List<Account> findAll() throws SQLException {
        AccessControl.requireAdmin();
        return dao.findAll();
    }

    public List<ReaderOption> findUnlinkedReaders() throws SQLException {
        AccessControl.requireAdmin();
        return dao.findUnlinkedReaders();
    }

    public int create(String username, char[] password, UserRole role, Integer readerId)
            throws SQLException {
        AccessControl.requireAdmin();
        if (role == UserRole.ADMIN) {
            throw new IllegalArgumentException("Chỉ có thể khởi tạo một tài khoản Admin mặc định.");
        }
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự.");
        }
        return dao.create(username, PasswordHasher.hash(password), role, readerId);
    }

    public void update(int accountId, String username, UserRole role, Integer readerId)
            throws SQLException {
        UserSession current = AccessControl.requireAdmin();
        if (role == UserRole.ADMIN) {
            throw new IllegalArgumentException("Không thể tạo thêm tài khoản Admin.");
        }
        if (current.accountId() == accountId) {
            throw new SQLException("Tài khoản Admin mặc định không thể sửa tại màn hình tài khoản.");
        }
        dao.update(accountId, username, role, readerId);
    }

    public void setActive(int accountId, boolean active) throws SQLException {
        UserSession current = AccessControl.requireAdmin();
        if (current.accountId() == accountId && !active) {
            throw new SQLException("Không thể khóa tài khoản đang đăng nhập.");
        }
        dao.setActive(accountId, active);
    }
}
