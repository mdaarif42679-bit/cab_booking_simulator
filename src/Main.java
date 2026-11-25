import java.util.Scanner;

/** Console menu for the Cab Booking Simulator. Runs until the user chooses Exit. */
public class Main {

    private static void printMenu() {
        System.out.println();
        System.out.println("========== CAB BOOKING SIMULATOR ==========");
        System.out.println();
        System.out.println("1. Book a Cab");
        System.out.println("2. Complete a Trip");
        System.out.println("3. View Driver Status");
        System.out.println("4. View Waiting Queue");
        System.out.println("5. Display City Road Map");
        System.out.println("6. View Trip History");
        System.out.println("7. Exit");
        System.out.println();
        System.out.println("===========================================");
    }

    /** Prints a prompt and reads one line. Returns null when the input stream has ended. */
    private static String ask(Scanner in, String prompt) {
        System.out.print(prompt);
        return in.hasNextLine() ? in.nextLine() : null;
    }

    public static void main(String[] args) {
        CabSimulatorManager manager = new CabSimulatorManager();
        Scanner in = new Scanner(System.in);
        System.out.println("Locations: A, B, C, D  |  Drivers: D1 (at A), D2 (at D)  |  Fare: Rs. "
                + CabSimulatorManager.FARE_PER_KM + " per km");

        boolean running = true;
        while (running) {
            printMenu();
            String line = ask(in, "Enter your choice (1-7): ");
            if (line == null) break;           // input ended
            line = line.trim();
            if (line.isEmpty()) {
                System.out.println("  [!] Empty input. Please enter a number from 1 to 7.");
                continue;
            }
            int choice;
            try {
                choice = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid input '" + line + "'. Please enter a number from 1 to 7.");
                continue;
            }

            switch (choice) {
                case 1: {
                    String pickup = ask(in, "  Enter pickup location : ");
                    if (pickup == null) { running = false; break; }
                    String drop = ask(in, "  Enter drop location   : ");
                    if (drop == null) { running = false; break; }
                    manager.requestCab(pickup, drop);
                    break;
                }
                case 2: {
                    String id = ask(in, "  Enter driver ID (e.g. D1): ");
                    if (id == null) { running = false; break; }
                    manager.completeTrip(id);
                    break;
                }
                case 3: manager.showDriverStatus(); break;
                case 4: manager.showWaitingQueue(); break;
                case 5: manager.showRoadMap(); break;
                case 6: manager.showHistory(); break;
                case 7:
                    System.out.println("  Thank you for using Cab Booking Simulator. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("  [!] Invalid choice " + choice + ". Please enter a number from 1 to 7.");
            }
        }
        in.close();
    }
}
