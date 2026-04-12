import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.cos;
import static java.lang.Math.round;
import static java.lang.Math.sin;

enum ToolMode {
    DRAW_POINT,
    DRAW_LINE,
    DRAW_CIRCLE,
    DRAW_POLYGON,
    SELECT,
    EDIT,
    PAN,
    ROTATE,
    SCALE
}

abstract class Shape {
    boolean selected = false;

abstract void draw(Graphics g, int offsetX, int offsetY, boolean highlight, PaintApp app, int vLeft, int vTop, int vRight, int vBottom);

    abstract boolean isFullyInside(Rectangle rect, int offsetX, int offsetY);

    abstract void translate(int dx, int dy);

    abstract void rotate(Point center, double angle);

    abstract void scale(Point center, double scaleX, double scaleY);

    public abstract Shape clone();

    boolean isPolygon() {
        return false;
    }

    boolean isPoint() {
        return false;
    }

    static Point rotatePoint(int px, int py, Point center, double angle) {
        double dx = px - center.x;
        double dy = py - center.y;
        return new Point(
                (int) round(center.x + dx * cos(angle) - dy * sin(angle)),
                (int) round(center.y + dx * sin(angle) + dy * cos(angle)));
    }
}

class Point2D extends Shape {
    int x, y;

    public Point2D(int x, int y) {
        this.x = x;
        this.y = y;
    }

    private Point2D(int x, int y, boolean selected) {
        this(x, y);
        this.selected = selected;
    }

@Override
    public void draw(Graphics g, int offsetX, int offsetY, boolean highlight, PaintApp app, int vLeft, int vTop, int vRight, int vBottom) {
        Graphics2D g2 = (Graphics2D) g;
        Utils.prepareShapeGraphics(g2, selected, highlight);
        g2.fillOval(x + offsetX - 4, y + offsetY - 4, 6, 6);
    }


    @Override
    public boolean isFullyInside(Rectangle rect, int offsetX, int offsetY) {
        return rect.contains(x + offsetX, y + offsetY);
    }

    @Override
    public void translate(int dx, int dy) {
        x += dx;
        y += dy;
    }

    @Override
    public void rotate(Point center, double angle) {
        Point rotated = Shape.rotatePoint(x, y, center, angle);
        x = rotated.x;
        y = rotated.y;
    }

    @Override
    public void scale(Point center, double scaleX, double scaleY) {
        x = center.x + (int) ((x - center.x) * scaleX);
        y = center.y + (int) ((y - center.y) * scaleY);
    }

    @Override
    public Point2D clone() {
        return new Point2D(x, y, selected);
    }

    boolean contains(int mx, int my, int offsetX, int offsetY) {
        return java.lang.Math.hypot(mx - (x + offsetX), my - (y + offsetY)) <= 6;
    }

    @Override
    boolean isPoint() {
        return true;
    }
}

class Line2D extends Shape {
    int x1, y1, x2, y2;

