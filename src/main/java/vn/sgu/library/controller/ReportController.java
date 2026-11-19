package vn.sgu.library.controller;

import vn.sgu.library.dao.ReportDAO;
import vn.sgu.library.model.LibraryStatistics;
import vn.sgu.library.model.PopularBook;
import java.sql.SQLException;
import java.util.List;

public final class ReportController {
    private final ReportDAO dao=new ReportDAO();
    public LibraryStatistics statistics()throws SQLException{AccessControl.requireStaff();return dao.statistics();}
    public List<PopularBook> popularBooks()throws SQLException{AccessControl.requireStaff();return dao.popularBooks();}
}
