package vn.sgu.library.model;

public record ReturnResult(int readerId, int loanDetailId, String condition, int overdueDays) { }