    public Line2D(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    Line2D(int x1, int y1, int x2, int y2, boolean selected) {
        this(x1, y1, x2, y2);
        this.selected = selected;
    }

@Override
    public void draw(Graphics g, int offsetX, int offsetY, boolean highlight, PaintApp app, int vLeft, int vTop, int vRight, int vBottom) {
        Graphics2D g2 = (Graphics2D) g;
        Utils.prepareShapeGraphics(g2, selected, highlight);
        boolean useDDA = (app != null && app.isUseDDAMode());
        double[] p1 = {x1, y1};
        double[] p2 = {x2, y2};
        if (cohenSutherlandClip(p1, p2, vLeft, vTop, vRight, vBottom)) {
            int sx1 = (int)p1[0] + offsetX, sy1 = (int)p1[1] + offsetY;
            int sx2 = (int)p2[0] + offsetX, sy2 = (int)p2[1] + offsetY;
            if (useDDA) {
                drawLineDDA(g2, sx1, sy1, sx2, sy2);
            } else {
                drawLineBresenham(g2, sx1, sy1, sx2, sy2);
            }
        }
    }

    @Override
    public boolean isFullyInside(Rectangle rect, int offsetX, int offsetY) {
        return rect.contains(x1 + offsetX, y1 + offsetY) && rect.contains(x2 + offsetX, y2 + offsetY);
    }

    @Override
    public void translate(int dx, int dy) {
        x1 += dx;
        y1 += dy;
        x2 += dx;
        y2 += dy;
    }

    @Override
    public void rotate(Point center, double angle) {
        Point p1 = Shape.rotatePoint(x1, y1, center, angle);
        Point p2 = Shape.rotatePoint(x2, y2, center, angle);
        x1 = p1.x;
        y1 = p1.y;
        x2 = p2.x;
        y2 = p2.y;
    }

    @Override
    public void scale(Point center, double scaleX, double scaleY) {
        x1 = center.x + (int) ((x1 - center.x) * scaleX);
        y1 = center.y + (int) ((y1 - center.y) * scaleY);
        x2 = center.x + (int) ((x2 - center.x) * scaleX);
        y2 = center.y + (int) ((y2 - center.y) * scaleY);
    }

    private int computeOutcode(double x, double y, int vLeft, int vTop, int vRight, int vBottom) {
        int code = 0;
        if (x < vLeft) code |= 1;           // LEFT
        else if (x > vRight) code |= 2;     // RIGHT
        if (y < vTop) code |= 4;            // TOP
        else if (y > vBottom) code |= 8;    // BOTTOM
        return code;
    }

    private boolean cohenSutherlandClip(double[] p1, double[] p2, int vLeft, int vTop, int vRight, int vBottom) {
        double x1 = p1[0], y1 = p1[1], x2 = p2[0], y2 = p2[1];
        int out1 = computeOutcode(x1, y1, vLeft, vTop, vRight, vBottom);
        int out2 = computeOutcode(x2, y2, vLeft, vTop, vRight, vBottom);

        while (true) {
            if ((out1 | out2) == 0) { // Trivial accept
                return true;
            } else if ((out1 & out2) != 0) { // Trivial reject
                return false;
            }

            int out = out1 != 0 ? out1 : out2;
            double x = 0, y = 0;

            if ((out & 8) != 0) { // 3 TOP
                x = x1 + (x2 - x1) * (vTop - y1) / (y2 - y1);
                y = vTop;
            } else if ((out & 4) != 0) { // 2 BOTTOM
                x = x1 + (x2 - x1) * (vBottom - y1) / (y2 - y1);
                y = vBottom;
            } else if ((out & 2) != 0) { // 1 RIGHT
                y = y1 + (y2 - y1) * (vRight - x1) / (x2 - x1);
                x = vRight;
            } else if ((out & 1) != 0) { // 0 LEFT
                y = y1 + (y2 - y1) * (vLeft - x1) / (x2 - x1);
                x = vLeft;
            }

            if (out == out1) {
                p1[0] = x;
                p1[1] = y;
                out1 = computeOutcode(x, y, vLeft, vTop, vRight, vBottom);
            } else {
                p2[0] = x;
                p2[1] = y;
                out2 = computeOutcode(x, y, vLeft, vTop, vRight, vBottom);
            }
        }
    }

    @Override
    public Line2D clone() {
        return new Line2D(x1, y1, x2, y2, selected);
    }

    /** Draw line using DDA (Digital Differential Analyzer) algorithm */
    public void drawLineDDA(Graphics2D g2, int x1, int y1, int x2, int y2) {

        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));

        if (steps == 0) {
            g2.drawLine(x1, y1, x2, y2);
            return;
        }

        float xIncrement = dx / (float) steps;
        float yIncrement = dy / (float) steps;

        float prevX = x1;
        float prevY = y1;
        float x = x1;
        float y = y1;

        for (int i = 0; i <= steps; i++) {
            x += xIncrement;
            y += yIncrement;
            g2.drawLine(Math.round(prevX), Math.round(prevY), Math.round(x), Math.round(y));
            prevX = x;
            prevY = y;
        }
    }

    /** Draw line using Bresenham's line algorithm */
    public void drawLineBresenham(Graphics2D g2, int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int x = x1;
        int y = y1;
        int prevX = x1;
        int prevY = y1;
        int c1, c2, p, xincr, yincr;

        if(dx >= 0) xincr = 1;
        else {
            xincr = -1;
            dx = -dx;
        }
        if(dy >= 0) yincr = 1;
        else {
            yincr = -1;
            dy = -dy;
        }
        if(dx > dy) {
            c1 = 2 * dy;
            c2 = 2 * (dy - dx);
            p = c1 - dx;
            for(int i = 0; i <= dx; i++) {
                if(p < 0) {
                    p += c1;
                } else {
                    g2.drawLine(prevX, prevY, x, y);
                    p += c2;
                    y += yincr;
                    prevX = x;
                    prevY = y;
                }
                x += xincr;
            }
        } else {
            c1 = 2 * dx;
            c2 = 2 * (dx - dy);
            p = c1 - dy;
            for(int i = 0; i <= dy; i++) {
                if(p < 0) {
                    p += c1;
                } else {
                    g2.drawLine(prevX, prevY, x, y);
                    p += c2;
                    x += xincr;
                    prevX = x;
                    prevY = y;
                }
                y += yincr;
            }
        }
    }
}

class Circle2D extends Shape {
    int cx, cy, radius;

    public Circle2D(int cx, int cy, int radius) {
        this.cx = cx;
        this.cy = cy;
        this.radius = radius;
    }

