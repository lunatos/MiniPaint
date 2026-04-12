import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

interface CanvasListener {
    void updateToolButtons();
}

class CanvasPanel extends JPanel {

    private static final int POLYGON_CLOSE_TOLERANCE = 10;

    private final CanvasListener listener;
    private final ArrayList<Shape> shapes = new ArrayList<>();

    private ToolMode currentMode = ToolMode.DRAW_POINT;
    private Shape previewShape = null;
    private Line2D polygonPreviewLine = null;

    private boolean showCloseHint = false;
    private Point closeHintPoint = null;

    // bounding rect of selection
    private Point[] selectionRect = new Point[4];

    // rotation/scale state
    private Point transformCenter = null;
    private double startAngle = 0;
    private final ArrayList<Shape> tempShapes = new ArrayList<>();
    private final ArrayList<Point> originalRectPoints = new ArrayList<>();
    private final ArrayList<Shape> originalShapes = new ArrayList<>();
    private double totalTransformAngle = 0.0;

    // cumulative rotation angle (normalized to [-PI, PI])
    private double rotationAngle = 0.0;

    // scale state
    private Point scaleCenter = null;
    private double startScaleDistance = 0;

    // pan/transform
    private int offsetX = 0, offsetY = 0;
    private int startWorldX, startWorldY;

    // mouse drag state
    private boolean drag = false;
    private int startX, startY;

    public ToolMode getCurrentMode() {
        return currentMode;
    }

    private boolean isDrawMode() {
        return currentMode == ToolMode.DRAW_POINT || currentMode == ToolMode.DRAW_LINE
                || currentMode == ToolMode.DRAW_CIRCLE || currentMode == ToolMode.DRAW_POLYGON;
    }

    private boolean isSelectionMode() {
        return currentMode == ToolMode.SELECT || currentMode == ToolMode.EDIT || currentMode == ToolMode.ROTATE || currentMode == ToolMode.SCALE;
    }

    // ---- Selection rect helpers ----
    private static boolean hasSelection(Point[] rect) {
        return rect[0] != null;
    }

    private static void clearSelectionRect(Point[] rect) {
        for (int i = 0; i < 4; i++) rect[i] = null;
    }

    private static void setSelectionRectFromDrag(Point[] rect, int x1, int y1, int x2, int y2) {
        int rx = Math.min(x1, x2);
        int ry = Math.min(y1, y2);
        int rw = Math.abs(x1 - x2);
        int rh = Math.abs(y1 - y2);
        rect[0] = new Point(rx, ry);
        rect[1] = new Point(rx + rw, ry);
        rect[2] = new Point(rx + rw, ry + rh);
        rect[3] = new Point(rx, ry + rh);
    }

    private static Point[] clonePoints(Point[] src) {
        Point[] dst = new Point[4];
        for (int i = 0; i < 4; i++) dst[i] = new Point(src[i].x, src[i].y);
        return dst;
    }

    public void restartSelection() {
        clearSelectionForDrawing();
        currentMode = ToolMode.SELECT;
        repaint();
    }

    public void clearSelection() {
        for (Shape s : shapes)
            s.selected = false;
    }

    public void clearSelectionForDrawing() {
        clearSelection();
        clearSelectionRect(selectionRect);
        rotationAngle = 0.0;
        repaint();
    }

    public void setMode(ToolMode mode) {
        polygonPreviewLine = null;
        showCloseHint = false;
        closeHintPoint = null;
        this.currentMode = mode;
        if (listener != null)
            listener.updateToolButtons();
        if (!isSelectionMode()) {
            clearSelectionForDrawing();
        }
        previewShape = null;
        repaint();
    }

    public void translate(int dx, int dy) {
        if (hasSelection(selectionRect)) {
            for (int i = 0; i < 4; i++) {
                selectionRect[i] = new Point(selectionRect[i].x + dx, selectionRect[i].y + dy);
            }
        }
        for (Shape s : shapes) {
            if (s.selected) {
                s.translate(dx, dy);
            }
        }
        repaint();
    }

