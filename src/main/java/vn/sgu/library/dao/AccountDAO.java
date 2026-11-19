package vn.sgu.library.dao;

import vn.sgu.library.Database;
import vn.sgu.library.model.Account;
import vn.sgu.library.model.ReaderOption;
import vn.sgu.library.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class AccountDAO {
    public List<Account> findAll() throws SQLException {
        String sql = """
                SELECT a.ma_tai_khoan,a.ten_dang_nhap,r.ten_vai_tro,a.trang_thai,a.so_lan_sai,
                       d.id,d.ma_doc_gia,d.ho_ten
                FROM dbo.tai_khoan a
                JOIN dbo.vai_tro r ON r.ma_vai_tro=a.ma_vai_tro
                LEFT JOIN dbo.doc_gia d ON d.ma_tai_khoan=a.ma_tai_khoan
                ORDER BY a.ten_dang_nhap
                """;
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<Account> accounts = new ArrayList<>();
            while (result.next()) {
                int linkedReaderId = result.getInt(6);
                Integer readerId = result.wasNull() ? null : linkedReaderId;
                accounts.add(new Account(result.getInt(1), result.getString(2),
                        UserRole.fromDatabase(result.getString(3)), result.getString(4),
                        result.getInt(5), readerId, result.getString(7), result.getString(8)));
            }
            return accounts;
        }
    }

    public List<ReaderOption> findUnlinkedReaders() throws SQLException {
        String sql = """
                SELECT id,ma_doc_gia,ho_ten
                FROM dbo.doc_gia
                WHERE ma_tai_khoan IS NULL
                ORDER BY ho_ten
                """;
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<ReaderOption> readers = new ArrayList<>();
            while (result.next()) {
                readers.add(new ReaderOption(result.getInt(1), result.getString(2), result.getString(3)));
            }
            return readers;
        }
    }

    public int create(String username, String passwordHash, UserRole role, Integer readerId)
            throws SQLException {
        validate(username, role, readerId);
        String sql = """
                INSERT INTO dbo.tai_khoan(ma_vai_tro,ten_dang_nhap,mat_khau_hash)
                SELECT r.ma_vai_tro,?,?
                FROM dbo.vai_tro r
                WHERE r.ten_vai_tro=?
                """;
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, username.trim());
                    statement.setString(2, passwordHash);
                    statement.setString(3, role.databaseValue());
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Không tìm thấy vai trò được chọn hoặc không tạo được tài khoản.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT TOP (1) ma_tai_khoan FROM dbo.tai_khoan "
                                + "WHERE ten_dang_nhap=?")) {
                    statement.setString(1, username.trim());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Không truy xuất được tài khoản vừa tạo.");
                        }
                        accountId = result.getInt(1);
                    }
                }
                if (role == UserRole.READER) {
                    linkReader(connection, accountId, readerId);
                }
                connection.commit();
                return accountId;
            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    public void update(int accountId, String username, UserRole role, Integer readerId)
            throws SQLException {
        validate(username, role, readerId);
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT 1 FROM dbo.tai_khoan a WITH (UPDLOCK, ROWLOCK) "
                                + "JOIN dbo.vai_tro r ON r.ma_vai_tro=a.ma_vai_tro "
                                + "WHERE a.ma_tai_khoan=? AND r.ten_vai_tro=N'Admin'")) {
                    statement.setInt(1, accountId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            throw new SQLException("Tài khoản Admin mặc định không thể sửa tại màn hình tài khoản.");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE dbo.tai_khoan SET ten_dang_nhap=?,ma_vai_tro="
                                + "(SELECT ma_vai_tro FROM dbo.vai_tro WHERE ten_vai_tro=?) "
                                + "WHERE ma_tai_khoan=?")) {
                    statement.setString(1, username.trim());
                    statement.setString(2, role.databaseValue());
                    statement.setInt(3, accountId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Không tìm thấy tài khoản.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE dbo.doc_gia SET ma_tai_khoan=NULL WHERE ma_tai_khoan=?")) {
                    statement.setInt(1, accountId);
                    statement.executeUpdate();
                }
                if (role == UserRole.READER) {
                    linkReader(connection, accountId, readerId);
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    public void setActive(int accountId, boolean active) throws SQLException {
        String status = active ? "HOAT_DONG" : "KHOA";
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE dbo.tai_khoan SET trang_thai=?,so_lan_sai=CASE WHEN ?=1 THEN 0 ELSE so_lan_sai END "
                             + "WHERE ma_tai_khoan=?")) {
            statement.setString(1, status);
            statement.setBoolean(2, active);
            statement.setInt(3, accountId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Không tìm thấy tài khoản.");
            }
        }
    }

    private static void linkReader(Connection connection, int accountId, Integer readerId)
            throws SQLException {
        if (readerId == null) {
            throw new IllegalArgumentException("Tài khoản Độc giả phải được liên kết với hồ sơ độc giả.");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE dbo.doc_gia SET ma_tai_khoan=? WHERE id=? AND ma_tai_khoan IS NULL")) {
            statement.setInt(1, accountId);
            statement.setInt(2, readerId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Hồ sơ độc giả không tồn tại hoặc đã liên kết với tài khoản khác.");
            }
        }
    }

    private static void validate(String username, UserRole role, Integer readerId) {
        if (username == null || username.isBlank() || username.trim().length() > 50) {
            throw new IllegalArgumentException("Tên đăng nhập bắt buộc và tối đa 50 ký tự.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Phải chọn vai trò tài khoản.");
        }
        if (role == UserRole.ADMIN) {
            throw new IllegalArgumentException("Chỉ có thể khởi tạo một tài khoản Admin mặc định.");
        }
        if (role == UserRole.READER && readerId == null) {
            throw new IllegalArgumentException("Tài khoản Độc giả phải được liên kết với hồ sơ độc giả.");
        }
        if (role != UserRole.READER && readerId != null) {
            throw new IllegalArgumentException("Chỉ tài khoản Độc giả mới được liên kết với hồ sơ độc giả.");
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
