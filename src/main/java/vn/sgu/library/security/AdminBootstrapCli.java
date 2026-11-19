package vn.sgu.library.security;

import vn.sgu.library.dao.AuthDAO;

import java.sql.SQLException;

public final class AdminBootstrapCli {
    private AdminBootstrapCli() { }

    public static void main(String[] args) throws SQLException {
        boolean created = new AuthDAO().ensureDefaultAdmin();
        System.out.printf(created
                ? "Đã tạo tài khoản Admin mặc định.%n"
                : "Đã khôi phục tài khoản Admin về thông tin mặc định.%n");
    }
}
