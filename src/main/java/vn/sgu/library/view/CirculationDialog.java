package vn.sgu.library.view;

import vn.sgu.library.LibraryApp;
import vn.sgu.library.controller.CirculationController;
import vn.sgu.library.model.ActiveLoan;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

public final class CirculationDialog {
    private final CirculationController controller=new CirculationController();
    private final DefaultTableModel model=new DefaultTableModel(new Object[]{"ID","Mã độc giả","Độc giả","Tên sách","Mã vạch","Ngày mượn","Hạn trả"},0){@Override public boolean isCellEditable(int r,int c){return false;}};
    private final JTable table=new JTable(model);
    private CirculationDialog(){}
    public static void show(Window owner){new CirculationDialog().open(owner);}

    private void open(Window owner){
        JDialog dialog=new JDialog(owner,"Mượn và trả sách",Dialog.ModalityType.APPLICATION_MODAL);dialog.setSize(900,500);dialog.setLocationRelativeTo(owner);dialog.setLayout(new BorderLayout(8,8));
        JPanel borrow=new JPanel(new FlowLayout(FlowLayout.LEFT));JTextField reader=new JTextField(14),barcodes=new JTextField(32);JButton issue=new JButton("Lập phiếu mượn");borrow.add(new JLabel("Mã độc giả:"));borrow.add(reader);borrow.add(new JLabel("Mã vạch (nhiều mã cách nhau dấu phẩy):"));borrow.add(barcodes);borrow.add(issue);dialog.add(borrow,BorderLayout.NORTH);
        table.setAutoCreateRowSorter(true);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.getColumnModel().getColumn(0).setMinWidth(0);table.getColumnModel().getColumn(0).setMaxWidth(0);dialog.add(new JScrollPane(table),BorderLayout.CENTER);
        JPanel bottom=new JPanel(new FlowLayout(FlowLayout.RIGHT));JButton refresh=new JButton("Làm mới"),returnCopy=new JButton("Ghi nhận trả cuốn đã chọn"),close=new JButton("Đóng");bottom.add(refresh);bottom.add(returnCopy);bottom.add(close);dialog.add(bottom,BorderLayout.SOUTH);
        issue.addActionListener(e->{try{String code=controller.borrow(reader.getText(),barcodes.getText());JOptionPane.showMessageDialog(dialog,"Đã tạo phiếu "+code+". Hạn trả mặc định 14 ngày.");barcodes.setText("");load(dialog);}catch(SQLException|IllegalArgumentException ex){LibraryApp.showError(dialog,ex);}});
        refresh.addActionListener(e->load(dialog));returnCopy.addActionListener(e->{int row=table.getSelectedRow();if(row<0){JOptionPane.showMessageDialog(dialog,"Chọn dòng mượn cần trả.");return;}int id=Integer.parseInt(String.valueOf(model.getValueAt(table.convertRowIndexToModel(row),0)));Object[] options={"TOT","HONG","MAT"};Object condition=JOptionPane.showInputDialog(dialog,"Tình trạng cuốn sách khi nhận:","Ghi nhận trả",JOptionPane.PLAIN_MESSAGE,null,options,"TOT");if(condition==null)return;try{controller.returnCopy(id,String.valueOf(condition));load(dialog);}catch(SQLException|IllegalArgumentException ex){LibraryApp.showError(dialog,ex);}});
        close.addActionListener(e->dialog.dispose());load(dialog);dialog.setVisible(true);
    }
    private void load(Component parent){try{model.setRowCount(0);for(ActiveLoan loan:controller.activeLoans())model.addRow(new Object[]{loan.detailId(),loan.readerCode(),loan.readerName(),loan.bookTitle(),loan.barcode(),loan.borrowedOn(),loan.dueOn()});}catch(SQLException ex){LibraryApp.showError(parent,ex);}}
}
