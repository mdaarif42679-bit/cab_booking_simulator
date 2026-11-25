import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;

/** Reusable custom-painted Swing components for the dark dashboard. */
final class UiKit {
    private UiKit() { }

    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color mix(Color a, Color b, float t) {
        float u = 1f - t;
        return new Color(Math.round(a.getRed() * u + b.getRed() * t),
                Math.round(a.getGreen() * u + b.getGreen() * t),
                Math.round(a.getBlue() * u + b.getBlue() * t));
    }

    static JLabel label(String text, float size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(style, size));
        l.setForeground(color);
        return l;
    }

    static void drawCentered(Graphics2D g, String s, float cx, float cy) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s) / 2f, cy + (fm.getAscent() - fm.getDescent()) / 2f);
    }


    /** Round icon: 0 info (i), 1 success (check), 2 warning (clock), 3 error (cross). */
    static void drawSeverityIcon(Graphics2D g0, int sev, double cx, double cy, double r, Color c) {
        Graphics2D g = (Graphics2D) g0.create();
        quality(g);
        g.setColor(alpha(c, 45));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(c);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double k = r / 11.0;
        if (sev == 1) {
            Path2D p = new Path2D.Double();
            p.moveTo(cx - 4.5 * k, cy + 0.5 * k); p.lineTo(cx - 1.2 * k, cy + 3.8 * k); p.lineTo(cx + 5 * k, cy - 3.5 * k);
            g.draw(p);
        } else if (sev == 2) {
            g.draw(new Line2D.Double(cx, cy - 5 * k, cx, cy));
            g.draw(new Line2D.Double(cx, cy, cx + 3.5 * k, cy + 2.5 * k));
        } else if (sev == 3) {
            g.draw(new Line2D.Double(cx - 3.6 * k, cy - 3.6 * k, cx + 3.6 * k, cy + 3.6 * k));
            g.draw(new Line2D.Double(cx - 3.6 * k, cy + 3.6 * k, cx + 3.6 * k, cy - 3.6 * k));
        } else {
            g.draw(new Line2D.Double(cx, cy - 0.5 * k, cx, cy + 5 * k));
            g.fill(new Ellipse2D.Double(cx - 1.3 * k, cy - 5.4 * k, 2.6 * k, 2.6 * k));
        }
        g.dispose();
    }

    // ------------------------------------------------------------------ Card
    /** Rounded panel with a subtle gradient and border. */
    static class Card extends JPanel {
        private final int arc;
        Color borderColor = Theme.BORDER;

        Card(LayoutManager lm, int arc) {
            super(lm);
            this.arc = arc;
            setOpaque(false);
        }

        Card(LayoutManager lm) { this(lm, 18); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            quality(g);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D rr = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, arc, arc);
            g.setPaint(new GradientPaint(0, 0, Theme.CARD_HI, 0, h, Theme.CARD));
            g.fill(rr);
            g.setColor(borderColor);
            g.setStroke(new BasicStroke(1f));
            g.draw(rr);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    // ---------------------------------------------------------------- Button
    static class RoundButton extends JButton {
        enum Style { PRIMARY, SUCCESS, DANGER, GHOST }

        private final Style style;
        private boolean hover, press;

        RoundButton(String text, Style style) {
            super(text);
            this.style = style;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            setFont(Theme.font(Font.BOLD, 14f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; press = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { press = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) { press = false; repaint(); }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText()) + 36, 42);
        }

        @Override
        public Dimension getMinimumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            quality(g);
            int w = getWidth(), h = getHeight();
            boolean en = isEnabled();
            Color top, bot, fg, border = null;
            switch (style) {
                case PRIMARY: top = Theme.CYAN; bot = Theme.BLUE; fg = Theme.DARK_TEXT; break;
                case SUCCESS: top = new Color(0x4ADE9F); bot = new Color(0x10B981); fg = Theme.DARK_TEXT; break;
                case DANGER:  top = alpha(Theme.RED, 22); bot = alpha(Theme.RED, 22); fg = Theme.RED; border = Theme.RED; break;
                default:      top = alpha(Color.WHITE, 14); bot = alpha(Color.WHITE, 8); fg = Theme.TEXT; border = Theme.BORDER_HI; break;
            }
            if (!en) {
                top = mix(Theme.CARD, top, 0.25f);
                bot = mix(Theme.CARD, bot, 0.25f);
                fg = Theme.DIM;
                if (border != null) border = alpha(border, 70);
            } else if (press) {
                top = mix(top, Color.BLACK, 0.18f); bot = mix(bot, Color.BLACK, 0.18f);
            } else if (hover) {
                top = mix(top, Color.WHITE, 0.14f); bot = mix(bot, Color.WHITE, 0.14f);
                if (border != null) border = mix(border, Color.WHITE, 0.3f);
            }
            RoundRectangle2D rr = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 14, 14);
            if (en && (style == Style.PRIMARY || style == Style.SUCCESS)) {
                g.setColor(alpha(bot, hover ? 90 : 55));
                g.fill(new RoundRectangle2D.Float(2, 5, w - 4, h - 3, 14, 14));
            }
            g.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
            g.fill(rr);
            if (border != null) {
                g.setColor(border);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(rr);
            }
            g.setFont(getFont());
            g.setColor(fg);
            drawCentered(g, getText(), w / 2f, h / 2f + (press ? 1 : 0));
            g.dispose();
        }
    }

    // ----------------------------------------------------------------- Pill
    /** Small rounded status tag, e.g. AVAILABLE / BUSY. */
    static class Pill extends JComponent {
        private String text = "";
        private Color color = Theme.GREEN;

        Pill(String text, Color color) {
            set(text, color);
            setFont(Theme.font(Font.BOLD, 11f));
        }

        String getText() { return text; }

        void set(String text, Color color) {
            this.text = text;
            this.color = color;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + 30, 22);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            quality(g);
            int w = getWidth(), h = getHeight();
            g.setColor(alpha(color, 36));
            g.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
            g.setColor(alpha(color, 120));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h));
            g.setColor(color);
            g.fill(new Ellipse2D.Float(9, h / 2f - 3, 6, 6));
            g.setFont(getFont());
            g.drawString(text, 20, h / 2f + (g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2f);
            g.dispose();
        }
    }

    // ----------------------------------------------------------------- Combo
    static class StyledCombo extends JComboBox<String> {
        StyledCombo(String[] items) {
            super(items);
            setUI(new DarkComboUI());
            setOpaque(false);
            setFont(Theme.font(Font.PLAIN, 14f));
            setForeground(Theme.TEXT);
            setBackground(Theme.CARD_HI);          // used by the popup list
            setBorder(BorderFactory.createEmptyBorder());
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumRowCount(8);
            setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean isSelected, boolean focus) {
                    JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, focus);
                    l.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
                    l.setFont(Theme.font(Font.PLAIN, 14f));
                    String s = String.valueOf(value);
                    boolean placeholder = s.startsWith("Select") || s.startsWith("No ");
                    if (index < 0) {
                        l.setOpaque(false);
                        l.setForeground(placeholder ? Theme.MUTED : Theme.TEXT);
                    } else {
                        l.setOpaque(true);
                        l.setBackground(isSelected ? Theme.BLUE : Theme.CARD_HI);
                        l.setForeground(isSelected ? Color.WHITE : (placeholder ? Theme.MUTED : Theme.TEXT));
                    }
                    return l;
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(110, 40);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            quality(g);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D rr = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 12, 12);
            g.setColor(isEnabled() ? Theme.FIELD : mix(Theme.FIELD, Theme.CARD, 0.5f));
            g.fill(rr);
            g.setColor(hasFocus() || isPopupVisible() ? Theme.CYAN : Theme.BORDER);
            g.setStroke(new BasicStroke(1.2f));
            g.draw(rr);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    static class DarkComboUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    quality(g);
                    float cx = getWidth() / 2f, cy = getHeight() / 2f;
                    Path2D p = new Path2D.Float();
                    p.moveTo(cx - 5, cy - 2.5f);
                    p.lineTo(cx, cy + 2.5f);
                    p.lineTo(cx + 5, cy - 2.5f);
                    g.setColor(comboBox.isEnabled() ? Theme.CYAN : Theme.DIM);
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(p);
                    g.dispose();
                }
            };
            b.setBorder(BorderFactory.createEmptyBorder());
            b.setContentAreaFilled(false);
            b.setOpaque(false);
            b.setFocusable(false);
            return b;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) { }

        @Override
        protected ComboPopup createPopup() {
            BasicComboPopup popup = new BasicComboPopup(comboBox) {
                @Override
                protected JScrollPane createScroller() {
                    JScrollPane sp = super.createScroller();
                    sp.getVerticalScrollBar().setUI(new SlimScrollBarUI());
                    sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 8));
                    return sp;
                }
            };
            popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER_HI));
            return popup;
        }
    }

    // ------------------------------------------------------------- Scrollbar
    static class SlimScrollBarUI extends BasicScrollBarUI {
        private JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            scrollbar.setOpaque(false);
        }

        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }

        @Override
        protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g = (Graphics2D) g0.create();
            quality(g);
            g.setColor(alpha(Theme.MUTED, isThumbRollover() || isDragging ? 150 : 85));
            boolean vertical = scrollbar.getOrientation() == Adjustable.VERTICAL;
            if (vertical) g.fill(new RoundRectangle2D.Float(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6));
            else g.fill(new RoundRectangle2D.Float(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6));
            g.dispose();
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return new Dimension(10, 10);
        }
    }

    static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new SlimScrollBarUI());
        sp.getVerticalScrollBar().setUnitIncrement(18);
        return sp;
    }

    /** A panel that always matches the width of its scroll-pane viewport. */
    static class ScrollColumn extends JPanel implements Scrollable {
        ScrollColumn(LayoutManager lm) { super(lm); setOpaque(false); }

        /** Width always follows the viewport; only the height is content-driven (prevents GridBag min-size mode). */
        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(1, d.height);
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 80; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ----------------------------------------------------------------- Table
    static void styleTable(JTable t) {
        t.setBackground(Theme.FIELD);
        t.setForeground(Theme.TEXT);
        t.setFont(Theme.font(Font.PLAIN, 13f));
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(alpha(Theme.BLUE, 90));
        t.setSelectionForeground(Color.WHITE);
        t.setFillsViewportHeight(true);
        t.setFocusable(false);
        t.setRowSelectionAllowed(true);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(100, 32));
        h.setDefaultRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = new JLabel(String.valueOf(v).toUpperCase(), SwingConstants.CENTER);
                l.setOpaque(true);
                l.setBackground(Theme.CARD_HI);
                l.setForeground(Theme.CYAN);
                l.setFont(Theme.font(Font.BOLD, 11f));
                l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
                return l;
            }
        });
    }

    static class ZebraRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, r, c);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            l.setForeground(Theme.TEXT);
            l.setBackground(sel ? t.getSelectionBackground() : (r % 2 == 0 ? Theme.FIELD : mix(Theme.FIELD, Theme.CARD, 0.45f)));
            return l;
        }
    }

    // ------------------------------------------------------------------ Car
    /** Draws a small taxi centred on (cx, cy), facing right when heading = 0 (radians). */
    static void drawCar(Graphics2D g0, double cx, double cy, Color body, double scale, double heading) {
        Graphics2D g = (Graphics2D) g0.create();
        quality(g);
        g.translate(cx, cy);
        double a = heading;
        boolean flip = false;
        if (Math.abs(a) > Math.PI / 2) { flip = true; a += Math.PI; }
        g.rotate(a);
        if (flip) g.scale(-1, 1);
        g.scale(scale, scale);
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(-17, 5.5, 34, 7));
        g.setColor(body);
        g.fill(new RoundRectangle2D.Double(-18, -6, 36, 14, 8, 8));
        Path2D cabin = new Path2D.Double();
        cabin.moveTo(-10, -5); cabin.lineTo(-6, -12.5); cabin.lineTo(7, -12.5); cabin.lineTo(12, -5); cabin.closePath();
        g.setColor(mix(body, Color.WHITE, 0.12f));
        g.fill(cabin);
        g.setColor(new Color(0x0B1635));
        Path2D w1 = new Path2D.Double();
        w1.moveTo(-7.5, -5.5); w1.lineTo(-4.5, -10.5); w1.lineTo(-0.8, -10.5); w1.lineTo(-0.8, -5.5); w1.closePath();
        Path2D w2 = new Path2D.Double();
        w2.moveTo(0.8, -5.5); w2.lineTo(0.8, -10.5); w2.lineTo(5.8, -10.5); w2.lineTo(9.5, -5.5); w2.closePath();
        g.fill(w1);
        g.fill(w2);
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(-2.5, -15.5, 8, 3.2, 2, 2));
        g.setColor(new Color(0xFFF3B0));
        g.fill(new RoundRectangle2D.Double(15, -2.5, 3.2, 4, 2, 2));
        g.setColor(mix(body, Color.BLACK, 0.4f));
        g.fill(new Rectangle2D.Double(-18, 1.5, 36, 1.2));
        g.setColor(new Color(0x050B1E));
        g.fill(new Ellipse2D.Double(-14, 3.5, 8.5, 8.5));
        g.fill(new Ellipse2D.Double(6, 3.5, 8.5, 8.5));
        g.setColor(new Color(0xCBD5E1));
        g.fill(new Ellipse2D.Double(-11.2, 6.3, 3, 3));
        g.fill(new Ellipse2D.Double(8.8, 6.3, 3, 3));
        g.dispose();
    }

    /** Round avatar with a taxi on it (used on the driver cards). */
    static class CarAvatar extends JComponent {
        private Color color;
        CarAvatar(Color c) { color = c; setPreferredSize(new Dimension(54, 54)); }
        void setColor(Color c) { color = c; repaint(); }
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            quality(g);
            int w = getWidth(), h = getHeight();
            g.setPaint(new GradientPaint(0, 0, alpha(color, 70), 0, h, alpha(color, 20)));
            g.fill(new Ellipse2D.Float(1, 1, w - 2, h - 2));
            g.setColor(alpha(color, 170));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new Ellipse2D.Float(1, 1, w - 2, h - 2));
            drawCar(g, w / 2.0, h / 2.0 + 1, color, 0.95, 0);
            g.dispose();
        }
    }
}
