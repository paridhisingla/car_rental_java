package model;

public class User {
    private int id;
    private String email;
    private String password;
    private String role;

    public User(int id, String email, String password, String role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
}
