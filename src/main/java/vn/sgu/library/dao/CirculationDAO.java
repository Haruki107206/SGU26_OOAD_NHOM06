package vn.sgu.library.dao;

import vn.sgu.library.Database;
import vn.sgu.library.model.ActiveLoan;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public final class CirculationDAO {
    public String borrow(String readerCode,String barcodeList)throws SQLException{
        LinkedHashSet<String> barcodes=new LinkedHashSet<>();
        for(String raw:barcodeList.split(",")){String barcode=raw.trim();if(!barcode.isEmpty())barcodes.add(barcode);}
        if(barcodes.isEmpty())throw new IllegalArgumentException("Nhập ít nhất một mã vạch sách.");
        if(barcodes.size()>5)throw new IllegalArgumentException("Một phiếu không được vượt quá 5 bản sao.");
        try(Connection c=Database.openConnection()){
            c.setAutoCommit(false);
            try{
                int readerId;
                String readerSql="SELECT d.id FROM doc_gia d JOIN the_doc_gia t ON t.doc_gia_id=d.id WHERE d.ma_doc_gia=? AND d.trang_thai='HOAT_DONG' AND t.trang_thai='HIEU_LUC' AND t.ngay_het_han>=CURRENT_DATE ORDER BY t.id DESC LIMIT 1 FOR UPDATE";
                try(PreparedStatement p=c.prepareStatement(readerSql)){p.setString(1,readerCode.trim());try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Không tìm thấy độc giả có thẻ còn hiệu lực.");readerId=r.getInt(1);}}
                int activeCount;
                try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM ct_phieu_muon ct JOIN phieu_muon pm ON pm.id=ct.phieu_muon_id WHERE pm.doc_gia_id=? AND ct.trang_thai='DANG_MUON'")){p.setInt(1,readerId);try(ResultSet r=p.executeQuery()){r.next();activeCount=r.getInt(1);}}
                if(activeCount+barcodes.size()>5)throw new SQLException("Độc giả đã mượn hoặc sắp vượt giới hạn 5 bản sao.");
                List<Integer> copyIds=new ArrayList<>();
                try(PreparedStatement p=c.prepareStatement("SELECT id FROM ban_sao_sach WHERE ma_vach=? AND trang_thai='SAN_SANG' FOR UPDATE")){
                    for(String barcode:barcodes){p.setString(1,barcode);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Bản sao không tồn tại hoặc không sẵn sàng: "+barcode);copyIds.add(r.getInt(1));}}
                }
                String loanCode="PM-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();int loanId;
                try(PreparedStatement p=c.prepareStatement("INSERT INTO phieu_muon(ma_phieu,doc_gia_id,ngay_muon,han_tra,trang_thai) VALUES(?,?,CURRENT_DATE,DATE_ADD(CURRENT_DATE,INTERVAL 14 DAY),'DANG_MUON')",Statement.RETURN_GENERATED_KEYS)){
                    p.setString(1,loanCode);p.setInt(2,readerId);p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){r.next();loanId=r.getInt(1);}
                }
                try(PreparedStatement detail=c.prepareStatement("INSERT INTO ct_phieu_muon(phieu_muon_id,ban_sao_sach_id,trang_thai) VALUES(?,?,'DANG_MUON')");PreparedStatement update=c.prepareStatement("UPDATE ban_sao_sach SET trang_thai='DANG_MUON' WHERE id=? AND trang_thai='SAN_SANG'")){
                    for(int copyId:copyIds){detail.setInt(1,loanId);detail.setInt(2,copyId);detail.executeUpdate();update.setInt(1,copyId);if(update.executeUpdate()!=1)throw new SQLException("Trạng thái bản sao vừa thay đổi, thử lại.");}
                }
                c.commit();return loanCode;
            }catch(Exception e){c.rollback();if(e instanceof SQLException s)throw s;if(e instanceof IllegalArgumentException a)throw a;throw new SQLException(e);}
        }
    }

    public List<ActiveLoan> findActive()throws SQLException{
        String sql="SELECT ct.id,d.ma_doc_gia,d.ho_ten,s.ten_sach,bs.ma_vach,pm.ngay_muon,pm.han_tra FROM ct_phieu_muon ct JOIN phieu_muon pm ON pm.id=ct.phieu_muon_id JOIN doc_gia d ON d.id=pm.doc_gia_id JOIN ban_sao_sach bs ON bs.id=ct.ban_sao_sach_id JOIN sach s ON s.id=bs.sach_id WHERE ct.trang_thai='DANG_MUON' ORDER BY pm.han_tra,d.ho_ten";
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){
            List<ActiveLoan> rows=new ArrayList<>();while(r.next())rows.add(new ActiveLoan(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getDate(6).toLocalDate(),r.getDate(7).toLocalDate()));return rows;
        }
    }

    public void returnCopy(int detailId,String returnCondition)throws SQLException{
        if(!List.of("TOT","HONG","MAT").contains(returnCondition))throw new IllegalArgumentException("Tình trạng trả không hợp lệ.");
        try(Connection c=Database.openConnection()){
            c.setAutoCommit(false);
            try{
                int copyId,loanId;
                try(PreparedStatement p=c.prepareStatement("SELECT ban_sao_sach_id,phieu_muon_id FROM ct_phieu_muon WHERE id=? AND trang_thai='DANG_MUON' FOR UPDATE")){p.setInt(1,detailId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Dòng mượn không còn ở trạng thái đang mượn.");copyId=r.getInt(1);loanId=r.getInt(2);}}
                String detailStatus=returnCondition.equals("TOT")?"DA_TRA":returnCondition;
                String copyStatus=returnCondition.equals("TOT")?"SAN_SANG":returnCondition;
                try(PreparedStatement p=c.prepareStatement("UPDATE ct_phieu_muon SET trang_thai=?,ngay_tra=CURRENT_DATE WHERE id=?")){p.setString(1,detailStatus);p.setInt(2,detailId);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("UPDATE ban_sao_sach SET trang_thai=? WHERE id=? AND trang_thai='DANG_MUON'")){p.setString(1,copyStatus);p.setInt(2,copyId);if(p.executeUpdate()!=1)throw new SQLException("Không cập nhật được trạng thái bản sao.");}
                try(PreparedStatement p=c.prepareStatement("UPDATE phieu_muon SET trang_thai=IF(NOT EXISTS(SELECT 1 FROM ct_phieu_muon WHERE phieu_muon_id=? AND trang_thai='DANG_MUON'),'DA_TRA','DANG_MUON') WHERE id=?")){p.setInt(1,loanId);p.setInt(2,loanId);p.executeUpdate();}
                c.commit();
            }catch(Exception e){c.rollback();if(e instanceof SQLException s)throw s;throw new SQLException(e);}
        }
    }
}
