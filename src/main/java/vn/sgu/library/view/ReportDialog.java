package vn.sgu.library.view;

import vn.sgu.library.LibraryApp;
import vn.sgu.library.controller.ReportController;
import vn.sgu.library.model.LibraryStatistics;
import vn.sgu.library.model.PopularBook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;

public final class ReportDialog {
    private final ReportController controller=new ReportController();
    private final JLabel stats=new JLabel(" ");
    private final DefaultTableModel model=new DefaultTableModel(new Object[]{"Mã sách","Tên sách","Lượt mượn"},0){@Override public boolean isCellEditable(int r,int c){return false;}};
    private final JTable table=new JTable(model);
    private List<PopularBook> popular=List.of();
    private ReportDialog(){}
    public static void show(Window owner){new ReportDialog().open(owner);}

    private void open(Window owner){
        JDialog dialog=new JDialog(owner,"Báo cáo tổng quan thư viện",Dialog.ModalityType.APPLICATION_MODAL);dialog.setSize(760,470);dialog.setLocationRelativeTo(owner);dialog.setLayout(new BorderLayout(10,10));
        stats.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));dialog.add(stats,BorderLayout.NORTH);table.setAutoCreateRowSorter(true);dialog.add(new JScrollPane(table),BorderLayout.CENTER);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));JButton refresh=new JButton("Làm mới"),export=new JButton("Xuất CSV"),close=new JButton("Đóng");buttons.add(refresh);buttons.add(export);buttons.add(close);dialog.add(buttons,BorderLayout.SOUTH);
        refresh.addActionListener(e->load(dialog));export.addActionListener(e->export(dialog));close.addActionListener(e->dialog.dispose());load(dialog);dialog.setVisible(true);
    }
    private void load(Component parent){
        try{LibraryStatistics s=controller.statistics();stats.setText("Đầu sách: "+s.activeTitles()+"   |   Bản sao: "+s.physicalCopies()+"   |   Sẵn sàng: "+s.availableCopies()+"   |   Độc giả: "+s.activeReaders()+"   |   Đang mượn: "+s.activeLoans()+"   |   Quá hạn: "+s.overdueLoans());popular=controller.popularBooks();model.setRowCount(0);for(PopularBook book:popular)model.addRow(new Object[]{book.code(),book.title(),book.loanCount()});}
        catch(SQLException ex){LibraryApp.showError(parent,ex);}
    }
    private void export(Component parent){
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File("bao-cao-thu-vien.csv"));if(chooser.showSaveDialog(parent)!=JFileChooser.APPROVE_OPTION)return;
        StringBuilder csv=new StringBuilder("\uFEFFMa sach,Ten sach,Luot muon\r\n");for(PopularBook b:popular)csv.append(csv(b.code())).append(',').append(csv(b.title())).append(',').append(b.loanCount()).append("\r\n");
        try{Files.writeString(chooser.getSelectedFile().toPath(),csv,StandardCharsets.UTF_8);JOptionPane.showMessageDialog(parent,"Đã xuất báo cáo.");}catch(IOException ex){LibraryApp.showError(parent,ex);}
    }
    private static String csv(String value){return "\""+value.replace("\"","\"\"")+"\"";}
}
