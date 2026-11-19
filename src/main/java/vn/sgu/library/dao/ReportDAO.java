package vn.sgu.library.dao;

import vn.sgu.library.Database;
import vn.sgu.library.model.LibraryStatistics;
import vn.sgu.library.model.PopularBook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ReportDAO {
    public LibraryStatistics statistics()throws SQLException{
        String sql="SELECT (SELECT COUNT(*) FROM sach WHERE trang_thai='HOAT_DONG'),(SELECT COUNT(*) FROM ban_sao_sach),(SELECT COUNT(*) FROM ban_sao_sach WHERE trang_thai='SAN_SANG'),(SELECT COUNT(*) FROM doc_gia WHERE trang_thai='HOAT_DONG'),(SELECT COUNT(*) FROM ct_phieu_muon WHERE trang_thai='DANG_MUON'),(SELECT COUNT(*) FROM ct_phieu_muon ct JOIN phieu_muon pm ON pm.id=ct.phieu_muon_id WHERE ct.trang_thai='DANG_MUON' AND pm.han_tra<CAST(GETDATE() AS DATE))";
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){
            r.next();return new LibraryStatistics(r.getInt(1),r.getInt(2),r.getInt(3),r.getInt(4),r.getInt(5),r.getInt(6));
        }
    }
    public List<PopularBook> popularBooks()throws SQLException{
        String sql="SELECT TOP (10) s.ma_sach,s.ten_sach,COUNT(*) AS so_luot FROM ct_phieu_muon ct JOIN ban_sao_sach bs ON bs.id=ct.ban_sao_sach_id JOIN sach s ON s.id=bs.sach_id GROUP BY s.id,s.ma_sach,s.ten_sach ORDER BY so_luot DESC,s.ten_sach";
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){
            List<PopularBook> rows=new ArrayList<>();while(r.next())rows.add(new PopularBook(r.getString(1),r.getString(2),r.getInt(3)));return rows;
        }
    }
}
