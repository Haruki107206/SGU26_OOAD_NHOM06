package vn.sgu.library.controller;

import vn.sgu.library.dao.AuthDAO;
import vn.sgu.library.model.UserRole;
import vn.sgu.library.model.UserSession;
import vn.sgu.library.model.SessionExpiredException;
import vn.sgu.library.security.SessionManager;

import java.sql.SQLException;
import java.util.Arrays;

public final class AccessControl {
    private AccessControl() { }

    public static UserSession require(UserRole... allowedRoles) throws SQLException {
        UserSession session = SessionManager.current()
                .orElseThrow(() -> new SessionExpiredException("Phiên đăng nhập đã kết thúc. Vui lòng đăng nhập lại."));
        if (!new AuthDAO().isActive(session.accountId())) {
            SessionManager.end();
            throw new SessionExpiredException("Tài khoản đã bị khóa hoặc không còn hoạt động.");
        }
        if (allowedRoles.length > 0
                && Arrays.stream(allowedRoles).noneMatch(role -> role == session.role())) {
            throw new SQLException("Bạn không có quyền thực hiện chức năng này.");
        }
        return session;
    }

    public static UserSession requireStaff() throws SQLException {
        return require(UserRole.ADMIN, UserRole.LIBRARIAN);
    }

    public static UserSession requireAdmin() throws SQLException {
        return require(UserRole.ADMIN);
    }
}