    Circle2D(int cx, int cy, int radius, boolean selected) {
        this(cx, cy, radius);
        this.selected = selected;
    }

@Override
    public void draw(Graphics g, int offsetX, int offsetY, boolean highlight, PaintApp app, int vLeft, int vTop, int vRight, int vBottom) {
        Graphics2D g2 = (Graphics2D) g;
        Utils.prepareShapeGraphics(g2, selected, highlight);
        if (app != null) {
            drawCircleBresenham(g2, cx + offsetX, cy + offsetY, radius);
        } else {
            g2.drawOval(cx + offsetX - radius, cy + offsetY - radius, 2 * radius, 2 * radius);
        }
    }

    @Override
    public boolean isFullyInside(Rectangle rect, int offsetX, int offsetY) {
        int left = cx - radius + offsetX;
        int right = cx + radius + offsetX;
        int top = cy - radius + offsetY;
        int bottom = cy + radius + offsetY;
        return rect.contains(cx + offsetX, cy + offsetY) &&
                left >= rect.x && right <= rect.x + rect.width &&
                top >= rect.y && bottom <= rect.y + rect.height;
    }


    @Override
    public void translate(int dx, int dy) {
        cx += dx;
        cy += dy;
    }

    @Override
    public void rotate(Point center, double angle) {
        Point rotated = Shape.rotatePoint(cx, cy, center, angle);
        cx = rotated.x;
        cy = rotated.y;
    }

    @Override
    public void scale(Point center, double scaleX, double scaleY) {
        cx = center.x + (int) ((cx - center.x) * scaleX);
        cy = center.y + (int) ((cy - center.y) * scaleY);
        radius = (int) (radius * Math.sqrt(scaleX * scaleX + scaleY * scaleY) / Math.sqrt(2));
    }

    @Override
    public Circle2D clone() {
        return new Circle2D(cx, cy, radius, selected);
    }

    /** Draw circle using Bresenham's circle algorithm */
    private void drawCircleBresenham(Graphics2D g2, int cx, int cy, int radius) {
        int x = 0;
        int y = radius;
        int p = 3 - 2 * radius;

        while (x < y) {
            // Draw all 8 octants
            g2.drawLine(cx + x, cy + y, cx + x, cy + y); // Octant 1
            g2.drawLine(cx - x, cy + y, cx - x, cy + y); // Octant 2
            g2.drawLine(cx + x, cy - y, cx + x, cy - y); // Octant 4
            g2.drawLine(cx - x, cy - y, cx - x, cy - y); // Octant 3
            g2.drawLine(cx + y, cy + x, cx + y, cy + x); // Octant 8
            g2.drawLine(cx - y, cy + x, cx - y, cy + x); // Octant 7
            g2.drawLine(cx + y, cy - x, cx + y, cy - x); // Octant 5
            g2.drawLine(cx - y, cy - x, cx - y, cy - x); // Octant 6

            if (p < 0) {
                p = p + 4 * x + 6;
            } else {
                p = p + 4 * (x - y) + 10;
                y--;
            }
            x++;
        }
    }
}

class Polygon2D extends Shape {
    ArrayList<Point2D> vertices;

    Polygon2D(List<Point2D> verts) {
        vertices = new ArrayList<>(verts);
    }

    Polygon2D(List<Point2D> verts, boolean selected) {
        vertices = new ArrayList<>(verts);
        this.selected = selected;
    }

    void add(int x, int y) {
        vertices.add(new Point2D(x, y));
    }

@Override
    public void draw(Graphics g, int offsetX, int offsetY, boolean highlight, PaintApp app, int vLeft, int vTop, int vRight, int vBottom) {
        Graphics2D g2 = (Graphics2D) g;
        Utils.prepareShapeGraphics(g2, selected, highlight);

        if (vertices.size() < 2) {
            return;
        }

        for (int i = 0; i < vertices.size() - 1; i++) {
            Point2D p1 = vertices.get(i);
            Point2D p2 = vertices.get(i + 1);
            Line2D tmp = new Line2D(p1.x, p1.y, p2.x, p2.y, selected);
            tmp.draw(g, offsetX, offsetY, highlight, app, vLeft, vTop, vRight, vBottom);
        }
    }


    @Override
    public boolean isFullyInside(Rectangle rect, int offsetX, int offsetY) {
        for (Point2D v : vertices) {
            if (!rect.contains(v.x + offsetX, v.y + offsetY)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void translate(int dx, int dy) {
        for (Point2D v : vertices)
            v.translate(dx, dy);
    }

    @Override
    public void rotate(Point center, double angle) {
        for (Point2D v : vertices)
            v.rotate(center, angle);
    }

    @Override
    public void scale(Point center, double scaleX, double scaleY) {
        for (Point2D v : vertices)
            v.scale(center, scaleX, scaleY);
    }

    @Override
    public Polygon2D clone() {
        ArrayList<Point2D> clonedVerts = new ArrayList<>();
        for (Point2D v : vertices) {
            clonedVerts.add(v.clone());
        }
        return new Polygon2D(clonedVerts, selected);
    }

    @Override
    boolean isPolygon() {
        return true;
    }
}
