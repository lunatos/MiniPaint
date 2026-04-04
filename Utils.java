import java.awt.*;
import java.util.Map;
import javax.swing.*;
import javax.swing.UIManager;

public class Utils {
    public static final BasicStroke NORMAL_STROKE = new BasicStroke(2);
    public static final BasicStroke THIN_STROKE = new BasicStroke(1);
    public static final BasicStroke HIGHLIGHT_STROKE = new BasicStroke(3);
    public static final BasicStroke SELECTED_STROKE = new BasicStroke(3);

    public static final Color SELECTED_COLOR = Color.RED;
    public static final Color HIGHLIGHT_COLOR = Color.BLUE;
    public static final Color NORMAL_COLOR = Color.BLACK;

    /**
     * Updates button backgrounds based on current mode
     */
    public static void updateToolButtons(ToolMode currentMode, Map<ToolMode, JButton> modeButtons) {
        Color highlight = new Color(173, 216, 230);
        Color normal = UIManager.getColor("Button.background");

        for (Map.Entry<ToolMode, JButton> entry : modeButtons.entrySet()) {
            ToolMode mode = entry.getKey();
            JButton btn = entry.getValue();
            boolean isActive = (currentMode == ToolMode.SELECT || currentMode == ToolMode.EDIT) ? 
                (mode == ToolMode.SELECT || mode == ToolMode.EDIT) : currentMode == mode;
            btn.setBackground(isActive ? highlight : normal);
            btn.repaint();
        }
    }

    /**
     * Prepares Graphics2D for drawing shape with his state
     */
    public static void prepareShapeGraphics(Graphics2D g2, boolean selected, boolean highlight) {
        if (selected) {
            g2.setColor(SELECTED_COLOR);
            g2.setStroke(SELECTED_STROKE);
        } else if (highlight) {
            g2.setColor(HIGHLIGHT_COLOR);
            g2.setStroke(HIGHLIGHT_STROKE);
        } else {
            g2.setColor(NORMAL_COLOR);
            g2.setStroke(NORMAL_STROKE);
        }
    }

    /**
     * Resets to normal thin stroke
     */
    public static void resetStroke(Graphics2D g2) {
        g2.setStroke(THIN_STROKE);
    }
}
