package vn.sgu.library.dao;

import vn.sgu.library.Database;
import vn.sgu.library.model.Book;
import vn.sgu.library.model.BookCopy;
import vn.sgu.library.model.BookRow;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class BookDAO {
    public List<BookRow> search(String text)throws SQLException{
        String sql="""
                SELECT s.ma_sach,s.ten_sach,
                       COALESCE(author_list.author_names,''),
                       tl.ten_the_loai,nxb.ten_nxb,s.nam_xuat_ban,COALESCE(ks.ma_ke,''),
                       COUNT(DISTINCT bs.id),COUNT(DISTINCT CASE WHEN bs.trang_thai='SAN_SANG' THEN bs.id END)
                FROM sach s
                OUTER APPLY (
                    SELECT STRING_AGG(a.ten_tac_gia, ', ') WITHIN GROUP (ORDER BY a.ten_tac_gia) AS author_names
                    FROM (SELECT DISTINCT tg2.ten_tac_gia
                          FROM sach_tac_gia sta2 JOIN tac_gia tg2 ON tg2.id=sta2.tac_gia_id
                          WHERE sta2.sach_id=s.id) a
                ) author_list
                LEFT JOIN sach_tac_gia sta ON sta.sach_id=s.id LEFT JOIN tac_gia tg ON tg.id=sta.tac_gia_id
                LEFT JOIN the_loai tl ON tl.id=s.the_loai_id LEFT JOIN nha_xuat_ban nxb ON nxb.id=s.nxb_id
                LEFT JOIN ke_sach ks ON ks.id=s.ke_sach_id LEFT JOIN ban_sao_sach bs ON bs.sach_id=s.id
                WHERE s.trang_thai='HOAT_DONG' AND (?='' OR s.ma_sach LIKE ? OR s.ten_sach LIKE ? OR tg.ten_tac_gia LIKE ? OR tl.ten_the_loai LIKE ? OR CAST(s.nam_xuat_ban AS VARCHAR(4)) LIKE ?)
                GROUP BY s.id,s.ma_sach,s.ten_sach,author_list.author_names,tl.ten_the_loai,nxb.ten_nxb,s.nam_xuat_ban,ks.ma_ke ORDER BY s.ten_sach
                """;
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement(sql)){
            String q=text==null?"":text.trim(),like="%"+q+"%";p.setString(1,q);for(int i=2;i<=6;i++)p.setString(i,like);
            try(ResultSet r=p.executeQuery()){List<BookRow> rows=new ArrayList<>();while(r.next())rows.add(new BookRow(r.getString(1),r.getString(2),r.getString(3),nvl(r.getString(4)),nvl(r.getString(5)),r.getInt(6),r.getString(7),r.getInt(8),r.getInt(9)));return rows;}
        }
    }
    public int idByCode(String code)throws SQLException{
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement("SELECT id FROM sach WHERE ma_sach=? AND trang_thai='HOAT_DONG'")){p.setString(1,code);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Không tìm thấy đầu sách.");return r.getInt(1);}}
    }
    public Book get(int id)throws SQLException{
        String sql="""
                SELECT s.ma_sach,s.ten_sach,COALESCE(author_list.author_names,''),tl.ten_the_loai,nxb.ten_nxb,s.nam_xuat_ban,COALESCE(ks.ma_ke,'')
                FROM sach s
                OUTER APPLY (
                    SELECT STRING_AGG(a.ten_tac_gia, ', ') WITHIN GROUP (ORDER BY a.ten_tac_gia) AS author_names
                    FROM (SELECT DISTINCT tg.ten_tac_gia
                          FROM sach_tac_gia sta JOIN tac_gia tg ON tg.id=sta.tac_gia_id
                          WHERE sta.sach_id=s.id) a
                ) author_list
                LEFT JOIN the_loai tl ON tl.id=s.the_loai_id
                LEFT JOIN nha_xuat_ban nxb ON nxb.id=s.nxb_id
                LEFT JOIN ke_sach ks ON ks.id=s.ke_sach_id
                WHERE s.id=?
                """;
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())return null;Book b=new Book();b.id=id;b.code=r.getString(1);b.title=r.getString(2);b.authors=nvl(r.getString(3));b.genre=nvl(r.getString(4));b.publisher=nvl(r.getString(5));b.publicationYear=r.getInt(6);b.shelf=nvl(r.getString(7));return b;}}
    }
    public void save(Book b)throws SQLException{
        if(b.code.isBlank()||b.title.isBlank()||b.authors.isBlank())throw new IllegalArgumentException("Mã sách, tên sách và tác giả không được để trống.");
        if(b.code.length()>40||b.title.length()>240||b.genre.length()>120||b.publisher.length()>180||b.shelf.length()>80)throw new IllegalArgumentException("Một trường nhập vượt quá độ dài cho phép của CSDL.");
        for(String author:b.authors.split(","))if(author.trim().isEmpty()||author.trim().length()>180)throw new IllegalArgumentException("Tên tác giả không được để trống và tối đa 180 ký tự.");
        if(b.publicationYear<0||b.publicationYear>2100)throw new IllegalArgumentException("Năm xuất bản không hợp lệ.");
        try(Connection c=Database.openConnection()){c.setAutoCommit(false);try{
            int genre=ensure(c,"the_loai","ten_the_loai",b.genre),publisher=ensure(c,"nha_xuat_ban","ten_nxb",b.publisher),shelf=ensure(c,"ke_sach","ma_ke",b.shelf),bookId;
            if(b.id>0){try(PreparedStatement p=c.prepareStatement("UPDATE sach SET ma_sach=?,ten_sach=?,the_loai_id=?,nxb_id=?,nam_xuat_ban=?,ke_sach_id=?,trang_thai='HOAT_DONG' WHERE id=?")){p.setString(1,b.code);p.setString(2,b.title);setNullable(p,3,genre);setNullable(p,4,publisher);p.setInt(5,b.publicationYear);setNullable(p,6,shelf);p.setInt(7,b.id);if(p.executeUpdate()==0)throw new SQLException("Không tìm thấy đầu sách để cập nhật.");bookId=b.id;}}
            else{try(PreparedStatement p=c.prepareStatement("INSERT INTO sach(ma_sach,ten_sach,the_loai_id,nxb_id,nam_xuat_ban,ke_sach_id) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){p.setString(1,b.code);p.setString(2,b.title);setNullable(p,3,genre);setNullable(p,4,publisher);p.setInt(5,b.publicationYear);setNullable(p,6,shelf);p.executeUpdate();try(ResultSet k=p.getGeneratedKeys()){k.next();bookId=k.getInt(1);}}}
            try(PreparedStatement p=c.prepareStatement("DELETE FROM sach_tac_gia WHERE sach_id=?")){p.setInt(1,bookId);p.executeUpdate();}
            List<Integer> authorIds=new ArrayList<>();
            for(String author:b.authors.split(",")){String name=author.trim();int authorId=ensure(c,"tac_gia","ten_tac_gia",name);if(!authorIds.contains(authorId))authorIds.add(authorId);}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO sach_tac_gia(sach_id,tac_gia_id) VALUES(?,?)")){
                for(int authorId:authorIds){p.setInt(1,bookId);p.setInt(2,authorId);p.executeUpdate();}
            }
            c.commit();
        }catch(Exception e){c.rollback();if(e instanceof SQLException s)throw s;if(e instanceof IllegalArgumentException a)throw a;throw new SQLException(e);}}
    }
    public void hide(int id)throws SQLException{
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement("UPDATE sach SET trang_thai='AN' WHERE id=? AND NOT EXISTS (SELECT 1 FROM ban_sao_sach WHERE sach_id=? AND trang_thai='DANG_MUON')")){p.setInt(1,id);p.setInt(2,id);if(p.executeUpdate()==0)throw new SQLException("Không thể ẩn sách đang có bản sao được mượn.");}
    }
    public List<BookCopy> copies(int id)throws SQLException{
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement("SELECT ma_vach,trang_thai FROM ban_sao_sach WHERE sach_id=? ORDER BY ma_vach")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){List<BookCopy> out=new ArrayList<>();while(r.next())out.add(new BookCopy(r.getString(1),r.getString(2)));return out;}}
    }
    public void addCopy(int id,String barcode)throws SQLException{
        if(barcode.isBlank())throw new IllegalArgumentException("Mã vạch không được để trống.");if(barcode.length()>80)throw new IllegalArgumentException("Mã vạch tối đa 80 ký tự.");
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement("INSERT INTO ban_sao_sach(sach_id,ma_vach,trang_thai) VALUES(?,?,'SAN_SANG')")){p.setInt(1,id);p.setString(2,barcode);p.executeUpdate();}
    }
    public void updateCopyState(int id,String barcode,String state)throws SQLException{
        if(!List.of("SAN_SANG","HONG","MAT").contains(state))throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement("UPDATE ban_sao_sach SET trang_thai=? WHERE sach_id=? AND ma_vach=? AND trang_thai<>'DANG_MUON'")){p.setString(1,state);p.setInt(2,id);p.setString(3,barcode);if(p.executeUpdate()==0)throw new SQLException("Không tìm thấy bản sao hoặc bản sao đang được mượn.");}
    }
    private int ensure(Connection c,String table,String column,String value)throws SQLException{
        if(value==null||value.isBlank())return 0;
        try(PreparedStatement p=c.prepareStatement("SELECT id FROM "+table+" WHERE "+column+"=?")){p.setString(1,value.trim());try(ResultSet r=p.executeQuery()){if(r.next())return r.getInt(1);}}
        try(PreparedStatement p=c.prepareStatement("INSERT INTO "+table+"("+column+") VALUES(?)",Statement.RETURN_GENERATED_KEYS)){p.setString(1,value.trim());p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){r.next();return r.getInt(1);}}
    }
    private static void setNullable(PreparedStatement p,int index,int value)throws SQLException{if(value==0)p.setNull(index,Types.INTEGER);else p.setInt(index,value);}
    private static String nvl(String value){return value==null?"":value;}
}
