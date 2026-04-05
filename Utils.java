import java.awt.*;

public class Utils {
    public static final BasicStroke NORMAL_STROKE = new BasicStroke(2);
    public static final BasicStroke SELECTED_STROKE = new BasicStroke(3);

    public static final Color SELECTED_COLOR = Color.RED;
    public static final Color PREVIEW_COLOR = Color.BLUE;
    public static final Color NORMAL_COLOR = Color.BLACK;

    public static void prepareShapeGraphics(Graphics2D g2, boolean selected, boolean highlight) {
        if (selected) {
            g2.setColor(SELECTED_COLOR);
            g2.setStroke(SELECTED_STROKE);
        } else if (highlight) {
            g2.setColor(PREVIEW_COLOR);
            g2.setStroke(SELECTED_STROKE);
        } else {
            g2.setColor(NORMAL_COLOR);
            g2.setStroke(NORMAL_STROKE);
        }
    }
}
