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
        String sql = onlyAvailable
                ? "SELECT * FROM cars WHERE available = true AND is_deleted = false"
                : "SELECT * FROM cars WHERE is_deleted = false";

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
        boolean validId = false;
        while (!validId) {
            System.out.println("\n--- Add Car ---");

            // Try to read the car ID, ensuring it's an integer
            System.out.print("Enter Car ID: ");
            int id = -1;  // Start with an invalid value for ID
            try {
                id = Integer.parseInt(scanner.nextLine());  // Try parsing the ID as an integer
            } catch (NumberFormatException e) {
                // If invalid, catch the exception and show a message
                System.out.println("Invalid input. Please enter a valid integer for the car ID.");
                continue;  // Skip the rest of the loop and prompt again for the ID
            }

            // Check if the car ID already exists in the database
            try (Connection con = DBConnection.getConnection()) {
                String checkSql = "SELECT * FROM cars WHERE id = ? AND is_deleted = false"; // Ensure checking only non-deleted cars
                PreparedStatement checkStmt = con.prepareStatement(checkSql);
                checkStmt.setInt(1, id);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    // Car ID already exists, show a message and prompt again
                    System.out.println("Car ID already exists. Please enter a different ID.");
                } else {
                    // If the ID doesn't exist, proceed with adding the car
                    System.out.print("Model: ");
                    String model = scanner.nextLine();
                    System.out.print("Price per Day: ");
                    double price = scanner.nextDouble();
                    scanner.nextLine(); // Consume newline after double input

                    String insertSql = "INSERT INTO cars (id, model, price_per_day, available) VALUES (?, ?, ?, true)";
                    PreparedStatement insertStmt = con.prepareStatement(insertSql);
                    insertStmt.setInt(1, id);  // Manually set the car ID
                    insertStmt.setString(2, model);
                    insertStmt.setDouble(3, price);
                    insertStmt.executeUpdate();

                    System.out.println("Car added.");
                    validId = true; // Set to true to exit the loop and finish
                }
            } catch (SQLException e) {
                // Catch any SQLException and show the error message
                System.out.println("Error: " + e.getMessage());
                validId = true; // Set to true to exit the loop and prevent infinite loop in case of error
            }
        }
    }




    public void deleteCar() throws SQLException {
        System.out.println("\n--- Delete Car ---");

        // Display a list of all cars (non-deleted)
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM cars WHERE is_deleted = false")) {

            // Check if there are any cars in the database
            boolean hasCars = false;
            while (rs.next()) {
                hasCars = true;
                System.out.printf("Car ID: %d | Model: %s | Price per Day: ₹%.2f | Available: %s%n",
                        rs.getInt("id"),
                        rs.getString("model"),
                        rs.getDouble("price_per_day"),
                        rs.getBoolean("available") ? "Available" : "Booked");
            }

            if (!hasCars) {
                System.out.println("No cars available to delete.");
                return;
            }

            // Ask the user to select a car to delete
            System.out.print("Enter the Car ID to delete: ");
            int id = scanner.nextInt();
            scanner.nextLine();  // Consume the newline character after integer input

            // Check if the car exists in the database before deleting
            String checkSql = "SELECT * FROM cars WHERE id = ? AND is_deleted = false";
            try (PreparedStatement checkStmt = con.prepareStatement(checkSql)) {
                checkStmt.setInt(1, id);
                ResultSet checkRs = checkStmt.executeQuery();

                if (checkRs.next()) {
                    // Car exists, proceed with soft delete (mark it as deleted)
                    String deleteSql = "UPDATE cars SET is_deleted = TRUE WHERE id = ?";
                    try (PreparedStatement deleteStmt = con.prepareStatement(deleteSql)) {
                        deleteStmt.setInt(1, id);
                        int rows = deleteStmt.executeUpdate();
                        if (rows > 0) {
                            System.out.println("Car marked as deleted.");
                        } else {
                            System.out.println("Error deleting the car.");
                        }
                    }
                } else {
                    // Car doesn't exist or already deleted
                    System.out.println("Car ID not found or already deleted.");
                }
            }

        } catch (SQLException e) {
            // Handle any SQL exception
            System.out.println("Error: " + e.getMessage());
        }
    }



    public void bookCar(int userId) throws SQLException {
        System.out.println("\n--- Book a Car ---");
        System.out.println("Do you want to sort available cars by price?");
        System.out.println("1. Yes");
        System.out.println("2. No");

        // Ensure the user enters a valid choice (1 or 2)
        int sortChoice = -1;
        while (sortChoice != 1 && sortChoice != 2) {
            System.out.print("Enter your choice: ");
            try {
                sortChoice = scanner.nextInt();
                scanner.nextLine(); // Consume the newline character
                if (sortChoice != 1 && sortChoice != 2) {
                    System.out.println("Invalid choice. Please enter 1 for Yes or 2 for No.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number (1 or 2).");
                scanner.nextLine(); // Clear the buffer
            }
        }

        System.out.println("\n--- Available Cars ---");
        String sql;

        // Sorting based on the user's choice
        if (sortChoice == 1) {
            // Sorting by price (ascending)
            sql = "SELECT * FROM cars WHERE available = true AND is_deleted = false ORDER BY price_per_day ASC";
        } else {
            // No sorting, just showing available cars
            sql = "SELECT * FROM cars WHERE available = true AND is_deleted = false";
        }

        // Displaying the available cars (sorted or not)
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            boolean hasCars = false;
            while (rs.next()) {
                hasCars = true;
                System.out.printf("Car ID: %d | Model: %s | ₹%.2f/day | Available%n",
                        rs.getInt("id"),
                        rs.getString("model"),
                        rs.getDouble("price_per_day"));
            }

            if (!hasCars) {
                System.out.println("No available cars to book.");
                return; // No cars available
            }

        } catch (SQLException e) {
            System.out.println("Error fetching available cars: " + e.getMessage());
            return; // Exit if there's an error fetching cars
        }

        // Continue with booking if cars are available
        System.out.print("Enter Car ID to book: ");
        int carId = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        System.out.print("Enter Booking Date (YYYY-MM-DD): ");
        String date = scanner.nextLine();

        try (Connection con = DBConnection.getConnection()) {
            // Check if the car is available before proceeding
            String checkSql = "SELECT available FROM cars WHERE id = ?";
            PreparedStatement checkStmt = con.prepareStatement(checkSql);
            checkStmt.setInt(1, carId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && !rs.getBoolean("available")) {
                System.out.println("Car already booked.");
                return;
            }

            // Proceed with the booking
            String bookSql = "INSERT INTO bookings (user_id, car_id, start_date) VALUES (?, ?, ?)";
            PreparedStatement bookStmt = con.prepareStatement(bookSql);
            bookStmt.setInt(1, userId);
            bookStmt.setInt(2, carId);
            bookStmt.setDate(3, java.sql.Date.valueOf(date));
            bookStmt.executeUpdate();

            // Mark the car as booked (not available anymore)
            String updateSql = "UPDATE cars SET available = false WHERE id = ?";
            PreparedStatement updateStmt = con.prepareStatement(updateSql);
            updateStmt.setInt(1, carId);
            updateStmt.executeUpdate();

            System.out.println("Car booked successfully!");
        } catch (SQLException e) {
            System.out.println("Error during booking: " + e.getMessage());
        }
    }


}