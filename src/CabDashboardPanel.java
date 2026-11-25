import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/** The main dashboard window. Every panel reads its data from the real simulator through SimulationController. */
public class CabDashboardPanel extends JPanel {

    private static final String PLACEHOLDER = "Select";
    private static final String NO_ACTIVE = "No active trips";
    private static final String ARROW = " " + Theme.ARROW + " ";

    final SimulationController sim = new SimulationController();
    final MapPanel map = new MapPanel(sim);

    // controls
    UiKit.StyledCombo pickupCombo, dropCombo, driverCombo;
    UiKit.RoundButton bookBtn, completeBtn, resetBtn, demoBtn;
    private final JLabel previewLabel = UiKit.label(" ", 12.5f, Font.PLAIN, Theme.MUTED);
    private final List<String> busyIds = new ArrayList<>();
    private boolean updating = false;
    private boolean uiLocked = false;
    private int focusRider = 0;

    // header
    private StatChip chipFree, chipWaiting, chipTrips, chipRevenue;

    // live trip
    private final UiKit.Pill tripPill = new UiKit.Pill("IDLE", Theme.MUTED);
    private final JLabel tRider = value(), tDriver = value(), tPickupDist = value(), tTripDist = value(), tRoute = value();
    private final JLabel tFare = UiKit.label("Rs. 0", 28f, Font.BOLD, Theme.GREEN);
    private final JLabel tFareNote = UiKit.label("Book a cab to see live trip details", 12f, Font.PLAIN, Theme.MUTED);

    // drivers, queue, history, log
    final List<DriverCard> cards = new ArrayList<>();
    private JPanel queueRows;
    private final UiKit.Pill queueCount = new UiKit.Pill("0 waiting", Theme.MUTED);
    private final HistoryModel historyModel = new HistoryModel();
    final JTable historyTable = new JTable(historyModel);
    private final JTextPane logPane = new JTextPane();

    // demo
    private boolean demoRunning = false;
    private javax.swing.Timer demoTimer;
    private int demoIndex;
    private List<Runnable> demoActions;
    private List<Integer> demoDelays;

    public CabDashboardPanel() {
        super(new BorderLayout());
        setOpaque(true);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        map.setListener(new MapPanel.Listener() {
            public void nodeClicked(String node) { onNodeClicked(node); }
            public void driverClicked(String id) { selectDriver(id); }
        });

        logLine("Simulator ready. Locations A, B, C, D " + Theme.DOT + " Drivers D1 (at A), D2 (at D) " + Theme.DOT
                + " Fare Rs. " + CabSimulatorManager.FARE_PER_KM + " per km", Theme.MUTED, false);
        refreshAll();
        map.showToast(0, "Welcome! Pick a pickup and drop location (or click the map), then press Book Cab.");
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setPaint(new GradientPaint(0, 0, Theme.BG_TOP, 0, getHeight(), Theme.BG));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    // ================================================================ header
    private JComponent buildHeader() {
        JPanel h = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setPaint(new GradientPaint(0, 0, new Color(0x13255A), getWidth(), 0, new Color(0x0C183C)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.BORDER);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        h.setOpaque(false);
        h.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.WEST;

        JComponent logo = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                UiKit.quality(g);
                g.setPaint(new GradientPaint(0, 0, Theme.CYAN, 0, getHeight(), Theme.BLUE));
                g.fill(new Ellipse2D.Float(0, 0, getWidth(), getHeight()));
                UiKit.drawCar(g, getWidth() / 2.0, getHeight() / 2.0 + 1, Color.WHITE, 0.95, 0);
                g.dispose();
            }
        };
        logo.setPreferredSize(new Dimension(46, 46));
        c.gridx = 0; c.insets = new Insets(0, 0, 0, 14);
        h.add(logo, c);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(UiKit.label("Cab Booking Simulator", 21f, Font.BOLD, Color.WHITE));
        titles.add(UiKit.label("DSA PS No. 86  " + Theme.DOT + "  Graph  " + Theme.DOT + "  Dijkstra  " + Theme.DOT
                + "  PriorityQueue  " + Theme.DOT + "  FIFO Queue", 12f, Font.PLAIN, Theme.CYAN));
        c.gridx = 1; c.insets = new Insets(0, 0, 0, 0);
        h.add(titles, c);

        JPanel filler = new JPanel();
        filler.setOpaque(false);
        c.gridx = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        h.add(filler, c);
        c.weightx = 0; c.fill = GridBagConstraints.NONE;

        chipFree = new StatChip("DRIVERS FREE", Theme.GREEN);
        chipWaiting = new StatChip("WAITING", Theme.AMBER);
        chipTrips = new StatChip("TRIPS", Theme.CYAN);
        chipRevenue = new StatChip("REVENUE", Theme.VIOLET);
        StatChip[] chips = {chipFree, chipWaiting, chipTrips, chipRevenue};
        int gx = 3;
        for (StatChip s : chips) {
            c.gridx = gx++; c.insets = new Insets(0, 0, 0, 8);
            h.add(s, c);
        }

        demoBtn = new UiKit.RoundButton("Run Demo", UiKit.RoundButton.Style.GHOST);
        demoBtn.setToolTipText("Replays Test Case 4 from the presentation: B-C, B-D, A-C, then D1 completes");
        demoBtn.addActionListener(e -> toggleDemo());
        resetBtn = new UiKit.RoundButton("Reset", UiKit.RoundButton.Style.DANGER);
        resetBtn.setToolTipText("Start a fresh simulation (D1 at A, D2 at D, empty queue)");
        resetBtn.addActionListener(e -> doReset());
        c.gridx = gx++; c.insets = new Insets(0, 8, 0, 8);
        h.add(demoBtn, c);
        c.gridx = gx; c.insets = new Insets(0, 0, 0, 0);
        h.add(resetBtn, c);
        return h;
    }

