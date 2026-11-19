package vn.sgu.library.model;

public record Account(int id, String username, UserRole role, String status,
                      int failedAttempts, Integer readerId, String readerCode,
                      String readerName) { }
