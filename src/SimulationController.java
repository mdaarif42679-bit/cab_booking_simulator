import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Bridge between the Swing GUI and the real simulation logic (CabSimulatorManager).
 * It adds NO logic of its own: every booking, nearest-driver choice, queue operation and trip completion
 * is performed by CabSimulatorManager. This class only reports what happened so the GUI can show it.
 */
public class SimulationController {

    public enum Kind { ASSIGNED, QUEUED, INVALID, COMPLETED, REJECTED }

    /** What one GUI action did. */
    public static class Result {
        public Kind kind;
        public String message = "";
        public String console = "";                 // text the simulator printed
        public Rider rider;                         // rider booked / rider whose trip completed
        public TripRecord trip;                     // trip created (ASSIGNED) or completed (COMPLETED)
        public int queuePosition;                   // QUEUED
        public String driverId;                     // COMPLETED: driver who finished
        public String fromLocation, toLocation;     // COMPLETED: driver's location before / after
        public final List<TripRecord> dispatched = new ArrayList<>();  // waiting riders served automatically
    }

    private CabSimulatorManager manager = new CabSimulatorManager();

    public CabSimulatorManager manager() { return manager; }
    public Graph graph() { return manager.getGraph(); }
    public List<Driver> drivers() { return manager.getDrivers(); }
    public List<Rider> waiting() { return manager.getWaitingRiders(); }
    public List<TripRecord> history() { return manager.getHistory(); }

    /** Starts a brand-new simulation (rider numbers restart at 1). */
    public void reset() { manager = new CabSimulatorManager(); }

    public int driverIndex(String id) {
        List<Driver> ds = drivers();
        for (int i = 0; i < ds.size(); i++) if (ds.get(i).getId().equals(id)) return i;
        return 0;
    }

    public Driver driver(String id) {
        for (Driver d : drivers()) if (d.getId().equals(id)) return d;
        return null;
    }

    /** The in-progress trip of a driver, or null. */
    public TripRecord activeTrip(String driverId) {
        TripRecord found = null;
        for (TripRecord t : history()) {
            if (t.getDriverId().equals(driverId) && t.getStatus().equals(TripRecord.IN_PROGRESS)) found = t;
        }
        return found;
    }

    public TripRecord tripOfRider(int riderId) {
        for (TripRecord t : history()) if (t.getRiderId() == riderId) return t;
        return null;
    }

    public Rider waitingRider(int riderId) {
        for (Rider r : waiting()) if (r.getId() == riderId) return r;
        return null;
    }

    /** Shortest route (list of locations) found with Dijkstra. */
    public List<String> route(String from, String to) { return graph().shortestPath(from, to); }

    /** Shortest road distance in km (Dijkstra), or -1 if a location is unknown. */
    public int distance(String from, String to) {
        if (!graph().hasLocation(from) || !graph().hasLocation(to)) return -1;
        return graph().shortestDistances(from).get(to);
    }

    public int completedRevenue() {
        int sum = 0;
        for (TripRecord t : history()) if (t.getStatus().equals(TripRecord.COMPLETED)) sum += t.getFare();
        return sum;
    }

    public int availableDrivers() {
        int n = 0;
        for (Driver d : drivers()) if (d.isAvailable()) n++;
        return n;
    }

    // ------------------------------------------------------------------ actions

    public Result book(String pickup, String drop) {
        Result r = new Result();
        int before = history().size();
        final String[] status = new String[1];
        r.console = capture(new Runnable() {
            public void run() { status[0] = manager.requestCab(pickup, drop); }
        });
        if (CabSimulatorManager.ASSIGNED.equals(status[0])) {
            r.kind = Kind.ASSIGNED;
            r.trip = history().get(before);
            r.rider = new Rider(r.trip.getRiderId(), r.trip.getPickup(), r.trip.getDrop());
            r.message = "Cab " + r.trip.getDriverId() + " assigned (" + r.trip.getDriverToPickupKm() + " km away). Trip "
                    + r.trip.getTripKm() + " km, Fare Rs. " + r.trip.getFare();
        } else if (CabSimulatorManager.QUEUED.equals(status[0])) {
            r.kind = Kind.QUEUED;
            List<Rider> w = waiting();
            r.rider = w.get(w.size() - 1);
            r.queuePosition = w.size();
            r.message = "No cab available. Rider " + r.rider.getId() + " added to waiting queue (position "
                    + r.queuePosition + ").";
        } else {
            r.kind = Kind.INVALID;
            r.message = firstLine(r.console, "Invalid request.");
        }
        return r;
    }

    public Result complete(String driverId) {
        Result r = new Result();
        Driver d = driver(driverId == null ? "" : driverId.trim().toUpperCase());
        String prev = d == null ? null : d.getLocation();
        Rider rider = d == null ? null : d.getCurrentRider();
        int before = history().size();
        final boolean[] ok = new boolean[1];
        r.console = capture(new Runnable() {
            public void run() { ok[0] = manager.completeTrip(driverId); }
        });
        if (!ok[0] || d == null || rider == null) {
            r.kind = Kind.REJECTED;
            r.message = firstLine(r.console, "Could not complete the trip.");
            return r;
        }
        r.kind = Kind.COMPLETED;
        r.driverId = d.getId();
        r.rider = rider;
        r.fromLocation = prev;
        r.toLocation = d.getLocation() == null ? rider.getDrop() : d.getLocation();
        r.trip = tripOfRider(rider.getId());
        for (int i = before; i < history().size(); i++) r.dispatched.add(history().get(i));
        StringBuilder sb = new StringBuilder("Trip of Rider " + rider.getId() + " completed. " + d.getId()
                + " is now at " + rider.getDrop() + " and available.");
        for (TripRecord t : r.dispatched) {
            sb.append(" Waiting Rider ").append(t.getRiderId()).append(" served by ").append(t.getDriverId())
              .append(" (").append(t.getDriverToPickupKm()).append(" km away, Rs. ").append(t.getFare()).append(").");
        }
        r.message = sb.toString();
        return r;
    }

    // ------------------------------------------------------------------ helpers

    /** Runs code while capturing everything it prints to System.out. */
    private static String capture(Runnable code) {
        PrintStream old = System.out;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(bos, true, "UTF-8"));
            code.run();
        } catch (java.io.UnsupportedEncodingException e) {
            code.run();
        } finally {
            System.setOut(old);
        }
        try {
            return bos.toString("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return bos.toString();
        }
    }

    private static String firstLine(String text, String fallback) {
        for (String line : text.split("\\r?\\n")) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("---")) continue;
            if (s.startsWith("[!]")) s = s.substring(3).trim();
            return s;
        }
        return fallback;
    }
}
