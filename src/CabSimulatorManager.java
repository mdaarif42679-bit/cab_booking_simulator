import java.util.*;

/**
 * Core logic: owns the Graph, the drivers, the FIFO waiting queue and the trip history.
 * Nearest driver is chosen with a PriorityQueue (min-heap) ordered by road distance.
 */
public class CabSimulatorManager {
    public static final int FARE_PER_KM = 15;
    public static final String ASSIGNED = "ASSIGNED";
    public static final String QUEUED = "QUEUED";
    public static final String INVALID = "INVALID";

    private final Graph graph = new Graph();
    private final List<Driver> drivers = new ArrayList<>();
    private final Queue<Rider> waitingQueue = new LinkedList<>();   // FIFO
    private final List<TripRecord> history = new ArrayList<>();
    private int nextRiderId = 1;

    public CabSimulatorManager() {
        buildMap();
        drivers.add(new Driver("D1", "A"));
        drivers.add(new Driver("D2", "D"));
    }

    private void buildMap() {
        graph.addRoad("A", "B", 2);
        graph.addRoad("B", "C", 3);
        graph.addRoad("A", "C", 6);
        graph.addRoad("C", "D", 4);
        graph.addRoad("B", "D", 8);
    }

    public Graph getGraph() { return graph; }
    public List<Driver> getDrivers() { return Collections.unmodifiableList(drivers); }
    public List<Rider> getWaitingRiders() { return new ArrayList<>(waitingQueue); }
    public List<TripRecord> getHistory() { return Collections.unmodifiableList(history); }

    /** A driver together with his distance from the pickup; ordered nearest first (ties: smaller driver ID). */
    private static class DriverChoice implements Comparable<DriverChoice> {
        final Driver driver;
        final int distance;

        DriverChoice(Driver driver, int distance) {
            this.driver = driver;
            this.distance = distance;
        }

        @Override
        public int compareTo(DriverChoice other) {
            if (distance != other.distance) return Integer.compare(distance, other.distance);
            return driver.getId().compareTo(other.driver.getId());
        }
    }

    /** Validates a booking request and assigns the nearest driver or queues the rider. Returns ASSIGNED, QUEUED or INVALID. */
    public String requestCab(String pickupInput, String dropInput) {
        String pickup = normalise(pickupInput);
        String drop = normalise(dropInput);

        if (pickup.isEmpty() || drop.isEmpty()) {
            System.out.println("  [!] Pickup and drop locations cannot be empty.");
            return INVALID;
        }
        if (!graph.hasLocation(pickup)) {
            System.out.println("  Location " + pickup + " not found.");
            return INVALID;
        }
        if (!graph.hasLocation(drop)) {
            System.out.println("  Location " + drop + " not found.");
            return INVALID;
        }
        if (pickup.equals(drop)) {
            System.out.println("  [!] Pickup and drop locations must be different.");
            return INVALID;
        }

        Rider rider = new Rider(nextRiderId++, pickup, drop);
        if (assignNearestDriver(rider)) {
            return ASSIGNED;
        }
        waitingQueue.add(rider);
        System.out.println();
        System.out.println("  No cab available. Rider " + rider.getId() + " added to waiting queue.");
        System.out.println("  Position in queue: " + waitingQueue.size() + "  (" + pickup + " -> " + drop + ")");
        return QUEUED;
    }

    /** Runs Dijkstra from the pickup, puts every available driver in a min-heap and assigns the nearest. */
    private boolean assignNearestDriver(Rider rider) {
        Map<String, Integer> dist = graph.shortestDistances(rider.getPickup());

        PriorityQueue<DriverChoice> heap = new PriorityQueue<>();
        for (Driver d : drivers) {
            if (d.isAvailable()) {
                heap.add(new DriverChoice(d, dist.get(d.getLocation())));
            }
        }
        if (heap.isEmpty()) {
            return false;
        }
        DriverChoice best = heap.poll();
        int tripKm = dist.get(rider.getDrop());
        int fare = tripKm * FARE_PER_KM;

        best.driver.assign(rider);
        history.add(new TripRecord(rider.getId(), best.driver.getId(), rider.getPickup(), rider.getDrop(),
                best.distance, tripKm, fare));

        System.out.println();
        System.out.println("  ---------- BOOKING CONFIRMED ----------");
        System.out.println("  Rider ID        : " + rider.getId());
        System.out.println("  Route           : " + rider.getPickup() + " -> " + rider.getDrop());
        System.out.println("  Cab " + best.driver.getId() + " assigned (" + best.distance + " km away).");
        System.out.println("  Trip distance: " + tripKm + " km");
        System.out.println("  Fare: Rs. " + fare + "  (" + tripKm + " km x Rs. " + FARE_PER_KM + ")");
        System.out.println("  Driver status   : " + best.driver.getId() + " is now BUSY");
        System.out.println("  ---------------------------------------");
        return true;
    }

