package vn.sgu.library.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Fine(long id, String readerCode, String readerName, String staffUsername,
                   LocalDateTime createdAt, BigDecimal total, String status,
                   String paymentMethod, LocalDateTime paidAt) { }
