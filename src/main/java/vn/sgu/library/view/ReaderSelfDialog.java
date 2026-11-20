package vn.sgu.library.view;

import vn.sgu.library.LibraryApp;
import vn.sgu.library.controller.ReaderController;
import vn.sgu.library.model.LoanHistory;
import vn.sgu.library.model.Reader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

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
            List<LoanHistory> loans = controller.ownHistory();

            JDialog dialog = new JDialog(owner, "Hồ sơ độc giả của tôi",
                    Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setSize(980, 650);
            dialog.setMinimumSize(new Dimension(820, 560));
            dialog.setLocationRelativeTo(owner);
            dialog.setLayout(new BorderLayout(12, 12));

            JButton close = new JButton("Đóng");
            close.addActionListener(event -> dialog.dispose());
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            actions.add(close);

            JPanel top = new JPanel(new BorderLayout(0, 14));
            top.add(createHeader(), BorderLayout.NORTH);
            top.add(createProfileSection(reader), BorderLayout.CENTER);
            JPanel main = new JPanel(new BorderLayout(12, 12));
            main.setBorder(new EmptyBorder(18, 20, 12, 20));
            main.add(top, BorderLayout.NORTH);
            main.add(createLoanSection(loans), BorderLayout.CENTER);
            main.add(actions, BorderLayout.SOUTH);
            dialog.setContentPane(main);
            dialog.getRootPane().setDefaultButton(close);
            dialog.setVisible(true);
        } catch (SQLException ex) {
            LibraryApp.showError(owner, ex);
        }
    }

    private static JPanel createHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Hồ sơ độc giả");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 21f));
        JLabel subtitle = new JLabel("Thông tin cá nhân, thẻ thư viện và các lượt mượn của bạn.");
        subtitle.setForeground(new Color(90, 98, 108));
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        header.add(title);
        header.add(subtitle);
        return header;
    }

    private static JPanel createProfileSection(Reader reader) {
        JPanel profile = new JPanel(new GridLayout(1, 2, 14, 0));
        profile.add(createDetailsPanel(reader));
        profile.add(createCardPanel(reader));
        return profile;
    }

    private static JPanel createDetailsPanel(Reader reader) {
        JPanel details = new JPanel(new GridBagLayout());
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Thông tin cá nhân"),
                new EmptyBorder(7, 10, 9, 10)));
        addField(details, 0, "Mã độc giả", reader.code());
        addField(details, 1, "Họ tên", reader.fullName());
        addField(details, 2, "Ngày sinh", format(reader.birthDate()));
        addField(details, 3, "Mã định danh", reader.identityCode());
        addField(details, 4, "Đơn vị/lớp", reader.unit());
        addField(details, 5, "Email", reader.email());
        addField(details, 6, "Điện thoại", reader.phone());
        return details;
    }

    private static JPanel createCardPanel(Reader reader) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Thẻ thư viện"),
                new EmptyBorder(7, 10, 9, 10)));
        addField(card, 0, "Mã thẻ", reader.cardCode());
        addField(card, 1, "Hạn thẻ", format(reader.cardExpiry()));
        addField(card, 2, "Trạng thái", displayCardStatus(reader.cardStatus()));
        JLabel note = new JLabel("Thẻ cần còn hiệu lực để mượn hoặc gia hạn sách.");
        note.setForeground(new Color(90, 98, 108));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(10, 0, 0, 0);
        card.add(note, constraints);
        return card;
    }

    private static JPanel createLoanSection(List<LoanHistory> loans) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setBorder(BorderFactory.createTitledBorder("Lịch sử mượn (" + loans.size() + ")"));

        if (loans.isEmpty()) {
            JPanel empty = new JPanel();
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.setBorder(new EmptyBorder(28, 12, 28, 12));
            JLabel title = new JLabel("Bạn chưa có lượt mượn nào.");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel message = new JLabel(
                    "Muốn mượn sách, hãy tìm sách và cung cấp mã vạch cho thủ thư lập phiếu.");
            message.setForeground(new Color(90, 98, 108));
            message.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.add(Box.createVerticalGlue());
            empty.add(title);
            empty.add(Box.createVerticalStrut(8));
            empty.add(message);
            empty.add(Box.createVerticalGlue());
            section.add(empty, BorderLayout.CENTER);
            return section;
        }

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Phiếu mượn", "Sách", "Ngày mượn", "Hạn trả", "Ngày trả",
                        "Trạng thái", "Số lần gia hạn"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (LoanHistory loan : loans) {
            model.addRow(new Object[]{loan.loanCode(), loan.bookTitle(), loan.borrowedOn(),
                    loan.dueOn(), loan.returnedOn() == null ? "Chưa trả" : loan.returnedOn(),
                    displayStatus(loan.status()), loan.renewalCount()});
        }
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(6).setPreferredWidth(110);
        section.add(new JScrollPane(table), BorderLayout.CENTER);
        return section;
    }

    private static void addField(JPanel panel, int row, String label, String value) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.insets = new Insets(4, 2, 4, 10);
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.gridx = 0;
        constraints.weightx = 0;
        panel.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        JLabel valueLabel = new JLabel(value == null || value.isBlank() ? "Chưa cập nhật" : value);
        panel.add(valueLabel, constraints);
    }

    private static String displayStatus(String status) {
        return switch (status) {
            case "DANG_MUON" -> "Đang mượn";
            case "DA_TRA" -> "Đã trả";
            case "HONG" -> "Hỏng";
            case "MAT" -> "Mất";
            default -> status == null ? "" : status;
        };
    }

    private static String displayCardStatus(String status) {
        return switch (status == null ? "" : status) {
            case "HIEU_LUC" -> "Còn hiệu lực";
            case "HET_HAN" -> "Hết hạn";
            case "KHOA" -> "Bị khóa";
            default -> status == null ? "" : status;
        };
    }

    private static String format(Object value) {
        return value == null ? null : value.toString();
    }
}
