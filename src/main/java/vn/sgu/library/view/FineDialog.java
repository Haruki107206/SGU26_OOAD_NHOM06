package vn.sgu.library.view;

import vn.sgu.library.LibraryApp;
import vn.sgu.library.controller.FineController;
import vn.sgu.library.controller.ReaderController;
import vn.sgu.library.model.Fine;
import vn.sgu.library.model.FineLine;
import vn.sgu.library.model.Reader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class FineDialog {
    private final FineController controller = new FineController();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Mã độc giả", "Độc giả", "Người lập", "Ngày lập",
                    "Tổng tiền", "Trạng thái", "Hình thức", "Ngày thanh toán"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    private FineDialog() { }

    public static void show(Window owner) {
        new FineDialog().open(owner);
    }

    public static void createForIncident(Window owner, vn.sgu.library.model.ReturnResult returned) {
        String type = returned.overdueDays() > 0 ? "QuaHan"
                : "HONG".equals(returned.condition()) ? "Hong" : "Mat";
        if (returned.overdueDays() > 0 && !"TOT".equals(returned.condition())) {
            JOptionPane.showMessageDialog(owner,
                    "Sách vừa quá hạn vừa " + returned.condition()
                            + ". Hãy thêm đủ hai chi tiết vi phạm vào cùng phiếu phạt.");
        }
        new FineDialog().createFine(owner, returned.readerId(), returned.loanDetailId(), type,
                returned.overdueDays());
    }

    private void open(Window owner) {
        JDialog dialog = new JDialog(owner, "Phiếu phạt và công nợ", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(980, 480);
        dialog.setLocationRelativeTo(owner);
        dialog.setLayout(new BorderLayout(8, 8));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton create = new JButton("Lập phiếu phạt");
        JButton details = new JButton("Chi tiết");
        JButton payment = new JButton("Ghi nhận đã thu");
        JButton reload = new JButton("Làm mới");
        JButton close = new JButton("Đóng");
        actions.add(create);
        actions.add(details);
        actions.add(payment);
        actions.add(reload);
        actions.add(close);
        dialog.add(actions, BorderLayout.SOUTH);
        create.addActionListener(event -> createFine(dialog));
        details.addActionListener(event -> showDetails(dialog));
        payment.addActionListener(event -> markPaid(dialog));
        reload.addActionListener(event -> refresh(dialog));
        close.addActionListener(event -> dialog.dispose());
        refresh(dialog);
        dialog.setVisible(true);
    }

    private void refresh(Component parent) {
        try {
            model.setRowCount(0);
            for (Fine fine : controller.findAll()) {
                model.addRow(new Object[]{fine.id(), fine.readerCode(), fine.readerName(),
                        fine.staffUsername(), fine.createdAt(), fine.total(), fine.status(),
                        fine.paymentMethod() == null ? "" : fine.paymentMethod(),
                        fine.paidAt() == null ? "" : fine.paidAt()});
            }
        } catch (SQLException ex) {
            LibraryApp.showError(parent, ex);
        }
    }

    private Long selectedFineId(Component parent) {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(parent, "Chọn một phiếu phạt trước.");
            return null;
        }
        return ((Number) model.getValueAt(table.convertRowIndexToModel(selected), 0)).longValue();
    }

    private void createFine(Component parent) {
        createFine(parent, null, null, null, null);
    }

    private void createFine(Component parent, Integer readerId, Integer loanDetailId,
                            String violationType, Integer overdueDays) {
        try {
            List<Reader> readers = new ReaderController().find("");
            if (readers.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Chưa có độc giả để lập phiếu phạt.");
                return;
            }
            JComboBox<Reader> reader = new JComboBox<>(readers.toArray(Reader[]::new));
            if (readerId != null) {
                for (int i = 0; i < reader.getItemCount(); i++) {
                    if (reader.getItemAt(i).id() == readerId) {
                        reader.setSelectedIndex(i);
                        break;
                    }
                }
            }
            DefaultTableModel details = new DefaultTableModel(
                    new Object[]{"Dòng mượn ID", "Loại vi phạm", "Ngày trễ", "Số tiền", "Mô tả"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            JTable detailTable = new JTable(details);
            JComboBox<String> type = new JComboBox<>(new String[]{"QuaHan", "Hong", "Mat", "Khac"});
            JTextField loanId = new JTextField(8);
            JTextField daysLate = new JTextField(6);
            JTextField amount = new JTextField(10);
            JTextField description = new JTextField(24);
            if (violationType != null) {
                type.setSelectedItem(violationType);
                loanId.setText(String.valueOf(loanDetailId));
                if (overdueDays != null && overdueDays > 0) {
                    daysLate.setText(String.valueOf(overdueDays));
                }
            }
            JPanel fields = new JPanel(new GridLayout(0, 2, 6, 6));
            fields.add(new JLabel("Loại vi phạm"));
            fields.add(type);
            fields.add(new JLabel("Dòng mượn ID (nếu liên quan)"));
            fields.add(loanId);
            fields.add(new JLabel("Số ngày trễ"));
            fields.add(daysLate);
            fields.add(new JLabel("Số tiền phạt/đền bù"));
            fields.add(amount);
            fields.add(new JLabel("Mô tả"));
            fields.add(description);
            JButton addLine = new JButton("Thêm chi tiết");
            JPanel content = new JPanel(new BorderLayout(6, 6));
            content.add(reader, BorderLayout.NORTH);
            content.add(fields, BorderLayout.CENTER);
            content.add(addLine, BorderLayout.EAST);
            addLine.addActionListener(event -> addDetail(parent, details, type, loanId,
                    daysLate, amount, description));
            JScrollPane detailScroll = new JScrollPane(detailTable);
            detailScroll.setPreferredSize(new Dimension(760, 160));
            content.add(detailScroll, BorderLayout.SOUTH);
            content.setPreferredSize(new Dimension(820, 330));

            while (true) {
                int result = JOptionPane.showConfirmDialog(parent, content, "Lập phiếu phạt",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
                if (details.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(parent, "Thêm ít nhất một chi tiết vi phạm.");
                    continue;
                }
                List<FineLine> lines = new ArrayList<>();
                for (int row = 0; row < details.getRowCount(); row++) {
                    Object rawLoanId = details.getValueAt(row, 0);
                    Object rawDays = details.getValueAt(row, 2);
                    lines.add(new FineLine(rawLoanId == null ? null : Integer.valueOf(rawLoanId.toString()),
                            details.getValueAt(row, 1).toString(),
                            rawDays == null ? null : Integer.valueOf(rawDays.toString()),
                            new BigDecimal(details.getValueAt(row, 3).toString()),
                            String.valueOf(details.getValueAt(row, 4))));
                }
                long fineId = controller.create(((Reader) reader.getSelectedItem()).id(), lines);
                JOptionPane.showMessageDialog(parent, "Đã lập phiếu phạt #" + fineId + ".");
                refresh(parent);
                return;
            }
        } catch (SQLException | IllegalArgumentException ex) {
            LibraryApp.showError(parent, ex);
        }
    }

    private void addDetail(Component parent, DefaultTableModel details, JComboBox<String> type,
                           JTextField loanId, JTextField daysLate, JTextField amount,
                           JTextField description) {
        try {
            Integer linkedLoanId = loanId.getText().isBlank()
                    ? null : Integer.valueOf(loanId.getText().trim());
            Integer days = daysLate.getText().isBlank()
                    ? null : Integer.valueOf(daysLate.getText().trim());
            BigDecimal money = new BigDecimal(amount.getText().trim());
            FineLine line = new FineLine(linkedLoanId, String.valueOf(type.getSelectedItem()),
                    days, money, description.getText());
            FineController.validateDetail(line);
            details.addRow(new Object[]{linkedLoanId, line.violationType(), days,
                    money.setScale(Math.min(money.scale(), 2)), line.description()});
            loanId.setText("");
            daysLate.setText("");
            amount.setText("");
            description.setText("");
        } catch (NumberFormatException ex) {
            LibraryApp.showError(parent, new IllegalArgumentException("Nhập số nguyên hợp lệ cho dòng mượn/ngày trễ và số tiền."));
        } catch (IllegalArgumentException ex) {
            LibraryApp.showError(parent, ex);
        }
    }

    private void showDetails(Component parent) {
        Long fineId = selectedFineId(parent);
        if (fineId == null) {
            return;
        }
        try {
            StringBuilder text = new StringBuilder();
            for (FineLine line : controller.findDetails(fineId)) {
                text.append(line.violationType())
                        .append(" | dòng mượn: ").append(line.loanDetailId() == null ? "" : line.loanDetailId())
                        .append(" | ngày trễ: ").append(line.overdueDays() == null ? "" : line.overdueDays())
                        .append(" | số tiền: ").append(line.amount())
                        .append(" | ").append(line.description() == null ? "" : line.description())
                        .append(System.lineSeparator());
            }
            JOptionPane.showMessageDialog(parent, text.isEmpty() ? "Phiếu chưa có chi tiết." : text.toString(),
                    "Chi tiết phiếu phạt #" + fineId, JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            LibraryApp.showError(parent, ex);
        }
    }

    private void markPaid(Component parent) {
        Long fineId = selectedFineId(parent);
        if (fineId == null) {
            return;
        }
        Object method = JOptionPane.showInputDialog(parent, "Chọn hình thức thanh toán đã nhận:",
                "Ghi nhận thanh toán", JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"TienMat", "QR"}, "TienMat");
        if (method == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(parent, "Xác nhận đã thu đủ khoản phạt này?",
                "Xác nhận thu tiền", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.markPaid(fineId, method.toString());
            refresh(parent);
        } catch (SQLException | IllegalArgumentException ex) {
            LibraryApp.showError(parent, ex);
        }
    }

}
