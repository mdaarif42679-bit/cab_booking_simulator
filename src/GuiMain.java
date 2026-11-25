import javax.swing.*;
import java.awt.*;

/** Entry point of the graphical Cab Booking Simulator. Run with: java -cp out GuiMain */
public class GuiMain {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Unexpected error: " + e, "Cab Booking Simulator", JOptionPane.ERROR_MESSAGE);
        });
        SwingUtilities.invokeLater(() -> {
            UIManager.put("ToolTip.background", new Color(0x0B1635));
            UIManager.put("ToolTip.foreground", Theme.TEXT);
            UIManager.put("ToolTip.border", BorderFactory.createLineBorder(Theme.BORDER_HI));
            UIManager.put("ToolTip.font", Theme.font(Font.PLAIN, 12f));
            new CabDashboardFrame().setVisible(true);
        });
    }
}
