import javax.swing.*;
import java.awt.*;

/** The application window: a thin JFrame around CabDashboardPanel. */
public class CabDashboardFrame extends JFrame {
    final CabDashboardPanel dashboard = new CabDashboardPanel();

    public CabDashboardFrame() {
        super("Cab Booking Simulator " + Theme.DOT + " PS No. 86");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(CabDashboardPanel.makeIcon());
        setContentPane(dashboard);
        Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(1100, 680));
        setSize(Math.min(1440, usable.width - 30), Math.min(880, usable.height - 20));
        setLocationRelativeTo(null);
        if (scr.width < 1400 || scr.height < 820) setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
}
