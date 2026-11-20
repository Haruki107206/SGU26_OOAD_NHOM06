package vn.sgu.library.model;

import java.time.LocalDate;

public record LoanHistory(String loanCode, String bookTitle, LocalDate borrowedOn,
                          LocalDate dueOn, LocalDate returnedOn, String status,
                          int renewalCount) { }
