package vn.sgu.library.model;

public enum UserRole {
    ADMIN("Admin"),
    LIBRARIAN("ThuThu"),
    READER("DocGia");

    private final String databaseValue;

    UserRole(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static UserRole fromDatabase(String value) {
        for (UserRole role : values()) {
            if (role.databaseValue.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Vai trò không được hỗ trợ: " + value);
    }

    @Override
    public String toString() {
        return switch (this) {
            case ADMIN -> "Admin";
            case LIBRARIAN -> "Thủ thư";
            case READER -> "Độc giả";
        };
    }
}
