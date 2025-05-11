package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import models.Admin;
import models.Book;
import models.BorrowRecord;
import models.Member;
import models.User;
import utils.Security;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:library.db";

    public static void resetDatabase() {
        String DB_URL = "jdbc:sqlite:library.db";
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS borrow_records");
            stmt.execute("DROP TABLE IF EXISTS books");
            stmt.execute("DROP TABLE IF EXISTS users");

            initializeDatabase();  // Recreate the schema

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS books ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "title TEXT UNIQUE, "
                    + "author TEXT, "
                    + "category TEXT, "
                    + "available INTEGER)");

            stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT UNIQUE NOT NULL, "
                    + "password TEXT NOT NULL, "
                    + "role TEXT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS borrow_records ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "book_id INTEGER, "
                    + "user_id INTEGER, "
                    + "due_date TEXT, "
                    + "borrow_date TEXT, "
                    + "FOREIGN KEY(book_id) REFERENCES books(id), "
                    + "FOREIGN KEY(user_id) REFERENCES users(id))");

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM users");
            if (rs.next() && rs.getInt("count") == 0) {
                stmt.execute("INSERT INTO users (username, password, role) VALUES ("
                        + "'admin', '" + Security.hashPassword("admin123") + "', 'admin')");

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean addBook(String title, String author, String category) {
        if (title == null || author == null || category == null
                || title.isBlank() || author.isBlank() || category.isBlank()) {
            return false;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO books(title, author, category, available) VALUES (?, ?, ?, 1)")) {
            pstmt.setString(1, title);
            pstmt.setString(2, author);
            pstmt.setString(3, category);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                // Duplicate title error
                return false;
            }
            e.printStackTrace();
            return false;
        }
    }

    // public static void addMember(String name) {
    //     if (name == null || name.isBlank()) {
    //         return;
    //     }
    //     try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO members(name) VALUES (?)")) {
    //         pstmt.setString(1, name);
    //         pstmt.executeUpdate();
    //     } catch (SQLException e) {
    //         e.printStackTrace();
    //     }
    // }
    public static boolean borrowBook(int bookId, int borrowerId, int actingUserId) {
        if (bookId <= 0 || borrowerId <= 0 || actingUserId <= 0) {
            return false;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);

            User actor = getUserObjectById(actingUserId);
            if (actor == null) {
                return false;
            }
            if (actor instanceof Member && borrowerId != actingUserId) {
                return false;
            }

            boolean bookExists = false;
            boolean isAvailable = false;

            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT available FROM books WHERE id = ?")) {
                check.setInt(1, bookId);
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    bookExists = true;
                    isAvailable = rs.getInt("available") == 1;
                }
            }

            if (!bookExists) {
                System.err.println("Borrow failed: Book does not exist (ID: " + bookId + ")");
                return false;
            }

            if (!isAvailable) {
                System.err.println("Borrow failed: Book is already borrowed (ID: " + bookId + ")");
                return false;
            }

            //Borrow book
            String borrowDate = java.time.LocalDate.now().toString();
            String dueDate = java.time.LocalDate.now().plusDays(14).toString();

            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO borrow_records(book_id, user_id, due_date, borrow_date) VALUES (?, ?, ?, ?)")) {
                insert.setInt(1, bookId);
                insert.setInt(2, borrowerId);
                insert.setString(3, dueDate);
                insert.setString(4, borrowDate);
                insert.executeUpdate();
            }

            try (PreparedStatement update = conn.prepareStatement("UPDATE books SET available = 0 WHERE id = ?")) {
                update.setInt(1, bookId);
                update.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // public static void returnBook(int bookId) {
    //     if (bookId <= 0) {
    //         return;
    //     }
    //     try (Connection conn = DriverManager.getConnection(DB_URL)) {
    //         try (PreparedStatement deleteBorrow = conn.prepareStatement("DELETE FROM borrow_records WHERE book_id = ?")) {
    //             deleteBorrow.setInt(1, bookId);
    //             deleteBorrow.executeUpdate();
    //         }
    //         try (PreparedStatement updateBook = conn.prepareStatement("UPDATE books SET available = 1 WHERE id = ?")) {
    //             updateBook.setInt(1, bookId);
    //             updateBook.executeUpdate();
    //         }
    //     } catch (SQLException e) {
    //         e.printStackTrace();
    //     }
    // }
    public static List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM books")) {
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category"),
                        rs.getInt("available") == 1
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public static List<Book> searchBooksByAuthor(String author) {
        List<Book> books = new ArrayList<>();
        if (author == null || author.isBlank()) {
            return books;
        }
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM books WHERE author LIKE ?")) {
            pstmt.setString(1, "%" + author + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category"),
                        rs.getInt("available") == 1
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public static List<Book> searchBooksByCategory(String category) {
        List<Book> books = new ArrayList<>();
        if (category == null || category.isBlank()) {
            return books;
        }
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM books WHERE category LIKE ?")) {
            pstmt.setString(1, "%" + category + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category"),
                        rs.getInt("available") == 1
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public static List<Book> getAvailableBooks() {
        List<Book> books = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM books WHERE available = 1")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category"),
                        true
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public static List<Book> getBorrowedBooks() {
        List<Book> books = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM books WHERE available = 0")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category"),
                        false
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public static List<String> getOverdueRecords() {
        List<String> overdueList = new ArrayList<>();
        String today = java.time.LocalDate.now().toString();
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(
                "SELECT b.title, u.username, r.due_date FROM borrow_records r "
                + "JOIN books b ON r.book_id = b.id "
                + "JOIN users u ON r.user_id = u.id "
                + "WHERE r.due_date < ?")) {
            pstmt.setString(1, today);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                overdueList.add(rs.getString("username") + " has overdue: '" + rs.getString("title") + "' (due: " + rs.getString("due_date") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return overdueList;
    }

    public static void deleteBook(int bookId) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement("DELETE FROM books WHERE id = ?")) {
            pstmt.setInt(1, bookId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Member> getAllMembers() {
        List<Member> members = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE role = 'member'")) {
            while (rs.next()) {
                members.add(new Member(rs.getInt("id"), rs.getString("username")));

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public static List<BorrowRecord> getBorrowRecords() {
        List<BorrowRecord> records = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(
                "SELECT r.id, b.title AS book_title, u.username AS member_name, r.borrow_date, r.due_date "
                + "FROM borrow_records r "
                + "JOIN books b ON r.book_id = b.id "
                + "JOIN users u ON r.user_id = u.id")) {

            while (rs.next()) {
                records.add(new BorrowRecord(
                        rs.getInt("id"),
                        rs.getString("book_title"),
                        rs.getString("member_name"),
                        rs.getString("borrow_date"),
                        rs.getString("due_date")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    public static User getUserObjectById(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String username = rs.getString("username");
                String role = rs.getString("role");
                if ("admin".equalsIgnoreCase(role)) {
                    return new Admin(id, username);
                } else {
                    return new Member(id, username);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean returnBookWithCheck(int bookId, int userId) {
        if (bookId <= 0 || userId <= 0) {
            return false;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // 1. Check if the book exists and is borrowed
            boolean isAvailable;
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT available FROM books WHERE id = ?")) {
                checkStmt.setInt(1, bookId);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    return false;
                }
                isAvailable = rs.getInt("available") == 1;
            }

            if (isAvailable) {
                return false;
            }

            User user = getUserObjectById(userId);
            if (user == null) {
                return false;
            }

            if (user instanceof Member) {
                // Member: ensure they actually borrowed the book
                try (PreparedStatement checkBorrow = conn.prepareStatement(
                        "SELECT * FROM borrow_records WHERE book_id = ? AND user_id = ?")) {
                    checkBorrow.setInt(1, bookId);
                    checkBorrow.setInt(2, userId);
                    ResultSet rs = checkBorrow.executeQuery();
                    if (!rs.next()) {
                        return false;  // Not borrowed by this member

                    }
                }
            }

            // 3. Remove borrow record
            try (PreparedStatement deleteBorrow = conn.prepareStatement(
                    "DELETE FROM borrow_records WHERE book_id = ?")) {
                deleteBorrow.setInt(1, bookId);
                deleteBorrow.executeUpdate();
            }

            // 4. Mark book as available
            try (PreparedStatement updateBook = conn.prepareStatement(
                    "UPDATE books SET available = 1 WHERE id = ?")) {
                updateBook.setInt(1, bookId);
                updateBook.executeUpdate();
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static User getUserObject(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                String role = rs.getString("role");
                if ("admin".equalsIgnoreCase(role)) {
                    return new Admin(id, username);
                } else {
                    return new Member(id, username);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getUserIdByUsername(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean validateUser(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
            stmt.setString(1, username);
            stmt.setString(2, Security.hashPassword(password));

            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean addUser(String username, String password, String role) {
        if (username == null || password == null || role == null
                || username.isBlank() || password.isBlank() || role.isBlank()) {
            return false;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO users (username, password, role) VALUES (?, ?, ?)")) {
            pstmt.setString(1, username);
            pstmt.setString(2, Security.hashPassword(password));

            pstmt.setString(3, role.toLowerCase());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                return false; // username already exists
            }
            e.printStackTrace();
            return false;
        }
    }

}
