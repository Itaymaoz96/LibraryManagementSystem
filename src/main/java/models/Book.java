// models/Book.java
package models;

public class Book {

    private int id;
    private String title;
    private String author;
    private String category;
    private boolean isAvailable;

    public Book(int id, String title, String author, String category, boolean isAvailable) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.isAvailable = isAvailable;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    @Override
    public String toString() {
        return id + ": " + title + " by " + author + " [" + category + "]" + (isAvailable ? " [Available]" : " [Borrowed]");
    }
}
