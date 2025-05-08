package controller;

import model.*;
//import DBConnection;
import java.sql.*;
import java.util.*;
public class CarRentalController {
    private Scanner scanner = new Scanner(System.in);

    //---------------------------------Login------------------------------------
    public User login() throws SQLException {
        System.out.println("\n--- Login ---");

        System.out.print("Email: ");
        String email = scanner.nextLine();

        // Check if email contains '@'
        if (!email.contains("@")) {
            System.out.println("Invalid email. Email must contain '@'. Returning to main menu...");
            return null;
        }

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
    //------------------------------For Registering--------------------------------
    public void register() throws SQLException {
        System.out.println("\n--- Register ---");
        String role = "";
        while (true) {
            System.out.println("1. Admin");
            System.out.println("2. User");
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                role = "admin";
                break;
            } else if (choice.equals("2")) {
                role = "user";
                break;
            } else {
                System.out.println("Invalid choice. Please enter 1 or 2.");
            }
        }
        //Ask for email
        System.out.print("Email: ");
        String email = scanner.nextLine();

        if (!email.contains("@")) {
            System.out.println("Invalid email. Email must contain '@'. Returning to main menu...");
            return;
        }
        //Ask for password
        System.out.print("Password: ");
        String password = scanner.nextLine();
        //Save to database
        try (Connection con = DBConnection.getConnection()) {
            String checkSql = "SELECT * FROM users WHERE email = ?";
            PreparedStatement checkStmt = con.prepareStatement(checkSql);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                System.out.println("User already exists.");
                return;
            }
            String insertSql = "INSERT INTO users (email, password, role) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = con.prepareStatement(insertSql);
            insertStmt.setString(1, email);
            insertStmt.setString(2, password);
            insertStmt.setString(3, role);
            insertStmt.executeUpdate();

            System.out.println("Registration successful as " + role + ".");
        }
    }

    //------------------View available cars----------------------------
    public void viewCars(boolean onlyAvailable) throws SQLException {
        String sql = onlyAvailable
                ? "SELECT * FROM cars WHERE available = true AND is_deleted = false"
                : "SELECT * FROM cars WHERE is_deleted = false"; // Show all cars

        // Create ArrayList to store cars
        ArrayList<Car> carList = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                carList.add(new Car(
                        rs.getInt("id"),
                        rs.getString("model"),
                        rs.getDouble("price_per_day"),
                        rs.getBoolean("available")
                ));
            }
            if (carList.isEmpty()) {
                System.out.println("No cars available to show.");
                return;
            }
            for (Car car : carList) {
                String availabilityStatus = car.isAvailable() ? "Available" : "Unavailable";
                System.out.printf("Car ID: %d | Model: %s | ₹%.2f/day | %s%n",
                        car.getId(),
                        car.getModel(),
                        car.getPrice(),
                        availabilityStatus);
            }
            System.out.println("\nTotal cars found: " + carList.size());

            if (!carList.isEmpty()) {
                double averagePrice = carList.stream()
                        .mapToDouble(Car::getPrice)
                        .average()
                        .orElse(0.0);
                System.out.printf("Average price: ₹%.2f/day%n", averagePrice);
            }
        }
    }
