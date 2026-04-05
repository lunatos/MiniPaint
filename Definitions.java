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
    ROTATE
}

abstract class Shape {
    boolean selected = false;

    abstract void draw(Graphics g, int offsetX, int offsetY, boolean highlight);

    abstract boolean isFullyInside(Rectangle rect, int offsetX, int offsetY);

    abstract void translate(int dx, int dy);

    abstract void rotate(Point center, double angle);

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
                (int) round(center.y + dx * sin(angle) + dy * cos(angle))
        );
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
    public void draw(Graphics g, int offsetX, int offsetY, boolean highlight) {
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
    public void draw(Graphics g, int offsetX, int offsetY, boolean highlight) {
        Graphics2D g2 = (Graphics2D) g;
        Utils.prepareShapeGraphics(g2, selected, highlight);
        g2.drawLine(x1 + offsetX, y1 + offsetY, x2 + offsetX, y2 + offsetY);
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
        x1 = p1.x; y1 = p1.y;
        x2 = p2.x; y2 = p2.y;
    }

    @Override
    public Line2D clone() {
        return new Line2D(x1, y1, x2, y2, selected);
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
    public void draw(Graphics g, int offsetX, int offsetY, boolean highlight) {
        Graphics2D g2 = (Graphics2D) g;
        Utils.prepareShapeGraphics(g2, selected, highlight);
        g2.drawOval(cx + offsetX - radius, cy + offsetY - radius, 2 * radius, 2 * radius);
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
    public Circle2D clone() {
        return new Circle2D(cx, cy, radius, selected);
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
    public void draw(Graphics g, int offsetX, int offsetY, boolean highlight) {
        Graphics2D g2 = (Graphics2D) g;
        Utils.prepareShapeGraphics(g2, selected, highlight);
        Polygon poly = new Polygon();
        for (Point2D v : vertices) {
            poly.addPoint(v.x + offsetX, v.y + offsetY);
        }
        g2.drawPolyline(poly.xpoints, poly.ypoints, vertices.size());
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
