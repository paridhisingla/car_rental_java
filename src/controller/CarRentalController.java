package controller;

import model.*;
import util.DBConnection;

import java.sql.*;
import java.util.*;
public class CarRentalController {
    private Scanner scanner = new Scanner(System.in);

    public User login() throws SQLException {
        System.out.println("\n--- Login ---");
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(rs.getInt("id"), email, password, rs.getString("role"));
            }
        }
        System.out.println("Invalid credentials.");
        return null;
    }

    public void register() throws SQLException {
        System.out.println("\n--- Register ---");
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        try (Connection con = DBConnection.getConnection()) {
            String checkSql = "SELECT * FROM users WHERE email = ?";
            PreparedStatement checkStmt = con.prepareStatement(checkSql);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                System.out.println("User already exists.");
                return;
            }

            String insertSql = "INSERT INTO users (email, password, role) VALUES (?, ?, 'user')";
            PreparedStatement insertStmt = con.prepareStatement(insertSql);
            insertStmt.setString(1, email);
            insertStmt.setString(2, password);
            insertStmt.executeUpdate();
            System.out.println("Registration successful.");
        }
    }

    public void viewCars(boolean onlyAvailable) throws SQLException {
        String sql = onlyAvailable ? "SELECT * FROM cars WHERE available = true" : "SELECT * FROM cars";
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.printf("Car ID: %d | Model: %s | ₹%.2f/day | %s%n",
                        rs.getInt("id"),
                        rs.getString("model"),
                        rs.getDouble("price_per_day"),
                        rs.getBoolean("available") ? "Available" : "Booked");
            }
        }
    }

    public void addCar() throws SQLException {
        System.out.println("\n--- Add Car ---");
        System.out.print("Model: ");
        String model = scanner.nextLine();
        System.out.print("Price per Day: ");
        double price = scanner.nextDouble();
        scanner.nextLine();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement("INSERT INTO cars (model, price_per_day, available) VALUES (?, ?, true)")) {
            stmt.setString(1, model);
            stmt.setDouble(2, price);
            stmt.executeUpdate();
            System.out.println("Car added.");
        }
    }


    public void deleteCar() throws SQLException {
        System.out.print("Enter Car ID to delete: ");
        int id = scanner.nextInt();
        scanner.nextLine();

        // Soft delete approach
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement("UPDATE cars SET is_deleted = TRUE WHERE id = ?")) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            System.out.println(rows > 0 ? "Car marked as deleted." : "Car not found.");
        }
    }


    public void bookCar(int userId) throws SQLException {
        viewCars(true);
        System.out.print("Enter Car ID to book: ");
        int carId = scanner.nextInt(); scanner.nextLine();
        System.out.print("Enter Booking Date (YYYY-MM-DD): ");
        String date = scanner.nextLine();

        try (Connection con = DBConnection.getConnection()) {
            String checkSql = "SELECT available FROM cars WHERE id = ?";
            PreparedStatement checkStmt = con.prepareStatement(checkSql);
            checkStmt.setInt(1, carId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && !rs.getBoolean("available")) {
                System.out.println("Car already booked.");
                return;
            }

            String bookSql = "INSERT INTO bookings (user_id, car_id, start_date) VALUES (?, ?, ?)";
            PreparedStatement bookStmt = con.prepareStatement(bookSql);
            bookStmt.setInt(1, userId);
            bookStmt.setInt(2, carId);
            bookStmt.setDate(3, java.sql.Date.valueOf(date));

            bookStmt.executeUpdate();

            String updateSql = "UPDATE cars SET available = false WHERE id = ?";
            PreparedStatement updateStmt = con.prepareStatement(updateSql);
            updateStmt.setInt(1, carId);
            updateStmt.executeUpdate();

            System.out.println("Car booked!");
        }
    }
}
