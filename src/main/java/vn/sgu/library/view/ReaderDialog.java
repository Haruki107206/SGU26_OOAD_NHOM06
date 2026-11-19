package vn.sgu.library.view;

import vn.sgu.library.LibraryApp;
import vn.sgu.library.controller.ReaderController;
import vn.sgu.library.model.Reader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class ReaderDialog {
    private final ReaderController controller = new ReaderController();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Mã độc giả", "Họ tên", "Ngày sinh", "Mã định danh", "Đơn vị",
                    "Email", "Điện thoại", "Mã thẻ", "Hạn thẻ", "Trạng thái thẻ", "Card ID", "Lý do khóa"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField(18);

    private ReaderDialog() { }

    public static void show(Window owner) {
        new ReaderDialog().open(owner);
    }

    private void open(Window owner) {
        JDialog dialog = new JDialog(owner, "Quản lý độc giả và thẻ", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(1180, 540);
        dialog.setLocationRelativeTo(owner);
        dialog.setLayout(new BorderLayout(8, 8));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Tìm độc giả:"));
        top.add(search);
        JButton find = new JButton("Tìm");
        JButton reload = new JButton("Tải lại");
        top.add(find);
        top.add(reload);
        dialog.add(top, BorderLayout.NORTH);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        hideColumn(0);
        hideColumn(11);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton add = new JButton("Lập thẻ độc giả");
        JButton edit = new JButton("Sửa hồ sơ");
        JButton lock = new JButton("Khóa thẻ");
        JButton renew = new JButton("Gia hạn thẻ");
        JButton close = new JButton("Đóng");
        actions.add(add);
        actions.add(edit);
        actions.add(lock);
        actions.add(renew);
        actions.add(close);
        dialog.add(actions, BorderLayout.SOUTH);
        find.addActionListener(event -> refresh(dialog));
        search.addActionListener(event -> refresh(dialog));
        reload.addActionListener(event -> {
            search.setText("");
            refresh(dialog);
        });
        add.addActionListener(event -> editReader(dialog, null));
        edit.addActionListener(event -> {
            Integer id = selectedReaderId(dialog);
            if (id != null) {
                editReader(dialog, id);
            }
        });
        lock.addActionListener(event -> lockSelectedCard(dialog));
        renew.addActionListener(event -> renewSelectedCard(dialog));
        close.addActionListener(event -> dialog.dispose());
        refresh(dialog);
        dialog.setVisible(true);
    }

    private void hideColumn(int index) {
        table.getColumnModel().getColumn(index).setMinWidth(0);
        table.getColumnModel().getColumn(index).setMaxWidth(0);
        table.getColumnModel().getColumn(index).setPreferredWidth(0);
    }

    private Integer selectedReaderId(Component parent) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(parent, "Chọn một độc giả trước.");
            return null;
        }
        return (Integer) model.getValueAt(table.convertRowIndexToModel(row), 0);
    }

    private Integer selectedCardId(Component parent) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(parent, "Chọn một độc giả trước.");
            return null;
        }
        int cardId = (Integer) model.getValueAt(table.convertRowIndexToModel(row), 11);
        if (cardId == 0) {
            JOptionPane.showMessageDialog(parent, "Độc giả chưa có thẻ.");
            return null;
        }
        return cardId;
    }

    private void lockSelectedCard(Component parent) {
        Integer cardId = selectedCardId(parent);
        if (cardId == null) {
            return;
        }
        String reason = JOptionPane.showInputDialog(parent, "Nhập lý do khóa thẻ:");
        if (reason == null) {
            return;
        }
        try {
            controller.lockCard(cardId, reason);
            refresh(parent);
        } catch (SQLException | IllegalArgumentException ex) {
            LibraryApp.showError(parent, ex);
        }
    }

    private void renewSelectedCard(Component parent) {
        Integer id = selectedReaderId(parent);
        if (id == null) {
            return;
        }
        String value = JOptionPane.showInputDialog(parent, "Số tháng gia hạn:");
        if (value == null) {
            return;
        }
        try {
            int months = Integer.parseInt(value.trim());
            controller.renewCard(id, months);
            refresh(parent);
        } catch (NumberFormatException ex) {
            LibraryApp.showError(parent, new IllegalArgumentException("Số tháng phải là số nguyên dương."));
        } catch (SQLException | IllegalArgumentException ex) {
            LibraryApp.showError(parent, ex);
        }
    }

    private void refresh(Component parent) {
        try {
            model.setRowCount(0);
            for (Reader reader : controller.find(search.getText())) {
                model.addRow(new Object[]{reader.id(), reader.code(), reader.fullName(),
                        reader.birthDate() == null ? "" : reader.birthDate(), reader.identityCode(),
                        reader.unit(), reader.email(), reader.phone(), reader.cardCode(),
                        reader.cardExpiry() == null ? "" : reader.cardExpiry(), reader.cardStatus(),
                        reader.cardId(), reader.cardLockReason()});
            }
        } catch (SQLException ex) {
            LibraryApp.showError(parent, ex);
        }
    }

    private void editReader(Component parent, Integer id) {
        Object[] current = null;
        if (id != null) {
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            current = new Object[]{model.getValueAt(row, 2), model.getValueAt(row, 3),
                    model.getValueAt(row, 4), model.getValueAt(row, 5), model.getValueAt(row, 6),
                    model.getValueAt(row, 7)};
        }
        JTextField name = new JTextField(current == null ? "" : String.valueOf(current[0]), 24);
        JTextField birthDate = new JTextField(current == null ? "" : String.valueOf(current[1]), 12);
        JTextField identity = new JTextField(current == null ? "" : String.valueOf(current[2]), 20);
        JTextField unit = new JTextField(current == null ? "" : String.valueOf(current[3]), 20);
        JTextField email = new JTextField(current == null ? "" : String.valueOf(current[4]), 24);
        JTextField phone = new JTextField(current == null ? "" : String.valueOf(current[5]), 16);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        field(form, "Họ tên", name);
        field(form, "Ngày sinh (yyyy-MM-dd, có thể bỏ trống)", birthDate);
        field(form, "MSSV/CCCD", identity);
        field(form, "Lớp/đơn vị", unit);
        field(form, "Email", email);
        field(form, "Điện thoại", phone);
        if (JOptionPane.showConfirmDialog(parent, form, id == null ? "Lập thẻ độc giả" : "Cập nhật hồ sơ",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            LocalDate birth = birthDate.getText().isBlank() ? null : LocalDate.parse(birthDate.getText().trim());
            if (id == null) {
                controller.create(name.getText(), birth, identity.getText(), unit.getText(),
                        email.getText(), phone.getText());
            } else {
                controller.update(id, name.getText(), birth, identity.getText(), unit.getText(),
                        email.getText(), phone.getText());
            }
            refresh(parent);
        } catch (DateTimeParseException ex) {
            LibraryApp.showError(parent, new IllegalArgumentException("Ngày sinh phải theo định dạng yyyy-MM-dd."));
        } catch (SQLException | IllegalArgumentException ex) {
            LibraryApp.showError(parent, ex);
        }
    }

    private static void field(JPanel form, String label, Component input) {
        form.add(new JLabel(label));
        form.add(input);
    }
}
