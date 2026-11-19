package vn.sgu.library;

import vn.sgu.library.controller.CatalogController;
import vn.sgu.library.dao.CatalogDAO;
import vn.sgu.library.model.CatalogItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

/** Maintains the reference lists used by book records. */
final class CatalogManagerDialog {
    private final Window owner;
    private final CatalogController controller;
    private final JComboBox<CatalogDAO.Category> catalog = new JComboBox<>(CatalogDAO.Category.values());
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Tên"}, 0) {
        @Override public boolean isCellEditable(int row,int column){return false;}
    };
    private final JTable table = new JTable(model);

    private CatalogManagerDialog(Window owner, CatalogController controller) {
        this.owner=owner;
        this.controller=controller;
    }
    static void show(Window owner, CatalogController controller) {
        new CatalogManagerDialog(owner, controller).open();
    }

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

    private CatalogDAO.Category selectedCatalog(){return (CatalogDAO.Category)catalog.getSelectedItem();}
    private Integer selectedId(Component parent){int row=table.getSelectedRow();if(row<0){JOptionPane.showMessageDialog(parent,"Chọn một dòng trước.");return null;}return Integer.valueOf(String.valueOf(model.getValueAt(table.convertRowIndexToModel(row),0)));}
    private void refresh(Component parent){
        CatalogDAO.Category kind=selectedCatalog();if(kind==null)return;
        try{
            model.setRowCount(0);for(CatalogItem item:controller.findCatalog(kind))model.addRow(new Object[]{item.id(),item.name()});
            table.getColumnModel().getColumn(0).setPreferredWidth(50);table.getColumnModel().getColumn(1).setPreferredWidth(400);
        }catch(SQLException ex){LibraryApp.showError(parent,ex);}
    }
    private void save(Component parent,boolean editing){
        CatalogDAO.Category kind=selectedCatalog();if(kind==null)return;Integer id=editing?selectedId(parent):null;if(editing&&id==null)return;
        String current="";if(editing)current=String.valueOf(model.getValueAt(table.convertRowIndexToModel(table.getSelectedRow()),1));
        String value=JOptionPane.showInputDialog(parent,kind.label()+" mới:",current);
        if(value==null)return;
        try{controller.saveCatalog(kind,id,value);refresh(parent);}
        catch(SQLException|IllegalArgumentException ex){LibraryApp.showError(parent,ex);}
    }
    private void delete(Component parent){
        CatalogDAO.Category kind=selectedCatalog();Integer id=selectedId(parent);if(kind==null||id==null)return;
        String name=String.valueOf(model.getValueAt(table.convertRowIndexToModel(table.getSelectedRow()),1));
        if(JOptionPane.showConfirmDialog(parent,"Xóa mục "+name+"?","Xác nhận xóa",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{controller.deleteCatalog(kind,id);refresh(parent);}
        catch(SQLException ex){LibraryApp.showError(parent,ex);}
    }
}