    public Point getCenterSelected() {
        if (!hasSelection(selectionRect)) return null;
        int cx = 0, cy = 0;
        for (int i = 0; i < 4; i++) {
            cx += selectionRect[i].x;
            cy += selectionRect[i].y;
        }
        return new Point(cx / 4, cy / 4);
    }

    private double normalizeAngle(double angle) {
        return angle % (2 * Math.PI);
    }

    // ---- Drawing helpers ----
    private static Point[] buildSelectionPolygon(Point[] rect) {
        Point[] pts = new Point[4];
        for (int i = 0; i < 4; i++) {
            pts[i] = new Point(rect[i].x, rect[i].y);
        }
        return pts;
    }

    public CanvasPanel(CanvasListener listener) {
        this.listener = listener;
        setBackground(Color.WHITE);

        MouseAdapter mouse = new MouseAdapter() {
            int lastX, lastY;

            @Override
            public void mousePressed(MouseEvent e) {
                startX = e.getX();
                startY = e.getY();
                startWorldX = startX - offsetX;
                startWorldY = startY - offsetY;
                lastX = startX;
                lastY = startY;

                if (currentMode == ToolMode.EDIT) {
                    if (hasSelection(selectionRect)) {
                        Polygon poly = new Polygon();
                        for (int i = 0; i < 4; i++) {
                            poly.addPoint(selectionRect[i].x, selectionRect[i].y);
                        }
                        if (poly.contains(startWorldX, startWorldY)) {
                            drag = true;
                            return;
                        }
                    }
                    restartSelection();
                    return;
                } else if (currentMode == ToolMode.SELECT) {
                    clearSelection();
                    drag = false;
                } else if (currentMode == ToolMode.ROTATE) {
                    startRotation();
                } else if (currentMode == ToolMode.SCALE) {
                    startScale();
                } else if (isDrawMode()) {
                    handleDrawPress(e);
                }
                requestFocusInWindow();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();

                if (currentMode == ToolMode.PAN) {
                    offsetX += x - lastX;
                    offsetY += y - lastY;
                } else if (currentMode == ToolMode.SELECT) {
                    int worldX = x - offsetX;
                    int worldY = y - offsetY;
                    setSelectionRectFromDrag(selectionRect, startWorldX, startWorldY, worldX, worldY);
                } else if (currentMode == ToolMode.EDIT && drag) {
                    translate(x - lastX, y - lastY);
                } else if (currentMode == ToolMode.ROTATE && transformCenter != null) {
                    double currentAngle = Math.atan2(y - transformCenter.y, x - transformCenter.x);
                    totalTransformAngle = normalizeAngle(currentAngle - startAngle);
                    rotate(totalTransformAngle);
                } else if (currentMode == ToolMode.SCALE && scaleCenter != null) {
                    double dx = x - scaleCenter.x;
                    double dy = y - scaleCenter.y;
                    if (startScaleDistance > 0) {
                        double currentDist = Math.hypot(dx, dy);
                        double factor = currentDist / startScaleDistance;
                        scale(factor, factor);
                    }
                } else if (isDrawMode()) {
                    handleDrawDrag(x, y);
                }

                lastX = x;
                lastY = y;
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                int worldX = x - offsetX;
                int worldY = y - offsetY;
                if (currentMode == ToolMode.DRAW_POLYGON && previewShape != null) {
                    if (previewShape.isPolygon()) {
                        // Close Hint logic: if cursor is within tolerance of first vertex, show hint to close polygon
                        Polygon2D poly = (Polygon2D) previewShape;
                        if (!poly.vertices.isEmpty()) {
                            Point2D lastVertex = poly.vertices.get(poly.vertices.size() - 1);
                            polygonPreviewLine = new Line2D(lastVertex.x, lastVertex.y, worldX, worldY);
                        }
                        if (poly.vertices.size() >= 3) {
                            Point2D first = poly.vertices.get(0);
                            showCloseHint = java.lang.Math.hypot(worldX - first.x, worldY - first.y) < POLYGON_CLOSE_TOLERANCE;
                        }
                    } else if (previewShape.isPoint()) {
                        // First vertex added, now dragging to second vertex. Convert previewShape to Polygon2D and show line to current cursor.
                        Point2D firstVertex = (Point2D) previewShape;
                        ArrayList<Point2D> verts = new ArrayList<>();
                        verts.add(firstVertex);
                        Polygon2D newPoly = new Polygon2D(verts);
                        previewShape = newPoly;
                        polygonPreviewLine = new Line2D(firstVertex.x, firstVertex.y, worldX, worldY);
                    }
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDrawMode()) {
                    if (currentMode == ToolMode.DRAW_LINE && previewShape != null && previewShape.isPoint()) {
                        previewShape = null;
                    }
                    if (currentMode != ToolMode.DRAW_POLYGON && previewShape != null) {
                        shapes.add(previewShape);
                    }
                } else if (currentMode == ToolMode.SELECT && hasSelection(selectionRect)) {
                    // Compute screen rect from world selectionRect
                    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
                    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
                    for (int i = 0; i < 4; i++) {
                        int sx = selectionRect[i].x + offsetX;
                        int sy = selectionRect[i].y + offsetY;
                        minX = Math.min(minX, sx);
                        maxX = Math.max(maxX, sx);
                        minY = Math.min(minY, sy);
                        maxY = Math.max(maxY, sy);
                    }
                    Rectangle selArea = new Rectangle(minX, minY, maxX - minX, maxY - minY);
                    for (Shape s : shapes) {
                        s.selected = s.isFullyInside(selArea, offsetX, offsetY);
                    }
                    setMode(ToolMode.EDIT);
                } else if (currentMode == ToolMode.ROTATE) {
                    finishRotation();
                } else if (currentMode == ToolMode.SCALE) {
                    finishScale();
                }
                repaint();
            }

            private void handleDrawPress(MouseEvent e) {
                int worldX = e.getX() - offsetX;
                int worldY = e.getY() - offsetY;

                if (currentMode == ToolMode.DRAW_POLYGON) {
                    if (previewShape == null) {
                        previewShape = new Point2D(worldX, worldY);
                        closeHintPoint = new Point(worldX, worldY);
                    } else if (previewShape.isPolygon()) {
                        Polygon2D poly = (Polygon2D) previewShape;
                        if (poly.vertices.size() >= 3) {
                            Point2D first = poly.vertices.get(0);
                            if (java.lang.Math.hypot(worldX - first.x, worldY - first.y) < POLYGON_CLOSE_TOLERANCE) {
                                poly.add(first.x, first.y);
                                shapes.add(poly);
                                setMode(currentMode);
                                return;
                            }
                        }
                        poly.add(worldX, worldY);
                        polygonPreviewLine = null;
                    }
                } else {
                    previewShape = new Point2D(worldX, worldY);
                }
                repaint();
            }

            private void handleDrawDrag(int x, int y) {
                int worldX = x - offsetX;
                int worldY = y - offsetY;
                int startWorldX = startX - offsetX;
                int startWorldY = startY - offsetY;

                switch (currentMode) {
                    case DRAW_LINE:
                        previewShape = new Line2D(startWorldX, startWorldY, worldX, worldY);
                        break;
                    case DRAW_CIRCLE:
                        int r = (int) java.lang.Math.hypot(worldX - startWorldX, worldY - startWorldY);
                        previewShape = new Circle2D(startWorldX, startWorldY, r);
                        break;
                    case DRAW_POINT:
                        previewShape = new Point2D(worldX, worldY);
                        break;
                    default:
                        break;
                }
            }
        };

        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (currentMode == ToolMode.DRAW_POLYGON && e.getKeyCode() == KeyEvent.VK_V) {
                    if (previewShape != null && previewShape.isPolygon()) {
                        Polygon2D tmp = (Polygon2D) previewShape;
                        if (tmp.vertices != null) {
                            shapes.add(tmp);
                        }
                        setMode(currentMode);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_X) {
                    if (isDrawMode()) {
                        previewShape = null;
                        setMode(currentMode);
                    } else {
                        for (int i = shapes.size() - 1; i >= 0; i--) {
                            if (shapes.get(i).selected) {
                                shapes.remove(i);
                            }
                        }
                        clearSelectionRect(selectionRect);
                        currentMode = ToolMode.SELECT;
                    }
                    repaint();
                }
            }
        });
    }

    // ---- Rotation helpers ----
    private void startRotation() {
        transformCenter = getCenterSelected();
        if (transformCenter == null) return;

        startAngle = Math.atan2(startY - transformCenter.y + offsetY, startX - transformCenter.x + offsetX);
        originalRectPoints.clear();
        tempShapes.clear();
        originalShapes.clear();
        totalTransformAngle = 0.0;

        for (int i = 0; i < shapes.size(); i++) {
            Shape s = shapes.get(i);
            if (s.selected) {
                originalShapes.add(s.clone());
                shapes.remove(i);
                i--;
            }
        }

        for (int i = 0; i < 4; i++) {
            originalRectPoints.add(new Point(selectionRect[i].x, selectionRect[i].y));
        }

        for (Shape s : shapes) {
            if (s.selected) {
                tempShapes.add(s);
            }
        }
    }

    private void finishRotation() {
        if (transformCenter != null && !originalShapes.isEmpty()) {
            for (Shape orig : originalShapes) {
                Shape rotated = orig.clone();
                rotated.rotate(transformCenter, totalTransformAngle);
                shapes.add(rotated);
            }
        }
        rotationAngle = normalizeAngle(rotationAngle + totalTransformAngle);
        transformCenter = null;
        originalShapes.clear();
        tempShapes.clear();
        originalRectPoints.clear();
        totalTransformAngle = 0.0;
        setMode(ToolMode.EDIT);
        if (listener != null)
            listener.updateToolButtons();
    }

    public void rotate(double angle) {
        if (transformCenter == null) return;

        tempShapes.clear();
        for (Shape orig : originalShapes) {
            Shape temp = orig.clone();
            temp.rotate(transformCenter, angle);
            tempShapes.add(temp);
        }

        for (int i = 0; i < 4; i++) {
            Point orig = originalRectPoints.get(i);
            selectionRect[i] = Shape.rotatePoint(orig.x, orig.y, transformCenter, angle);
        }
        repaint();
    }

    // ---- Scale helpers ----
    private void startScale() {
        scaleCenter = getCenterSelected();
        if (scaleCenter == null) return;

        originalRectPoints.clear();
        tempShapes.clear();
        originalShapes.clear();
        startScaleDistance = Math.hypot(startX - scaleCenter.x, startY - scaleCenter.y);

        for (int i = 0; i < shapes.size(); i++) {
            Shape s = shapes.get(i);
            if (s.selected) {
                originalShapes.add(s.clone());
                shapes.remove(i);
                i--;
            }
        }

        for (int i = 0; i < 4; i++) {
            originalRectPoints.add(new Point(selectionRect[i].x, selectionRect[i].y));
        }

        for (Shape s : shapes) {
            if (s.selected) {
                tempShapes.add(s);
            }
        }
    }

    public void scale(double scaleX, double scaleY) {
        if (scaleCenter == null) return;

        tempShapes.clear();
        for (Shape orig : originalShapes) {
            Shape temp = orig.clone();
            temp.scale(scaleCenter, scaleX, scaleY);
            tempShapes.add(temp);
        }

        for (int i = 0; i < 4; i++) {
            Point orig = originalRectPoints.get(i);
            selectionRect[i] = new Point(
                scaleCenter.x + (int) ((orig.x - scaleCenter.x) * scaleX),
                scaleCenter.y + (int) ((orig.y - scaleCenter.y) * scaleY));
        }
        repaint();
    }

    private void finishScale() {
        if (scaleCenter != null && !originalShapes.isEmpty() && !originalRectPoints.isEmpty()) {
            // Compute final scale factor from current selectionRect vs original
            double dx = selectionRect[1].x - scaleCenter.x;
            double dy = selectionRect[1].y - scaleCenter.y;
            double currDist = Math.hypot(dx, dy);
            double origDx = originalRectPoints.get(1).x - scaleCenter.x;
            double origDy = originalRectPoints.get(1).y - scaleCenter.y;
            double origDist = Math.hypot(origDx, origDy);
            double finalScale = origDist > 0 ? currDist / origDist : 1.0;

            for (Shape orig : originalShapes) {
                Shape scaled = orig.clone();
                scaled.scale(scaleCenter, finalScale, finalScale);
                shapes.add(scaled);
            }
        }
        scaleCenter = null;
        originalShapes.clear();
        tempShapes.clear();
        originalRectPoints.clear();
        setMode(ToolMode.EDIT);
        if (listener != null)
            listener.updateToolButtons();
    }

    // ---- Reflection helpers ----
    public void reflectX() {
        applyReflection(-1, 1);
    }

    public void reflectY() {
        applyReflection(1, -1);
    }

    public void reflectXY() {
        applyReflection(-1, -1);
    }

    private void applyReflection(double scaleX, double scaleY) {
        if (!hasSelection(selectionRect)) return;
        Point center = getCenterSelected();
        if (center == null) return;

        // Reflect about the shape's local axes: unrotate > reflect > re-rotate
        double ra = rotationAngle;
        for (Shape s : shapes) {
            if (s.selected) {
                s.rotate(center, -ra);
                s.scale(center, scaleX, scaleY);
                s.rotate(center, ra);
            }
        }
        for (Shape s : tempShapes) {
            if (s.selected) {
                s.rotate(center, -ra);
                s.scale(center, scaleX, scaleY);
                s.rotate(center, ra);
            }
        }

        // Update selection rect corners the same way
        Point[] oldCorners = clonePoints(selectionRect);
        for (int i = 0; i < 4; i++) {
            oldCorners[i] = Shape.rotatePoint(oldCorners[i].x, oldCorners[i].y, center, -ra);
            oldCorners[i].x = center.x + (int) ((oldCorners[i].x - center.x) * scaleX);
            oldCorners[i].y = center.y + (int) ((oldCorners[i].y - center.y) * scaleY);
            selectionRect[i] = Shape.rotatePoint(oldCorners[i].x, oldCorners[i].y, center, ra);
        }

        repaint();
    }

    // ---- Paint ----
@Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int vLeft = -offsetX, vTop = -offsetY, vRight = getWidth() - offsetX, vBottom = getHeight() - offsetY;

        // Draw all shapes
        for (Shape s : shapes) {
            s.draw(g2, offsetX, offsetY, false, listener != null ? (PaintApp) listener : null, vLeft, vTop, vRight, vBottom);
        }

        // Draw transformed temp shapes during ROTATE/SCALE mode
        if ((currentMode == ToolMode.ROTATE || currentMode == ToolMode.SCALE) && !tempShapes.isEmpty()) {
            for (Shape s : tempShapes) {
                s.draw(g2, offsetX, offsetY, false, listener != null ? (PaintApp) listener : null, vLeft, vTop, vRight, vBottom);
            }
        }

        // Draw preview shape
        if (previewShape != null) {
            previewShape.draw(g2, offsetX, offsetY, true, listener != null ? (PaintApp) listener : null, vLeft, vTop, vRight, vBottom);
        }
        if (polygonPreviewLine != null) {
            polygonPreviewLine.draw(g2, offsetX, offsetY, true, listener != null ? (PaintApp) listener : null, vLeft, vTop, vRight, vBottom);
        }

        // Draw polygon close hint
        if (showCloseHint && closeHintPoint != null) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2f));
            int cx = closeHintPoint.x + offsetX;
            int cy = closeHintPoint.y + offsetY;
            int radius = 8;
            g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g2.setStroke(new BasicStroke(1));
        }

        // Draw selection rect
        if (hasSelection(selectionRect)) {
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(3f));
            if (currentMode != ToolMode.EDIT) {
                float[] dash = {8f, 4f};
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
            }
            Polygon poly = new Polygon();
            Point[] pts = buildSelectionPolygon(selectionRect);
            for (Point p : pts) {
                poly.addPoint(p.x + offsetX, p.y + offsetY);
            }
            g2.draw(poly);
        }
    }

}
