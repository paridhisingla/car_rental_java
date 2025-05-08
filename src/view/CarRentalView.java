package view;

import controller.CarRentalController;
import controller.DBConnection;
import model.User;

import java.util.Scanner;

public class CarRentalView {
    private static final Scanner scanner = new Scanner(System.in);
    private static final CarRentalController controller = new CarRentalController();

    //-----------------------Common menu----------------------
    public static void start() throws Exception {
        while (true) {
            System.out.println("\n==== Luxury Car Rental ====");
            System.out.println("1. Login");
            System.out.println("2. Register");

            System.out.println("3. Exit");
            System.out.print("Choice: ");
            int choice = scanner.nextInt(); scanner.nextLine();

            switch (choice) {
                case 1 -> {
                    User user = controller.login();
                    if (user != null) {
                        if (user.getRole().equals("admin")) adminMenu(user);
                        else userMenu(user);
                    }
                }
                case 2 -> controller.register();
                case 3 -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    //-------------------------Admin menu---------------------------------
    private static void adminMenu(User user) throws Exception {
        while (true) {
            System.out.println("\n--- Admin Dashboard ---");
            System.out.println("1. View Cars");
            System.out.println("2. Add Car");
            System.out.println("3. Delete Car");
            System.out.println("4. Update Car Info");
            System.out.println("5. Search Car by Model");
            System.out.println("6. Logout");
            System.out.print("Choice: ");
            int choice = scanner.nextInt(); scanner.nextLine();
            switch (choice) {
                case 1 -> controller.viewCars(false);
                case 2 -> controller.addCar();
                case 3 -> controller.deleteCar();
                case 4 -> controller.updateCarInfo();
                case 5 -> controller. searchCarByModelBinary();
                case 6 -> { return; }
                default -> System.out.println("Invalid.");
            }
        }
    }

    //---------------User Menu----------------------------
    private static void userMenu(User user) throws Exception {
        while (true) {
            System.out.println("\n--- User Dashboard ---");
            System.out.println("1. View All Cars");
            System.out.println("2. Book Car");
            System.out.println("3. Update Booking Date");
            System.out.println("4. Search Car by Model");
            System.out.println("5. Logout");
            System.out.print("Choice: ");
            int choice = scanner.nextInt(); scanner.nextLine();
            switch (choice) {
                case 1 -> controller.viewCars(false);
                case 2 -> controller.bookCar(user.getId());
                case 3 -> controller.updateBookingDate(user.getId());
                case 4 -> controller.searchCarByModelBinary();
                case 5 -> { return; }
                default -> System.out.println("Invalid.");
            }
        }
    }

}
