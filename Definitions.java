import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.BasicStroke;
import java.util.List;
import java.util.ArrayList;
import static java.lang.Math.*;

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

    abstract void scale(double sx, double sy, Point center);

    abstract void reflectX(Point center);

    abstract void reflectY(Point center);

    abstract boolean isPolygon();

    abstract boolean isPoint();
}

class Point2D extends Shape {
    int x, y;

    public Point2D(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void draw(Graphics g, int offsetX, int offsetY, boolean highlight) {
        Graphics2D g2 = (Graphics2D) g;
        if (selected) {
            g2.setColor(Color.RED);
        } else if (highlight) {
            g2.setColor(Color.BLUE);
        } else {
            g2.setColor(Color.BLACK);
        }
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
        double dx = x - center.x;
        double dy = y - center.y;
        x = (int) Math.round(center.x + dx * cos(angle) - dy * sin(angle));
        y = (int) Math.round(center.y + dx * sin(angle) + dy * cos(angle));
    }

    @Override
    public void scale(double sx, double sy, Point center) {
        int dx = x - center.x;
        int dy = y - center.y;
        x = center.x + (int) (dx * sx);
        y = center.y + (int) (dy * sy);
    }

    @Override
    public void reflectX(Point center) {
        y = center.y - (y - center.y);
    }

    @Override
    public void reflectY(Point center) {
        x = center.x - (x - center.x);
    }

    public boolean contains(int mx, int my, int offsetX, int offsetY) {
        return hypot(mx - (x + offsetX), my - (y + offsetY)) <= 6;
    }

    @Override
    public boolean isPolygon() {
        return false;
    }

    @Override
    public boolean isPoint() {
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

    @Override
    public void draw(Graphics g, int offsetX, int offsetY, boolean highlight) {
        Graphics2D g2 = (Graphics2D) g;
        if (selected) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(3));
        } else if (highlight) {
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(3));
        } else {
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
        }
        g2.drawLine(x1 + offsetX, y1 + offsetY, x2 + offsetX, y2 + offsetY);
        g2.setStroke(new BasicStroke(2));
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
        double dx1 = x1 - center.x;
        double dy1 = y1 - center.y;
        x1 = (int) Math.round(center.x + dx1 * cos(angle) - dy1 * sin(angle));
        y1 = (int) Math.round(center.y + dx1 * sin(angle) + dy1 * cos(angle));
        double dx2 = x2 - center.x;
        double dy2 = y2 - center.y;
        x2 = (int) Math.round(center.x + dx2 * cos(angle) - dy2 * sin(angle));
        y2 = (int) Math.round(center.y + dx2 * sin(angle) + dy2 * cos(angle));
    }

    @Override
    public void scale(double sx, double sy, Point center) {
        int dx1 = x1 - center.x;
        int dy1 = y1 - center.y;
        x1 = center.x + (int) (dx1 * sx);
        y1 = center.y + (int) (dy1 * sy);
        int dx2 = x2 - center.x;
        int dy2 = y2 - center.y;
        x2 = center.x + (int) (dx2 * sx);
        y2 = center.y + (int) (dy2 * sy);
    }

    @Override
    public void reflectX(Point center) {
        y1 = center.y - (y1 - center.y);
        y2 = center.y - (y2 - center.y);
    }

    @Override
    public void reflectY(Point center) {
        x1 = center.x - (x1 - center.x);
        x2 = center.x - (x2 - center.x);
    }

    @Override
    public boolean isPolygon() {
        return false;
    }

    @Override
    public boolean isPoint() {
        return false;
    }
}

class Circle2D extends Shape {
    int cx, cy, radius;

    public Circle2D(int cx, int cy, int radius) {
        this.cx = cx;
        this.cy = cy;
        this.radius = radius;
    }

    @Override
    public void draw(Graphics g, int offsetX, int offsetY, boolean highlight) {
        Graphics2D g2 = (Graphics2D) g;
        if (selected) {
            g2.setColor(Color.RED);
        } else if (highlight) {
            g2.setColor(Color.BLUE);
        } else {
            g2.setColor(Color.BLACK);
        }
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
        double dx = cx - center.x;
        double dy = cy - center.y;
        cx = (int) Math.round(center.x + dx * cos(angle) - dy * sin(angle));
        cy = (int) Math.round(center.y + dx * sin(angle) + dy * cos(angle));
    }

    @Override
    public void scale(double sx, double sy, Point center) {
        double factor = (sx + sy) / 2;
        int dx = cx - center.x;
        int dy = cy - center.y;
        cx = center.x + (int) (dx * sx);
        cy = center.y + (int) (dy * sy);
        radius = Math.max(1, (int) (radius * factor));
    }

    @Override
    public void reflectX(Point center) {
        cy = center.y - (cy - center.y);
    }

    @Override
    public void reflectY(Point center) {
        cx = center.x - (cx - center.x);
    }

    @Override
    public boolean isPolygon() {
        return false;
    }

    @Override
    public boolean isPoint() {
        return false;
    }
}

class Polygon2D extends Shape {
    ArrayList<Point2D> vertices;

    public Polygon2D(List<Point2D> verts) {
        vertices = new ArrayList<>(verts);
    }

    public void add(int x, int y) {
        vertices.add(new Point2D(x, y));
    }

    @Override
    public void draw(Graphics g, int offsetX, int offsetY, boolean highlight) {
        Graphics2D g2 = (Graphics2D) g;
        if (selected) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(3));
        } else if (highlight) {
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(3));
        } else {
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1));
        }
        Polygon poly = new Polygon();
        for (Point2D v : vertices) {
            poly.addPoint(v.x + offsetX, v.y + offsetY);
        }

        g2.drawPolyline(poly.xpoints, poly.ypoints, vertices.size());

        g2.setStroke(new BasicStroke(1));
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
    public void scale(double sx, double sy, Point center) {
        for (Point2D v : vertices)
            v.scale(sx, sy, center);
    }

    @Override
    public void reflectX(Point center) {
        for (Point2D v : vertices)
            v.reflectX(center);
    }

    @Override
    public void reflectY(Point center) {
        for (Point2D v : vertices)
            v.reflectY(center);
    }

    @Override
    public boolean isPolygon() {
        return true;
    }

    @Override
    public boolean isPoint() {
        return false;
    }
}
