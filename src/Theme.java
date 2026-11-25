import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Colours and fonts of the dark dashboard theme (matches the project presentation palette). */
final class Theme {
    private Theme() { }

    static final Color BG        = new Color(0x08102A);
    static final Color BG_TOP    = new Color(0x0C1838);
    static final Color CARD      = new Color(0x11204A);
    static final Color CARD_HI   = new Color(0x172A5C);
    static final Color FIELD     = new Color(0x0B1635);
    static final Color BORDER    = new Color(0x26397A);
    static final Color BORDER_HI = new Color(0x3A55A8);

    static final Color CYAN   = new Color(0x22D3EE);
    static final Color BLUE   = new Color(0x3B82F6);
    static final Color AMBER  = new Color(0xFBBF24);
    static final Color GREEN  = new Color(0x34D399);
    static final Color RED    = new Color(0xF87171);
    static final Color VIOLET = new Color(0xA78BFA);

    static final Color TEXT   = new Color(0xF1F5F9);
    static final Color MUTED  = new Color(0x94A3B8);
    static final Color DIM    = new Color(0x64748B);
    static final Color DARK_TEXT = new Color(0x08102A);

    static final String ARROW = "\u2192";
    static final String DOT   = "\u00B7";
    static final String TIMES = "\u00D7";
    static final String DASH  = "\u2014";

    static final Color[] DRIVER_COLORS = { AMBER, VIOLET, new Color(0x38BDF8), new Color(0xFB923C) };

    static Color driverColor(int index) {
        return DRIVER_COLORS[index % DRIVER_COLORS.length];
    }

    private static final Set<String> FAMILIES = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

    private static String pick(String... names) {
        for (String n : names) {
            if (FAMILIES.contains(n)) return n;
        }
        return names[names.length - 1];
    }

    static final String FAMILY = pick("Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Roboto",
            "Ubuntu", "DejaVu Sans", "SansSerif");
    static final String MONO = pick("Consolas", "JetBrains Mono", "Cascadia Mono", "DejaVu Sans Mono", "Monospaced");

    static Font font(int style, float size) {
        return new Font(FAMILY, style, 1).deriveFont(style, size);
    }

    static Font mono(int style, float size) {
        return new Font(MONO, style, 1).deriveFont(style, size);
    }
}
