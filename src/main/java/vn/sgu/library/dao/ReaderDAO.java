package vn.sgu.library.dao;

import vn.sgu.library.Database;
import vn.sgu.library.model.Reader;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ReaderDAO {
    private static final String READER_COLUMNS = """
            SELECT d.id,d.ma_doc_gia,d.ho_ten,d.ma_dinh_danh,COALESCE(d.don_vi,''),COALESCE(d.email,''),
                   COALESCE(d.sdt,''),COALESCE(t.ma_the,''),t.ngay_het_han,
                   CASE WHEN t.id IS NULL THEN 'CHUA_CO_THE'
                        WHEN t.trang_thai='HIEU_LUC' AND t.ngay_het_han<CAST(GETDATE() AS DATE) THEN 'HET_HAN'
                        ELSE t.trang_thai END,
                   d.ngay_sinh,COALESCE(t.id,0),COALESCE(t.ly_do_khoa,'')
            FROM dbo.doc_gia d
            OUTER APPLY (
                SELECT TOP (1) id,ma_the,ngay_het_han,trang_thai,ly_do_khoa
                FROM dbo.the_doc_gia
                WHERE doc_gia_id=d.id
                ORDER BY id DESC
            ) t
            """;

    public List<Reader> findAll(String keyword) throws SQLException {
        String sql = READER_COLUMNS + """
                WHERE (?='' OR d.ma_doc_gia LIKE ? OR d.ho_ten LIKE ?
                       OR d.ma_dinh_danh LIKE ? OR d.sdt LIKE ?)
                ORDER BY d.ho_ten
                """;
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String query = keyword == null ? "" : keyword.trim();
            String like = "%" + query + "%";
            statement.setString(1, query);
            for (int i = 2; i <= 5; i++) {
                statement.setString(i, like);
            }
            try (ResultSet result = statement.executeQuery()) {
                List<Reader> readers = new ArrayList<>();
                while (result.next()) {
                    readers.add(readReader(result));
                }
                return readers;
            }
        }
    }

    public Reader findByAccountId(int accountId) throws SQLException {
        String sql = READER_COLUMNS + " WHERE d.ma_tai_khoan=?";
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? readReader(result) : null;
            }
        }
    }

    public void create(String fullName, LocalDate birthDate, String identity, String unit,
                       String email, String phone) throws SQLException {
        validate(fullName, birthDate, identity, unit, email, phone);
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                String readerCode = "DG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                int readerId;
                String sql = """
                        INSERT INTO dbo.doc_gia(ma_doc_gia,ho_ten,ngay_sinh,ma_dinh_danh,don_vi,email,sdt)
                        OUTPUT INSERTED.id VALUES(?,?,?,?,?,?,?)
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, readerCode);
                    statement.setString(2, fullName.trim());
                    setDate(statement, 3, birthDate);
                    statement.setString(4, identity.trim());
                    statement.setString(5, blankToNull(unit));
                    statement.setString(6, blankToNull(email));
                    statement.setString(7, blankToNull(phone));
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Không tạo được hồ sơ độc giả.");
                        }
                        readerId = result.getInt(1);
                    }
                }
                issueCard(connection, readerId);
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    public void update(int id, String fullName, LocalDate birthDate, String identity,
                       String unit, String email, String phone) throws SQLException {
        validate(fullName, birthDate, identity, unit, email, phone);
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE dbo.doc_gia
                     SET ho_ten=?,ngay_sinh=?,ma_dinh_danh=?,don_vi=?,email=?,sdt=?
                     WHERE id=?
                     """)) {
            statement.setString(1, fullName.trim());
            setDate(statement, 2, birthDate);
            statement.setString(3, identity.trim());
            statement.setString(4, blankToNull(unit));
            statement.setString(5, blankToNull(email));
            statement.setString(6, blankToNull(phone));
            statement.setInt(7, id);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Không tìm thấy độc giả.");
            }
        }
    }

    public void lockCard(int cardId, String reason) throws SQLException {
        if (reason == null || reason.isBlank() || reason.trim().length() > 250) {
            throw new IllegalArgumentException("Lý do khóa thẻ bắt buộc và tối đa 250 ký tự.");
        }
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE dbo.the_doc_gia
                     SET trang_thai='KHOA',ly_do_khoa=?
                     WHERE id=? AND trang_thai IN ('HIEU_LUC','HET_HAN')
                     """)) {
            statement.setString(1, reason.trim());
            statement.setInt(2, cardId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Thẻ không tồn tại hoặc đã bị khóa.");
            }
        }
    }

    public void renewCard(int readerId, int months) throws SQLException {
        if (months <= 0) {
            throw new IllegalArgumentException("Số tháng gia hạn phải lớn hơn 0.");
        }
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT 1 FROM dbo.doc_gia WITH (UPDLOCK, ROWLOCK) WHERE id=? AND trang_thai='HOAT_DONG'")) {
                    statement.setInt(1, readerId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Không tìm thấy độc giả đang hoạt động.");
                        }
                    }
                }
                if (FineDAO.hasOutstandingDebt(connection, readerId)) {
                    throw new SQLException("Không thể gia hạn thẻ khi độc giả còn nợ phạt chưa thanh toán.");
                }

                int cardId;
                String cardStatus;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT TOP (1) id,trang_thai
                        FROM dbo.the_doc_gia WITH (UPDLOCK, ROWLOCK)
                        WHERE doc_gia_id=?
                        ORDER BY id DESC
                        """)) {
                    statement.setInt(1, readerId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Độc giả chưa có thẻ để gia hạn.");
                        }
                        cardId = result.getInt(1);
                        cardStatus = result.getString(2);
                    }
                }
                if (!List.of("HIEU_LUC", "HET_HAN").contains(cardStatus)) {
                    throw new SQLException("Thẻ đang bị khóa hoặc không ở trạng thái có thể gia hạn.");
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE dbo.the_doc_gia
                        SET ngay_het_han=DATEADD(MONTH,?,
                            CASE WHEN ngay_het_han>=CAST(GETDATE() AS DATE)
                                 THEN ngay_het_han ELSE CAST(GETDATE() AS DATE) END),
                            trang_thai='HIEU_LUC',ly_do_khoa=NULL
                        WHERE id=?
                        """)) {
                    statement.setInt(1, months);
                    statement.setInt(2, cardId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Không cập nhật được ngày hết hạn thẻ.");
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    private static void issueCard(Connection connection, int readerId) throws SQLException {
        String code = "THE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO dbo.the_doc_gia(ma_the,doc_gia_id,ngay_cap,ngay_het_han,trang_thai)
                VALUES(?,?,CAST(GETDATE() AS DATE),DATEADD(MONTH,12,CAST(GETDATE() AS DATE)),'HIEU_LUC')
                """)) {
            statement.setString(1, code);
            statement.setInt(2, readerId);
            statement.executeUpdate();
        }
    }

    private static Reader readReader(ResultSet result) throws SQLException {
        Date birth = result.getDate(11);
        Date expiry = result.getDate(9);
        return new Reader(result.getInt(1), result.getString(2), result.getString(3),
                result.getString(4), result.getString(5), result.getString(6), result.getString(7),
                result.getString(8), expiry == null ? null : expiry.toLocalDate(), result.getString(10),
                birth == null ? null : birth.toLocalDate(), result.getInt(12), result.getString(13));
    }

    private static void validate(String name, LocalDate birthDate, String identity, String unit,
                                 String email, String phone) {
        if (name == null || name.isBlank() || identity == null || identity.isBlank()) {
            throw new IllegalArgumentException("Họ tên và mã định danh bắt buộc.");
        }
        if (name.length() > 120 || identity.length() > 40 || length(unitSafe(unit)) > 120
                || length(unitSafe(email)) > 160 || length(unitSafe(phone)) > 20) {
            throw new IllegalArgumentException("Thông tin vượt quá độ dài cho phép.");
        }
        if (email != null && !email.isBlank()
                && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Email không đúng định dạng.");
        }
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không thể ở tương lai.");
        }
    }

    private static void setDate(PreparedStatement statement, int index, LocalDate date)
            throws SQLException {
        if (date == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(date));
        }
    }

    private static String unitSafe(String value) { return value == null ? "" : value; }
    private static int length(String value) { return value.length(); }
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void rollback(Connection connection, Exception original) throws SQLException {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }
}
