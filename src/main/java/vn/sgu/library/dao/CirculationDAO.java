package vn.sgu.library.dao;

import vn.sgu.library.Database;
import vn.sgu.library.model.ActiveLoan;
import vn.sgu.library.model.FineLine;
import vn.sgu.library.model.LoanHistory;
import vn.sgu.library.model.ReturnResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public final class CirculationDAO {
    public String borrow(String readerCode, String barcodeList, int staffAccountId) throws SQLException {
        LinkedHashSet<String> barcodes = new LinkedHashSet<>();
        for (String raw : barcodeList.split(",")) {
            String barcode = raw.trim();
            if (!barcode.isEmpty()) {
                barcodes.add(barcode);
            }
        }
        if (barcodes.isEmpty()) {
            throw new IllegalArgumentException("Nhập ít nhất một mã vạch sách.");
        }
        if (barcodes.size() > 5) {
            throw new IllegalArgumentException("Một phiếu không được vượt quá 5 bản sao.");
        }

        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                requireActiveStaff(connection, staffAccountId);
                int readerId;
                String readerSql = """
                        SELECT TOP (1) d.id
                        FROM dbo.doc_gia d WITH (UPDLOCK, ROWLOCK)
                        JOIN dbo.the_doc_gia t WITH (UPDLOCK, ROWLOCK) ON t.doc_gia_id=d.id
                        WHERE d.ma_doc_gia=? AND d.trang_thai='HOAT_DONG'
                          AND t.trang_thai='HIEU_LUC' AND t.ngay_het_han>=CAST(GETDATE() AS DATE)
                        ORDER BY t.id DESC
                        """;
                try (PreparedStatement statement = connection.prepareStatement(readerSql)) {
                    statement.setString(1, readerCode.trim());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Không tìm thấy độc giả có thẻ còn hiệu lực.");
                        }
                        readerId = result.getInt(1);
                    }
                }
                if (FineDAO.hasOutstandingDebt(connection, readerId)) {
                    throw new SQLException("Không thể mượn sách khi độc giả còn nợ phạt chưa thanh toán.");
                }

                int activeCount;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT COUNT(*)
                        FROM dbo.ct_phieu_muon ct
                        JOIN dbo.phieu_muon pm ON pm.id=ct.phieu_muon_id
                        WHERE pm.doc_gia_id=? AND ct.trang_thai='DANG_MUON'
                        """)) {
                    statement.setInt(1, readerId);
                    try (ResultSet result = statement.executeQuery()) {
                        result.next();
                        activeCount = result.getInt(1);
                    }
                }
                if (activeCount + barcodes.size() > 5) {
                    throw new SQLException("Độc giả đã mượn hoặc sắp vượt giới hạn 5 bản sao.");
                }

                List<Integer> copyIds = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id FROM dbo.ban_sao_sach WITH (UPDLOCK, ROWLOCK)
                        WHERE ma_vach=? AND trang_thai='SAN_SANG'
                        """)) {
                    for (String barcode : barcodes) {
                        statement.setString(1, barcode);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new SQLException("Bản sao không tồn tại hoặc không sẵn sàng: " + barcode);
                            }
                            copyIds.add(result.getInt(1));
                        }
                    }
                }

                String loanCode = "PM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                int loanId;
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO dbo.phieu_muon
                            (ma_phieu,doc_gia_id,ma_thu_thu,ngay_muon,han_tra,trang_thai)
                        OUTPUT INSERTED.id
                        VALUES(?,?,?,CAST(GETDATE() AS DATE),
                               DATEADD(DAY,14,CAST(GETDATE() AS DATE)),'DANG_MUON')
                        """)) {
                    statement.setString(1, loanCode);
                    statement.setInt(2, readerId);
                    statement.setInt(3, staffAccountId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Không tạo được phiếu mượn.");
                        }
                        loanId = result.getInt(1);
                    }
                }
                try (PreparedStatement detail = connection.prepareStatement("""
                             INSERT INTO dbo.ct_phieu_muon(phieu_muon_id,ban_sao_sach_id,han_tra,trang_thai)
                             VALUES(?,?,DATEADD(DAY,14,CAST(GETDATE() AS DATE)),'DANG_MUON')
                             """);
                     PreparedStatement update = connection.prepareStatement("""
                             UPDATE dbo.ban_sao_sach
                             SET trang_thai='DANG_MUON'
                             WHERE id=? AND trang_thai='SAN_SANG'
                             """)) {
                    for (int copyId : copyIds) {
                        detail.setInt(1, loanId);
                        detail.setInt(2, copyId);
                        detail.executeUpdate();
                        update.setInt(1, copyId);
                        if (update.executeUpdate() != 1) {
                            throw new SQLException("Trạng thái bản sao vừa thay đổi, hãy thử lại.");
                        }
                    }
                }
                connection.commit();
                return loanCode;
            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    public List<ActiveLoan> findActive() throws SQLException {
        String sql = """
                SELECT ct.id,d.ma_doc_gia,d.ho_ten,s.ten_sach,bs.ma_vach,pm.ngay_muon,ct.han_tra,
                       (SELECT COUNT(*) FROM dbo.gia_han_muon gh
                        WHERE gh.ct_phieu_muon_id=ct.id),
                       CASE WHEN ct.trang_thai='DANG_MUON'
                            THEN CASE WHEN DATEDIFF(DAY,ct.han_tra,CAST(GETDATE() AS DATE))>0
                                      THEN DATEDIFF(DAY,ct.han_tra,CAST(GETDATE() AS DATE))
                                      ELSE 0 END
                            ELSE 0 END
                FROM dbo.ct_phieu_muon ct
                JOIN dbo.phieu_muon pm ON pm.id=ct.phieu_muon_id
                JOIN dbo.doc_gia d ON d.id=pm.doc_gia_id
                JOIN dbo.ban_sao_sach bs ON bs.id=ct.ban_sao_sach_id
                JOIN dbo.sach s ON s.id=bs.sach_id
                WHERE ct.trang_thai='DANG_MUON'
                ORDER BY ct.han_tra,d.ho_ten
                """;
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<ActiveLoan> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new ActiveLoan(result.getInt(1), result.getString(2), result.getString(3),
                result.getString(4), result.getString(5), result.getDate(6).toLocalDate(),
                result.getDate(7).toLocalDate(), result.getInt(8), result.getInt(9)));
            }
            return rows;
        }
    }

    public List<LoanHistory> findHistory(int readerId) throws SQLException {
        String sql = """
                SELECT pm.ma_phieu,s.ten_sach,pm.ngay_muon,ct.han_tra,ct.ngay_tra,ct.trang_thai,
                       (SELECT COUNT(*) FROM dbo.gia_han_muon gh
                        WHERE gh.ct_phieu_muon_id=ct.id)
                FROM dbo.ct_phieu_muon ct
                JOIN dbo.phieu_muon pm ON pm.id=ct.phieu_muon_id
                JOIN dbo.ban_sao_sach bs ON bs.id=ct.ban_sao_sach_id
                JOIN dbo.sach s ON s.id=bs.sach_id
                WHERE pm.doc_gia_id=?
                ORDER BY pm.ngay_muon DESC,pm.id DESC
                """;
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, readerId);
            try (ResultSet result = statement.executeQuery()) {
                List<LoanHistory> rows = new ArrayList<>();
                while (result.next()) {
                    java.sql.Date returned = result.getDate(5);
                    rows.add(new LoanHistory(result.getString(1), result.getString(2),
                            result.getDate(3).toLocalDate(), result.getDate(4).toLocalDate(),
                            returned == null ? null : returned.toLocalDate(), result.getString(6),
                            result.getInt(7)));
                }
                return rows;
            }
        }
    }

    public ReturnResult returnCopy(int detailId, String returnCondition, BigDecimal compensation,
                                   int staffAccountId) throws SQLException {
        if (!List.of("TOT", "HONG", "MAT").contains(returnCondition)) {
            throw new IllegalArgumentException("Tình trạng trả không hợp lệ.");
        }
        if (!"TOT".equals(returnCondition)
                && (compensation == null || compensation.signum() < 0
                || compensation.scale() > 2 || compensation.precision() > 12)) {
            throw new IllegalArgumentException(
                    "Tiền bồi thường phải được nhập, không âm và tối đa 2 chữ số thập phân.");
        }
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                requireActiveStaff(connection, staffAccountId);
                int copyId;
                int loanId;
                int readerId;
                int overdueDays;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT ct.ban_sao_sach_id,ct.phieu_muon_id,pm.doc_gia_id,
                               DATEDIFF(DAY,ct.han_tra,CAST(GETDATE() AS DATE)),ct.han_tra
                        FROM dbo.ct_phieu_muon ct WITH (UPDLOCK, ROWLOCK)
                        JOIN dbo.phieu_muon pm ON pm.id=ct.phieu_muon_id
                        WHERE ct.id=? AND ct.trang_thai='DANG_MUON'
                        """)) {
                    statement.setInt(1, detailId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Dòng mượn không còn ở trạng thái đang mượn.");
                        }
                        copyId = result.getInt(1);
                        loanId = result.getInt(2);
                        readerId = result.getInt(3);
                        overdueDays = Math.max(0, result.getInt(4));
                    }
                }
                List<FineLine> violations = new ArrayList<>();
                if (overdueDays > 0) {
                    violations.add(new FineLine(detailId, "QuaHan", overdueDays,
                            FineDAO.calculateOverdueFine(overdueDays),
                            "Quá hạn " + overdueDays + " ngày x "
                                    + FineDAO.OVERDUE_FINE_PER_DAY.toPlainString() + " đồng/ngày."));
                }
                if ("HONG".equals(returnCondition) || "MAT".equals(returnCondition)) {
                    String type = "HONG".equals(returnCondition) ? "Hong" : "Mat";
                    violations.add(new FineLine(detailId, type, null, compensation,
                            "Bồi thường sách " + ("HONG".equals(returnCondition) ? "hỏng" : "mất") + "."));
                }
                String detailStatus = returnCondition.equals("TOT") ? "DA_TRA" : returnCondition;
                String copyStatus = returnCondition.equals("TOT") ? "SAN_SANG" : returnCondition;
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE dbo.ct_phieu_muon SET trang_thai=?,ngay_tra=CAST(GETDATE() AS DATE)
                        WHERE id=? AND trang_thai='DANG_MUON'
                        """)) {
                    statement.setString(1, detailStatus);
                    statement.setInt(2, detailId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Không cập nhật được trạng thái dòng mượn.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE dbo.ban_sao_sach SET trang_thai=?
                        WHERE id=? AND trang_thai='DANG_MUON'
                        """)) {
                    statement.setString(1, copyStatus);
                    statement.setInt(2, copyId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Không cập nhật được trạng thái bản sao.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE dbo.phieu_muon
                        SET trang_thai=CASE WHEN NOT EXISTS (
                            SELECT 1 FROM dbo.ct_phieu_muon
                            WHERE phieu_muon_id=? AND trang_thai='DANG_MUON'
                        ) THEN 'DA_TRA' ELSE 'DANG_MUON' END
                        WHERE id=?
                        """)) {
                    statement.setInt(1, loanId);
                    statement.setInt(2, loanId);
                    statement.executeUpdate();
                }
                Long fineId = null;
                BigDecimal fineTotal = BigDecimal.ZERO.setScale(2);
                if (!violations.isEmpty()) {
                    fineId = FineDAO.createWithinTransaction(connection, readerId,
                            staffAccountId, violations);
                    fineTotal = violations.stream().map(FineLine::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
                }
                connection.commit();
                return new ReturnResult(readerId, detailId, returnCondition, overdueDays,
                        fineId, fineTotal);
            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    public LocalDate renew(int detailId, int staffAccountId) throws SQLException {
        try (Connection connection = Database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                requireActiveStaff(connection, staffAccountId);
                int readerId;
                LocalDate oldDueDate;
                boolean eligible;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT pm.doc_gia_id,ct.han_tra,
                               CASE WHEN d.trang_thai='HOAT_DONG'
                                          AND card.trang_thai='HIEU_LUC'
                                          AND card.ngay_het_han>=CAST(GETDATE() AS DATE)
                                          AND ct.han_tra>=CAST(GETDATE() AS DATE)
                                    THEN 1 ELSE 0 END
                        FROM dbo.ct_phieu_muon ct WITH (UPDLOCK, ROWLOCK)
                        JOIN dbo.phieu_muon pm ON pm.id=ct.phieu_muon_id
                        JOIN dbo.doc_gia d WITH (UPDLOCK, ROWLOCK) ON d.id=pm.doc_gia_id
                        OUTER APPLY (
                            SELECT TOP (1) t.trang_thai,t.ngay_het_han
                            FROM dbo.the_doc_gia t WITH (UPDLOCK, ROWLOCK)
                            WHERE t.doc_gia_id=d.id
                            ORDER BY t.id DESC
                        ) card
                        WHERE ct.id=? AND ct.trang_thai='DANG_MUON'
                        """)) {
                    statement.setInt(1, detailId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Không tìm thấy lượt mượn đang còn hiệu lực để gia hạn.");
                        }
                        readerId = result.getInt(1);
                        oldDueDate = result.getDate(2).toLocalDate();
                        eligible = result.getBoolean(3);
                    }
                }
                if (!eligible) {
                    throw new SQLException(
                            "Chỉ gia hạn được sách chưa quá hạn của độc giả đang hoạt động, có thẻ còn hiệu lực.");
                }
                if (FineDAO.hasOutstandingDebt(connection, readerId)) {
                    throw new SQLException("Không thể gia hạn khi độc giả còn nợ phạt chưa thanh toán.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT 1 FROM dbo.gia_han_muon WITH (UPDLOCK, HOLDLOCK) "
                                + "WHERE ct_phieu_muon_id=?")) {
                    statement.setInt(1, detailId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            throw new SQLException("Lượt mượn này đã được gia hạn một lần.");
                        }
                    }
                }

                LocalDate newDueDate = oldDueDate.plusDays(7);
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE dbo.ct_phieu_muon
                        SET han_tra=DATEADD(DAY,7,han_tra)
                        WHERE id=? AND trang_thai='DANG_MUON'
                        """)) {
                    statement.setInt(1, detailId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Lượt mượn vừa thay đổi; hãy tải lại danh sách.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO dbo.gia_han_muon
                            (ct_phieu_muon_id,ma_thu_thu,han_tra_cu,han_tra_moi)
                        VALUES(?,?,?,?)
                        """)) {
                    statement.setInt(1, detailId);
                    statement.setInt(2, staffAccountId);
                    statement.setDate(3, java.sql.Date.valueOf(oldDueDate));
                    statement.setDate(4, java.sql.Date.valueOf(newDueDate));
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE dbo.phieu_muon
                        SET han_tra=(SELECT MAX(ct.han_tra)
                                     FROM dbo.ct_phieu_muon ct
                                     WHERE ct.phieu_muon_id=dbo.phieu_muon.id)
                        WHERE id=(SELECT phieu_muon_id FROM dbo.ct_phieu_muon WHERE id=?)
                        """)) {
                    statement.setInt(1, detailId);
                    statement.executeUpdate();
                }
                connection.commit();
                return newDueDate;
            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
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

    private static void rollback(Connection connection, Exception original) throws SQLException {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }
}
