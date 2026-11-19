package vn.sgu.library.dao;

import vn.sgu.library.Database;
import vn.sgu.library.model.CatalogItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class CatalogDAO {
    public List<CatalogItem> findAll(Category category) throws SQLException {
        String sql = "SELECT id," + category.nameColumn + " FROM " + category.table
                + " ORDER BY " + category.nameColumn;
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<CatalogItem> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new CatalogItem(result.getInt(1), result.getString(2)));
            }
            return rows;
        }
    }

    public void save(Category category, Integer id, String name) throws SQLException {
        String sql = id == null
                ? "INSERT INTO " + category.table + "(" + category.nameColumn + ") VALUES(?)"
                : "UPDATE " + category.table + " SET " + category.nameColumn + "=? WHERE id=?";
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            if (id != null) {
                statement.setInt(2, id);
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Không tìm thấy mục danh mục để cập nhật.");
                }
            } else {
                statement.executeUpdate();
            }
        }
    }

    public void delete(Category category, int id) throws SQLException {
        String sql = "DELETE FROM " + category.table + " WHERE id=? AND NOT EXISTS "
                + "(SELECT 1 FROM " + category.referenceTable + " WHERE "
                + category.referenceColumn + "=?)";
        try (Connection connection = Database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, id);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Danh mục đang được sách sử dụng hoặc không còn tồn tại.");
            }
        }
    }

    public enum Category {
        AUTHORS("Tác giả", "tac_gia", "ten_tac_gia", 180, "sach_tac_gia", "tac_gia_id"),
        GENRES("Thể loại", "the_loai", "ten_the_loai", 120, "sach", "the_loai_id"),
        PUBLISHERS("Nhà xuất bản", "nha_xuat_ban", "ten_nxb", 180, "sach", "nxb_id"),
        SHELVES("Kệ sách", "ke_sach", "ma_ke", 80, "sach", "ke_sach_id");

        private final String label;
        private final String table;
        private final String nameColumn;
        private final int maxLength;
        private final String referenceTable;
        private final String referenceColumn;

        Category(String label, String table, String nameColumn, int maxLength,
                 String referenceTable, String referenceColumn) {
            this.label = label;
            this.table = table;
            this.nameColumn = nameColumn;
            this.maxLength = maxLength;
            this.referenceTable = referenceTable;
            this.referenceColumn = referenceColumn;
        }

        public String label() { return label; }
        public int maxLength() { return maxLength; }

        @Override
        public String toString() { return label; }
    }
}
