package vn.sgu.library.controller;

import vn.sgu.library.dao.CirculationDAO;
import vn.sgu.library.model.ActiveLoan;
import vn.sgu.library.model.ReturnResult;
import vn.sgu.library.model.UserSession;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public final class CirculationController {
    private final CirculationDAO dao = new CirculationDAO();

    public String borrow(String readerCode, String barcodes) throws SQLException {
        UserSession user = AccessControl.requireStaff();
        return dao.borrow(readerCode, barcodes, user.accountId());
    }

    public List<ActiveLoan> activeLoans() throws SQLException {
        AccessControl.requireStaff();
        return dao.findActive();
    }

    public ReturnResult returnCopy(int detailId, String condition, BigDecimal compensation)
            throws SQLException {
        UserSession user = AccessControl.requireStaff();
        return dao.returnCopy(detailId, condition, compensation, user.accountId());
    }

    public LocalDate renew(int detailId) throws SQLException {
        UserSession user = AccessControl.requireStaff();
        return dao.renew(detailId, user.accountId());
    }
}
