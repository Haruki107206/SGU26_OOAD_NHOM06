package vn.sgu.library.view;

import vn.sgu.library.LibraryApp;
import vn.sgu.library.controller.ReaderController;
import vn.sgu.library.model.LoanHistory;
import vn.sgu.library.model.Reader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

public final class ReaderSelfDialog {
    private final ReaderController controller = new ReaderController();

    private ReaderSelfDialog() { }

    public static void show(Window owner) {
        new ReaderSelfDialog().open(owner);
    }

    private void open(Window owner) {
        try {
            Reader reader = controller.ownProfile();
            if (reader == null) {
                throw new SQLException("Tài khoản chưa được liên kết với hồ sơ độc giả.");
            }
            JDialog dialog = new JDialog(owner, "Hồ sơ độc giả của tôi", Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setSize(820, 440);
            dialog.setLocationRelativeTo(owner);
            dialog.setLayout(new BorderLayout(8, 8));
            JPanel profile = new JPanel(new GridLayout(0, 2, 8, 4));
            add(profile, "Mã độc giả", reader.code());
            add(profile, "Họ tên", reader.fullName());
            add(profile, "Ngày sinh", reader.birthDate() == null ? "" : reader.birthDate().toString());
            add(profile, "Mã định danh", reader.identityCode());
            add(profile, "Đơn vị/lớp", reader.unit());
            add(profile, "Email", reader.email());
            add(profile, "Điện thoại", reader.phone());
            add(profile, "Mã thẻ", reader.cardCode());
            add(profile, "Hạn thẻ", reader.cardExpiry() == null ? "" : reader.cardExpiry().toString());
            add(profile, "Trạng thái thẻ", reader.cardStatus());
            dialog.add(profile, BorderLayout.NORTH);

            DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Phiếu mượn", "Sách", "Ngày mượn", "Hạn trả", "Ngày trả", "Trạng thái"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            for (LoanHistory loan : controller.ownHistory()) {
                model.addRow(new Object[]{loan.loanCode(), loan.bookTitle(), loan.borrowedOn(),
                        loan.dueOn(), loan.returnedOn() == null ? "" : loan.returnedOn(), loan.status()});
            }
            dialog.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
            JButton close = new JButton("Đóng");
            close.addActionListener(event -> dialog.dispose());
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            actions.add(close);
            dialog.add(actions, BorderLayout.SOUTH);
            dialog.setVisible(true);
        } catch (SQLException ex) {
            LibraryApp.showError(owner, ex);
        }
    }

    private static void add(JPanel panel, String label, String value) {
        panel.add(new JLabel(label));
        panel.add(new JLabel(value == null ? "" : value));
    }
}
