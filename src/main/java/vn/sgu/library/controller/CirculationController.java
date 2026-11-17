package vn.sgu.library.controller;

import vn.sgu.library.dao.CirculationDAO;
import vn.sgu.library.model.ActiveLoan;
import java.sql.SQLException;
import java.util.List;

public final class CirculationController {
    private final CirculationDAO dao=new CirculationDAO();
    public String borrow(String readerCode,String barcodes)throws SQLException{return dao.borrow(readerCode,barcodes);}
    public List<ActiveLoan> activeLoans()throws SQLException{return dao.findActive();}
    public void returnCopy(int detailId,String condition)throws SQLException{dao.returnCopy(detailId,condition);}
}
