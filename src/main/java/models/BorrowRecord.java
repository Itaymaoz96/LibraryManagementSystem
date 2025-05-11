package models;

public class BorrowRecord {

    private int id;
    private String bookTitle;
    private String memberName;
    private String borrowDate;
    private String dueDate;

    public BorrowRecord(int id, String bookTitle, String memberName, String borrowDate, String dueDate) {
        this.id = id;
        this.bookTitle = bookTitle;
        this.memberName = memberName;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
    }

    @Override
    public String toString() {
        return memberName + " borrowed \"" + bookTitle + "\" on " + borrowDate + ", due " + dueDate;
    }
}
