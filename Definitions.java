import java.awt.Color;
import java.awt.Graphics;
import static java.lang.Math.*;

enum ToolMode {
    DRAW_POINT,
    SELECT,
    EDIT,
    PAN,
    ROTATE
}

class Point2D {
    int x, y;
    boolean selected = false;

    public Point2D(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics g, int offsetX, int offsetY) {
        if (selected)
            g.setColor(Color.RED);
        else
            g.setColor(Color.BLACK);

        g.fillOval(x + offsetX - 4, y + offsetY - 4, 8, 8);
    }

    public boolean contains(int mx, int my, int offsetX, int offsetY) {
        return hypot(mx - (x + offsetX), my - (y + offsetY)) <= 6;
    }
}
