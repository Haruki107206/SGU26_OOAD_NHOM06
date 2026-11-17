package vn.sgu.library.model;

public record LibraryStatistics(int activeTitles,int physicalCopies,int availableCopies,
                                int activeReaders,int activeLoans,int overdueLoans) { }
