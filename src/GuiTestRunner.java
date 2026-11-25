import java.util.List;

/**
 * Tests of the GUI layer. Works with or without a display (the dashboard is built but never shown).
 * It presses the REAL buttons and checks that every panel shows the real simulator state.
 * Run with: java -cp out GuiTestRunner
 */
public class GuiTestRunner {
    private static int passed = 0, failed = 0;

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + name);
        if (ok) passed++; else failed++;
    }

    private static CabDashboardPanel panel;

    private static void book(String p, String d) {
        panel.pickupCombo.setSelectedItem(p);
        panel.dropCombo.setSelectedItem(d);
        panel.bookBtn.doClick();
    }

    private static void completeDriver(String id) {
        for (int i = 0; i < panel.driverCombo.getItemCount(); i++) {
            if (panel.driverCombo.getItemAt(i).startsWith(id)) panel.driverCombo.setSelectedIndex(i);
        }
        panel.completeBtn.doClick();
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        MapPanel.ANIMATE = false;
        final java.io.PrintStream realOut = System.out;

        // ---------- controller (GUI <-> simulator bridge) ----------
        System.out.println("=== Controller: Test Case 4 through SimulationController ===");
        SimulationController c = new SimulationController();
        SimulationController.Result r1 = c.book("B", "C");
        check("Rider 1 ASSIGNED to D1, 2 km away, trip 3 km, Rs. 45",
                r1.kind == SimulationController.Kind.ASSIGNED && r1.trip.getDriverId().equals("D1")
                        && r1.trip.getDriverToPickupKm() == 2 && r1.trip.getTripKm() == 3 && r1.trip.getFare() == 45);
        SimulationController.Result r2 = c.book("B", "D");
        check("Rider 2 ASSIGNED to D2, 7 km away, trip 7 km, Rs. 105",
                r2.trip.getDriverId().equals("D2") && r2.trip.getDriverToPickupKm() == 7 && r2.trip.getTripKm() == 7 && r2.trip.getFare() == 105);
        SimulationController.Result r3 = c.book("A", "C");
        check("Rider 3 QUEUED at position 1", r3.kind == SimulationController.Kind.QUEUED && r3.queuePosition == 1);
        SimulationController.Result done = c.complete("D1");
        check("D1 completes Rider 1, moves to C", done.kind == SimulationController.Kind.COMPLETED
                && done.fromLocation.equals("A") && done.toLocation.equals("C"));
        check("Waiting Rider 3 dispatched to D1 (5 km away, Rs. 75)", done.dispatched.size() == 1
                && done.dispatched.get(0).getDriverId().equals("D1") && done.dispatched.get(0).getDriverToPickupKm() == 5
                && done.dispatched.get(0).getFare() == 75);
        check("queue empty afterwards", c.waiting().isEmpty());
        check("route A->D is A-B-C-D (9 km)", GuiTestRunner.eq(c.route("A", "D"), "A", "B", "C", "D") && c.distance("A", "D") == 9);
        check("route A->C is A-B-C (5 km, not the direct 6 km)", GuiTestRunner.eq(c.route("A", "C"), "A", "B", "C"));
        check("route B->D is B-C-D (7 km, not the direct 8 km)", GuiTestRunner.eq(c.route("B", "D"), "B", "C", "D"));
        check("invalid location rejected", c.book("Z", "C").kind == SimulationController.Kind.INVALID);
        check("same pickup/drop rejected", c.book("A", "A").kind == SimulationController.Kind.INVALID);
        check("empty pickup rejected", c.book("", "C").kind == SimulationController.Kind.INVALID);
        check("completing an available driver rejected", new SimulationController().complete("D1").kind == SimulationController.Kind.REJECTED);
        c.reset();
        check("reset restores D1 at A, D2 at D, no history", c.drivers().get(0).getLocation().equals("A")
                && c.drivers().get(1).getLocation().equals("D") && c.history().isEmpty());

        // ---------- dashboard driven through its real buttons ----------
        System.out.println("\n=== Dashboard: Test Case 4 by pressing the real buttons ===");
        panel = new CabDashboardPanel();
        check("starts with 2/2 drivers free, 0 waiting", panel.chipText(0).equals("2/2") && panel.chipText(1).equals("0"));
        check("history empty, Complete Trip disabled", panel.historyRows() == 0 && !panel.completeBtn.isEnabled());

        book("B", "C");
        check("Book B->C: history has 1 row, driver D1, 2 km, 3 km, Rs. 45",
                panel.historyRows() == 1 && panel.historyCell(0, 1).equals("D1") && panel.historyCell(0, 3).equals("2 km")
                        && panel.historyCell(0, 4).equals("3 km") && panel.historyCell(0, 6).equals("Rs. 45"));
        check("live trip card shows D1, B to C, Rs. 45", panel.liveDriver().equals("D1")
                && panel.liveRoute().equals("B \u2192 C") && panel.liveFare().equals("Rs. 45"));
        check("D1 card is BUSY with Rider 1", panel.cards.get(0).statusText().equals("BUSY") && panel.cards.get(0).riderText().equals("Rider 1"));
        check("Complete Trip button now enabled", panel.completeBtn.isEnabled());

        book("B", "D");
        check("Book B->D: D2 assigned, 7 km, Rs. 105", panel.historyCell(0, 1).equals("D2") && panel.historyCell(0, 3).equals("7 km")
                && panel.historyCell(0, 6).equals("Rs. 105"));
        check("live route for B->D is B, C, D", panel.liveRoute().equals("B \u2192 C \u2192 D"));

        book("A", "C");
        check("Book A->C: no driver free, rider queued", panel.queueSize() == 1 && panel.chipText(1).equals("1")
                && panel.historyCell(0, 7).equals("WAITING") && panel.chipText(0).equals("0/2"));
        check("live trip pill shows WAITING", panel.livePill().equals("WAITING"));

        completeDriver("D1");
        check("D1 card now at C, busy with Rider 3 (5 km pickup, Rs. 75)",
                panel.cards.get(0).locText().equals("C") && panel.cards.get(0).riderText().equals("Rider 3")
                        && panel.cards.get(0).distText().equals("5 km"));
        check("waiting queue empty after auto-dispatch", panel.queueSize() == 0 && panel.chipText(1).equals("0"));
        check("history: 3 rows, Rider 1 COMPLETED, Rider 3 IN PROGRESS Rs. 75",
                panel.historyRows() == 3 && panel.historyCell(2, 7).equals("COMPLETED")
                        && panel.historyCell(0, 7).equals("IN PROGRESS") && panel.historyCell(0, 6).equals("Rs. 75"));
        check("revenue = Rs. 45 (only completed trips)", panel.chipText(3).equals("Rs. 45"));

        panel.pickupCombo.setSelectedItem("A");
        panel.dropCombo.setSelectedItem("A");
        panel.bookBtn.doClick();
        check("same pickup and drop adds no booking", panel.historyRows() == 3);
        panel.pickupCombo.setSelectedIndex(0);
        panel.dropCombo.setSelectedIndex(0);
        panel.bookBtn.doClick();
        check("booking with nothing selected adds no booking", panel.historyRows() == 3);

        panel.resetBtn.doClick();
        check("Reset: empty history, 2/2 free, D1 at A, D2 at D", panel.historyRows() == 0 && panel.chipText(0).equals("2/2")
                && panel.cards.get(0).locText().equals("A") && panel.cards.get(1).locText().equals("D"));

        // ---------- map painting must not throw ----------
        System.out.println("\n=== Rendering ===");
        boolean ok = true;
        try {
            panel.setSize(1300, 800);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1300, 800, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = img.createGraphics();
            book("B", "C");
            panel.pickupCombo.setSelectedItem("A");
            panel.dropCombo.setSelectedItem("D");
            panel.map.setSize(700, 450);
            panel.map.paint(g);
            g.dispose();
        } catch (Throwable t) {
            ok = false;
            t.printStackTrace();
        }
        check("map paints with routes, tags and drivers without errors", ok);

        System.out.println("\nResult: " + passed + " passed, " + failed + " failed.");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static boolean eq(List<String> route, String... expected) {
        return route.equals(java.util.Arrays.asList(expected));
    }
}
