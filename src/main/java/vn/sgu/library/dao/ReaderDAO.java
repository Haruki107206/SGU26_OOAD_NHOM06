package vn.sgu.library.dao;

import vn.sgu.library.Database;
import vn.sgu.library.model.Reader;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ReaderDAO {
    public List<Reader> findAll(String keyword) throws SQLException {
        String sql="""
                SELECT d.id,d.ma_doc_gia,d.ho_ten,d.ma_dinh_danh,COALESCE(d.don_vi,''),COALESCE(d.email,''),COALESCE(d.sdt,''),
                       COALESCE(t.ma_the,''),t.ngay_het_han,
                       CASE WHEN t.id IS NULL THEN 'CHUA_CO_THE'
                            WHEN t.trang_thai='HIEU_LUC' AND t.ngay_het_han<CURRENT_DATE THEN 'HET_HAN'
                            ELSE t.trang_thai END
                FROM doc_gia d LEFT JOIN the_doc_gia t ON t.id=(SELECT MAX(t2.id) FROM the_doc_gia t2 WHERE t2.doc_gia_id=d.id)
                WHERE (?='' OR d.ma_doc_gia LIKE ? OR d.ho_ten LIKE ? OR d.ma_dinh_danh LIKE ? OR d.sdt LIKE ?)
                ORDER BY d.ho_ten
                """;
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement(sql)){
            String q=keyword==null?"":keyword.trim(), like="%"+q+"%";p.setString(1,q);for(int i=2;i<=5;i++)p.setString(i,like);
            try(ResultSet r=p.executeQuery()){List<Reader> rows=new ArrayList<>();while(r.next()){Date expiry=r.getDate(9);rows.add(new Reader(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getString(8),expiry==null?null:expiry.toLocalDate(),r.getString(10)));}return rows;}
        }
    }

    public void create(String fullName,String identity,String unit,String email,String phone)throws SQLException{
        validate(fullName,identity,unit,email,phone);
        try(Connection c=Database.openConnection()){
            c.setAutoCommit(false);
            try{
                String readerCode="DG-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();int readerId;
                try(PreparedStatement p=c.prepareStatement("INSERT INTO doc_gia(ma_doc_gia,ho_ten,ma_dinh_danh,don_vi,email,sdt) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                    p.setString(1,readerCode);p.setString(2,fullName.trim());p.setString(3,identity.trim());p.setString(4,blankToNull(unit));p.setString(5,blankToNull(email));p.setString(6,blankToNull(phone));p.executeUpdate();try(ResultSet k=p.getGeneratedKeys()){k.next();readerId=k.getInt(1);}
                }
                issueCard(c,readerId);c.commit();
            }catch(Exception e){c.rollback();if(e instanceof SQLException s)throw s;if(e instanceof IllegalArgumentException a)throw a;throw new SQLException(e);}
        }
    }

    public void update(int id,String fullName,String identity,String unit,String email,String phone)throws SQLException{
        validate(fullName,identity,unit,email,phone);
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement("UPDATE doc_gia SET ho_ten=?,ma_dinh_danh=?,don_vi=?,email=?,sdt=? WHERE id=?")){
            p.setString(1,fullName.trim());p.setString(2,identity.trim());p.setString(3,blankToNull(unit));p.setString(4,blankToNull(email));p.setString(5,blankToNull(phone));p.setInt(6,id);if(p.executeUpdate()==0)throw new SQLException("Không tìm thấy độc giả.");
        }
    }

    public void setCardLocked(int readerId,boolean locked)throws SQLException{
        String sql=locked
                ?"UPDATE the_doc_gia SET trang_thai='KHOA' WHERE doc_gia_id=? AND trang_thai='HIEU_LUC'"
                :"UPDATE the_doc_gia SET trang_thai='HET_HAN' WHERE doc_gia_id=? AND trang_thai='KHOA'";
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,readerId);if(p.executeUpdate()==0)throw new SQLException("Không có thẻ phù hợp để đổi trạng thái.");}
    }

    public void renewCard(int readerId)throws SQLException{
        try(Connection c=Database.openConnection()){
            c.setAutoCommit(false);
            try{
                try(PreparedStatement p=c.prepareStatement("UPDATE the_doc_gia SET trang_thai='HET_HAN' WHERE doc_gia_id=? AND trang_thai IN ('HIEU_LUC','KHOA')")){p.setInt(1,readerId);p.executeUpdate();}
                issueCard(c,readerId);c.commit();
            }catch(Exception e){c.rollback();if(e instanceof SQLException s)throw s;throw new SQLException(e);}
        }
    }

    private static void issueCard(Connection c,int readerId)throws SQLException{
        String code="THE-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();
        try(PreparedStatement p=c.prepareStatement("INSERT INTO the_doc_gia(ma_the,doc_gia_id,ngay_cap,ngay_het_han,trang_thai) VALUES(?,?,CURRENT_DATE,DATE_ADD(CURRENT_DATE,INTERVAL 12 MONTH),'HIEU_LUC')")){
            p.setString(1,code);p.setInt(2,readerId);p.executeUpdate();
        }
    }
    private static void validate(String name,String identity,String unit,String email,String phone){
        if(name==null||name.isBlank()||identity==null||identity.isBlank())throw new IllegalArgumentException("Họ tên và mã định danh bắt buộc.");
        if(name.length()>120||identity.length()>40||length(unitSafe(unit))>120||length(unitSafe(email))>160||length(unitSafe(phone))>20)throw new IllegalArgumentException("Thông tin vượt quá độ dài cho phép.");
        if(email!=null&&!email.isBlank()&&!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))throw new IllegalArgumentException("Email không đúng định dạng.");
    }
    private static String unitSafe(String s){return s==null?"":s;}
    private static int length(String s){return s.length();}
    private static String blankToNull(String s){return s==null||s.isBlank()?null:s.trim();}
}

