import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Custom-painted city map. It reads the REAL simulator state on every repaint, so the roads (from the Graph),
 * driver positions, active trips and highlights always match the data shown in every other panel.
 */
public class MapPanel extends JPanel {

    /** Set to false (tests) to make trip-completion animations finish instantly. */
    static boolean ANIMATE = true;

    interface Listener {
        void nodeClicked(String node);
        void driverClicked(String driverId);
    }

    private static final Color ROAD_EDGE = new Color(0x0A1330);
    private static final Color ROAD_FILL = new Color(0x1B2B5E);
    private static final Color PICKUP_COLOR = Theme.GREEN;
    private static final Color DROP_COLOR = new Color(0xFB7185);

    private final SimulationController sim;
    private Listener listener;

    private final Map<String, Point2D.Double> frac = new LinkedHashMap<>();
    private final Map<String, Point2D.Double> pos = new HashMap<>();
    private double nodeR = 27;

    private String selPickup = "", selDrop = "";
    private int focusRider = 0;
    private String hoverNode, hoverDriver;
    private final Map<String, Double> heading = new HashMap<>();

    // animation of a driver along a route
    private String animDriver;
    private List<String> animPath;
    private long animStart, animDuration;
    private Runnable animDone;

    private final long t0 = System.currentTimeMillis();
    private final javax.swing.Timer ticker;

