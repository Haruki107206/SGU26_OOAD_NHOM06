package vn.sgu.library.model;

import java.sql.SQLException;

public final class SessionExpiredException extends SQLException {
    public SessionExpiredException(String message) {
        super(message);
    }
}
