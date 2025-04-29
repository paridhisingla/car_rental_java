package view;

import controller.CarRentalController;
import model.User;

import java.util.Scanner;

public class CarRentalView {
    private static final Scanner scanner = new Scanner(System.in);
    private static final CarRentalController controller = new CarRentalController();

    public static void start() throws Exception {
        while (true) {
            System.out.println("\n==== Luxury Car Rental ====");
            System.out.println("1. Login");
            System.out.println("2. Register (User)");
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

    private static void adminMenu(User user) throws Exception {
        while (true) {
            System.out.println("\n--- Admin Dashboard ---");
            System.out.println("1. View Cars");
            System.out.println("2. Add Car");
            System.out.println("3. Delete Car");
            System.out.println("4. Logout");
            System.out.print("Choice: ");
            int choice = scanner.nextInt(); scanner.nextLine();
            switch (choice) {
                case 1 -> controller.viewCars(false);
                case 2 -> controller.addCar();
                case 3 -> controller.deleteCar();
                case 4 -> { return; }
                default -> System.out.println("Invalid.");
            }
        }
    }

    private static void userMenu(User user) throws Exception {
        while (true) {
            System.out.println("\n--- User Dashboard ---");
            System.out.println("1. View Available Cars");
            System.out.println("2. Book Car");
            System.out.println("3. Logout");
            System.out.print("Choice: ");
            int choice = scanner.nextInt(); scanner.nextLine();
            switch (choice) {
                case 1 -> controller.viewCars(true);
                case 2 -> controller.bookCar(user.getId());
                case 3 -> { return; }
                default -> System.out.println("Invalid.");
            }
        }
    }
}