    /** Completes the current trip of a driver, then serves waiting riders in FIFO order. Returns true on success. */
    public boolean completeTrip(String driverIdInput) {
        String id = normalise(driverIdInput);
        if (id.isEmpty()) {
            System.out.println("  [!] Driver ID cannot be empty.");
            return false;
        }
        Driver driver = null;
        for (Driver d : drivers) {
            if (d.getId().equals(id)) driver = d;
        }
        if (driver == null) {
            System.out.println("  [!] Driver " + id + " not found. Valid IDs: " + driverIds());
            return false;
        }
        if (driver.isAvailable()) {
            System.out.println("  [!] Driver " + id + " is already available. No trip to complete.");
            return false;
        }

        Rider rider = driver.getCurrentRider();
        for (TripRecord t : history) {
            if (t.getRiderId() == rider.getId() && t.getStatus().equals(TripRecord.IN_PROGRESS)) {
                t.markCompleted();
            }
        }
        driver.finishTrip();
        System.out.println();
        System.out.println("  Trip of Rider " + rider.getId() + " completed.");
        System.out.println("  Driver " + id + " is now at " + driver.getLocation() + " and is available.");

        dispatchWaitingRiders();
        return true;
    }

    /** Serves the waiting queue front-first while a driver is available. */
    private void dispatchWaitingRiders() {
        while (!waitingQueue.isEmpty() && anyDriverAvailable()) {
            Rider next = waitingQueue.poll();
            System.out.println("  Serving waiting Rider " + next.getId() + "...");
            assignNearestDriver(next);
        }
    }

    private boolean anyDriverAvailable() {
        for (Driver d : drivers) {
            if (d.isAvailable()) return true;
        }
        return false;
    }

    private String driverIds() {
        StringBuilder sb = new StringBuilder();
        for (Driver d : drivers) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(d.getId());
        }
        return sb.toString();
    }

    private static String normalise(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    // ---------------- Display methods ----------------

    public void showDriverStatus() {
        System.out.println();
        System.out.println("  DRIVER STATUS");
        System.out.println("  +-----------+------------------+-----------+----------------------+");
        System.out.println("  | Driver ID | Current Location | Status    | Current Rider        |");
        System.out.println("  +-----------+------------------+-----------+----------------------+");
        for (Driver d : drivers) {
            String rider = d.isAvailable() ? "-"
                    : "Rider " + d.getCurrentRider().getId() + " (" + d.getCurrentRider().getPickup()
                    + " -> " + d.getCurrentRider().getDrop() + ")";
            System.out.printf("  | %-9s | %-16s | %-9s | %-20s |%n",
                    d.getId(), d.getLocation(), d.isAvailable() ? "AVAILABLE" : "BUSY", rider);
        }
        System.out.println("  +-----------+------------------+-----------+----------------------+");
    }

    public void showWaitingQueue() {
        System.out.println();
        System.out.println("  WAITING QUEUE (FIFO)");
        if (waitingQueue.isEmpty()) {
            System.out.println("  The waiting queue is empty.");
            return;
        }
        System.out.println("  +----------+----------+--------+--------+");
        System.out.println("  | Position | Rider ID | Pickup | Drop   |");
        System.out.println("  +----------+----------+--------+--------+");
        int pos = 1;
        for (Rider r : waitingQueue) {
            System.out.printf("  | %-8d | %-8d | %-6s | %-6s |%n", pos++, r.getId(), r.getPickup(), r.getDrop());
        }
        System.out.println("  +----------+----------+--------+--------+");
    }

    public void showRoadMap() {
        System.out.println();
        System.out.println("  CITY ROAD MAP (bidirectional roads)");
        System.out.println("  Adjacency list:");
        for (String loc : graph.getLocations()) {
            StringBuilder sb = new StringBuilder();
            for (Road r : graph.getRoads(loc)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(r.getTo()).append("(").append(r.getKm()).append(" km)");
            }
            System.out.println("    " + loc + " : " + sb);
        }
        System.out.println();
        System.out.println("  Shortest road distances in km (calculated by Dijkstra):");
        StringBuilder header = new StringBuilder("    From\\To");
        for (String to : graph.getLocations()) header.append(String.format("%5s", to));
        System.out.println(header);
        for (String from : graph.getLocations()) {
            Map<String, Integer> dist = graph.shortestDistances(from);
            StringBuilder row = new StringBuilder(String.format("    %-7s", from));
            for (String to : graph.getLocations()) row.append(String.format("%5d", dist.get(to)));
            System.out.println(row);
        }
    }

    public void showHistory() {
        System.out.println();
        System.out.println("  TRIP HISTORY");
        if (history.isEmpty()) {
            System.out.println("  No trips yet.");
            return;
        }
        String line = "  +-------+--------+--------+------+--------------+-----------+---------+-------------+";
        System.out.println(line);
        System.out.println("  | Rider | Driver | Pickup | Drop | Driver->Pick | Trip (km) | Fare    | Status      |");
        System.out.println(line);
        for (TripRecord t : history) {
            System.out.printf("  | %-5d | %-6s | %-6s | %-4s | %-12s | %-9d | %-7s | %-11s |%n",
                    t.getRiderId(), t.getDriverId(), t.getPickup(), t.getDrop(),
                    t.getDriverToPickupKm() + " km", t.getTripKm(), "Rs. " + t.getFare(), t.getStatus());
        }
        System.out.println(line);
    }
}
