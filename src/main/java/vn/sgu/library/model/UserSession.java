package vn.sgu.library.model;

public record UserSession(int accountId, String username, UserRole role, Integer readerId) { }
