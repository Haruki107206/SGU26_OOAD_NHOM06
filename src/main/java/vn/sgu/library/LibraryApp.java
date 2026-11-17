package vn.sgu.library;

import vn.sgu.library.view.CirculationDialog;
import vn.sgu.library.view.ReaderDialog;
import vn.sgu.library.view.ReportDialog;
import vn.sgu.library.dao.BookDAO;
import vn.sgu.library.model.Book;
import vn.sgu.library.model.BookCopy;
import vn.sgu.library.model.BookRow;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

/** Standalone catalog and copy-management module for the library project. */
public final class LibraryApp {
    private final BookDAO dao = new BookDAO();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Mã sách", "Tên sách", "Tác giả", "Thể loại", "Nhà xuất bản", "Năm", "Vị trí", "Tổng bản", "Có thể mượn"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField(26);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibraryApp().show());
    }

    private void show() {
        JFrame frame = new JFrame("Quản lý sách và tra cứu - Thư viện SGU");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1050, 580));
        frame.setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(18, 20, 18, 20));
        JLabel title = new JLabel("DANH MỤC SÁCH", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        root.add(title, BorderLayout.NORTH);

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        JPanel searchActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        searchActions.add(new JLabel("Tìm theo mã, tên, tác giả, thể loại hoặc năm:"));
        searchActions.add(search);
        JButton find = new JButton("Tìm kiếm");
        JButton reset = new JButton("Tải lại");
        searchActions.add(find); searchActions.add(reset);
        JPanel bookActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        JPanel serviceActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        JButton add = new JButton("Thêm sách");
        JButton edit = new JButton("Sửa sách");
        JButton hide = new JButton("Ẩn sách");
        JButton copies = new JButton("Quản lý bản sao");
        JButton catalogs = new JButton("Danh mục nền");
        JButton readers = new JButton("Độc giả/thẻ");
        JButton circulation = new JButton("Mượn/trả");
        JButton reports = new JButton("Báo cáo");
        bookActions.add(add); bookActions.add(edit); bookActions.add(hide); bookActions.add(copies); bookActions.add(catalogs);
        serviceActions.add(readers); serviceActions.add(circulation); serviceActions.add(reports);
        actions.add(searchActions); actions.add(bookActions); actions.add(serviceActions);
        root.add(actions, BorderLayout.PAGE_START);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths={90,270,180,140,200,65,90,80,100};
        for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setContentPane(root);

        find.addActionListener(e -> refresh(search.getText()));
        search.addActionListener(e -> refresh(search.getText()));
        reset.addActionListener(e -> { search.setText(""); refresh(""); });
        add.addActionListener(e -> editBook(frame, null));
        edit.addActionListener(e -> { Integer id = selectedBookId(frame); if (id != null) editBook(frame, id); });
        hide.addActionListener(e -> hideBook(frame));
        copies.addActionListener(e -> manageCopies(frame));
        catalogs.addActionListener(e -> { CatalogManagerDialog.show(frame); refresh(search.getText()); });
        readers.addActionListener(e -> ReaderDialog.show(frame));
        circulation.addActionListener(e -> CirculationDialog.show(frame));
        reports.addActionListener(e -> ReportDialog.show(frame));
        refresh("");
        frame.setVisible(true);
    }

    private void refresh(String query) {
        try {
            model.setRowCount(0);
            for (BookRow row : dao.search(query)) model.addRow(new Object[]{row.code(), row.title(), row.authors(),
                    row.genre(), row.publisher(), row.year()==0?"":row.year(), row.shelf(), row.total(), row.available()});
        } catch (SQLException ex) { showError(null, ex); }
    }

    private Integer selectedBookId(Component parent) {
        int view = table.getSelectedRow();
        if (view < 0) { JOptionPane.showMessageDialog(parent, "Vui lòng chọn một đầu sách."); return null; }
        int row = table.convertRowIndexToModel(view);
        try { return dao.idByCode(String.valueOf(model.getValueAt(row, 0))); }
        catch (SQLException ex) { showError(parent, ex); return null; }
    }

    private void editBook(Component parent, Integer id) {
        try {
            Book book = id == null ? new Book() : dao.get(id);
            if (book == null) { JOptionPane.showMessageDialog(parent, "Không tìm thấy sách."); return; }
            book.id = id == null ? 0 : id;
            JTextField code = new JTextField(book.code, 18), title = new JTextField(book.title, 28);
            JTextField authors = new JTextField(book.authors, 28), genre = new JTextField(book.genre, 20);
            JTextField publisher = new JTextField(book.publisher, 20), year = new JTextField(book.publicationYear == 0 ? "" : String.valueOf(book.publicationYear), 8);
            JTextField shelf = new JTextField(book.shelf, 15);
            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            addField(form, "Mã sách", code); addField(form, "Tên sách", title); addField(form, "Tác giả (nhiều tên, ngăn bằng dấu phẩy)", authors);
            addField(form, "Thể loại", genre); addField(form, "Nhà xuất bản", publisher); addField(form, "Năm xuất bản", year); addField(form, "Vị trí kệ", shelf);
            int result = JOptionPane.showConfirmDialog(parent, form, id == null ? "Thêm đầu sách" : "Cập nhật đầu sách", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            book.code = code.getText().trim(); book.title = title.getText().trim(); book.authors = authors.getText().trim();
            book.genre = genre.getText().trim(); book.publisher = publisher.getText().trim(); book.shelf = shelf.getText().trim();
            try { book.publicationYear = year.getText().isBlank() ? 0 : Integer.parseInt(year.getText().trim()); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("Năm xuất bản phải là số."); }
            dao.save(book);
            refresh(search.getText());
        } catch (SQLException | IllegalArgumentException ex) { showError(parent, ex); }
    }

    private void hideBook(Component parent) {
        Integer id = selectedBookId(parent);
        if (id == null) return;
        if (JOptionPane.showConfirmDialog(parent, "Ẩn đầu sách đã chọn? Lịch sử và bản sao sẽ được giữ lại.", "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try { dao.hide(id); refresh(search.getText()); }
        catch (SQLException ex) { showError(parent, ex); }
    }

    private void manageCopies(Component parent) {
        Integer id = selectedBookId(parent);
        if (id == null) return;
        try {
            Book book = dao.get(id);
            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Bản sao - " + book.title, Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setSize(620, 390); dialog.setLocationRelativeTo(parent); dialog.setLayout(new BorderLayout(8,8));
            DefaultTableModel copyModel = new DefaultTableModel(new Object[]{"Mã vạch", "Trạng thái"},0) {
                @Override public boolean isCellEditable(int row,int column){return false;}
            };
            JTable copyTable = new JTable(copyModel); copyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            dialog.add(new JScrollPane(copyTable),BorderLayout.CENTER);
            JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton add=new JButton("Thêm bản sao"); JButton state=new JButton("Cập nhật trạng thái"); JButton close=new JButton("Đóng");
            buttons.add(add);buttons.add(state);buttons.add(close);dialog.add(buttons,BorderLayout.SOUTH);
            Runnable load=()->{try{copyModel.setRowCount(0);for(BookCopy row:dao.copies(id))copyModel.addRow(new Object[]{row.barcode(),row.status()});}catch(SQLException ex){showError(dialog,ex);}};
            load.run();
            add.addActionListener(e->{String barcode=JOptionPane.showInputDialog(dialog,"Nhập mã vạch cho bản sao mới:");if(barcode==null)return;try{dao.addCopy(id,barcode.trim());load.run();refresh(search.getText());}catch(SQLException|IllegalArgumentException ex){showError(dialog,ex);}});
            state.addActionListener(e->{int row=copyTable.getSelectedRow();if(row<0){JOptionPane.showMessageDialog(dialog,"Vui lòng chọn một bản sao.");return;}String barcode=String.valueOf(copyModel.getValueAt(row,0)),old=String.valueOf(copyModel.getValueAt(row,1));if("DANG_MUON".equals(old)){JOptionPane.showMessageDialog(dialog,"Trạng thái ĐANG_MUON chỉ được cập nhật bởi phân hệ mượn/trả.");return;}Object[] states={"SAN_SANG","HONG","MAT"};Object selected=JOptionPane.showInputDialog(dialog,"Chọn trạng thái mới:","Cập nhật bản sao",JOptionPane.PLAIN_MESSAGE,null,states,old);if(selected==null)return;try{dao.updateCopyState(id,barcode,String.valueOf(selected));load.run();refresh(search.getText());}catch(SQLException ex){showError(dialog,ex);}});
            close.addActionListener(e->dialog.dispose());dialog.setVisible(true);
        } catch (SQLException ex) { showError(parent, ex); }
    }

    private static void addField(JPanel panel, String label, Component input) { panel.add(new JLabel(label)); panel.add(input); }
    public static void showError(Component parent, Exception ex) {
        String message=ex.getMessage()==null?"Đã xảy ra lỗi không xác định.":ex.getMessage();
        if(ex instanceof SQLException sqlEx){
            if("23000".equals(sqlEx.getSQLState())) message="Mã sách hoặc mã vạch đã tồn tại. Hãy dùng mã khác.";
            else if(sqlEx.getSQLState()!=null&&sqlEx.getSQLState().startsWith("08")) message="Không kết nối được CSDL. Kiểm tra MySQL/MariaDB, thông tin kết nối và file database/library_catalog.sql.";
        }
        JOptionPane.showMessageDialog(parent, message, "Không thể thực hiện", JOptionPane.ERROR_MESSAGE);
    }

}
