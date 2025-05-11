package gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import db.DatabaseManager;
import models.Admin;
import models.Book;
import models.BorrowRecord;
import models.Member;
import models.User;

public class LibraryApp extends JFrame {

    private JTextArea outputArea;
    private User currentUser;
    private String username;
    private String password;

    public LibraryApp() {
        DatabaseManager.initializeDatabase();
        setTitle("Library Management System");
        setSize(800, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        boolean authenticated = false;

        while (!authenticated) {
            username = JOptionPane.showInputDialog(null, "Username:");
            if (username == null) {
                System.exit(0);
            }

            while (username.isBlank()) {
                JOptionPane.showMessageDialog(null, "please enter username", "Login Failed", JOptionPane.ERROR_MESSAGE);
                username = JOptionPane.showInputDialog(null, "Username:");
                if (username == null) {
                    System.exit(0);
                }
            }

            password = JOptionPane.showInputDialog(null, "Password:");
            if (username == null) {
                System.exit(0);
            }

            while (password.isBlank()) {
                JOptionPane.showMessageDialog(null, "please enter password", "Login Failed", JOptionPane.ERROR_MESSAGE);
                password = JOptionPane.showInputDialog(null, "Password:");
                if (password == null) {
                    System.exit(0);
                }
            }

            if (DatabaseManager.validateUser(username, password)) {
                currentUser = DatabaseManager.getUserObject(username);
                authenticated = true;
            } else {
                JOptionPane.showMessageDialog(null, "Invalid username or password. Please try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }

        outputArea = new JTextArea();
        JCheckBox filterAvailable = new JCheckBox("Show Only Available");
        JCheckBox filterBorrowed = new JCheckBox("Show Only Borrowed");
        JCheckBox showAllBooks = new JCheckBox("Show All Books");

        filterAvailable.addActionListener(e -> {
            if (filterAvailable.isSelected()) {
                filterBorrowed.setSelected(false);
                showAllBooks.setSelected(false);
            }
            showBooksWithFilters(filterAvailable.isSelected(), filterBorrowed.isSelected());
        });
        filterBorrowed.addActionListener(e -> {
            if (filterBorrowed.isSelected()) {
                filterAvailable.setSelected(false);
                showAllBooks.setSelected(false);
            }
            showBooksWithFilters(filterAvailable.isSelected(), filterBorrowed.isSelected());
        });

        showAllBooks.addActionListener(e -> {
            if (showAllBooks.isSelected()) {
                filterBorrowed.setSelected(false);
                filterAvailable.setSelected(false);
            }
            showBooksWithFilters(filterAvailable.isSelected(), filterBorrowed.isSelected());
        });

        JPanel filterPanel = new JPanel();
        filterPanel.add(showAllBooks);
        filterPanel.add(filterAvailable);
        filterPanel.add(filterBorrowed);
        add(filterPanel, BorderLayout.NORTH);

        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(5, 3, 5, 5));

        JButton addBookBtn = new JButton("Add Book");
        JButton deleteBookBtn = new JButton("Delete Book");

        JButton showMembersBtn = new JButton("Show All Members");

        JButton addUserBtn = new JButton("Add User");

        JButton borrowBtn = new JButton("Borrow Book");
        JButton returnBtn = new JButton("Return Book");
        JButton showBorrowsBtn = new JButton("Show Borrow Records");

        JButton searchByAuthorBtn = new JButton("Search Books by Author");
        JButton searchByCategoryBtn = new JButton("Search Books by Category");

        JButton showOverdueBtn = new JButton("Show Overdue Books");
        JButton changeUserBtn = new JButton("Change User");

        buttonPanel.add(addBookBtn);
        buttonPanel.add(deleteBookBtn);

        buttonPanel.add(showMembersBtn);
        buttonPanel.add(borrowBtn);

        buttonPanel.add(returnBtn);
        buttonPanel.add(showBorrowsBtn);
        buttonPanel.add(searchByAuthorBtn);

        buttonPanel.add(searchByCategoryBtn);

        buttonPanel.add(showOverdueBtn);

        buttonPanel.add(addUserBtn);
        buttonPanel.add(changeUserBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        if (!"admin".equals(currentUser.getRole())) {
            addBookBtn.setEnabled(false);
            deleteBookBtn.setEnabled(false);
            addUserBtn.setEnabled(false);
            showBorrowsBtn.setEnabled(false);
            showOverdueBtn.setEnabled(false);
            showMembersBtn.setEnabled(false);
        }

        addBookBtn.addActionListener(e -> {
            String title = JOptionPane.showInputDialog("Enter Book Title:");
            if (title == null || title.isBlank()) {
                return;
            }
            String author = JOptionPane.showInputDialog("Enter Author:");
            if (author == null || author.isBlank()) {
                return;
            }
            String category = JOptionPane.showInputDialog("Enter Category:");
            if (category == null || category.isBlank()) {
                return;
            }
            boolean success = DatabaseManager.addBook(title, author, category);
            if (!success) {
                JOptionPane.showMessageDialog(this, "Book with this title already exists.", "Add Failed", JOptionPane.WARNING_MESSAGE);
            }
            showBooks();

        });

        deleteBookBtn.addActionListener(e -> {
            String idStr = JOptionPane.showInputDialog("Enter Book ID to Delete:");
            if (idStr == null || idStr.isBlank()) {
                return;
            }
            try {
                int id = Integer.parseInt(idStr);
                DatabaseManager.deleteBook(id);
                showBooks();
            } catch (NumberFormatException ex) {
                showError("Invalid ID");
            }
        });

        addUserBtn.addActionListener(e -> {
            String newUsername = JOptionPane.showInputDialog(this, "Enter new username:");
            if (newUsername == null || newUsername.isBlank()) {
                return;
            }

            String newPassword = JOptionPane.showInputDialog(this, "Enter password for user:");
            if (newPassword == null || newPassword.isBlank()) {
                return;
            }

            String[] roles = {"member", "admin"};
            String selectedRole = (String) JOptionPane.showInputDialog(this,
                    "Select role for new user:",
                    "User Role",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    roles,
                    roles[0]);

            if (selectedRole == null) {
                return;
            }

            boolean success = DatabaseManager.addUser(newUsername, newPassword, selectedRole);
            if (success) {
                JOptionPane.showMessageDialog(this, "User added successfully.");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add user. Username may already exist.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        borrowBtn.addActionListener(e -> {
            String bookIdStr = JOptionPane.showInputDialog("Enter Book ID to Borrow:");
            if (bookIdStr == null) {
                return;  // Cancel
            }
            try {
                int bookId = Integer.parseInt(bookIdStr);
                int borrowerId = currentUser.getId();  // default: self

                // If current user is Admin, allow selecting any member
                if (currentUser instanceof Admin) {
                    List<Member> members = DatabaseManager.getAllMembers();
                    if (members.isEmpty()) {
                        showError("No members available.");
                        return;
                    }

                    String[] options = members.stream()
                            .map(m -> m.getId() + ": " + m.getUsername())
                            .toArray(String[]::new);

                    String selected = (String) JOptionPane.showInputDialog(
                            null,
                            "Choose a user to borrow for:",
                            "Borrow for Member",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            options,
                            options[0]);

                    if (selected == null) {
                        return;  // Cancel

                                        }borrowerId = Integer.parseInt(selected.split(":")[0]);
                }

                boolean success = DatabaseManager.borrowBook(bookId, borrowerId, currentUser.getId());

                if (!success) {
                    showError("Failed to borrow book. It may not exist, is already borrowed, or you're not authorized.");
                } else {
                    showBooks();
                    showBorrows();
                }

            } catch (NumberFormatException ex) {
                showError("Invalid input. Please enter a numeric Book ID.");
            }
        });

        returnBtn.addActionListener(e -> {
            String bookIdStr = JOptionPane.showInputDialog("Enter Book ID to Return:");
            if (bookIdStr == null || bookIdStr.isBlank()) {
                return;
            }

            try {
                int bookId = Integer.parseInt(bookIdStr);
                if (!DatabaseManager.returnBookWithCheck(bookId, currentUser.getId())) {
                    showError("Cannot return this book. Either it's already returned or you are not the borrower.");
                } else {
                    showBorrows();
                    showBooks();
                }

            } catch (NumberFormatException ex) {
                showError("Invalid Book ID");
            }
        });

        changeUserBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to switch user?",
                    "Confirm Logout",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                this.dispose(); // Close the current window
                new LibraryApp(); // Restart login and app
            }
        });

        showMembersBtn.addActionListener(e -> showMembers());
        showBorrowsBtn.addActionListener(e -> showBorrows());

        searchByAuthorBtn.addActionListener(e -> {
            String author = JOptionPane.showInputDialog("Enter author name:");
            if (author == null || author.isBlank()) {
                return;
            }
            List<Book> books = DatabaseManager.searchBooksByAuthor(author);
            outputArea.setText("Books by Author '" + author + "':\n");
            for (Book b : books) {
                outputArea.append(b + "\n");
            }
        });

        searchByCategoryBtn.addActionListener(e -> {
            String category = JOptionPane.showInputDialog("Enter category:");
            if (category == null || category.isBlank()) {
                return;
            }
            List<Book> books = DatabaseManager.searchBooksByCategory(category);
            outputArea.setText("Books in Category '" + category + "':\n");
            for (Book b : books) {
                outputArea.append(b + "\n");
            }
        });

        showOverdueBtn.addActionListener(e -> {
            List<String> overdue = DatabaseManager.getOverdueRecords();
            outputArea.setText("Overdue Books:\n");
            for (String line : overdue) {
                outputArea.append(line + "\n");
            }
        });

        showBooks();
        setVisible(true);
    }

    private void showBooks() {
        showBooksWithFilters(false, false);
    }

    private void showMembers() {
        List<Member> members = DatabaseManager.getAllMembers();
        outputArea.setText("Members:\n");
        for (Member m : members) {
            outputArea.append(m + "\n");
        }
    }

    private void showBorrows() {
        List<BorrowRecord> records = DatabaseManager.getBorrowRecords();
        outputArea.setText("Borrow Records:\n");
        for (BorrowRecord r : records) {
            outputArea.append(r + "\n");
        }
    }

    private void showBooksWithFilters(boolean onlyAvailable, boolean onlyBorrowed) {
        List<Book> books = DatabaseManager.getAllBooks();
        outputArea.setText("Books:\n");
        for (Book b : books) {
            if (onlyAvailable && !b.isAvailable()) {
                continue;
            }
            if (onlyBorrowed && b.isAvailable()) {
                continue;
            }

            outputArea.append(b + "\n");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

}