    MapPanel(SimulationController sim) {
        this.sim = sim;
        setOpaque(false);
        setBackground(Theme.BG);
        setMinimumSize(new Dimension(420, 320));
        setPreferredSize(new Dimension(560, 420));
        frac.put("A", new Point2D.Double(0.07, 0.66));
        frac.put("B", new Point2D.Double(0.42, 0.10));
        frac.put("C", new Point2D.Double(0.62, 0.90));
        frac.put("D", new Point2D.Double(0.95, 0.48));
        ToolTipManager.sharedInstance().registerComponent(this);

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { updateHover(e.getPoint()); }
            @Override public void mouseExited(MouseEvent e) { hoverNode = null; hoverDriver = null; setCursor(Cursor.getDefaultCursor()); }
            @Override public void mouseClicked(MouseEvent e) {
                updateHover(e.getPoint());
                if (listener == null || animDriver != null) return;
                if (hoverDriver != null) listener.driverClicked(hoverDriver);
                else if (hoverNode != null) listener.nodeClicked(hoverNode);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        ticker = new javax.swing.Timer(33, e -> {
            if (animDriver != null && System.currentTimeMillis() - animStart >= animDuration) finishAnimation();
            if (isShowing()) repaint();
        });
        ticker.start();
    }

    void setListener(Listener l) { listener = l; }

    void setSelection(String pickup, String drop) {
        selPickup = pickup == null ? "" : pickup;
        selDrop = drop == null ? "" : drop;
        repaint();
    }

    void setFocusRider(int riderId) { focusRider = riderId; repaint(); }

    boolean isAnimating() { return animDriver != null; }

    /** Animates a driver's car along a route. The final position always equals the real simulator state. */
    void animateDrive(String driverId, List<String> path, int km, Runnable done) {
        if (!ANIMATE || path.size() < 2) {
            if (done != null) done.run();
            return;
        }
        animDriver = driverId;
        animPath = new ArrayList<>(path);
        animStart = System.currentTimeMillis();
        animDuration = Math.max(1400, Math.min(4200, km * 330L));
        animDone = done;
    }

    private void finishAnimation() {
        Runnable d = animDone;
        animDriver = null;
        animPath = null;
        animDone = null;
        repaint();
        if (d != null) d.run();
    }

    // ------------------------------------------------------------ geometry
    private Rectangle inner() {
        int w = getWidth(), h = getHeight();
        int l = 100, r = 118, t = 125, b = 118;
        if (w < 560) { l = 70; r = 100; }
        if (h < 400) { t = 105; b = 100; }
        return new Rectangle(l, t, Math.max(50, w - l - r), Math.max(50, h - t - b));
    }

    private void layoutNodes() {
        pos.clear();
        Rectangle in = inner();
        List<String> names = new ArrayList<>(sim.graph().getLocations());
        int extra = 0;
        for (String n : names) {
            Point2D.Double f = frac.get(n);
            if (f == null) {
                double ang = 2 * Math.PI * extra++ / Math.max(1, names.size());
                f = new Point2D.Double(0.5 + 0.4 * Math.cos(ang), 0.5 + 0.4 * Math.sin(ang));
            }
            pos.put(n, new Point2D.Double(in.x + f.x * in.width, in.y + f.y * in.height));
        }
        nodeR = Math.max(22, Math.min(30, Math.min(getWidth(), getHeight()) / 17.0));
    }

    private Point2D.Double nodePos(String n) {
        Point2D.Double p = pos.get(n);
        return p == null ? new Point2D.Double(0, 0) : p;
    }

    private Point2D.Double driverSlot(Driver d) {
        Point2D.Double p = nodePos(d.getLocation());
        int idx = sim.driverIndex(d.getId());
        double dx = idx % 2 == 0 ? -(nodeR + 22) : (nodeR + 22);
        double dy = nodeR + 20 + (idx / 2) * 34;
        return new Point2D.Double(p.x + dx, p.y + dy);
    }

    /** Position of the driver's car: animated while driving, otherwise parked at its real location. */
    private Point2D.Double carPosition(Driver d) {
        if (animDriver != null && animDriver.equals(d.getId()) && animPath != null) {
            List<Point2D.Double> pts = new ArrayList<>();
            for (String n : animPath) pts.add(nodePos(n));
            double[] cum = new double[pts.size()];
            for (int i = 1; i < pts.size(); i++) cum[i] = cum[i - 1] + pts.get(i).distance(pts.get(i - 1));
            double total = cum[cum.length - 1];
            double t = Math.min(1, (System.currentTimeMillis() - animStart) / (double) animDuration);
            t = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;       // ease in-out
            double target = t * total;
            for (int i = 1; i < pts.size(); i++) {
                if (target <= cum[i] || i == pts.size() - 1) {
                    Point2D.Double a = pts.get(i - 1), b = pts.get(i);
                    double seg = cum[i] - cum[i - 1];
                    double u = seg == 0 ? 1 : Math.min(1, (target - cum[i - 1]) / seg);
                    heading.put(d.getId(), Math.atan2(b.y - a.y, b.x - a.x));
                    return new Point2D.Double(a.x + (b.x - a.x) * u, a.y + (b.y - a.y) * u);
                }
            }
        }
        return driverSlot(d);
    }

    private void updateHover(Point p) {
        layoutNodes();
        String hn = null, hd = null;
        for (Driver d : sim.drivers()) {
            if (carPosition(d).distance(p) < 28) hd = d.getId();
        }
        for (Map.Entry<String, Point2D.Double> e : pos.entrySet()) {
            if (e.getValue().distance(p) < nodeR + 4) hn = e.getKey();
        }
        hoverDriver = hd;
        hoverNode = hd != null ? null : hn;
        setCursor(hoverNode != null || hoverDriver != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        layoutNodes();
        for (Driver d : sim.drivers()) {
            if (carPosition(d).distance(p) < 28) {
                StringBuilder sb = new StringBuilder("<html><b>Driver " + d.getId() + "</b><br>Location: " + d.getLocation()
                        + "<br>Status: " + (d.isAvailable() ? "Available" : "Busy"));
                TripRecord t = sim.activeTrip(d.getId());
                if (t != null) sb.append("<br>Rider ").append(t.getRiderId()).append(": ").append(t.getPickup())
                        .append(" ").append(Theme.ARROW).append(" ").append(t.getDrop());
                return sb.append("</html>").toString();
            }
        }
        for (Map.Entry<String, Point2D.Double> en : pos.entrySet()) {
            if (en.getValue().distance(p) < nodeR + 4) {
                StringBuilder sb = new StringBuilder("<html><b>Location " + en.getKey() + "</b>");
                for (Road r : sim.graph().getRoads(en.getKey())) {
                    sb.append("<br>").append(r.getTo()).append(": ").append(r.getKm()).append(" km");
                }
                return sb.append("<br><i>Click to choose as pickup / drop</i></html>").toString();
            }
        }
        return null;
    }

    private static String edgeKey(String a, String b) { return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a; }


    // ---------------------------------------------------------------- toast
    private String toastText = "";
    private int toastSeverity;
    private long toastUntil;

    /** Shows a short message on top of the map. severity: 0 info, 1 success, 2 warning, 3 error. */
    void showToast(int severity, String text) {
        toastSeverity = severity;
        toastText = text;
        toastUntil = System.currentTimeMillis() + 6500;
        repaint();
    }

    private void paintToast(Graphics2D g, int w, int h) {
        long left = toastUntil - System.currentTimeMillis();
        if (left <= 0 || toastText.isEmpty()) return;
        float fade = Math.min(1f, left / 700f);
        Color c = toastSeverity == 1 ? Theme.GREEN : toastSeverity == 2 ? Theme.AMBER : toastSeverity == 3 ? Theme.RED : Theme.CYAN;
        g.setFont(Theme.font(Font.PLAIN, 13f));
        FontMetrics fm = g.getFontMetrics();
        int boxW = Math.max(160, Math.min(w - 60, 560));
        int textW = boxW - 62;
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : toastText.split(" ")) {
            if (cur.length() > 0 && fm.stringWidth(cur + " " + word) > textW) { lines.add(cur.toString()); cur.setLength(0); }
            if (cur.length() > 0) cur.append(' ');
            cur.append(word);
        }
        if (cur.length() > 0) lines.add(cur.toString());
        if (lines.size() > 3) lines = lines.subList(0, 3);
        int lh = fm.getHeight();
        int boxH = Math.max(40, lines.size() * lh + 20);
        int x = (w - boxW) / 2, y = 46;
        RoundRectangle2D box = new RoundRectangle2D.Float(x, y, boxW, boxH, 16, 16);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade));
        g.setColor(new Color(0x08102A, false));
        g.setColor(UiKit.alpha(new Color(0x08102A), 240));
        g.fill(box);
        g.setColor(UiKit.alpha(c, 200));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(box);
        UiKit.drawSeverityIcon(g, toastSeverity, x + 28, y + boxH / 2f, 11, c);
        g.setColor(Theme.TEXT);
        int ty = y + (boxH - lines.size() * lh) / 2 + fm.getAscent();
        for (String line : lines) { g.drawString(line, x + 52, ty); ty += lh; }
        g.setComposite(AlphaComposite.SrcOver);
    }

    // -------------------------------------------------------------- painting
    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        UiKit.quality(g);
        layoutNodes();
        int w = getWidth(), h = getHeight();
        double phase = (System.currentTimeMillis() - t0) / 1000.0;
        RoundRectangle2D frame = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 18, 18);
        g.setClip(frame);

        paintBackground(g, w, h);

        // collect edges
        List<String[]> edges = new ArrayList<>();
        for (String a : sim.graph().getLocations()) {
            for (Road r : sim.graph().getRoads(a)) {
                if (a.compareTo(r.getTo()) < 0) edges.add(new String[]{a, r.getTo(), String.valueOf(r.getKm())});
            }
        }

        // roads
        for (String[] e : edges) {
            Point2D.Double a = nodePos(e[0]), b = nodePos(e[1]);
            g.setStroke(new BasicStroke(24f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(ROAD_EDGE);
            g.draw(new Line2D.Double(a, b));
            g.setStroke(new BasicStroke(19f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(ROAD_FILL);
            g.draw(new Line2D.Double(a, b));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{9f, 11f}, 0f));
            g.setColor(UiKit.alpha(Color.WHITE, 55));
            g.draw(new Line2D.Double(a, b));
        }

        // routes (highlights)
        Map<String, Color> edgeColor = new HashMap<>();
        String pk = null, dp = null;           // pickup / drop tags
        boolean selecting = !selPickup.isEmpty() || !selDrop.isEmpty();

        // active trips: driver->pickup (dashed) and passenger trip (solid)
        for (Driver d : sim.drivers()) {
            TripRecord t = sim.activeTrip(d.getId());
            if (t == null) continue;
            boolean focused = focusRider == 0 || focusRider == t.getRiderId();
            Color c = Theme.driverColor(sim.driverIndex(d.getId()));
            int alphaV = focused ? 255 : 95;
            List<String> trip = sim.route(t.getPickup(), t.getDrop());
            List<String> toPickup = sim.route(d.getLocation(), t.getPickup());
            drawRoute(g, trip, UiKit.alpha(c, alphaV), focused ? 8f : 5f, false, 0, focused);
            drawRoute(g, toPickup, UiKit.alpha(c, (int) (alphaV * 0.85)), focused ? 5f : 3.5f, true, phase, false);
            markEdges(edgeColor, trip, c);
            markEdges(edgeColor, toPickup, c);
            if (!selecting && focusRider == t.getRiderId()) { pk = t.getPickup(); dp = t.getDrop(); }
        }
        // focused waiting rider
        Rider wr = sim.waitingRider(focusRider);
        if (wr != null && !selecting) {
            List<String> trip = sim.route(wr.getPickup(), wr.getDrop());
            drawRoute(g, trip, UiKit.alpha(Theme.MUTED, 200), 5f, true, phase, false);
            markEdges(edgeColor, trip, Theme.MUTED);
            pk = wr.getPickup(); dp = wr.getDrop();
        }
        // route preview while choosing pickup and drop
        if (selecting) {
            if (!selPickup.isEmpty() && !selDrop.isEmpty() && !selPickup.equals(selDrop)
                    && sim.graph().hasLocation(selPickup) && sim.graph().hasLocation(selDrop)) {
                List<String> prev = sim.route(selPickup, selDrop);
                drawRoute(g, prev, UiKit.alpha(Theme.CYAN, 230), 6f, true, phase * 1.4, true);
                markEdges(edgeColor, prev, Theme.CYAN);
            }
            pk = selPickup.isEmpty() ? null : selPickup;
            dp = selDrop.isEmpty() ? null : selDrop;
        }

        // distance labels
        g.setFont(Theme.font(Font.BOLD, 12.5f));
        for (String[] e : edges) {
            Point2D.Double a = nodePos(e[0]), b = nodePos(e[1]);
            String text = e[2] + " km";
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text) + 20, th = 24;
            double mx = (a.x + b.x) / 2, my = (a.y + b.y) / 2;
            Color ec = edgeColor.get(edgeKey(e[0], e[1]));
            RoundRectangle2D pill = new RoundRectangle2D.Double(mx - tw / 2.0, my - th / 2.0, tw, th, th, th);
            g.setColor(new Color(0x08102A));
            g.fill(pill);
            g.setColor(ec != null ? ec : UiKit.alpha(Theme.CYAN, 190));
            g.setStroke(new BasicStroke(ec != null ? 2f : 1.3f));
            g.draw(pill);
            g.setColor(ec != null ? ec : Theme.CYAN);
            UiKit.drawCentered(g, text, (float) mx, (float) my);
        }

        // nodes
        for (String n : pos.keySet()) paintNode(g, n, n.equals(pk), n.equals(dp), phase);

        // drivers
        for (Driver d : sim.drivers()) paintDriver(g, d, phase);

        paintOverlay(g, w, h, phase);
        paintToast(g, w, h);
        g.setClip(null);
        g.setColor(Theme.BORDER);
        g.setStroke(new BasicStroke(1f));
        g.draw(frame);
        g.dispose();
    }

    private void paintBackground(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x0E1C42), 0, h, new Color(0x080F27)));
        g.fillRect(0, 0, w, h);
        // soft glow
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w * 0.5f, h * 0.45f), Math.max(w, h) * 0.6f,
                new float[]{0f, 1f}, new Color[]{new Color(40, 90, 190, 55), new Color(40, 90, 190, 0)}));
        g.fillRect(0, 0, w, h);
        // grid
        g.setColor(new Color(120, 150, 255, 14));
        g.setStroke(new BasicStroke(1f));
        for (int x = 0; x < w; x += 40) g.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 40) g.drawLine(0, y, w, y);
        // decorative city blocks (deterministic)
        Random rnd = new Random(86);
        for (int i = 0; i < 26; i++) {
            int bw = 30 + rnd.nextInt(70), bh = 24 + rnd.nextInt(50);
            int bx = rnd.nextInt(Math.max(1, w - bw)), by = rnd.nextInt(Math.max(1, h - bh));
            g.setColor(new Color(90, 130, 255, 9 + rnd.nextInt(8)));
            g.fill(new RoundRectangle2D.Float(bx, by, bw, bh, 10, 10));
        }
    }

    private void drawRoute(Graphics2D g, List<String> route, Color c, float width, boolean dashed, double phase, boolean glow) {
        if (route.size() < 2) return;
        Path2D p = new Path2D.Double();
        for (int i = 0; i < route.size(); i++) {
            Point2D.Double q = nodePos(route.get(i));
            if (i == 0) p.moveTo(q.x, q.y); else p.lineTo(q.x, q.y);
        }
        if (glow) {
            g.setStroke(new BasicStroke(width + 11, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(UiKit.alpha(c, 38));
            g.draw(p);
        }
        if (dashed) {
            float[] dash = {11f, 9f};
            float offset = (float) ((-phase * 22) % 20.0);      // marching-ants effect; dash phase must be >= 0
            if (offset < 0) offset += 20f;
            g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, offset));
        } else {
            g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        g.setColor(c);
        g.draw(p);
    }

    private void markEdges(Map<String, Color> m, List<String> route, Color c) {
        for (int i = 1; i < route.size(); i++) m.put(edgeKey(route.get(i - 1), route.get(i)), c);
    }

    private void paintNode(Graphics2D g, String n, boolean pickup, boolean drop, double phase) {
        Point2D.Double p = nodePos(n);
        boolean hov = n.equals(hoverNode);
        double r = nodeR + (hov ? 2 : 0);
        // glow
        g.setPaint(new RadialGradientPaint(p, (float) (r * 2.1), new float[]{0f, 1f},
                new Color[]{UiKit.alpha(Theme.CYAN, hov ? 95 : 60), UiKit.alpha(Theme.CYAN, 0)}));
        g.fill(new Ellipse2D.Double(p.x - r * 2.1, p.y - r * 2.1, r * 4.2, r * 4.2));
        // markers rings
        if (pickup || drop) {
            Color c = pickup ? PICKUP_COLOR : DROP_COLOR;
            double pulse = (phase * 1.3) % 1.0;
            double rr = r + 7 + pulse * 14;
            g.setStroke(new BasicStroke(2.5f));
            g.setColor(UiKit.alpha(c, (int) (200 * (1 - pulse))));
            g.draw(new Ellipse2D.Double(p.x - rr, p.y - rr, rr * 2, rr * 2));
            g.setStroke(new BasicStroke(3.2f));
            g.setColor(c);
            g.draw(new Ellipse2D.Double(p.x - r - 6, p.y - r - 6, (r + 6) * 2, (r + 6) * 2));
        }
        // body
        g.setPaint(new GradientPaint((float) p.x, (float) (p.y - r), new Color(0x67E8F9), (float) p.x, (float) (p.y + r), new Color(0x06B6D4)));
        g.fill(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
        g.setColor(Theme.DARK_TEXT);
        g.setFont(Theme.font(Font.BOLD, (float) (r * 0.78)));
        UiKit.drawCentered(g, n, (float) p.x, (float) p.y);
        // tag
        if (pickup || drop) {
            String text = pickup ? "PICKUP" : "DROP";
            Color c = pickup ? PICKUP_COLOR : DROP_COLOR;
            g.setFont(Theme.font(Font.BOLD, 11f));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text) + 18, th = 20;
            double tx = p.x - tw / 2.0, ty = p.y - r - 14 - th;
            RoundRectangle2D tag = new RoundRectangle2D.Double(tx, ty, tw, th, th, th);
            g.setColor(c);
            g.fill(tag);
            Path2D tri = new Path2D.Double();
            tri.moveTo(p.x - 5, ty + th - 0.5); tri.lineTo(p.x + 5, ty + th - 0.5); tri.lineTo(p.x, ty + th + 6);
            tri.closePath();
            g.fill(tri);
            g.setColor(Theme.DARK_TEXT);
            UiKit.drawCentered(g, text, (float) p.x, (float) (ty + th / 2.0));
        }
    }

    private void paintDriver(Graphics2D g, Driver d, double phase) {
        Point2D.Double p = carPosition(d);
        Color c = Theme.driverColor(sim.driverIndex(d.getId()));
        boolean driving = d.getId().equals(animDriver);
        boolean hov = d.getId().equals(hoverDriver);
        double hd = heading.containsKey(d.getId()) ? heading.get(d.getId()) : 0.0;
        if (!driving) hd = 0;
        // halo
        double r = 25 + (hov ? 3 : 0);
        g.setColor(UiKit.alpha(new Color(0x08102A), 215));
        g.fill(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
        g.setStroke(new BasicStroke(2.2f));
        g.setColor(UiKit.alpha(c, d.isAvailable() ? 255 : 210));
        g.draw(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
        if (!d.isAvailable() || driving) {
            double pulse = (phase * 1.1) % 1.0;
            double rr = r + pulse * 10;
            g.setStroke(new BasicStroke(1.8f));
            g.setColor(UiKit.alpha(c, (int) (150 * (1 - pulse))));
            g.draw(new Ellipse2D.Double(p.x - rr, p.y - rr, rr * 2, rr * 2));
        }
        UiKit.drawCar(g, p.x, p.y + 1, c, 0.92, hd);
        // tag
        String text = d.getId() + "  " + (d.isAvailable() ? "FREE" : "BUSY");
        g.setFont(Theme.font(Font.BOLD, 11.5f));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text) + 28, th = 22;
        double tx = p.x - tw / 2.0, ty = p.y + r + 5;
        RoundRectangle2D tag = new RoundRectangle2D.Double(tx, ty, tw, th, th, th);
        g.setColor(new Color(0x08102A));
        g.fill(tag);
        g.setColor(UiKit.alpha(c, 220));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(tag);
        g.setColor(d.isAvailable() ? Theme.GREEN : Theme.AMBER);
        g.fill(new Ellipse2D.Double(tx + 9, ty + th / 2.0 - 3.5, 7, 7));
        g.setColor(Theme.TEXT);
        g.drawString(text, (float) (tx + 21), (float) (ty + th / 2.0 + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    private void paintOverlay(Graphics2D g, int w, int h, double phase) {
        // title + live dot
        g.setFont(Theme.font(Font.BOLD, 12f));
        g.setColor(Theme.CYAN);
        g.drawString("LIVE CITY MAP", 22, 30);
        int tw = g.getFontMetrics().stringWidth("LIVE CITY MAP");
        double pulse = 0.5 + 0.5 * Math.sin(phase * 3);
        g.setColor(UiKit.alpha(Theme.GREEN, (int) (110 + 145 * pulse)));
        g.fill(new Ellipse2D.Double(22 + tw + 12, 22, 8, 8));
        g.setFont(Theme.font(Font.PLAIN, 12f));
        g.setColor(Theme.MUTED);
        int roads = 0;
        for (String a : sim.graph().getLocations()) roads += sim.graph().getRoads(a).size();
        String info = sim.graph().getLocations().size() + " locations " + Theme.DOT + " " + (roads / 2) + " roads "
                + Theme.DOT + " " + sim.drivers().size() + " drivers";
        g.drawString(info, w - 22 - g.getFontMetrics().stringWidth(info), 30);

        // legend
        int lx = 22, ly = h - 28;
        g.setFont(Theme.font(Font.PLAIN, 11.5f));
        FontMetrics fm = g.getFontMetrics();
        String[] labels = {"Passenger trip", "Driver to pickup", "Pickup", "Drop"};
        int boxW = 0;
        for (String s : labels) boxW += fm.stringWidth(s) + 52;
        boxW += 8;
        if (w > boxW + 280) {
            g.setColor(UiKit.alpha(new Color(0x08102A), 190));
            g.fill(new RoundRectangle2D.Float(lx - 10, ly - 17, boxW, 30, 14, 14));
            int x = lx;
            for (int i = 0; i < labels.length; i++) {
                if (i == 0) { g.setColor(Theme.AMBER); g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(new Line2D.Double(x, ly - 2, x + 22, ly - 2)); }
                if (i == 1) { g.setColor(Theme.AMBER); g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 4f}, 0f)); g.draw(new Line2D.Double(x, ly - 2, x + 22, ly - 2)); }
                if (i == 2) { g.setColor(PICKUP_COLOR); g.fill(new Ellipse2D.Double(x + 6, ly - 8, 10, 10)); }
                if (i == 3) { g.setColor(DROP_COLOR); g.fill(new Ellipse2D.Double(x + 6, ly - 8, 10, 10)); }
                g.setColor(Theme.MUTED);
                g.drawString(labels[i], x + 30, ly + 2);
                x += fm.stringWidth(labels[i]) + 52;
            }
        }
        String hint = "Click a location to set pickup, then drop";
        g.setColor(Theme.DIM);
        g.setFont(Theme.font(Font.ITALIC, 11.5f));
        g.drawString(hint, w - 22 - g.getFontMetrics().stringWidth(hint), h - 18);
    }
}
