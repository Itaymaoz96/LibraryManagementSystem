package models;

public class Admin extends User {

    public Admin(int id, String username) {
        super(id, username, "admin");
    }

    @Override
    public String toString() {
        return id + ": " + username + " (Admin)";
    }
}
