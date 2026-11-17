package vn.sgu.library.view;

import vn.sgu.library.LibraryApp;
import vn.sgu.library.controller.ReaderController;
import vn.sgu.library.model.Reader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

public final class ReaderDialog {
    private final ReaderController controller=new ReaderController();
    private final DefaultTableModel model=new DefaultTableModel(new Object[]{"ID","Mã độc giả","Họ tên","Mã định danh","Đơn vị","Email","Điện thoại","Mã thẻ","Hạn thẻ","Trạng thái thẻ"},0){@Override public boolean isCellEditable(int r,int c){return false;}};
    private final JTable table=new JTable(model);
    private final JTextField search=new JTextField(18);
    private ReaderDialog(){}
    public static void show(Window owner){new ReaderDialog().open(owner);}

    private void open(Window owner){
        JDialog dialog=new JDialog(owner,"Quản lý độc giả và thẻ",Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(1040,520);dialog.setLocationRelativeTo(owner);dialog.setLayout(new BorderLayout(8,8));
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT));top.add(new JLabel("Tìm độc giả:"));top.add(search);
        JButton find=new JButton("Tìm"),reload=new JButton("Tải lại");top.add(find);top.add(reload);dialog.add(top,BorderLayout.NORTH);
        table.setAutoCreateRowSorter(true);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.getColumnModel().getColumn(0).setMinWidth(0);table.getColumnModel().getColumn(0).setMaxWidth(0);
        dialog.add(new JScrollPane(table),BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));JButton add=new JButton("Lập thẻ độc giả"),edit=new JButton("Sửa hồ sơ"),lock=new JButton("Khóa thẻ"),renew=new JButton("Gia hạn thẻ"),close=new JButton("Đóng");
        actions.add(add);actions.add(edit);actions.add(lock);actions.add(renew);actions.add(close);dialog.add(actions,BorderLayout.SOUTH);
        find.addActionListener(e->refresh(dialog));search.addActionListener(e->refresh(dialog));reload.addActionListener(e->{search.setText("");refresh(dialog);});
        add.addActionListener(e->editReader(dialog,null));edit.addActionListener(e->{Integer id=selectedId(dialog);if(id!=null)editReader(dialog,id);});
        lock.addActionListener(e->{Integer id=selectedId(dialog);if(id==null)return;try{controller.lockCard(id,true);refresh(dialog);}catch(SQLException ex){LibraryApp.showError(dialog,ex);}});
        renew.addActionListener(e->{Integer id=selectedId(dialog);if(id==null)return;try{controller.renewCard(id);refresh(dialog);}catch(SQLException ex){LibraryApp.showError(dialog,ex);}});
        close.addActionListener(e->dialog.dispose());refresh(dialog);dialog.setVisible(true);
    }

    private Integer selectedId(Component parent){int row=table.getSelectedRow();if(row<0){JOptionPane.showMessageDialog(parent,"Chọn một độc giả trước.");return null;}return Integer.valueOf(String.valueOf(model.getValueAt(table.convertRowIndexToModel(row),0)));}
    private void refresh(Component parent){try{model.setRowCount(0);for(Reader r:controller.find(search.getText()))model.addRow(new Object[]{r.id(),r.code(),r.fullName(),r.identityCode(),r.unit(),r.email(),r.phone(),r.cardCode(),r.cardExpiry()==null?"":r.cardExpiry(),r.cardStatus()});}catch(SQLException ex){LibraryApp.showError(parent,ex);}}
    private void editReader(Component parent,Integer id){
        Object[] current=null;if(id!=null){int row=table.convertRowIndexToModel(table.getSelectedRow());current=new Object[]{model.getValueAt(row,2),model.getValueAt(row,3),model.getValueAt(row,4),model.getValueAt(row,5),model.getValueAt(row,6)};}
        JTextField name=new JTextField(current==null?"":String.valueOf(current[0]),24),identity=new JTextField(current==null?"":String.valueOf(current[1]),20),unit=new JTextField(current==null?"":String.valueOf(current[2]),20),email=new JTextField(current==null?"":String.valueOf(current[3]),24),phone=new JTextField(current==null?"":String.valueOf(current[4]),16);
        JPanel form=new JPanel(new GridLayout(0,2,8,8));field(form,"Họ tên",name);field(form,"MSSV/CCCD",identity);field(form,"Lớp/đơn vị",unit);field(form,"Email",email);field(form,"Điện thoại",phone);
        if(JOptionPane.showConfirmDialog(parent,form,id==null?"Lập thẻ độc giả":"Cập nhật hồ sơ",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{if(id==null)controller.create(name.getText(),identity.getText(),unit.getText(),email.getText(),phone.getText());else controller.update(id,name.getText(),identity.getText(),unit.getText(),email.getText(),phone.getText());refresh(parent);}
        catch(SQLException|IllegalArgumentException ex){LibraryApp.showError(parent,ex);}
    }
    private static void field(JPanel form,String label,Component input){form.add(new JLabel(label));form.add(input);}
}
