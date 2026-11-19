package vn.sgu.library.security;

import vn.sgu.library.model.UserSession;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class SessionManager {
    private static final AtomicReference<UserSession> CURRENT = new AtomicReference<>();

    private SessionManager() { }

    public static void start(UserSession session) {
        CURRENT.set(session);
    }

    public static Optional<UserSession> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void end() {
        CURRENT.set(null);
    }
}
