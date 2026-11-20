package vn.sgu.library.model;

import java.math.BigDecimal;

public record ReturnResult(int readerId, int loanDetailId, String condition, int overdueDays,
                           Long fineId, BigDecimal fineTotal) { }
