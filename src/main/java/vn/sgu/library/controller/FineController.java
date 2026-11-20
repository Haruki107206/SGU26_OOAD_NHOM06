package vn.sgu.library.controller;

import vn.sgu.library.dao.FineDAO;
import vn.sgu.library.model.Fine;
import vn.sgu.library.model.FineLine;
import vn.sgu.library.model.UserSession;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public final class FineController {
    private final FineDAO dao = new FineDAO();

    public List<Fine> findAll() throws SQLException {
        AccessControl.requireStaff();
        return dao.findAll();
    }

    public List<FineLine> findDetails(long fineId) throws SQLException {
        AccessControl.requireStaff();
        return dao.findDetails(fineId);
    }

    public long create(int readerId, List<FineLine> details) throws SQLException {
        UserSession staff = AccessControl.requireStaff();
        return dao.create(readerId, staff.accountId(), details);
    }

    public static void validateDetail(FineLine detail) {
        FineDAO.validate(detail);
    }

    public static BigDecimal calculateOverdueFine(int overdueDays) {
        return FineDAO.calculateOverdueFine(overdueDays);
    }

    public void markPaid(long fineId, String paymentMethod) throws SQLException {
        UserSession staff = AccessControl.requireStaff();
        dao.markPaid(fineId, paymentMethod, staff.accountId());
    }
}
