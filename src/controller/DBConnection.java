package controller;

//----------------Connection to SQL------------------------------
import java.sql.*;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/carrentaldb";
    private static final String USER = "root";
    private static final String PASSWORD = "leehomin";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
