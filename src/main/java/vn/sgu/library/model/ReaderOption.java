package vn.sgu.library.model;

public record ReaderOption(int id, String code, String fullName) {
    @Override
    public String toString() {
        return code + " - " + fullName;
    }
}
