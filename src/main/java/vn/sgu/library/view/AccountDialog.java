package vn.sgu.library.view;

import vn.sgu.library.LibraryApp;
import vn.sgu.library.controller.AccountController;
import vn.sgu.library.model.Account;
import vn.sgu.library.model.ReaderOption;
import vn.sgu.library.model.UserRole;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public final class AccountDialog {
    private final AccountController controller = new AccountController();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Tên đăng nhập", "Vai trò", "Trạng thái", "Sai liên tiếp", "Mã độc giả", "Độc giả"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    private AccountDialog() { }

    public static void show(Window owner) {
        new AccountDialog().open(owner);
    }

    private void open(Window owner) {
        JDialog dialog = new JDialog(owner, "Quản lý tài khoản và phân quyền",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(860, 430);
        dialog.setLocationRelativeTo(owner);
        dialog.setLayout(new BorderLayout(8, 8));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton add = new JButton("Tạo tài khoản");
        JButton edit = new JButton("Cập nhật");
        JButton toggle = new JButton("Khóa / mở khóa");
        JButton reload = new JButton("Làm mới");
        JButton close = new JButton("Đóng");
        actions.add(add);
        actions.add(edit);
        actions.add(toggle);
        actions.add(reload);
        actions.add(close);
        dialog.add(actions, BorderLayout.SOUTH);
        add.addActionListener(event -> editAccount(dialog, null));
        edit.addActionListener(event -> {
            Account account = selectedAccount(dialog);
            if (account != null) {
                editAccount(dialog, account);
            }
        });
        toggle.addActionListener(event -> toggleStatus(dialog));
        reload.addActionListener(event -> refresh(dialog));
        close.addActionListener(event -> dialog.dispose());
        refresh(dialog);
        dialog.setVisible(true);
    }

    private void refresh(Component parent) {
        try {
            model.setRowCount(0);
            for (Account account : controller.findAll()) {
                model.addRow(new Object[]{account.id(), account.username(), account.role(),
                        account.status(), account.failedAttempts(), account.readerCode(), account.readerName()});
            }
        } catch (SQLException ex) {
            LibraryApp.showError(parent, ex);
        }
    }

    private Account selectedAccount(Component parent) {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(parent, "Chọn một tài khoản trước.");
            return null;
        }
        int row = table.convertRowIndexToModel(selected);
        int accountId = (Integer) model.getValueAt(row, 0);
        try {
            return controller.findAll().stream()
                    .filter(account -> account.id() == accountId)
                    .findFirst().orElse(null);
        } catch (SQLException ex) {
            LibraryApp.showError(parent, ex);
            return null;
        }
    }

    private void toggleStatus(Component parent) {
        Account account = selectedAccount(parent);
        if (account == null) {
            return;
        }
        boolean activate = "KHOA".equals(account.status());
        String action = activate ? "mở khóa" : "khóa";
        if (JOptionPane.showConfirmDialog(parent, "Xác nhận " + action + " tài khoản "
                + account.username() + "?", "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.setActive(account.id(), activate);
            refresh(parent);
        } catch (SQLException ex) {
            LibraryApp.showError(parent, ex);
        }
    }

    private void editAccount(Component parent, Account existing) {
        if (existing != null && existing.role() == UserRole.ADMIN) {
            JOptionPane.showMessageDialog(parent,
                    "Tài khoản Admin mặc định không thể sửa tại màn hình này.");
            return;
        }
        try {
            JTextField username = new JTextField(existing == null ? "" : existing.username(), 22);
            JPasswordField password = new JPasswordField(22);
            JComboBox<UserRole> role = new JComboBox<>(
                    new UserRole[]{UserRole.LIBRARIAN, UserRole.READER});
            if (existing != null) {
                role.setSelectedItem(existing.role());
            } else {
                role.setSelectedItem(UserRole.LIBRARIAN);
            }
            List<ReaderOption> readers = new ArrayList<>(controller.findUnlinkedReaders());
            if (existing != null && existing.readerId() != null) {
                readers.add(new ReaderOption(existing.readerId(), existing.readerCode(), existing.readerName()));
            }
            JComboBox<ReaderOption> reader = new JComboBox<>(readers.toArray(ReaderOption[]::new));
            if (existing != null && existing.readerId() != null) {
                for (int i = 0; i < reader.getItemCount(); i++) {
                    if (reader.getItemAt(i).id() == existing.readerId()) {
                        reader.setSelectedIndex(i);
                        break;
                    }
                }
            }
            reader.setEnabled(role.getSelectedItem() == UserRole.READER);
            role.addActionListener(event -> reader.setEnabled(role.getSelectedItem() == UserRole.READER));
            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.add(new JLabel("Tên đăng nhập"));
            form.add(username);
            if (existing == null) {
                form.add(new JLabel("Mật khẩu ban đầu"));
                form.add(password);
            }
            form.add(new JLabel("Vai trò"));
            form.add(role);
            form.add(new JLabel("Hồ sơ độc giả (bắt buộc với DocGia)"));
            form.add(reader);
            if (JOptionPane.showConfirmDialog(parent, form,
                    existing == null ? "Tạo tài khoản" : "Cập nhật tài khoản",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                password.setText("");
                return;
            }

            UserRole selectedRole = (UserRole) role.getSelectedItem();
            ReaderOption linkedReader = selectedRole == UserRole.READER
                    ? (ReaderOption) reader.getSelectedItem() : null;
            Integer readerId = linkedReader == null ? null : linkedReader.id();
            if (existing == null) {
                char[] suppliedPassword = password.getPassword();
                try {
                    controller.create(username.getText(), suppliedPassword, selectedRole, readerId);
                } finally {
                    Arrays.fill(suppliedPassword, '\0');
                    password.setText("");
                }
            } else {
                controller.update(existing.id(), username.getText(), selectedRole, readerId);
            }
            refresh(parent);
        } catch (SQLException | IllegalArgumentException ex) {
            LibraryApp.showError(parent, ex);
        }
    }
}
