package vn.sgu.library.model;

import java.math.BigDecimal;

public record FineLine(Integer loanDetailId, String violationType, Integer overdueDays,
                       BigDecimal amount, String description) { }
