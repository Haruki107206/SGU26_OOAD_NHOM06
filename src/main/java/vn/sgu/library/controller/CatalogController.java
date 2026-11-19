package vn.sgu.library.controller;

import vn.sgu.library.dao.BookDAO;
import vn.sgu.library.dao.CatalogDAO;
import vn.sgu.library.model.Book;
import vn.sgu.library.model.BookCopy;
import vn.sgu.library.model.BookRow;
import vn.sgu.library.model.CatalogItem;
import vn.sgu.library.model.UserRole;

import java.sql.SQLException;
import java.util.List;

public final class CatalogController {
    private final BookDAO books = new BookDAO();
    private final CatalogDAO catalogs = new CatalogDAO();

    public List<BookRow> search(String text) throws SQLException {
        AccessControl.require(UserRole.ADMIN, UserRole.LIBRARIAN, UserRole.READER);
        return books.search(text);
    }

    public int idByCode(String code) throws SQLException {
        AccessControl.requireStaff();
        return books.idByCode(code);
    }

    public Book get(int id) throws SQLException {
        AccessControl.requireStaff();
        return books.get(id);
    }

    public void save(Book book) throws SQLException {
        AccessControl.requireStaff();
        books.save(book);
    }

    public void hide(int id) throws SQLException {
        AccessControl.requireStaff();
        books.hide(id);
    }

    public List<BookCopy> copies(int id) throws SQLException {
        AccessControl.requireStaff();
        return books.copies(id);
    }

    public void addCopy(int id, String barcode) throws SQLException {
        AccessControl.requireStaff();
        books.addCopy(id, barcode);
    }

    public void updateCopyState(int id, String barcode, String state) throws SQLException {
        AccessControl.requireStaff();
        books.updateCopyState(id, barcode, state);
    }

    public List<CatalogItem> findCatalog(CatalogDAO.Category category) throws SQLException {
        AccessControl.requireStaff();
        return catalogs.findAll(category);
    }

    public void saveCatalog(CatalogDAO.Category category, Integer id, String name)
            throws SQLException {
        AccessControl.requireStaff();
        if (name == null || name.isBlank() || name.trim().length() > category.maxLength()) {
            throw new IllegalArgumentException("Tên danh mục bắt buộc và không vượt quá "
                    + category.maxLength() + " ký tự.");
        }
        catalogs.save(category, id, name.trim());
    }

    public void deleteCatalog(CatalogDAO.Category category, int id) throws SQLException {
        AccessControl.requireStaff();
        catalogs.delete(category, id);
    }
}
