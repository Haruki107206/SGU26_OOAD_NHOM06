package vn.sgu.library.model;

public record BookRow(String code,String title,String authors,String genre,String publisher,
                      int year,String shelf,int total,int available) { }
