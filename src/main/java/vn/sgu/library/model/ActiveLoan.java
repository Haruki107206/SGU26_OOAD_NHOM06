package vn.sgu.library.model;

import java.time.LocalDate;

public record ActiveLoan(int detailId, String readerCode, String readerName, String bookTitle,
                         String barcode, LocalDate borrowedOn, LocalDate dueOn,
                         int renewalCount, int overdueDays) { }