    // ================================================================== body
    private JComponent buildBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        UiKit.ScrollColumn colA = new UiKit.ScrollColumn(new GridBagLayout());
        GridBagConstraints a = new GridBagConstraints();
        a.gridx = 0; a.weightx = 1; a.fill = GridBagConstraints.HORIZONTAL; a.gridy = 0;
        colA.add(buildBookingCard(), a);
        a.gridy = 1; a.insets = new Insets(10, 0, 0, 0);
        colA.add(buildLiveTripCard(), a);
        a.gridy = 2; a.weighty = 1; a.fill = GridBagConstraints.BOTH;
        JPanel sp = new JPanel(); sp.setOpaque(false);
        colA.add(sp, a);
        JScrollPane scrollA = UiKit.scroll(colA);
        scrollA.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollA.setPreferredSize(new Dimension(350, 300));
        scrollA.setMinimumSize(new Dimension(340, 120));

        UiKit.ScrollColumn colC = new UiKit.ScrollColumn(new GridBagLayout());
        GridBagConstraints k = new GridBagConstraints();
        k.gridx = 0; k.weightx = 1; k.fill = GridBagConstraints.HORIZONTAL; k.gridy = 0;
        colC.add(sectionLabel("DRIVER DASHBOARD"), k);
        int row = 1;
        for (Driver d : sim.drivers()) {
            DriverCard dc = new DriverCard(d.getId(), sim.driverIndex(d.getId()));
            cards.add(dc);
            k.gridy = row++; k.insets = new Insets(8, 0, 0, 0);
            colC.add(dc, k);
        }
        k.gridy = row++; k.insets = new Insets(12, 0, 0, 0);
        colC.add(buildQueueCard(), k);
        k.gridy = row; k.weighty = 1; k.fill = GridBagConstraints.BOTH; k.insets = new Insets(0, 0, 0, 0);
        JPanel sp2 = new JPanel(); sp2.setOpaque(false);
        colC.add(sp2, k);
        JScrollPane scrollC = UiKit.scroll(colC);
        scrollC.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollC.setPreferredSize(new Dimension(340, 300));
        scrollC.setMinimumSize(new Dimension(330, 120));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0; c.gridy = 0; c.weightx = 0; c.weighty = 1;
        body.add(scrollA, c);
        c.gridx = 1; c.weightx = 1; c.insets = new Insets(0, 12, 0, 12);
        body.add(map, c);
        c.gridx = 2; c.weightx = 0; c.insets = new Insets(0, 0, 0, 0);
        body.add(scrollC, c);

        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setOpaque(false);
        GridBagConstraints b = new GridBagConstraints();
        b.fill = GridBagConstraints.BOTH; b.gridy = 0; b.weighty = 1;
        b.gridx = 0; b.weightx = 1;
        bottom.add(buildHistoryCard(), b);
        b.gridx = 1; b.weightx = 0; b.insets = new Insets(0, 12, 0, 0);
        bottom.add(buildLogCard(), b);
        bottom.setPreferredSize(new Dimension(300, 184));
        bottom.setMinimumSize(new Dimension(300, 160));
        c.gridx = 0; c.gridy = 1; c.gridwidth = 3; c.weightx = 1; c.weighty = 0; c.insets = new Insets(12, 0, 0, 0);
        body.add(bottom, c);
        return body;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = UiKit.label(text, 11.5f, Font.BOLD, Theme.CYAN);
        l.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 0));
        return l;
    }

    private static JLabel value() { return UiKit.label("\u2014", 14f, Font.BOLD, Theme.TEXT); }

    // ---------------------------------------------------------- booking card
    private JComponent buildBookingCard() {
        UiKit.Card card = new UiKit.Card(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; c.gridx = 0; c.gridwidth = 2;

        c.gridy = 0;
        card.add(UiKit.label("BOOK A CAB", 11.5f, Font.BOLD, Theme.CYAN), c);

        List<String> locs = new ArrayList<>();
        locs.add(PLACEHOLDER);
        locs.addAll(sim.graph().getLocations());
        String[] items = locs.toArray(new String[0]);
        pickupCombo = new UiKit.StyledCombo(items);
        dropCombo = new UiKit.StyledCombo(items);

        c.gridwidth = 1; c.gridy = 1; c.insets = new Insets(8, 0, 4, 6);
        card.add(UiKit.label("Pickup location", 12f, Font.PLAIN, Theme.MUTED), c);
        c.gridx = 1; c.insets = new Insets(8, 6, 4, 0);
        card.add(UiKit.label("Drop location", 12f, Font.PLAIN, Theme.MUTED), c);
        c.gridy = 2; c.gridx = 0; c.insets = new Insets(0, 0, 0, 6);
        card.add(pickupCombo, c);
        c.gridx = 1; c.insets = new Insets(0, 6, 0, 0);
        card.add(dropCombo, c);

        c.gridx = 0; c.gridwidth = 2; c.gridy = 3; c.insets = new Insets(6, 2, 0, 0);
        card.add(previewLabel, c);

        bookBtn = new UiKit.RoundButton("Book Cab", UiKit.RoundButton.Style.PRIMARY);
        bookBtn.setFont(Theme.font(Font.BOLD, 15f));
        bookBtn.addActionListener(e -> doBook());
        c.gridy = 4; c.insets = new Insets(8, 0, 0, 0);
        card.add(bookBtn, c);

        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER);
        sep.setBackground(Theme.BORDER);
        c.gridy = 5; c.insets = new Insets(12, 0, 10, 0);
        card.add(sep, c);

        c.gridy = 6; c.insets = new Insets(0, 0, 0, 0);
        card.add(UiKit.label("COMPLETE TRIP", 11.5f, Font.BOLD, Theme.CYAN), c);

        driverCombo = new UiKit.StyledCombo(new String[]{NO_ACTIVE});
        c.gridy = 7; c.insets = new Insets(8, 0, 0, 0);
        card.add(driverCombo, c);

        completeBtn = new UiKit.RoundButton("Complete Trip", UiKit.RoundButton.Style.SUCCESS);
        completeBtn.setFont(Theme.font(Font.BOLD, 15f));
        completeBtn.addActionListener(e -> doComplete());
        c.gridy = 8; c.insets = new Insets(8, 0, 0, 0);
        card.add(completeBtn, c);

        pickupCombo.addActionListener(e -> onSelectionChanged());
        dropCombo.addActionListener(e -> onSelectionChanged());
        driverCombo.addActionListener(e -> {
            if (updating) return;
            int i = driverCombo.getSelectedIndex();
            if (i >= 0 && i < busyIds.size()) {
                TripRecord t = sim.activeTrip(busyIds.get(i));
                if (t != null) { focusRider = t.getRiderId(); map.setFocusRider(focusRider); updateLiveTrip(); updateCards(); }
            }
        });
        return card;
    }

    // ------------------------------------------------------- live trip card
    private JComponent buildLiveTripCard() {
        UiKit.Card card = new UiKit.Card(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.gridx = 0; c.anchor = GridBagConstraints.WEST; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        card.add(UiKit.label("LIVE TRIP INFORMATION", 11.5f, Font.BOLD, Theme.CYAN), c);
        c.gridx = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.EAST;
        card.add(tripPill, c);

        String[] keys = {"Rider ID", "Assigned driver", "Driver " + Theme.ARROW + " pickup", "Trip distance (shortest)", "Trip route"};
        JLabel[] vals = {tRider, tDriver, tPickupDist, tTripDist, tRoute};
        int y = 1;
        for (int i = 0; i < keys.length; i++) {
            GridBagConstraints kc = new GridBagConstraints();
            kc.gridy = y; kc.gridx = 0; kc.anchor = GridBagConstraints.WEST; kc.insets = new Insets(i == 0 ? 8 : 5, 0, 0, 8);
            card.add(UiKit.label(keys[i], 12.5f, Font.PLAIN, Theme.MUTED), kc);
            GridBagConstraints vc = new GridBagConstraints();
            vc.gridy = y++; vc.gridx = 1; vc.anchor = GridBagConstraints.EAST; vc.insets = new Insets(i == 0 ? 8 : 5, 0, 0, 0);
            card.add(vals[i], vc);
        }
        JPanel fare = new JPanel(new BorderLayout());
        fare.setOpaque(false);
        fare.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        JPanel fl = new JPanel();
        fl.setOpaque(false);
        fl.setLayout(new BoxLayout(fl, BoxLayout.Y_AXIS));
        fl.add(UiKit.label("FARE", 11f, Font.BOLD, Theme.MUTED));
        fl.add(tFareNote);
        fare.add(fl, BorderLayout.CENTER);
        fare.add(tFare, BorderLayout.EAST);
        GridBagConstraints fc = new GridBagConstraints();
        fc.gridy = y; fc.gridx = 0; fc.gridwidth = 2; fc.fill = GridBagConstraints.HORIZONTAL; fc.insets = new Insets(10, 0, 0, 0);
        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER);
        card.add(sep, fc);
        fc.gridy = y + 1; fc.insets = new Insets(0, 0, 0, 0);
        card.add(fare, fc);
        return card;
    }

    // ------------------------------------------------------------ queue card
    private JComponent buildQueueCard() {
        UiKit.Card card = new UiKit.Card(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UiKit.label("WAITING QUEUE (FIFO)", 11.5f, Font.BOLD, Theme.CYAN), BorderLayout.WEST);
        top.add(queueCount, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);
        queueRows = new JPanel();
        queueRows.setOpaque(false);
        queueRows.setLayout(new BoxLayout(queueRows, BoxLayout.Y_AXIS));
        card.add(queueRows, BorderLayout.CENTER);
        return card;
    }

    // ---------------------------------------------------------- history card
    private JComponent buildHistoryCard() {
        UiKit.Card card = new UiKit.Card(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        card.add(UiKit.label("BOOKING HISTORY", 11.5f, Font.BOLD, Theme.CYAN), BorderLayout.NORTH);
        UiKit.styleTable(historyTable);
        historyTable.setDefaultRenderer(Object.class, new UiKit.ZebraRenderer());
        TableColumnModel cm = historyTable.getColumnModel();
        int[] widths = {55, 60, 85, 85, 85, 150, 65, 105};
        for (int i = 0; i < widths.length; i++) cm.getColumn(i).setPreferredWidth(widths[i]);
        cm.getColumn(1).setCellRenderer(new UiKit.ZebraRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                String id = String.valueOf(v);
                if (!id.startsWith("\u2014")) l.setForeground(Theme.driverColor(sim.driverIndex(id)));
                l.setFont(Theme.font(Font.BOLD, 13f));
                return l;
            }
        });
        cm.getColumn(7).setCellRenderer(new UiKit.ZebraRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                String st = String.valueOf(v);
                l.setForeground(st.equals(TripRecord.COMPLETED) ? Theme.GREEN : st.equals(TripRecord.IN_PROGRESS) ? Theme.AMBER : Theme.CYAN);
                l.setFont(Theme.font(Font.BOLD, 12f));
                return l;
            }
        });
        JScrollPane sp = UiKit.scroll(historyTable);
        sp.setColumnHeaderView(historyTable.getTableHeader());
        sp.getColumnHeader().setOpaque(false);
        sp.getViewport().setOpaque(true);
        sp.getViewport().setBackground(Theme.FIELD);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildLogCard() {
        UiKit.Card card = new UiKit.Card(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        card.add(UiKit.label("SIMULATOR ACTIVITY LOG", 11.5f, Font.BOLD, Theme.CYAN), BorderLayout.NORTH);
        logPane.setEditable(false);
        logPane.setBackground(Theme.FIELD);
        logPane.setFont(Theme.mono(Font.PLAIN, 12f));
        logPane.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane sp = UiKit.scroll(logPane);
        sp.getViewport().setOpaque(true);
        sp.getViewport().setBackground(Theme.FIELD);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        card.add(sp, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(440, 200));
        card.setMinimumSize(new Dimension(380, 120));
        return card;
    }

    // ================================================================ actions
    private String selected(JComboBox<String> box) {
        Object o = box.getSelectedItem();
        String s = o == null ? "" : o.toString();
        return s.equals(PLACEHOLDER) ? "" : s;
    }

    private void onSelectionChanged() {
        if (updating) return;
        String p = selected(pickupCombo), d = selected(dropCombo);
        map.setSelection(p, d);
        updatePreview();
    }

    private void updatePreview() {
        String p = selected(pickupCombo), d = selected(dropCombo);
        if (p.isEmpty() && d.isEmpty()) {
            previewLabel.setForeground(Theme.MUTED);
            previewLabel.setText("Pick both locations to preview the route");
        } else if (p.isEmpty() || d.isEmpty()) {
            previewLabel.setForeground(Theme.MUTED);
            previewLabel.setText(p.isEmpty() ? "Now choose a pickup location" : "Now choose a drop location");
        } else if (p.equals(d)) {
            previewLabel.setForeground(Theme.RED);
            previewLabel.setText("Pickup and drop must be different");
        } else {
            int km = sim.distance(p, d);
            previewLabel.setForeground(Theme.CYAN);
            previewLabel.setText("Shortest route: " + join(sim.route(p, d)) + "  (" + km + " km, Rs. " + (km * CabSimulatorManager.FARE_PER_KM) + ")");
        }
    }

    private void onNodeClicked(String node) {
        if (uiLocked) return;
        String p = selected(pickupCombo), d = selected(dropCombo);
        if (p.isEmpty() || !d.isEmpty()) {
            pickupCombo.setSelectedItem(node);
            dropCombo.setSelectedIndex(0);
        } else if (!node.equals(p)) {
            dropCombo.setSelectedItem(node);
        } else {
            map.showToast(2, "Drop must be different from the pickup. Click another location.");
        }
    }

    private void clearSelection() {
        updating = true;
        pickupCombo.setSelectedIndex(0);
        dropCombo.setSelectedIndex(0);
        updating = false;
        map.setSelection("", "");
        updatePreview();
    }

    void doBook() {
        String p = selected(pickupCombo), d = selected(dropCombo);
        SimulationController.Result r = sim.book(p, d);
        logLine(timestamp() + "Book Cab: " + (p.isEmpty() ? "?" : p) + ARROW + (d.isEmpty() ? "?" : d), Theme.CYAN, true);
        logConsole(r.console);
        switch (r.kind) {
            case ASSIGNED:
                focusRider = r.rider.getId();
                map.showToast(1, "Rider " + r.rider.getId() + ": " + r.message);
                clearSelection();
                break;
            case QUEUED:
                focusRider = r.rider.getId();
                map.showToast(2, r.message);
                clearSelection();
                break;
            default:
                map.showToast(3, r.message);
                break;
        }
        map.setFocusRider(focusRider);
        refreshAll();
    }

    void doComplete() {
        int idx = driverCombo.getSelectedIndex();
        if (busyIds.isEmpty() || idx < 0 || idx >= busyIds.size()) {
            map.showToast(2, "No active trip to complete. Book a cab first.");
            return;
        }
        final String id = busyIds.get(idx);
        SimulationController.Result r = sim.complete(id);
        logLine(timestamp() + "Complete Trip: " + id, Theme.GREEN, true);
        logConsole(r.console);
        if (r.kind != SimulationController.Kind.COMPLETED) {
            map.showToast(3, r.message);
            refreshAll();
            return;
        }
        focusRider = r.dispatched.isEmpty() ? r.rider.getId() : r.dispatched.get(0).getRiderId();
        map.setFocusRider(focusRider);
        map.showToast(1, r.message);

        // The state is already final. The car now drives to its (real) new location.
        List<String> path = new ArrayList<>(sim.route(r.fromLocation, r.rider.getPickup()));
        List<String> trip = sim.route(r.rider.getPickup(), r.rider.getDrop());
        if (!path.isEmpty() && trip.size() > 1) path.addAll(trip.subList(1, trip.size()));
        else path.addAll(trip);
        int km = r.trip == null ? 0 : r.trip.getDriverToPickupKm() + r.trip.getTripKm();
        lock(true);
        refreshAll();
        map.animateDrive(id, path, km, () -> { lock(false); });
    }

    void doReset() {
        if (demoRunning) stopDemo();
        sim.reset();
        focusRider = 0;
        map.setFocusRider(0);
        clearSelection();
        logLine(timestamp() + "Simulation reset: D1 at A, D2 at D, queue empty", Theme.AMBER, true);
        map.showToast(0, "Simulation reset. D1 is at A, D2 is at D.");
        refreshAll();
    }

    private void selectDriver(String id) {
        if (uiLocked) return;
        TripRecord t = sim.activeTrip(id);
        if (t != null) {
            focusRider = t.getRiderId();
            map.setFocusRider(focusRider);
            int i = busyIds.indexOf(id);
            if (i >= 0) { updating = true; driverCombo.setSelectedIndex(i); updating = false; }
            refreshAll();
        } else {
            map.showToast(0, "Driver " + id + " is available at " + sim.driver(id).getLocation() + ".");
        }
    }

    private void lock(boolean on) {
        uiLocked = on;
        updateControls();
    }

    private void updateControls() {
        boolean free = !uiLocked && !demoRunning;
        bookBtn.setEnabled(free);
        pickupCombo.setEnabled(free);
        dropCombo.setEnabled(free);
        driverCombo.setEnabled(free && !busyIds.isEmpty());
        completeBtn.setEnabled(free && !busyIds.isEmpty());
        resetBtn.setEnabled(!uiLocked);
        demoBtn.setEnabled(!uiLocked || demoRunning);
    }

    // ================================================================== demo
    private void toggleDemo() {
        if (demoRunning) { stopDemo(); map.showToast(0, "Demo stopped."); return; }
        sim.reset();
        focusRider = 0;
        map.setFocusRider(0);
        clearSelection();
        logLine(timestamp() + "Demo: Test Case 4 (B" + Theme.ARROW + "C, B" + Theme.ARROW + "D, A" + Theme.ARROW
                + "C, then D1 completes)", Theme.AMBER, true);
        refreshAll();
        demoActions = new ArrayList<>();
        demoDelays = new ArrayList<>();
        String[][] bookings = {{"B", "C"}, {"B", "D"}, {"A", "C"}};
        for (String[] b : bookings) {
            final String p = b[0], d = b[1];
            demoActions.add(() -> { pickupCombo.setSelectedItem(p); dropCombo.setSelectedItem(d); });
            demoDelays.add(1300);
            demoActions.add(this::doBook);
            demoDelays.add(2400);
        }
        demoActions.add(() -> { int i = busyIds.indexOf("D1"); if (i >= 0) driverCombo.setSelectedIndex(i); });
        demoDelays.add(1200);
        demoActions.add(this::doComplete);
        demoDelays.add(1500);
        demoActions.add(this::stopDemo);
        demoDelays.add(0);
        demoRunning = true;
        demoIndex = 0;
        demoBtn.setText("Stop Demo");
        updateControls();
        demoTimer = new javax.swing.Timer(900, e -> demoTick());
        demoTimer.setRepeats(false);
        demoTimer.start();
    }

    private void demoTick() {
        if (!demoRunning) return;
        if (map.isAnimating()) { demoTimer.setInitialDelay(250); demoTimer.restart(); return; }
        int i = demoIndex++;
        boolean wasRunning = demoRunning;
        demoActions.get(i).run();
        if (!demoRunning || !wasRunning) return;
        demoTimer.setInitialDelay(Math.max(50, demoDelays.get(i)));
        demoTimer.restart();
    }

    private void stopDemo() {
        demoRunning = false;
        if (demoTimer != null) demoTimer.stop();
        demoBtn.setText("Run Demo");
        updateControls();
        if (demoIndex > 0) map.showToast(1, "Demo finished. Use the controls to continue from this state.");
    }

    // ================================================================ refresh
    void refreshAll() {
        List<TripRecord> hist = sim.history();
        chipFree.set(sim.availableDrivers() + "/" + sim.drivers().size());
        chipWaiting.set(String.valueOf(sim.waiting().size()));
        chipTrips.set(String.valueOf(hist.size()));
        chipRevenue.set("Rs. " + sim.completedRevenue());
        updateBusyCombo();
        updateCards();
        updateQueue();
        updateLiveTrip();
        historyModel.reload();
        updatePreview();
        updateControls();
        map.repaint();
    }

    private void updateBusyCombo() {
        String keep = null;
        int cur = driverCombo.getSelectedIndex();
        if (cur >= 0 && cur < busyIds.size()) keep = busyIds.get(cur);
        TripRecord focused = sim.tripOfRider(focusRider);
        if (focused != null && focused.getStatus().equals(TripRecord.IN_PROGRESS)) keep = focused.getDriverId();
        updating = true;
        busyIds.clear();
        driverCombo.removeAllItems();
        for (Driver d : sim.drivers()) {
            if (!d.isAvailable()) {
                Rider r = d.getCurrentRider();
                busyIds.add(d.getId());
                driverCombo.addItem(d.getId() + "  " + Theme.DOT + "  Rider " + r.getId() + "  " + Theme.DOT + "  "
                        + r.getPickup() + ARROW + r.getDrop());
            }
        }
        if (busyIds.isEmpty()) driverCombo.addItem(NO_ACTIVE);
        else driverCombo.setSelectedIndex(Math.max(0, busyIds.indexOf(keep)));
        updating = false;
    }

    private void updateCards() {
        TripRecord focused = sim.tripOfRider(focusRider);
        for (DriverCard dc : cards) {
            Driver d = sim.driver(dc.id);
            dc.update(d, sim.activeTrip(d.getId()), focused != null && focused.getDriverId().equals(d.getId())
                    && focused.getStatus().equals(TripRecord.IN_PROGRESS));
        }
    }

    private void updateQueue() {
        queueRows.removeAll();
        List<Rider> w = sim.waiting();
        queueCount.set(w.size() + " waiting", w.isEmpty() ? Theme.MUTED : Theme.AMBER);
        if (w.isEmpty()) {
            JLabel l = UiKit.label("No riders waiting. All requests are being served.", 12.5f, Font.PLAIN, Theme.MUTED);
            l.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 0));
            queueRows.add(l);
        } else {
            int pos = 1;
            for (Rider r : w) {
                queueRows.add(new QueueRow(pos, r, pos == 1, r.getId() == focusRider));
                queueRows.add(Box.createVerticalStrut(6));
                pos++;
            }
            JLabel cap = UiKit.label("Front of the queue is served first", 11f, Font.ITALIC, Theme.DIM);
            queueRows.add(cap);
        }
        queueRows.revalidate();
        queueRows.repaint();
    }

    private int effectiveFocus() {
        if (sim.tripOfRider(focusRider) != null || sim.waitingRider(focusRider) != null) return focusRider;
        TripRecord last = null;
        for (TripRecord t : sim.history()) if (t.getStatus().equals(TripRecord.IN_PROGRESS)) last = t;
        if (last != null) return last.getRiderId();
        List<TripRecord> h = sim.history();
        return h.isEmpty() ? 0 : h.get(h.size() - 1).getRiderId();
    }

    private void updateLiveTrip() {
        int id = effectiveFocus();
        TripRecord t = sim.tripOfRider(id);
        Rider w = sim.waitingRider(id);
        String dash = Theme.DASH;
        if (t != null) {
            boolean active = t.getStatus().equals(TripRecord.IN_PROGRESS);
            tripPill.set(active ? "IN PROGRESS" : "COMPLETED", active ? Theme.AMBER : Theme.GREEN);
            tRider.setText("Rider " + t.getRiderId() + "  (" + t.getPickup() + ARROW + t.getDrop() + ")");
            tDriver.setText(t.getDriverId());
            tDriver.setForeground(Theme.driverColor(sim.driverIndex(t.getDriverId())));
            Driver d = sim.driver(t.getDriverId());
            String toPickup = t.getDriverToPickupKm() + " km";
            if (active && d != null) toPickup += "  (" + join(sim.route(d.getLocation(), t.getPickup())) + ")";
            tPickupDist.setText(toPickup);
            tTripDist.setText(t.getTripKm() + " km");
            tRoute.setText(join(sim.route(t.getPickup(), t.getDrop())));
            tFare.setText("Rs. " + t.getFare());
            tFare.setForeground(Theme.GREEN);
            tFareNote.setText(t.getTripKm() + " km " + Theme.TIMES + " Rs. " + CabSimulatorManager.FARE_PER_KM + " per km");
        } else if (w != null) {
            int km = sim.distance(w.getPickup(), w.getDrop());
            tripPill.set("WAITING", Theme.CYAN);
            tRider.setText("Rider " + w.getId() + "  (" + w.getPickup() + ARROW + w.getDrop() + ")");
            tDriver.setText("Not assigned yet");
            tDriver.setForeground(Theme.MUTED);
            tPickupDist.setText(dash);
            tTripDist.setText(km + " km");
            tRoute.setText(join(sim.route(w.getPickup(), w.getDrop())));
            tFare.setText("Rs. " + (km * CabSimulatorManager.FARE_PER_KM));
            tFare.setForeground(Theme.MUTED);
            tFareNote.setText("Estimate. Waiting in queue position " + (sim.waiting().indexOf(w) + 1));
        } else {
            tripPill.set("IDLE", Theme.MUTED);
            for (JLabel l : new JLabel[]{tRider, tDriver, tPickupDist, tTripDist, tRoute}) { l.setText(dash); l.setForeground(Theme.TEXT); }
            tFare.setText("Rs. 0");
            tFare.setForeground(Theme.DIM);
            tFareNote.setText("Book a cab to see live trip details");
        }
    }

    // ================================================================== log
    private String timestamp() {
        return "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] ";
    }

    private void logLine(String text, Color color, boolean bold) {
        StyledDocument doc = logPane.getStyledDocument();
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, color);
        StyleConstants.setBold(a, bold);
        StyleConstants.setFontFamily(a, Theme.MONO);
        StyleConstants.setFontSize(a, 12);
        try {
            doc.insertString(doc.getLength(), text + "\n", a);
        } catch (javax.swing.text.BadLocationException ignored) { }
        logPane.setCaretPosition(doc.getLength());
    }

    private void logConsole(String console) {
        for (String raw : console.split("\\r?\\n")) {
            String s = raw.trim();
            if (s.isEmpty() || s.startsWith("-----") && !s.contains("BOOKING")) continue;
            if (s.matches("-+")) continue;
            Color c = Theme.MUTED;
            String low = s.toLowerCase();
            if (low.contains("assigned") || low.contains("booking confirmed")) c = Theme.GREEN;
            else if (low.contains("no cab") || low.contains("queue") || low.contains("serving")) c = Theme.AMBER;
            else if (low.contains("not found") || s.startsWith("[!]") || low.contains("cannot")) c = Theme.RED;
            else if (low.contains("completed") || low.contains("now at") || low.startsWith("fare")) c = Theme.CYAN;
            logLine("    " + s, c, false);
        }
    }

    // ================================================================ helpers
    static String join(List<String> route) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < route.size(); i++) {
            if (i > 0) sb.append(ARROW);
            sb.append(route.get(i));
        }
        return sb.toString();
    }

    String historyCell(int row, int col) { return String.valueOf(historyModel.getValueAt(row, col)); }
    int historyRows() { return historyModel.getRowCount(); }
    int queueSize() { return sim.waiting().size(); }
    boolean isDemoRunning() { return demoRunning; }
    boolean isLocked() { return uiLocked; }
    String chipText(int i) { return new StatChip[]{chipFree, chipWaiting, chipTrips, chipRevenue}[i].text; }
    String liveFare() { return tFare.getText(); }
    String liveDriver() { return tDriver.getText(); }
    String liveRoute() { return tRoute.getText(); }
    String livePickupDistance() { return tPickupDist.getText(); }
    String liveTripDistance() { return tTripDist.getText(); }
    String livePill() { return tripPill.getText(); }

    static Image makeIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        UiKit.quality(g);
        g.setPaint(new GradientPaint(0, 0, Theme.CYAN, 0, 64, Theme.BLUE));
        g.fill(new Ellipse2D.Float(1, 1, 62, 62));
        UiKit.drawCar(g, 32, 34, Color.WHITE, 1.35, 0);
        g.dispose();
        return img;
    }

    // ================================================================ widgets
    /** Small KPI chip in the header. */
    static class StatChip extends JComponent {
        private final String label;
        private final Color color;
        String text = "0";

        StatChip(String label, Color color) {
            this.label = label;
            this.color = color;
            setPreferredSize(new Dimension(112, 48));
        }

        void set(String t) { text = t; repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            UiKit.quality(g);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D rr = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 14, 14);
            g.setColor(UiKit.alpha(Color.WHITE, 12));
            g.fill(rr);
            g.setColor(Theme.BORDER);
            g.draw(rr);
            g.setColor(color);
            g.fill(new RoundRectangle2D.Float(0, 10, 3, h - 20, 3, 3));
            g.setFont(Theme.font(Font.BOLD, 10f));
            g.setColor(Theme.MUTED);
            g.drawString(label, 14, 18);
            g.setFont(Theme.font(Font.BOLD, 19f));
            g.setColor(Theme.TEXT);
            g.drawString(text, 14, 40);
            g.dispose();
        }
    }

    /** One driver on the Driver Dashboard. */
    class DriverCard extends UiKit.Card {
        final String id;
        private final int index;
        private final UiKit.Pill status = new UiKit.Pill("AVAILABLE", Theme.GREEN);
        private final JLabel loc = value(), rider = value(), dist = value(), dest = value();
        private final UiKit.CarAvatar avatar;
        private boolean selectedCard;

        DriverCard(String id, int index) {
            super(new GridBagLayout(), 16);
            this.id = id;
            this.index = index;
            Color c = Theme.driverColor(index);
            avatar = new UiKit.CarAvatar(c);
            setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0; g.gridy = 0; g.gridheight = 1; g.insets = new Insets(0, 0, 0, 12);
            add(avatar, g);
            JPanel names = new JPanel();
            names.setOpaque(false);
            names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
            names.add(UiKit.label("Driver " + id, 17f, Font.BOLD, Color.WHITE));
            names.add(UiKit.label("Cab #" + (index + 1), 12f, Font.PLAIN, c));
            g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(0, 0, 0, 0);
            add(names, g);
            g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE; g.anchor = GridBagConstraints.EAST;
            add(status, g);

            JPanel grid = new JPanel(new GridLayout(2, 2, 12, 8));
            grid.setOpaque(false);
            grid.add(field("CURRENT LOCATION", loc));
            grid.add(field("ASSIGNED RIDER", rider));
            grid.add(field("PICKUP DISTANCE", dist));
            grid.add(field("DESTINATION", dest));
            g.gridx = 0; g.gridy = 1; g.gridwidth = 3; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
            g.insets = new Insets(12, 0, 0, 0);
            add(grid, g);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { selectDriver(DriverCard.this.id); }
            });
        }

        private JPanel field(String caption, JLabel v) {
            JPanel p = new JPanel();
            p.setOpaque(false);
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            JLabel c = UiKit.label(caption, 10f, Font.BOLD, Theme.MUTED);
            p.add(c);
            p.add(Box.createVerticalStrut(2));
            p.add(v);
            return p;
        }

        String statusText() { return status.getText(); }
        String riderText() { return rider.getText(); }
        String locText() { return loc.getText(); }
        String distText() { return dist.getText(); }

        void update(Driver d, TripRecord t, boolean selected) {
            selectedCard = selected;
            borderColor = selected ? Theme.driverColor(index) : Theme.BORDER;
            loc.setText(d.getLocation());
            if (d.isAvailable() || t == null) {
                status.set("AVAILABLE", Theme.GREEN);
                rider.setText(Theme.DASH);
                dist.setText(Theme.DASH);
                dest.setText(Theme.DASH);
            } else {
                status.set("BUSY", Theme.AMBER);
                rider.setText("Rider " + t.getRiderId());
                dist.setText(t.getDriverToPickupKm() + " km");
                dest.setText(t.getDrop() + "  (from " + t.getPickup() + ")");
            }
            repaint();
        }
    }

    /** One waiting rider in the FIFO queue list. */
    static class QueueRow extends JComponent {
        private final int pos;
        private final Rider rider;
        private final boolean front, focused;
        private final int km;

        QueueRow(int pos, Rider r, boolean front, boolean focused) {
            this.pos = pos;
            this.rider = r;
            this.front = front;
            this.focused = focused;
            this.km = 0;
            setPreferredSize(new Dimension(100, 40));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setAlignmentX(0f);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            UiKit.quality(g);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D rr = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 12, 12);
            g.setColor(Theme.FIELD);
            g.fill(rr);
            g.setColor(focused ? Theme.CYAN : Theme.BORDER);
            g.draw(rr);
            g.setColor(front ? Theme.AMBER : Theme.BORDER_HI);
            g.fill(new Ellipse2D.Float(8, h / 2f - 12, 24, 24));
            g.setFont(Theme.font(Font.BOLD, 13f));
            g.setColor(front ? Theme.DARK_TEXT : Theme.TEXT);
            UiKit.drawCentered(g, String.valueOf(pos), 20, h / 2f);
            g.setFont(Theme.font(Font.BOLD, 13.5f));
            g.setColor(Theme.TEXT);
            g.drawString("Rider " + rider.getId(), 44, h / 2f + 5);
            g.setFont(Theme.font(Font.PLAIN, 13f));
            g.setColor(Theme.CYAN);
            String route = rider.getPickup() + ARROW + rider.getDrop();
            g.drawString(route, 112, h / 2f + 5);
            if (front) {
                g.setFont(Theme.font(Font.BOLD, 10.5f));
                String s = "NEXT";
                int tw = g.getFontMetrics().stringWidth(s) + 16;
                RoundRectangle2D tag = new RoundRectangle2D.Float(w - tw - 10, h / 2f - 10, tw, 20, 20, 20);
                g.setColor(UiKit.alpha(Theme.AMBER, 40));
                g.fill(tag);
                g.setColor(Theme.AMBER);
                g.draw(tag);
                UiKit.drawCentered(g, s, w - 10 - tw / 2f, h / 2f);
            }
            g.dispose();
        }
    }

    /** Booking history = every booked rider (waiting + assigned + completed), newest first. */
    class HistoryModel extends AbstractTableModel {
        private final String[] cols = {"Rider", "Driver", "Request", "Cab to pickup", "Trip", "Route", "Fare", "Status"};
        private List<Object[]> rows = new ArrayList<>();

        void reload() {
            List<Object[]> list = new ArrayList<>();
            for (TripRecord t : sim.history()) {
                list.add(new Object[]{t.getRiderId(), t.getDriverId(), t.getPickup() + ARROW + t.getDrop(),
                        t.getDriverToPickupKm() + " km", t.getTripKm() + " km", join(sim.route(t.getPickup(), t.getDrop())),
                        "Rs. " + t.getFare(), t.getStatus()});
            }
            for (Rider r : sim.waiting()) {
                int km = sim.distance(r.getPickup(), r.getDrop());
                list.add(new Object[]{r.getId(), Theme.DASH, r.getPickup() + ARROW + r.getDrop(), Theme.DASH, km + " km",
                        join(sim.route(r.getPickup(), r.getDrop())), "Rs. " + (km * CabSimulatorManager.FARE_PER_KM), "WAITING"});
            }
            Collections.sort(list, (a, b) -> Integer.compare((Integer) b[0], (Integer) a[0]));
            rows = list;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) { return rows.get(r)[c]; }
    }
}
