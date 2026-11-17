package vn.sgu.library.model;

import java.time.LocalDate;

public record Reader(int id, String code, String fullName, String identityCode, String unit,
                     String email, String phone, String cardCode, LocalDate cardExpiry,
                     String cardStatus) { }
