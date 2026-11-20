package vn.sgu.library.view;

import vn.sgu.library.LibraryApp;
import vn.sgu.library.controller.CirculationController;
import vn.sgu.library.model.ActiveLoan;
import vn.sgu.library.model.ReturnResult;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public final class CirculationDialog {
    private final CirculationController controller = new CirculationController();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Mã độc giả", "Độc giả", "Tên sách", "Mã vạch",
                    "Ngày mượn", "Hạn trả", "Gia hạn", "Ngày trễ"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    private CirculationDialog() { }

    public static void show(Window owner) {
        new CirculationDialog().open(owner);
    }

    private void open(Window owner) {
        JDialog dialog = new JDialog(owner, "Mượn và trả sách",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(1080, 540);
        dialog.setMinimumSize(new Dimension(900, 450));
        dialog.setLocationRelativeTo(owner);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel borrow = new JPanel(new GridBagLayout());
        borrow.setBorder(BorderFactory.createTitledBorder("Lập phiếu mượn"));
        GridBagConstraints fields = new GridBagConstraints();
        fields.insets = new Insets(5, 6, 5, 6);
        fields.gridy = 0;
        fields.gridx = 0;
        fields.anchor = GridBagConstraints.LINE_START;
        borrow.add(new JLabel("Mã độc giả"), fields);
        JTextField reader = new JTextField(13);
        fields.gridx = 1;
        borrow.add(reader, fields);
        fields.gridx = 2;
        borrow.add(new JLabel("Mã vạch (phân cách bằng dấu phẩy)"), fields);
        JTextField barcodes = new JTextField(32);
        fields.gridx = 3;
        fields.weightx = 1;
        fields.fill = GridBagConstraints.HORIZONTAL;
        borrow.add(barcodes, fields);
        JButton issue = new JButton("Lập phiếu");
        fields.gridx = 4;
        fields.weightx = 0;
        fields.fill = GridBagConstraints.NONE;
        borrow.add(issue, fields);
        dialog.add(borrow, BorderLayout.NORTH);

        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("Làm mới");
        JButton renew = new JButton("Gia hạn thêm 7 ngày");
        JButton returnCopy = new JButton("Ghi nhận trả");
        JButton close = new JButton("Đóng");
        bottom.add(refresh);
        bottom.add(renew);
        bottom.add(returnCopy);
        bottom.add(close);
        dialog.add(bottom, BorderLayout.SOUTH);

        issue.addActionListener(event -> {
            try {
                String code = controller.borrow(reader.getText(), barcodes.getText());
                JOptionPane.showMessageDialog(dialog,
                        "Đã tạo phiếu " + code + ". Mỗi cuốn có hạn trả sau 14 ngày.");
                barcodes.setText("");
                load(dialog);
            } catch (SQLException | IllegalArgumentException ex) {
                LibraryApp.showError(dialog, ex);
            }
        });
        refresh.addActionListener(event -> load(dialog));
        renew.addActionListener(event -> renewSelected(dialog));
        returnCopy.addActionListener(event -> returnSelected(dialog));
        close.addActionListener(event -> dialog.dispose());

        load(dialog);
        dialog.setVisible(true);
    }

    private void renewSelected(Component parent) {
        Integer detailId = selectedDetailId(parent);
        if (detailId == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(parent,
                "Gia hạn lượt mượn đã chọn thêm 7 ngày? Mỗi lượt chỉ được gia hạn một lần.",
                "Xác nhận gia hạn", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            LocalDate newDueDate = controller.renew(detailId);
            JOptionPane.showMessageDialog(parent, "Đã gia hạn. Hạn trả mới: " + newDueDate + ".");
            load(parent);
        } catch (SQLException | IllegalArgumentException ex) {
            LibraryApp.showError(parent, ex);
        }
    }

    private void returnSelected(Component parent) {
        Integer detailId = selectedDetailId(parent);
        if (detailId == null) {
            return;
        }
        Object selected = JOptionPane.showInputDialog(parent,
                "Chọn tình trạng sách khi nhận:", "Ghi nhận trả",
                JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"Tốt", "Hỏng", "Mất"}, "Tốt");
        if (selected == null) {
            return;
        }
        String condition = switch (selected.toString()) {
            case "Hỏng" -> "HONG";
            case "Mất" -> "MAT";
            default -> "TOT";
        };
        BigDecimal compensation = null;
        if (!"TOT".equals(condition)) {
            String value = JOptionPane.showInputDialog(parent,
                    "Nhập tiền bồi thường sách " + selected + " (đồng):");
            if (value == null) {
                return;
            }
            try {
                compensation = new BigDecimal(value.trim());
                if (compensation.signum() < 0 || compensation.scale() > 2
                        || compensation.precision() > 12) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                LibraryApp.showError(parent, new IllegalArgumentException(
                        "Tiền bồi thường phải là số không âm, tối đa 2 chữ số thập phân."));
                return;
            }
        }

        try {
            ReturnResult result = controller.returnCopy(detailId, condition, compensation);
            String message = "Đã ghi nhận trả sách.";
            if (result.overdueDays() > 0) {
                message += "\nQuá hạn " + result.overdueDays() + " ngày.";
            }
            if (result.fineId() != null) {
                message += "\nĐã lập phiếu phạt #" + result.fineId()
                        + ", tổng tiền " + result.fineTotal().toPlainString() + " đồng.";
            } else {
                message += "\nKhông phát sinh tiền phạt.";
            }
            JOptionPane.showMessageDialog(parent, message);
            load(parent);
        } catch (SQLException | IllegalArgumentException ex) {
            LibraryApp.showError(parent, ex);
        }
    }

    private Integer selectedDetailId(Component parent) {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(parent, "Chọn một lượt mượn trước.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(selected);
        return ((Number) model.getValueAt(modelRow, 0)).intValue();
    }

    private void load(Component parent) {
        try {
            model.setRowCount(0);
            for (ActiveLoan loan : controller.activeLoans()) {
                model.addRow(new Object[]{loan.detailId(), loan.readerCode(), loan.readerName(),
                        loan.bookTitle(), loan.barcode(), loan.borrowedOn(), loan.dueOn(),
                        loan.renewalCount(), loan.overdueDays()});
            }
        } catch (SQLException ex) {
            LibraryApp.showError(parent, ex);
        }
    }
}
