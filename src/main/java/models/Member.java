package models;

public class Member extends User {

    public Member(int id, String username) {
        super(id, username, "user");
    }

    @Override
    public String toString() {
        return id + ": " + username + " (Member)";
    }
}
