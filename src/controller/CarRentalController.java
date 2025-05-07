package controller;

import model.*;
//import DBConnection;

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
                : "SELECT * FROM cars WHERE is_deleted = false"; // Show all cars (available + booked)

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            boolean hasCars = false;
            while (rs.next()) {
                String availabilityStatus = rs.getBoolean("available") ? "Available" : "Unavailable";

                System.out.printf("Car ID: %d | Model: %s | ₹%.2f/day | %s%n",
                        rs.getInt("id"),
                        rs.getString("model"),
                        rs.getDouble("price_per_day"),
                        availabilityStatus);

                hasCars = true;
            }

            if (!hasCars) {
                System.out.println("No cars available to show.");
            }
        }
    }


    public void addCar() throws SQLException {
        boolean validId = false;
        while (!validId) {
            System.out.println("\n--- Add Car ---");
            System.out.print("Enter Car ID: ");
            int id = -1;
            try {
                id = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer for the car ID.");
                continue;
            }

            try (Connection con = DBConnection.getConnection()) {
                String checkSql = "SELECT * FROM cars WHERE id = ?";
                PreparedStatement checkStmt = con.prepareStatement(checkSql);
                checkStmt.setInt(1, id);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    boolean isDeleted = rs.getBoolean("is_deleted");
                    if (!isDeleted) {
                        System.out.println("Car ID already exists. Please enter a different ID.");
                    } else {
                        // Restore the deleted car
                        System.out.print("Model: ");
                        String model = scanner.nextLine();
                        System.out.print("Price per Day: ");
                        double price = scanner.nextDouble();
                        scanner.nextLine(); // consume newline

                        String updateSql = "UPDATE cars SET model = ?, price_per_day = ?, available = true, is_deleted = false WHERE id = ?";
                        PreparedStatement updateStmt = con.prepareStatement(updateSql);
                        updateStmt.setString(1, model);
                        updateStmt.setDouble(2, price);
                        updateStmt.setInt(3, id);
                        updateStmt.executeUpdate();

                        System.out.println("Previously deleted car restored with new data.");
                        validId = true;
                    }
                } else {
                    // Insert a completely new car
                    System.out.print("Model: ");
                    String model = scanner.nextLine();
                    System.out.print("Price per Day: ");
                    double price = scanner.nextDouble();
                    scanner.nextLine(); // consume newline

                    String insertSql = "INSERT INTO cars (id, model, price_per_day, available, is_deleted) VALUES (?, ?, ?, true, false)";
                    PreparedStatement insertStmt = con.prepareStatement(insertSql);
                    insertStmt.setInt(1, id);
                    insertStmt.setString(2, model);
                    insertStmt.setDouble(3, price);
                    insertStmt.executeUpdate();

                    System.out.println("Car added.");
                    validId = true;
                }

            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
                validId = true; // exit loop on error
            }
        }
    }





    public void deleteCar() throws SQLException {
        System.out.println("\n--- Delete Car ---");
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM cars WHERE is_deleted = false")) {

            boolean hasCars = false;
            while (rs.next()) {
                hasCars = true;
                int carId = rs.getInt("id");
                String model = rs.getString("model");
                double pricePerDay = rs.getDouble("price_per_day");
                boolean available = rs.getBoolean("available");

                // Prevent deletion if car is booked
                if (available) {
                    System.out.printf("Car ID: %d | Model: %s | Price per Day: ₹%.2f | Available%n", carId, model, pricePerDay);
                } else {
                    System.out.printf("Car ID: %d | Model: %s | Price per Day: ₹%.2f | Booked%n", carId, model, pricePerDay);
                }
            }

            if (!hasCars) {
                System.out.println("No cars available to delete.");
                return;
            }

            System.out.print("Enter the Car ID to delete: ");
            int id = scanner.nextInt();
            scanner.nextLine();

            String checkSql = "SELECT * FROM cars WHERE id = ? AND is_deleted = false";
            try (PreparedStatement checkStmt = con.prepareStatement(checkSql)) {
                checkStmt.setInt(1, id);
                ResultSet checkRs = checkStmt.executeQuery();

                if (checkRs.next()) {
                    boolean available = checkRs.getBoolean("available");
                    if (available) {
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
                        System.out.println("Car is currently booked and cannot be deleted.");
                    }
                } else {
                    System.out.println("Car ID not found or already deleted.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    private boolean isValidDate(String date) {
        String regex = "\\d{4}-\\d{2}-\\d{2}";
        return date.matches(regex);
    }




    public void bookCar(int userId) throws SQLException {
        System.out.println("\n--- Book a Car ---");
        System.out.println("Do you want to sort available cars by price?");
        System.out.println("1. Yes");
        System.out.println("2. No");
        int sortChoice = -1;
        while (sortChoice != 1 && sortChoice != 2) {
            System.out.print("Enter your choice: ");
            try {
                sortChoice = scanner.nextInt();
                scanner.nextLine();
                if (sortChoice != 1 && sortChoice != 2) {
                    System.out.println("Invalid choice. Please enter 1 or 2.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
            }
        }

        String sql = sortChoice == 1
                ? "SELECT * FROM cars WHERE available = true AND is_deleted = false ORDER BY price_per_day ASC"
                : "SELECT * FROM cars WHERE available = true AND is_deleted = false";

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            boolean hasCars = false;
            System.out.println("\n--- Available Cars ---");
            while (rs.next()) {
                hasCars = true;
                System.out.printf("Car ID: %d | Model: %s | ₹%.2f/day | Available%n",
                        rs.getInt("id"),
                        rs.getString("model"),
                        rs.getDouble("price_per_day"));
            }

            if (!hasCars) {
                System.out.println("No available cars to book.");
                return;
            }

        } catch (SQLException e) {
            System.out.println("Error fetching available cars: " + e.getMessage());
            return;
        }

        System.out.print("\nEnter Car ID to book (or 0 to cancel): ");
        int carId = scanner.nextInt();
        scanner.nextLine(); // consume newline
        if (carId == 0) {
            System.out.println("Booking cancelled.");
            return;
        }

        System.out.print("Enter start date (YYYY-MM-DD): ");
        String startDate = scanner.nextLine();
        System.out.print("Enter end date (YYYY-MM-DD): ");
        String endDate = scanner.nextLine();

        // Validate date input
        if (!isValidDate(startDate) || !isValidDate(endDate)) {
            System.out.println("Invalid date format. Please enter dates in YYYY-MM-DD format.");
            return;
        }
        if (startDate.compareTo(endDate) > 0) {
            System.out.println("End date must be after start date. Please try again.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // Fetch car price
            String fetchCarPriceSQL = "SELECT price_per_day FROM cars WHERE id = ?";
            double pricePerDay = 0;
            try (PreparedStatement pstmt = con.prepareStatement(fetchCarPriceSQL)) {
                pstmt.setInt(1, carId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    pricePerDay = rs.getDouble("price_per_day");
                }
            }

            // Calculate the number of days between the start and end date
            long startTime = java.sql.Date.valueOf(startDate).getTime();
            long endTime = java.sql.Date.valueOf(endDate).getTime();
            long durationInMillis = endTime - startTime;
            long durationInDays = durationInMillis / (1000 * 60 * 60 * 24);

            if (durationInDays <= 0) {
                System.out.println("The booking duration must be at least one day.");
                return;
            }

            // Calculate total price
            double totalPrice = pricePerDay * durationInDays;

            // Insert booking details
            String insertBookingSQL = "INSERT INTO bookings (user_id, car_id, start_date, end_date) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = con.prepareStatement(insertBookingSQL)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, carId);
                pstmt.setString(3, startDate);
                pstmt.setString(4, endDate);
                pstmt.executeUpdate();
            }

            // Update car availability
            String updateCarSQL = "UPDATE cars SET available = false WHERE id = ?";
            try (PreparedStatement pstmt = con.prepareStatement(updateCarSQL)) {
                pstmt.setInt(1, carId);
                int rowsUpdated = pstmt.executeUpdate();
                System.out.println("Updated availability for car ID " + carId + " (Rows affected: " + rowsUpdated + ")");
            }

            System.out.println("Car booked successfully!");
            System.out.println("Total price for booking (" + durationInDays + " day(s)): ₹" + totalPrice);

        } catch (SQLException e) {
            System.out.println("Error booking car: " + e.getMessage());
        }
    }



// -------------------------------------sorting---------------------------------
    public List<Car> mergeSort(List<Car> cars) {
        if (cars.size() <= 1) {
            return cars;
        }

        List<Car> left = new ArrayList<>();
        List<Car> right = new ArrayList<>();
        int middle = cars.size() / 2;

        // Split the list into two halves
        for (int i = 0; i < middle; i++) {
            left.add(cars.get(i));
        }
        for (int i = middle; i < cars.size(); i++) {
            right.add(cars.get(i));
        }
        left = mergeSort(left);
        right = mergeSort(right);
        return merge(left, right);
    }

    public List<Car> merge(List<Car> left, List<Car> right) {
        List<Car> result = new ArrayList<>();
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            if (left.get(i).getPrice() < right.get(j).getPrice()) {
                result.add(left.get(i));
                i++;
            } else {
                result.add(right.get(j));
                j++;
            }
        }
        while (i < left.size()) {
            result.add(left.get(i));
            i++;
        }
        while (j < right.size()) {
            result.add(right.get(j));
            j++;
        }

        return result;
    }



}