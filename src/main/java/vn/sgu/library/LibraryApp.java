package vn.sgu.library;

import vn.sgu.library.view.CirculationDialog;
import vn.sgu.library.view.ReaderDialog;
import vn.sgu.library.view.ReaderSelfDialog;
import vn.sgu.library.view.ReportDialog;
import vn.sgu.library.view.AccountDialog;
import vn.sgu.library.view.ChangePasswordDialog;
import vn.sgu.library.view.FineDialog;
import vn.sgu.library.view.LoginDialog;
import vn.sgu.library.controller.CatalogController;
import vn.sgu.library.model.Book;
import vn.sgu.library.model.BookCopy;
import vn.sgu.library.model.BookRow;
import vn.sgu.library.model.UserRole;
import vn.sgu.library.model.UserSession;
import vn.sgu.library.model.SessionExpiredException;
import vn.sgu.library.security.SessionManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

/** Main Swing application for the library system. */
public final class LibraryApp {
    private static JFrame activeFrame;
    private final CatalogController dao = new CatalogController();
    private final UserSession session;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Mã sách", "Tên sách", "Tác giả", "Thể loại", "Nhà xuất bản", "Năm", "Vị trí", "Tổng bản", "Có thể mượn"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField(26);

    public LibraryApp(UserSession session) {
        this.session = session;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginDialog::showLogin);
    }

    public void show() {
        JFrame frame = new JFrame("Quản lý sách và tra cứu - Thư viện SGU");
        activeFrame = frame;
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1050, 580));
        frame.setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(18, 20, 18, 20));
        JPanel north = new JPanel(new BorderLayout(8, 8));
        JPanel heading = new JPanel(new BorderLayout());
        JLabel title = new JLabel("DANH MỤC SÁCH", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        heading.add(title, BorderLayout.WEST);
        JPanel accountActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        accountActions.add(new JLabel(session.username() + " — " + session.role()));
        JButton changePassword = new JButton("Đổi mật khẩu");
        JButton logout = new JButton("Đăng xuất");
        accountActions.add(changePassword);
        accountActions.add(logout);
        heading.add(accountActions, BorderLayout.EAST);
        north.add(heading, BorderLayout.NORTH);

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
        JButton accounts = new JButton("Tài khoản");
        JButton fines = new JButton("Phạt/công nợ");
        JButton myProfile = new JButton("Hồ sơ của tôi");
        bookActions.add(add); bookActions.add(edit); bookActions.add(hide); bookActions.add(copies); bookActions.add(catalogs);
        serviceActions.add(readers); serviceActions.add(circulation); serviceActions.add(fines); serviceActions.add(reports);
        serviceActions.add(accounts); serviceActions.add(myProfile);
        actions.add(searchActions); actions.add(bookActions); actions.add(serviceActions);
        north.add(actions, BorderLayout.CENTER);
        root.add(north, BorderLayout.NORTH);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths={90,270,180,140,200,65,90,80,100};
        for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setContentPane(root);

        boolean readerRole = session.role() == UserRole.READER;
        boolean adminRole = session.role() == UserRole.ADMIN;
        add.setVisible(!readerRole);
        edit.setVisible(!readerRole);
        hide.setVisible(!readerRole);
        copies.setVisible(!readerRole);
        catalogs.setVisible(!readerRole);
        readers.setVisible(!readerRole);
        circulation.setVisible(!readerRole);
        fines.setVisible(!readerRole);
        reports.setVisible(!readerRole);
        accounts.setVisible(adminRole);
        myProfile.setVisible(readerRole);

        find.addActionListener(e -> refresh(search.getText()));
        search.addActionListener(e -> refresh(search.getText()));
        reset.addActionListener(e -> { search.setText(""); refresh(""); });
        add.addActionListener(e -> editBook(frame, null));
        edit.addActionListener(e -> { Integer id = selectedBookId(frame); if (id != null) editBook(frame, id); });
        hide.addActionListener(e -> hideBook(frame));
        copies.addActionListener(e -> manageCopies(frame));
        catalogs.addActionListener(e -> { CatalogManagerDialog.show(frame, dao); refresh(search.getText()); });
        readers.addActionListener(e -> ReaderDialog.show(frame));
        circulation.addActionListener(e -> CirculationDialog.show(frame));
        reports.addActionListener(e -> ReportDialog.show(frame));
        accounts.addActionListener(e -> AccountDialog.show(frame));
        fines.addActionListener(e -> FineDialog.show(frame));
        myProfile.addActionListener(e -> ReaderSelfDialog.show(frame));
        changePassword.addActionListener(e -> ChangePasswordDialog.show(frame));
        logout.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(frame, "Đăng xuất khỏi hệ thống?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                SessionManager.end();
                frame.dispose();
                activeFrame = null;
                LoginDialog.showLogin();
            }
        });
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
            if("23000".equals(sqlEx.getSQLState())) message="Dữ liệu bị trùng hoặc vi phạm ràng buộc CSDL. Hãy kiểm tra mã/tên đã tồn tại.";
            else if(sqlEx.getSQLState()!=null&&sqlEx.getSQLState().startsWith("08")) message="Không kết nối được SQL Server. Kiểm tra dịch vụ, thông tin kết nối và file database/library_catalog.sql.";
        }
        JOptionPane.showMessageDialog(parent, message, "Không thể thực hiện", JOptionPane.ERROR_MESSAGE);
        if (ex instanceof SessionExpiredException) {
            if (parent instanceof Window window && window != activeFrame) {
                window.dispose();
            }
            if (activeFrame != null) {
                activeFrame.dispose();
                activeFrame = null;
            }
            LoginDialog.showLogin();
        }
    }

}
