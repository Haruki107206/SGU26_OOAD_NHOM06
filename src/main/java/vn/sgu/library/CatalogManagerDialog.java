package vn.sgu.library;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Maintains the reference lists used by book records. */
final class CatalogManagerDialog {
    private final Window owner;
    private final JComboBox<Catalog> catalog = new JComboBox<>(Catalog.values());
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Tên"}, 0) {
        @Override public boolean isCellEditable(int row,int column){return false;}
    };
    private final JTable table = new JTable(model);

    private CatalogManagerDialog(Window owner) { this.owner=owner; }
    static void show(Window owner) { new CatalogManagerDialog(owner).open(); }

    private void open() {
        JDialog dialog=new JDialog(owner,"Quản lý danh mục nền",Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(560,420);dialog.setLocationRelativeTo(owner);dialog.setLayout(new BorderLayout(10,10));
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT));top.add(new JLabel("Loại danh mục:"));top.add(catalog);
        dialog.add(top,BorderLayout.NORTH);dialog.add(new JScrollPane(table),BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton add=new JButton("Thêm"),edit=new JButton("Sửa tên"),remove=new JButton("Xóa mục chưa dùng"),close=new JButton("Đóng");
        actions.add(add);actions.add(edit);actions.add(remove);actions.add(close);dialog.add(actions,BorderLayout.SOUTH);
        catalog.addActionListener(e->refresh(dialog));
        add.addActionListener(e->save(dialog,false));edit.addActionListener(e->save(dialog,true));
        remove.addActionListener(e->delete(dialog));close.addActionListener(e->dialog.dispose());
        refresh(dialog);dialog.setVisible(true);
    }

    private Catalog selectedCatalog(){return (Catalog)catalog.getSelectedItem();}
    private Integer selectedId(Component parent){int row=table.getSelectedRow();if(row<0){JOptionPane.showMessageDialog(parent,"Chọn một dòng trước.");return null;}return Integer.valueOf(String.valueOf(model.getValueAt(table.convertRowIndexToModel(row),0)));}
    private void refresh(Component parent){
        Catalog kind=selectedCatalog();if(kind==null)return;
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement("SELECT id,"+kind.nameColumn+" FROM "+kind.table+" ORDER BY "+kind.nameColumn);ResultSet r=p.executeQuery()){
            model.setRowCount(0);while(r.next())model.addRow(new Object[]{r.getInt(1),r.getString(2)});
            table.getColumnModel().getColumn(0).setPreferredWidth(50);table.getColumnModel().getColumn(1).setPreferredWidth(400);
        }catch(SQLException ex){LibraryApp.showError(parent,ex);}
    }
    private void save(Component parent,boolean editing){
        Catalog kind=selectedCatalog();if(kind==null)return;Integer id=editing?selectedId(parent):null;if(editing&&id==null)return;
        String current="";if(editing)current=String.valueOf(model.getValueAt(table.convertRowIndexToModel(table.getSelectedRow()),1));
        String value=JOptionPane.showInputDialog(parent,kind.label+" mới:",current);
        if(value==null)return;value=value.trim();if(value.isBlank()){JOptionPane.showMessageDialog(parent,"Tên danh mục không được để trống.");return;}
        if(value.length()>kind.maxLength){JOptionPane.showMessageDialog(parent,"Tối đa "+kind.maxLength+" ký tự.");return;}
        String sql=editing?"UPDATE "+kind.table+" SET "+kind.nameColumn+"=? WHERE id=?":"INSERT INTO "+kind.table+"("+kind.nameColumn+") VALUES(?)";
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,value);if(editing)p.setInt(2,id);p.executeUpdate();refresh(parent);}
        catch(SQLException ex){LibraryApp.showError(parent,ex);}
    }
    private void delete(Component parent){
        Catalog kind=selectedCatalog();Integer id=selectedId(parent);if(kind==null||id==null)return;
        String name=String.valueOf(model.getValueAt(table.convertRowIndexToModel(table.getSelectedRow()),1));
        if(JOptionPane.showConfirmDialog(parent,"Xóa mục "+name+"?","Xác nhận xóa",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        String sql="DELETE FROM "+kind.table+" WHERE id=? AND NOT EXISTS (SELECT 1 FROM "+kind.referenceTable+" WHERE "+kind.referenceColumn+"=?)";
        try(Connection c=Database.openConnection();PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,id);p.setInt(2,id);if(p.executeUpdate()==0)throw new SQLException("Danh mục đang được sách sử dụng; không thể xóa.");refresh(parent);}
        catch(SQLException ex){LibraryApp.showError(parent,ex);}
    }

    private enum Catalog {
        AUTHORS("Tác giả","tac_gia","ten_tac_gia",180,"sach_tac_gia","tac_gia_id"),
        GENRES("Thể loại","the_loai","ten_the_loai",120,"sach","the_loai_id"),
        PUBLISHERS("Nhà xuất bản","nha_xuat_ban","ten_nxb",180,"sach","nxb_id"),
        SHELVES("Kệ sách","ke_sach","ma_ke",80,"sach","ke_sach_id");
        final String label,table,nameColumn,referenceTable,referenceColumn;final int maxLength;
        Catalog(String label,String table,String nameColumn,int maxLength,String referenceTable,String referenceColumn){this.label=label;this.table=table;this.nameColumn=nameColumn;this.maxLength=maxLength;this.referenceTable=referenceTable;this.referenceColumn=referenceColumn;}
        @Override public String toString(){return label;}
    }
}
