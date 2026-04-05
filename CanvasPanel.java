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
    private Rectangle selectionRect = null;
    private Shape previewShape = null;
    private Line2D polygonPreviewLine = null;

    private boolean showCloseHint = false;
    private Point closeHintPoint = null;

    // rotation state
    private Point rotationCenter = null;
    private double startAngle = 0;
    private final ArrayList<Shape> tempShapes = new ArrayList<>();
    private final ArrayList<Point> originalRectPoints = new ArrayList<>();
    private final ArrayList<Shape> originalShapes = new ArrayList<>();
    private double totalRotationAngle = 0.0;
    private Point[] rotatedRect = new Point[4];

    // pan/transform
    private int offsetX = 0, offsetY = 0;

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
        return currentMode == ToolMode.SELECT || currentMode == ToolMode.EDIT || currentMode == ToolMode.ROTATE;
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
        selectionRect = null;
        rotatedRect = new Point[4];
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
        if (selectionRect != null) {
            if (rotatedRect[0] != null) {
                Polygon poly = new Polygon();
                for (int i = 0; i < 4; i++) {
                    poly.addPoint(rotatedRect[i].x, rotatedRect[i].y);
                }
                poly.translate(dx, dy);
                for (int i = 0; i < 4; i++) {
                    rotatedRect[i] = new Point(poly.xpoints[i], poly.ypoints[i]);
                }
            } else {
                selectionRect.translate(dx, dy);
            }
        }
        for (Shape s : shapes) {
            if (s.selected) {
                s.translate(dx, dy);
            }
        }
        repaint();
    }

    private Point getCenterSelected() {
        if (selectionRect == null)
            return null;
        return new Point(selectionRect.x + selectionRect.width / 2 - offsetX,
                selectionRect.y + selectionRect.height / 2 - offsetY);
    }

    private double normalizeAngle(double angle) {
        while (angle < -Math.PI)
            angle += 2 * Math.PI;
        while (angle > Math.PI)
            angle -= 2 * Math.PI;
        return angle;
    }

    public void rotate(double angle) {
        if (rotationCenter == null)
            return;

        tempShapes.clear();
        for (Shape orig : originalShapes) {
            Shape temp = orig.clone();
            temp.rotate(rotationCenter, angle);
            tempShapes.add(temp);
        }

        for (int i = 0; i < 4; i++) {
            Point orig = originalRectPoints.get(i);
            rotatedRect[i] = Shape.rotatePoint(orig.x, orig.y, rotationCenter, angle);
        }
        repaint();
    }

    // ---- Rect corners helper ----
    private ArrayList<Point> getRectCorners(Rectangle rect) {
        ArrayList<Point> corners = new ArrayList<>(4);
        corners.add(new Point(rect.x, rect.y));
        corners.add(new Point(rect.x + rect.width, rect.y));
        corners.add(new Point(rect.x + rect.width, rect.y + rect.height));
        corners.add(new Point(rect.x, rect.y + rect.height));
        return corners;
    }

    // ---- Drawing helpers ----
    private static Point[] buildRotatedPolygon(Point[] rotatedRect, int offX, int offY) {
        Point[] pts = new Point[4];
        for (int i = 0; i < 4; i++) {
            pts[i] = new Point(rotatedRect[i].x + offX, rotatedRect[i].y + offY);
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
                lastX = startX;
                lastY = startY;

                if (currentMode == ToolMode.EDIT) {
                    if (rotatedRect[0] != null) {
                        Polygon poly = new Polygon();
                        for (int i = 0; i < 4; i++) {
                            poly.addPoint(rotatedRect[i].x, rotatedRect[i].y);
                        }
                        if (poly.contains(startX, startY)) {
                            drag = true;
                            return;
                        }
                    } else {
                        if (selectionRect != null && selectionRect.contains(startX, startY)) {
                            drag = true;
                            return;
                        }
                    }
                    restartSelection();
                    return;

                } else if (currentMode == ToolMode.SELECT) {
                    clearSelection();
                    selectionRect = new Rectangle(startX, startY, 0, 0);
                    drag = false;

                } else if (currentMode == ToolMode.ROTATE) {
                    startRotation();

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
                    int rx = Math.min(startX, x);
                    int ry = Math.min(startY, y);
                    int rw = Math.abs(startX - x);
                    int rh = Math.abs(startY - y);
                    selectionRect = new Rectangle(rx, ry, rw, rh);
                } else if (currentMode == ToolMode.EDIT && drag) {
                    translate(x - lastX, y - lastY);
                } else if (currentMode == ToolMode.ROTATE && rotationCenter != null) {
                    double currentAngle = Math.atan2(y - rotationCenter.y, x - rotationCenter.x);
                    totalRotationAngle = normalizeAngle(currentAngle - startAngle);
                    rotate(totalRotationAngle);
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
                        Polygon2D poly = (Polygon2D) previewShape;
                        if (!poly.vertices.isEmpty()) {
                            Point2D lastVertex = poly.vertices.get(poly.vertices.size() - 1);
                            polygonPreviewLine = new Line2D(lastVertex.x, lastVertex.y, worldX, worldY);
                        }
                        showCloseHint = poly.vertices.size() >= 3
                                && java.lang.Math.hypot(worldX - closeHintPoint.x, worldY - closeHintPoint.y) < POLYGON_CLOSE_TOLERANCE;
                    } else if (previewShape.isPoint()) {
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
                } else if (currentMode == ToolMode.SELECT) {
                    if (selectionRect != null) {
                        for (Shape s : shapes) {
                            s.selected = s.isFullyInside(selectionRect, offsetX, offsetY);
                        }
                        setMode(ToolMode.EDIT);
                    }
                } else if (currentMode == ToolMode.ROTATE) {
                    finishRotation();
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
                        selectionRect = null;
                        currentMode = ToolMode.SELECT;
                    }
                    repaint();
                }
            }
        });
    }

    // ---- Rotation helpers ----
    private void startRotation() {
        rotationCenter = getCenterSelected();
        if (rotationCenter == null)
            return;

        startAngle = Math.atan2(startY - rotationCenter.y + offsetY, startX - rotationCenter.x + offsetX);
        originalRectPoints.clear();
        tempShapes.clear();
        originalShapes.clear();
        totalRotationAngle = 0.0;

        for (int i = 0; i < shapes.size(); i++) {
            Shape s = shapes.get(i);
            if (s.selected) {
                originalShapes.add(s.clone());
                shapes.remove(i);
                i--;
            }
        }

        if (rotatedRect[0] != null) {
            for (int i = 0; i < 4; i++) {
                originalRectPoints.add(new Point(rotatedRect[i].x, rotatedRect[i].y));
            }
        } else if (selectionRect != null) {
            originalRectPoints.addAll(getRectCorners(selectionRect));
        }

        for (Shape s : shapes) {
            if (s.selected) {
                tempShapes.add(s);
            }
        }
    }

    private void finishRotation() {
        if (rotationCenter != null && !originalShapes.isEmpty()) {
            for (Shape orig : originalShapes) {
                Shape rotated = orig.clone();
                rotated.rotate(rotationCenter, totalRotationAngle);
                shapes.add(rotated);
            }
        }
        rotationCenter = null;
        originalShapes.clear();
        tempShapes.clear();
        originalRectPoints.clear();
        totalRotationAngle = 0.0;
        setMode(ToolMode.EDIT);
        if (listener != null)
            listener.updateToolButtons();
    }

    // ---- Paint ----
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Draw all shapes
        for (Shape s : shapes) {
            s.draw(g2, offsetX, offsetY, false);
        }

        // Draw rotated temp shapes during ROTATE mode
        if (currentMode == ToolMode.ROTATE && !tempShapes.isEmpty()) {
            for (Shape s : tempShapes) {
                s.draw(g2, offsetX, offsetY, false);
            }
        }

        // Draw preview shape
        if (previewShape != null) {
            previewShape.draw(g2, offsetX, offsetY, true);
        }
        if (polygonPreviewLine != null) {
            polygonPreviewLine.draw(g2, offsetX, offsetY, true);
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

        // Draw selection rectangle
        if (selectionRect != null) {
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(3f));
            if (currentMode != ToolMode.EDIT) {
                float[] dash = {8f, 4f};
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
            }
            if (rotatedRect[0] != null) {
                Polygon poly = new Polygon();
                Point[] pts = buildRotatedPolygon(rotatedRect, offsetX, offsetY);
                for (Point p : pts) {
                    poly.addPoint(p.x, p.y);
                }
                g2.draw(poly);
            } else {
                g2.draw(selectionRect);
            }
        }
    }
}
