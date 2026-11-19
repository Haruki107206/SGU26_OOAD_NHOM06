package vn.sgu.library.dao;

import vn.sgu.library.Database;
import vn.sgu.library.model.AuthenticationException;
import vn.sgu.library.model.UserRole;
import vn.sgu.library.model.UserSession;
import vn.sgu.library.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AuthDAO {
    private static final int LOCK_THRESHOLD = 6;
    private static final String DEFAULT_ADMIN_USERNAME = "Admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin@12345";

    public UserSession authenticate(String username, char[] password)
            throws SQLException, AuthenticationException {
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId;
                int failedAttempts;
                Integer readerId;
                String storedHash;
                String status;
                String roleName;
                String sql = """
                        SELECT a.ma_tai_khoan,a.mat_khau_hash,a.trang_thai,a.so_lan_sai,
                               r.ten_vai_tro,d.id
                        FROM dbo.tai_khoan a WITH (UPDLOCK, ROWLOCK)
                        JOIN dbo.vai_tro r ON r.ma_vai_tro=a.ma_vai_tro
                        LEFT JOIN dbo.doc_gia d ON d.ma_tai_khoan=a.ma_tai_khoan
                        WHERE a.ten_dang_nhap=?
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, username.trim());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.commit();
                            throw new AuthenticationException("Tên đăng nhập không tồn tại.");
                        }
                        accountId = result.getInt(1);
                        storedHash = result.getString(2);
                        status = result.getString(3);
                        failedAttempts = result.getInt(4);
                        roleName = result.getString(5);
                        int linkedReaderId = result.getInt(6);
                        readerId = result.wasNull() ? null : linkedReaderId;
                    }
                }

                if (!"HOAT_DONG".equals(status)) {
                    connection.commit();
                    throw new AuthenticationException("Tài khoản đã bị khóa.");
                }
                if (!PasswordHasher.matches(password, storedHash)) {
                    int nextAttempts = Math.min(LOCK_THRESHOLD, failedAttempts + 1);
                    String nextStatus = nextAttempts >= LOCK_THRESHOLD ? "KHOA" : "HOAT_DONG";
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE dbo.tai_khoan SET so_lan_sai=?,trang_thai=? WHERE ma_tai_khoan=?")) {
                        statement.setInt(1, nextAttempts);
                        statement.setString(2, nextStatus);
                        statement.setInt(3, accountId);
                        statement.executeUpdate();
                    }
                    connection.commit();
                    if (nextAttempts >= LOCK_THRESHOLD) {
                        throw new AuthenticationException("Đăng nhập sai 6 lần; tài khoản đã bị khóa.");
                    }
                    throw new AuthenticationException("Mật khẩu không chính xác.");
                }

                UserRole role = UserRole.fromDatabase(roleName);
                if (role == UserRole.READER && readerId == null) {
                    connection.commit();
                    throw new AuthenticationException("Tài khoản độc giả chưa được liên kết với hồ sơ độc giả.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE dbo.tai_khoan SET so_lan_sai=0 WHERE ma_tai_khoan=?")) {
                    statement.setInt(1, accountId);
                    statement.executeUpdate();
                }
                connection.commit();
                return new UserSession(accountId, username.trim(), role, readerId);
            } catch (AuthenticationException | SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    public boolean isActive(int accountId) throws SQLException {
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM dbo.tai_khoan WHERE ma_tai_khoan=? AND trang_thai='HOAT_DONG'")) {
            statement.setInt(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public void changePassword(int accountId, char[] currentPassword, char[] newPassword)
            throws SQLException, AuthenticationException {
        if (currentPassword == null || currentPassword.length == 0
                || newPassword == null || newPassword.length < 8) {
            throw new AuthenticationException("Mật khẩu mới phải có ít nhất 8 ký tự.");
        }
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                String currentHash;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT mat_khau_hash FROM dbo.tai_khoan WITH (UPDLOCK, ROWLOCK) "
                                + "WHERE ma_tai_khoan=? AND trang_thai='HOAT_DONG'")) {
                    statement.setInt(1, accountId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.commit();
                            throw new AuthenticationException("Tài khoản không còn hoạt động.");
                        }
                        currentHash = result.getString(1);
                    }
                }
                if (!PasswordHasher.matches(currentPassword, currentHash)) {
                    connection.commit();
                    throw new AuthenticationException("Mật khẩu hiện tại không chính xác.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE dbo.tai_khoan SET mat_khau_hash=?,so_lan_sai=0 WHERE ma_tai_khoan=?")) {
                    statement.setString(1, PasswordHasher.hash(newPassword));
                    statement.setInt(2, accountId);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (AuthenticationException | SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    public boolean ensureDefaultAdmin() throws SQLException {
        char[] password = DEFAULT_ADMIN_PASSWORD.toCharArray();
        try {
            String encodedPassword = PasswordHasher.hash(password);
            try (Connection connection = Database.openConnection()) {
                connection.setAutoCommit(false);
                try {
                    int adminRoleId;
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT ma_vai_tro FROM dbo.vai_tro WITH (UPDLOCK, HOLDLOCK) "
                                    + "WHERE ten_vai_tro=N'Admin'");
                         ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Vai trò Admin chưa được tạo; chạy script database/library_catalog.sql.");
                        }
                        adminRoleId = result.getInt(1);
                    }

                    Integer adminAccountId = null;
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT a.ma_tai_khoan FROM dbo.tai_khoan a WITH (UPDLOCK, HOLDLOCK) "
                                    + "JOIN dbo.vai_tro r ON r.ma_vai_tro=a.ma_vai_tro "
                                    + "WHERE r.ten_vai_tro=N'Admin' ORDER BY a.ma_tai_khoan");
                         ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            adminAccountId = result.getInt(1);
                            if (result.next()) {
                                throw new SQLException("Có nhiều tài khoản Admin; cần xử lý dữ liệu trùng trước khi tiếp tục.");
                            }
                        }
                    }

                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT ma_tai_khoan FROM dbo.tai_khoan WITH (UPDLOCK, HOLDLOCK) "
                                    + "WHERE ten_dang_nhap=?")) {
                        statement.setString(1, DEFAULT_ADMIN_USERNAME);
                        try (ResultSet result = statement.executeQuery()) {
                            if (result.next() && (adminAccountId == null
                                    || result.getInt(1) != adminAccountId)) {
                                throw new SQLException("Tên đăng nhập Admin đang được một tài khoản khác sử dụng.");
                            }
                        }
                    }

                    boolean created = adminAccountId == null;
                    if (created) {
                        try (PreparedStatement statement = connection.prepareStatement(
                                "INSERT INTO dbo.tai_khoan(ma_vai_tro,ten_dang_nhap,mat_khau_hash) "
                                        + "VALUES(?,?,?)")) {
                            statement.setInt(1, adminRoleId);
                            statement.setString(2, DEFAULT_ADMIN_USERNAME);
                            statement.setString(3, encodedPassword);
                            statement.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement statement = connection.prepareStatement(
                                "UPDATE dbo.tai_khoan SET ten_dang_nhap=?,mat_khau_hash=?,"
                                        + "trang_thai='HOAT_DONG',so_lan_sai=0 WHERE ma_tai_khoan=?")) {
                            statement.setString(1, DEFAULT_ADMIN_USERNAME);
                            statement.setString(2, encodedPassword);
                            statement.setInt(3, adminAccountId);
                            statement.executeUpdate();
                        }
                    }
                    connection.commit();
                    return created;
                } catch (SQLException | RuntimeException ex) {
                    rollback(connection, ex);
                    throw ex;
                }
            }
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    private static void rollback(Connection connection, Exception original) throws SQLException {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }
}
