package db;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import models.Book;
import utils.Security;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseManagerTest {

    @BeforeAll
    public static void setupDatabase() {
        DatabaseManager.resetDatabase();
    }

    @Test
    @Order(1)
    public void testAddAdmin() {
        assertTrue(DatabaseManager.addUser("itay", "itay", "admin"));
    }

    @Test
    @Order(2)
    public void testAddMember() {
        assertTrue(DatabaseManager.addUser("alice", "1234", "member"));
    }

    @Test
    @Order(3)
    public void testLoginValid() {
        assertTrue(DatabaseManager.validateUser("itay", "itay"));
    }

    @Test
    @Order(4)
    public void testLoginInvalidPassword() {
        assertFalse(DatabaseManager.validateUser("itay", "wrong"));
    }

    @Test
    @Order(5)
    public void testDuplicateUserFails() {
        assertFalse(DatabaseManager.addUser("itay", "again", "member"));
    }

    @Test
    @Order(6)
    public void testPasswordHashing() {
        String hash1 = Security.hashPassword("abc");
        String hash2 = Security.hashPassword("abc");
        assertEquals(hash1, hash2);
        assertNotEquals("abc", hash1);
    }

    @Test
    @Order(7)
    public void testAddBook1() {
        assertTrue(DatabaseManager.addBook("Dune", "Frank Herbert", "Sci-Fi"));
    }

    @Test
    @Order(8)
    public void testDuplicateBookFails() {
        assertFalse(DatabaseManager.addBook("Dune", "Someone", "Other"));
    }

    @Test
    @Order(9)
    public void testGetAllBooks() {
        List<Book> books = DatabaseManager.getAllBooks();
        assertFalse(books.isEmpty());
    }

    @Test
    @Order(10)
    public void testBorrowBookAsAdmin() {

        int borrowerId = DatabaseManager.getUserObject("itay").getId();

        // Find the book "Dune" by title
        int bookId = DatabaseManager.getAllBooks().stream()
                .filter(b -> b.getTitle().equals("Dune"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Book 'Dune' not found"))
                .getId();

        boolean result = DatabaseManager.borrowBook(bookId, borrowerId, borrowerId);
        assertTrue(result, "Borrow operation should succeed");

        // Verify the borrowed book appears in the borrowed list
        boolean bookIsBorrowed = DatabaseManager.getBorrowedBooks().stream()
                .anyMatch(b -> b.getId() == bookId);

        assertTrue(bookIsBorrowed, "Book should appear in borrowed list");
    }

    @Test
    @Order(11)
    public void testReturnBook() {
        assertTrue(DatabaseManager.returnBookWithCheck(1, 2));
    }

    @Test
    @Order(12)
    public void testAddBook2() {
        assertTrue(DatabaseManager.addBook("Neuromancer", "William Gibson", "Cyberpunk"));
    }

    @Test
    @Order(13)
    public void testAdminCanDeleteBook() {
        Book toDelete = DatabaseManager.getAllBooks().stream()
                .filter(b -> b.getTitle().equals("Neuromancer"))
                .findFirst()
                .orElse(null);

        assertNotNull(toDelete);
        DatabaseManager.deleteBook(toDelete.getId());

        List<Book> allBooks = DatabaseManager.getAllBooks();
        assertTrue(allBooks.stream().noneMatch(b -> b.getId() == toDelete.getId()));
    }

    @Test
    @Order(14)
    public void testSearchBookByAuthor() {
        List<Book> results = DatabaseManager.searchBooksByAuthor("Frank Herbert");

        assertFalse(results.isEmpty(), "Search result should not be empty");
        boolean found = results.stream().anyMatch(b -> b.getTitle().equals("Dune"));
        assertTrue(found, "'Dune' should be found by author 'Frank Herbert'");
    }

    @Test
    @Order(15)
    public void testSearchBookByCategory() {
        List<Book> results = DatabaseManager.searchBooksByCategory("Sci-Fi");

        assertFalse(results.isEmpty(), "Search result should not be empty");
        boolean found = results.stream().anyMatch(b -> b.getTitle().equals("Dune"));
        assertTrue(found, "'Dune' should be found in category 'Sci-Fi'");
    }

    @Test
    @Order(16)
    public void testChangeUserToMember() {
        assertTrue(DatabaseManager.validateUser("alice", "1234"));
    }

    @Test
    @Order(17)
    public void testBorrowBookAsMember() {

        int borrowerId = DatabaseManager.getUserObject("alice").getId();

        // Find the book "Dune" by title
        int bookId = DatabaseManager.getAllBooks().stream()
                .filter(b -> b.getTitle().equals("Dune"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Book 'Dune' not found"))
                .getId();

        boolean result = DatabaseManager.borrowBook(bookId, borrowerId, borrowerId);
        assertTrue(result, "Borrow operation should succeed");

        // Verify the borrowed book appears in the borrowed list
        boolean bookIsBorrowed = DatabaseManager.getBorrowedBooks().stream()
                .anyMatch(b -> b.getId() == bookId);

        assertTrue(bookIsBorrowed, "Book should appear in borrowed list");
    }

    @Test
    @Order(18)
    public void testAliceCannotBorrowDuneTwice() {
        int aliceId = DatabaseManager.getUserObject("alice").getId();
        int duneId = DatabaseManager.getAllBooks().stream()
                .filter(b -> b.getTitle().equals("Dune"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Book 'Dune' not found"))
                .getId();

        // Book is already borrowed by Alice in previous test
        boolean result = DatabaseManager.borrowBook(duneId, aliceId, aliceId);
        assertFalse(result, "Alice should not be able to borrow 'Dune' again while it's already borrowed");
    }

    @Test
    @Order(19)
    public void testAddBob() {
        boolean added = DatabaseManager.addUser("bob", "bob123", "member");
        assertTrue(added, "Bob should be added or already exist");
    }

    @Test
    @Order(20)
    public void testBobCannotBorrowDune() {
        int bobId = DatabaseManager.getUserObject("bob").getId();
        int duneId = DatabaseManager.getAllBooks().stream()
                .filter(b -> b.getTitle().equals("Dune"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Book 'Dune' not found"))
                .getId();

        boolean result = DatabaseManager.borrowBook(duneId, bobId, bobId);
        assertFalse(result, "Bob should not be able to borrow 'Dune' while it's already borrowed by Alice");
    }

}
