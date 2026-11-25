import java.util.List;

/** Automated check of the four required test cases. Run with: java TestRunner */
public class TestRunner {
    private static int passed = 0, failed = 0;

    private static void check(String name, boolean condition) {
        System.out.println((condition ? "  PASS  " : "  FAIL  ") + name);
        if (condition) passed++; else failed++;
    }

    private static boolean matches(TripRecord t, String driver, int away, int trip, int fare) {
        return t.getDriverId().equals(driver) && t.getDriverToPickupKm() == away
                && t.getTripKm() == trip && t.getFare() == fare;
    }

    public static void main(String[] args) {
        System.out.println("=== Test Case 1: Pickup B, Drop C ===");
        CabSimulatorManager m = new CabSimulatorManager();
        check("result is ASSIGNED", m.requestCab("B", "C").equals(CabSimulatorManager.ASSIGNED));
        check("D1, 2 km away, trip 3 km, Rs. 45", matches(m.getHistory().get(0), "D1", 2, 3, 45));

        System.out.println("\n=== Test Case 2: Pickup A, Drop B ===");
        m = new CabSimulatorManager();
        check("result is ASSIGNED", m.requestCab("A", "B").equals(CabSimulatorManager.ASSIGNED));
        check("D1, 0 km away, trip 2 km, Rs. 30", matches(m.getHistory().get(0), "D1", 0, 2, 30));

        System.out.println("\n=== Test Case 3: Pickup Z, Drop C ===");
        m = new CabSimulatorManager();
        check("result is INVALID", m.requestCab("Z", "C").equals(CabSimulatorManager.INVALID));
        check("no trip recorded and both drivers still available",
                m.getHistory().isEmpty() && m.getDrivers().get(0).isAvailable() && m.getDrivers().get(1).isAvailable());

        System.out.println("\n=== Test Case 4: Multiple riders ===");
        m = new CabSimulatorManager();
        check("Rider 1 ASSIGNED", m.requestCab("B", "C").equals(CabSimulatorManager.ASSIGNED));
        check("Rider 2 ASSIGNED", m.requestCab("B", "D").equals(CabSimulatorManager.ASSIGNED));
        check("Rider 3 QUEUED", m.requestCab("A", "C").equals(CabSimulatorManager.QUEUED));
        List<TripRecord> h = m.getHistory();
        check("Rider 1: D1, 2 km away, trip 3 km, Rs. 45", matches(h.get(0), "D1", 2, 3, 45));
        check("Rider 2: D2, 7 km away, trip 7 km, Rs. 105", matches(h.get(1), "D2", 7, 7, 105));
        check("waiting queue holds exactly Rider 3", m.getWaitingRiders().size() == 1 && m.getWaitingRiders().get(0).getId() == 3);
        check("D1 completes trip", m.completeTrip("D1"));
        check("D1 moved to C", m.getDrivers().get(0).getLocation().equals("C"));
        check("Rider 3 assigned to D1: 5 km away, trip 5 km, Rs. 75", h.size() == 3 && matches(h.get(2), "D1", 5, 5, 75));
        check("waiting queue is now empty", m.getWaitingRiders().isEmpty());
        check("Rider 1 trip marked COMPLETED", h.get(0).getStatus().equals(TripRecord.COMPLETED));

        System.out.println("\n=== Extra: input validation ===");
        m = new CabSimulatorManager();
        check("available driver cannot be completed", !m.completeTrip("D1"));
        check("unknown driver rejected", !m.completeTrip("D9"));
        check("empty driver ID rejected", !m.completeTrip(""));
        check("empty pickup rejected", m.requestCab("", "C").equals(CabSimulatorManager.INVALID));
        check("lower-case input accepted", m.requestCab("b", "c").equals(CabSimulatorManager.ASSIGNED));
        check("dijkstra A->D = 9 km", m.getGraph().shortestDistances("A").get("D") == 9);
        check("dijkstra A->C = 5 km (not direct 6)", m.getGraph().shortestDistances("A").get("C") == 5);

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed.");
        System.exit(failed == 0 ? 0 : 1);
    }
}
