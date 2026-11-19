package vn.sgu.library.controller;

import vn.sgu.library.dao.ReaderDAO;
import vn.sgu.library.model.LoanHistory;
import vn.sgu.library.model.Reader;
import vn.sgu.library.model.UserRole;
import vn.sgu.library.dao.CirculationDAO;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public final class ReaderController {
    private final ReaderDAO dao=new ReaderDAO();
    public List<Reader> find(String keyword)throws SQLException{AccessControl.requireStaff();return dao.findAll(keyword);}
    public Reader ownProfile()throws SQLException{var session=AccessControl.require(UserRole.READER);return dao.findByAccountId(session.accountId());}
    public List<LoanHistory> ownHistory()throws SQLException{var session=AccessControl.require(UserRole.READER);Reader reader=dao.findByAccountId(session.accountId());if(reader==null)throw new SQLException("Tài khoản chưa liên kết hồ sơ độc giả.");return new CirculationDAO().findHistory(reader.id());}
    public void create(String name,LocalDate birthDate,String identity,String unit,String email,String phone)throws SQLException{AccessControl.requireStaff();dao.create(name,birthDate,identity,unit,email,phone);}
    public void update(int id,String name,LocalDate birthDate,String identity,String unit,String email,String phone)throws SQLException{AccessControl.requireStaff();dao.update(id,name,birthDate,identity,unit,email,phone);}
    public void lockCard(int cardId,String reason)throws SQLException{AccessControl.requireStaff();dao.lockCard(cardId,reason);}
    public void renewCard(int id,int months)throws SQLException{AccessControl.requireStaff();dao.renewCard(id,months);}
}