//---------------------------Adding of car--------------------------------
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
                }
                else {
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
                validId = true;
            }
        }
    }

    //------------------------------Delete Car---------------------------
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

    //-----------------------Book Car---------------------------------
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
        List<Car> availableCars = new ArrayList<>();
        String sql = "SELECT * FROM cars WHERE available = true AND is_deleted = false";

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            boolean hasCars = false;
            while (rs.next()) {
                hasCars = true;
                Car car = new Car(
                        rs.getInt("id"),
                        rs.getString("model"),
                        rs.getDouble("price_per_day"),
                        rs.getBoolean("available")
                );
                availableCars.add(car);
            }
            if (!hasCars) {
                System.out.println("No available cars to book.");
                return;
            }
            // Apply merge sort
            if (sortChoice == 1) {
                availableCars = mergeSort(availableCars);
            }
            System.out.println("\n--- Available Cars ---");
            for (Car car : availableCars) {
                System.out.printf("Car ID: %d | Model: %s | ₹%.2f/day | Available: %b%n",
                        car.getId(), car.getModel(), car.getPrice(), car.isAvailable());
            }
        } catch (SQLException e) {
            System.out.println("Error fetching available cars: " + e.getMessage());
            return;
        }
        System.out.print("\nEnter Car ID to book (or 0 to cancel): ");
        int carId = scanner.nextInt();
        scanner.nextLine();
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

            // Calculate the number of days
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

    //Update Car Information from Admin side
    public void updateCarInfo() {
        Scanner scanner = new Scanner(System.in);
        try (Connection con = DBConnection.getConnection()) {
            String fetchCars = "SELECT * FROM cars WHERE is_deleted = false";
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(fetchCars)) {

                boolean hasCars = false;
                while (rs.next()) {
                    hasCars = true;
                    System.out.printf("Car ID: %d | Model: %s | Price/Day: ₹%.2f | Available: %s%n",
                            rs.getInt("id"),
                            rs.getString("model"),
                            rs.getDouble("price_per_day"),
                            rs.getBoolean("available") ? "Yes" : "No");
                }

                if (!hasCars) {
                    System.out.println("No cars available to update.");
                    return;
                }
            }

            System.out.print("Enter Car ID to update: ");
            int carId = scanner.nextInt();
            scanner.nextLine();

            String checkSql = "SELECT * FROM cars WHERE id = ? AND is_deleted = false";
            try (PreparedStatement checkStmt = con.prepareStatement(checkSql)) {
                checkStmt.setInt(1, carId);
                ResultSet rs = checkStmt.executeQuery();

                if (!rs.next()) {
                    System.out.println("Car not found.");
                    return;
                }

                String currentModel = rs.getString("model");
                double currentPrice = rs.getDouble("price_per_day");

                System.out.println("Current Model: " + currentModel);
                System.out.println("Current Price/Day: ₹" + currentPrice);

                System.out.print("New Model (press Enter to keep same): ");
                String newModel = scanner.nextLine();
                if (newModel.isBlank()) newModel = currentModel;

                System.out.print("New Price per Day (-1 to keep same): ");
                String priceInput = scanner.nextLine();
                double newPrice = priceInput.isBlank() ? currentPrice : Double.parseDouble(priceInput);
                if (newPrice < 0) newPrice = currentPrice;

                String updateSql = "UPDATE cars SET model = ?, price_per_day = ? WHERE id = ?";
                try (PreparedStatement updateStmt = con.prepareStatement(updateSql)) {
                    updateStmt.setString(1, newModel);
                    updateStmt.setDouble(2, newPrice);
                    updateStmt.setInt(3, carId);
                    updateStmt.executeUpdate();
                    System.out.println("Car info updated successfully.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    //Update car information from User side
    public void updateBookingDate(int userId) throws SQLException {
        System.out.println("\n--- Update Booking Dates ---");

        System.out.print("Enter your Booking ID: ");
        int bookingId = scanner.nextInt();
        scanner.nextLine(); // consume newline

        // Verify booking exists
        String checkBookingSql = "SELECT * FROM bookings WHERE id = ? AND user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement checkStmt = con.prepareStatement(checkBookingSql)) {

            checkStmt.setInt(1, bookingId);
            checkStmt.setInt(2, userId);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("Booking ID not found or does not belong to you.");
                return;
            }

            //Get current booking dates
            String currentStartDate = rs.getString("start_date");
            String currentEndDate = rs.getString("end_date");
            int carId = rs.getInt("car_id");

            System.out.println("Current Booking Dates: Start Date: " + currentStartDate + ", End Date: " + currentEndDate);

            //New dates
            System.out.print("Enter new start date (YYYY-MM-DD): ");
            String newStartDate = scanner.nextLine();
            System.out.print("Enter new end date (YYYY-MM-DD): ");
            String newEndDate = scanner.nextLine();
            if (!isValidDate(newStartDate) || !isValidDate(newEndDate)) {
                System.out.println("Invalid date format. Please enter dates in YYYY-MM-DD format.");
                return;
            }
            if (newStartDate.compareTo(newEndDate) > 0) {
                System.out.println("End date must be after start date.");
                return;
            }

            //Calculate duration
            long startTime = java.sql.Date.valueOf(newStartDate).getTime();
            long endTime = java.sql.Date.valueOf(newEndDate).getTime();
            long durationInDays = (endTime - startTime) / (1000 * 60 * 60 * 24);

            if (durationInDays <= 0) {
                System.out.println("The booking duration must be at least one day.");
                return;
            }

            double pricePerDay = 0;
            String carSql = "SELECT price_per_day FROM cars WHERE id = ?";
            try (PreparedStatement carStmt = con.prepareStatement(carSql)) {
                carStmt.setInt(1, carId);
                ResultSet carRs = carStmt.executeQuery();
                if (carRs.next()) {
                    pricePerDay = carRs.getDouble("price_per_day");
                } else {
                    System.out.println("Car not found for this booking.");
                    return;
                }
            }

            //Calculate new total price
            double newTotalPrice = durationInDays * pricePerDay;
            System.out.printf("New total price for this booking: ₹%.2f\n", newTotalPrice);

            //Update booking
            String updateSql = "UPDATE bookings SET start_date = ?, end_date = ?, price = ? WHERE id = ? AND user_id = ?";
            try (PreparedStatement updateStmt = con.prepareStatement(updateSql)) {
                updateStmt.setString(1, newStartDate);
                updateStmt.setString(2, newEndDate);
                updateStmt.setDouble(3, newTotalPrice);
                updateStmt.setInt(4, bookingId);
                updateStmt.setInt(5, userId);

                int rows = updateStmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("Booking updated successfully!");
                } else {
                    System.out.println("Error updating booking.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error updating booking: " + e.getMessage());
        }
    }
    //------------------------------Binary Search ------------------------------
    public void searchCarByModelBinary() {
        try {
            List<Car> sortedCars = getAllCarsSortedByModel();

            if (sortedCars.isEmpty()) {
                System.out.println("No cars available in the system.");
                return;
            }
            System.out.print("\nEnter car model to search: ");
            String targetModel = scanner.nextLine().trim();
            List<Car> foundCars = findAllAvailableCarsByModel(sortedCars, targetModel);

            if (!foundCars.isEmpty()) {
                System.out.println("\nFound " + foundCars.size() + " available car(s) with model '" + targetModel + "':");
                for (Car car : foundCars) {
                    System.out.println("\nCar details:");
                    System.out.println("ID: " + car.getId());
                    System.out.println("Model: " + car.getModel());
                    System.out.println("Price: " + car.getPrice());
                }
            } else {
                System.out.println("No available cars found with model '" + targetModel + "'");
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
    // Modified to find only available cars
    private List<Car> findAllAvailableCarsByModel(List<Car> cars, String targetModel) {
        List<Car> foundCars = new ArrayList<>();
        int index = binarySearchFirstOccurrence(cars, targetModel);

        if (index >= 0) {
            int left = index;
            while (left >= 0 && cars.get(left).getModel().equalsIgnoreCase(targetModel)) {
                if (cars.get(left).isAvailable()) {
                    foundCars.add(cars.get(left));
                }
                left--;
            }
            int right = index + 1;
            while (right < cars.size() && cars.get(right).getModel().equalsIgnoreCase(targetModel)) {
                if (cars.get(right).isAvailable()) {
                    foundCars.add(cars.get(right));
                }
                right++;
            }
        }
        return foundCars;
    }
    // For finding first occurrence
    private int binarySearchFirstOccurrence(List<Car> cars, String targetModel) {
        int left = 0;
        int right = cars.size() - 1;
        int result = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            String currentModel = cars.get(mid).getModel();
            int comparison = currentModel.compareToIgnoreCase(targetModel);

            if (comparison == 0) {
                result = mid;
                right = mid - 1;
            } else if (comparison < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return result;
    }
    private List<Car> getAllCarsSortedByModel() throws SQLException {
        List<Car> cars = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT id, model, price_per_day as price, available FROM cars WHERE is_deleted = false ORDER BY model";
            PreparedStatement stmt = con.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Car car = new Car(
                        rs.getInt("id"),
                        rs.getString("model"),
                        rs.getDouble("price"),
                        rs.getBoolean("available")
                );
                cars.add(car);
            }
        }
        return cars;
    }
    // ------------------------------Sorting---------------------------------
    public List<Car> mergeSort(List<Car> cars) {
        if (cars.size() <= 1) {
            return cars;
        }
        int mid = cars.size() / 2;
        List<Car> left = cars.subList(0, mid);
        List<Car> right = cars.subList(mid, cars.size());

        return merge(mergeSort(left), mergeSort(right));
    }
    // Merge method for merge sort
    public List<Car> merge(List<Car> left, List<Car> right) {
        List<Car> merged = new ArrayList<>();
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            if (left.get(i).getPrice() <= right.get(j).getPrice()) {
                merged.add(left.get(i));
                i++;
            } else {
                merged.add(right.get(j));
                j++;
            }
        }
        while (i < left.size()) {
            merged.add(left.get(i));
            i++;
        }
        while (j < right.size()) {
            merged.add(right.get(j));
            j++;
        }

        return merged;
    }
}
