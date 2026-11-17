package vn.sgu.library.controller;

import vn.sgu.library.dao.ReaderDAO;
import vn.sgu.library.model.Reader;
import java.sql.SQLException;
import java.util.List;

public final class ReaderController {
    private final ReaderDAO dao=new ReaderDAO();
    public List<Reader> find(String keyword)throws SQLException{return dao.findAll(keyword);}
    public void create(String name,String identity,String unit,String email,String phone)throws SQLException{dao.create(name,identity,unit,email,phone);}
    public void update(int id,String name,String identity,String unit,String email,String phone)throws SQLException{dao.update(id,name,identity,unit,email,phone);}
    public void lockCard(int id,boolean lock)throws SQLException{dao.setCardLocked(id,lock);}
    public void renewCard(int id)throws SQLException{dao.renewCard(id);}
}
