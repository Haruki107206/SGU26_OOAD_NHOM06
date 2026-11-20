package vn.sgu.library.dao;

import vn.sgu.library.Database;
import vn.sgu.library.model.Fine;
import vn.sgu.library.model.FineLine;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class FineDAO {
    public static final BigDecimal OVERDUE_FINE_PER_DAY = new BigDecimal("2000.00");

    public List<Fine> findAll() throws SQLException {
        String sql = """
                SELECT pp.ma_phieu_phat,d.ma_doc_gia,d.ho_ten,a.ten_dang_nhap,
                       pp.ngay_lap,pp.tong_tien,pp.trang_thai,pp.hinh_thuc_tt,pp.ngay_thanh_toan
                FROM dbo.phieu_phat pp
                JOIN dbo.doc_gia d ON d.id=pp.doc_gia_id
                JOIN dbo.tai_khoan a ON a.ma_tai_khoan=pp.ma_thu_thu
                ORDER BY pp.ngay_lap DESC,pp.ma_phieu_phat DESC
                """;
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<Fine> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new Fine(result.getLong(1), result.getString(2), result.getString(3),
                        result.getString(4), result.getTimestamp(5).toLocalDateTime(),
                        result.getBigDecimal(6), result.getString(7), result.getString(8),
                        localDateTime(result, 9)));
            }
            return rows;
        }
    }

    public List<FineLine> findDetails(long fineId) throws SQLException {
        String sql = """
                SELECT ma_ct_muon,loai_vi_pham,so_ngay_tre,so_tien,mo_ta
                FROM dbo.ct_phieu_phat
                WHERE ma_phieu_phat=?
                ORDER BY ma_ct_phat
                """;
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, fineId);
            try (ResultSet result = statement.executeQuery()) {
                List<FineLine> details = new ArrayList<>();
                while (result.next()) {
                    int loanDetailId = result.getInt(1);
                    Integer loanId = result.wasNull() ? null : loanDetailId;
                    int overdueDays = result.getInt(3);
                    Integer days = result.wasNull() ? null : overdueDays;
                    details.add(new FineLine(loanId, result.getString(2), days,
                            result.getBigDecimal(4), result.getString(5)));
                }
                return details;
            }
        }
    }

    public long create(int readerId, int staffAccountId, List<FineLine> details) throws SQLException {
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("Phiếu phạt phải có ít nhất một chi tiết vi phạm.");
        }
        details.forEach(FineDAO::validate);
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT 1 FROM dbo.doc_gia WITH (UPDLOCK, ROWLOCK) WHERE id=?")) {
                    statement.setInt(1, readerId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Không tìm thấy độc giả.");
                        }
                    }
                }
                long fineId = createWithinTransaction(connection, readerId, staffAccountId, details);
                connection.commit();
                return fineId;
            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    static long createWithinTransaction(Connection connection, int readerId, int staffAccountId,
                                        List<FineLine> details) throws SQLException {
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("Phiếu phạt phải có ít nhất một chi tiết vi phạm.");
        }
        requireActiveStaff(connection, staffAccountId);
        for (FineLine detail : details) {
            validate(detail);
            if (detail.loanDetailId() != null) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT 1
                        FROM dbo.ct_phieu_muon ct
                        JOIN dbo.phieu_muon pm ON pm.id=ct.phieu_muon_id
                        WHERE ct.id=? AND pm.doc_gia_id=?
                        """)) {
                    statement.setInt(1, detail.loanDetailId());
                    statement.setInt(2, readerId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Dòng mượn không thuộc độc giả đang lập phiếu phạt.");
                        }
                    }
                }
            }
        }

        long fineId;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO dbo.phieu_phat(doc_gia_id,ma_thu_thu) "
                        + "OUTPUT INSERTED.ma_phieu_phat VALUES(?,?)")) {
            statement.setInt(1, readerId);
            statement.setInt(2, staffAccountId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Không tạo được phiếu phạt.");
                }
                fineId = result.getLong(1);
            }
        }

        String insert = """
                INSERT INTO dbo.ct_phieu_phat
                    (ma_phieu_phat,ma_ct_muon,loai_vi_pham,so_ngay_tre,so_tien,mo_ta)
                VALUES (?,?,?,?,?,?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            for (FineLine detail : details) {
                statement.setLong(1, fineId);
                if (detail.loanDetailId() == null) {
                    statement.setNull(2, Types.INTEGER);
                } else {
                    statement.setInt(2, detail.loanDetailId());
                }
                statement.setString(3, detail.violationType());
                if (detail.overdueDays() == null) {
                    statement.setNull(4, Types.INTEGER);
                } else {
                    statement.setInt(4, detail.overdueDays());
                }
                statement.setBigDecimal(5, detail.amount());
                statement.setString(6, blankToNull(detail.description()));
                statement.executeUpdate();
            }
        }
        return fineId;
    }

    public void markPaid(long fineId, String paymentMethod, int staffAccountId) throws SQLException {
        if (!List.of("TienMat", "QR").contains(paymentMethod)) {
            throw new IllegalArgumentException("Hình thức thanh toán không hợp lệ.");
        }
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                requireActiveStaff(connection, staffAccountId);
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE dbo.phieu_phat WITH (UPDLOCK, ROWLOCK)
                        SET trang_thai='DaThanhToan',hinh_thuc_tt=?,ngay_thanh_toan=SYSDATETIME()
                        WHERE ma_phieu_phat=? AND trang_thai='ChuaThanhToan'
                        """)) {
                    statement.setString(1, paymentMethod);
                    statement.setLong(2, fineId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Phiếu phạt không tồn tại hoặc đã được thanh toán.");
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    public static boolean hasOutstandingDebt(Connection connection, int readerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TOP (1) 1
                FROM dbo.phieu_phat WITH (UPDLOCK, HOLDLOCK)
                WHERE doc_gia_id=? AND trang_thai='ChuaThanhToan' AND tong_tien>0
                """)) {
            statement.setInt(1, readerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static void requireActiveStaff(Connection connection, int accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM dbo.tai_khoan a WITH (UPDLOCK, ROWLOCK)
                JOIN dbo.vai_tro r ON r.ma_vai_tro=a.ma_vai_tro
                WHERE a.ma_tai_khoan=? AND a.trang_thai='HOAT_DONG'
                  AND r.ten_vai_tro IN (N'Admin',N'ThuThu')
                """)) {
            statement.setInt(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Tài khoản thủ thư không còn hoạt động.");
                }
            }
        }
    }

    public static void validate(FineLine detail) {
        if (detail == null || detail.violationType() == null
                || !List.of("QuaHan", "Hong", "Mat", "Khac").contains(detail.violationType())) {
            throw new IllegalArgumentException("Loại vi phạm không hợp lệ.");
        }
        if (detail.amount() == null || detail.amount().signum() < 0
                || detail.amount().scale() > 2 || detail.amount().precision() > 12) {
            throw new IllegalArgumentException("Số tiền phải từ 0 trở lên và tối đa 2 chữ số thập phân.");
        }
        if (detail.overdueDays() != null && detail.overdueDays() < 0) {
            throw new IllegalArgumentException("Số ngày trễ không được âm.");
        }
        if ("QuaHan".equals(detail.violationType())
                && (detail.overdueDays() == null || detail.overdueDays() < 1)) {
            throw new IllegalArgumentException("Vi phạm quá hạn phải có ít nhất một ngày trễ.");
        }
        if ("QuaHan".equals(detail.violationType())
                && detail.amount().compareTo(calculateOverdueFine(detail.overdueDays())) != 0) {
            throw new IllegalArgumentException("Tiền phạt quá hạn phải bằng 2.000 đồng nhân số ngày trễ.");
        }
        if (detail.description() != null && detail.description().length() > 250) {
            throw new IllegalArgumentException("Mô tả tối đa 250 ký tự.");
        }
    }

    public static BigDecimal calculateOverdueFine(int overdueDays) {
        if (overdueDays < 0) {
            throw new IllegalArgumentException("Số ngày trễ không được âm.");
        }
        return OVERDUE_FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LocalDateTime localDateTime(ResultSet result, int column) throws SQLException {
        java.sql.Timestamp value = result.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static void rollback(Connection connection, Exception original) throws SQLException {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }
}
