import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

interface CanvasListener {
    void updateToolButtons();
}

class CanvasPanel extends JPanel {

    CanvasListener listener;
    ArrayList<Shape> shapes = new ArrayList<>();
    ToolMode currentMode = ToolMode.DRAW_POINT;
    Rectangle selectionRect = null;
    int startX, startY;
    int offsetX = 0, offsetY = 0;
    boolean drag = false;

    // rotation state
    Point rotationCenter = null;
    double startAngle = 0;
    ArrayList<Point> originalRectPoints = new ArrayList<>();
    ArrayList<Shape> tempShapes = new ArrayList<>();
    Point[] rotatedRect = new Point[4];
    ArrayList<Shape> originalShapes = new ArrayList<>();
    double totalRotationAngle = 0.0;

    // drawing state
    Shape previewShape = null;
    Line2D polygonPreviewLine = null;
    static final int POLYGON_CLOSE_TOLERANCE = 10;
    private boolean showCloseHint = false;
    private Point closeHintPoint = null;

    public ToolMode getCurrentMode() {
        return currentMode;
    }

    private boolean isDrawMode() {
        return currentMode == ToolMode.DRAW_POINT || currentMode == ToolMode.DRAW_LINE
                || currentMode == ToolMode.DRAW_CIRCLE || currentMode == ToolMode.DRAW_POLYGON;
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
        if (mode != ToolMode.SELECT && mode != ToolMode.EDIT && mode != ToolMode.ROTATE) {
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

        // Populate tempShapes from original clones each
        tempShapes.clear();
        for (Shape orig : originalShapes) {
            Shape temp = orig.clone();
            temp.rotate(rotationCenter, angle);
            tempShapes.add(temp);
        }

        // Rotate the selection rect corners from original
        for (int i = 0; i < 4; i++) {
            Point orig = originalRectPoints.get(i);
            double dx = orig.x - rotationCenter.x;
            double dy = orig.y - rotationCenter.y;
            rotatedRect[i] = new Point(
                    (int) Math.round(rotationCenter.x + dx * Math.cos(angle) - dy * Math.sin(angle)),
                    (int) Math.round(rotationCenter.y + dx * Math.sin(angle) + dy * Math.cos(angle)));
        }
        repaint();
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
                        } else {
                            restartSelection();
                        }
                    } else {
                        if (selectionRect != null && selectionRect.contains(startX, startY)) {
                            drag = true;
                        } else {
                            restartSelection();
                        }
                    }

                } else if (currentMode == ToolMode.SELECT) {
                    clearSelection();
                    selectionRect = new Rectangle(startX, startY, 0, 0);
                    drag = false;
                } else if (currentMode == ToolMode.ROTATE) {
                    rotationCenter = getCenterSelected();

                    if (rotationCenter != null) {
                        startAngle = Math.atan2(startY - rotationCenter.y + offsetY,
                                startX - rotationCenter.x + offsetX);

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
                                Point p = new Point(rotatedRect[i].x, rotatedRect[i].y);
                                originalRectPoints.add(p);
                            }
                        } else if (selectionRect != null) {
                            for (int i = 0; i < 4; i++) {
                                Point p;
                                switch (i) {
                                    case 0:
                                        p = new Point(selectionRect.x, selectionRect.y);
                                        break;
                                    case 1:
                                        p = new Point(selectionRect.x + selectionRect.width, selectionRect.y);
                                        break;
                                    case 2:
                                        p = new Point(selectionRect.x + selectionRect.width,
                                                selectionRect.y + selectionRect.height);
                                        break;
                                    case 3:
                                        p = new Point(selectionRect.x, selectionRect.y + selectionRect.height);
                                        break;
                                    default:
                                        p = new Point();
                                        break;
                                }
                                originalRectPoints.add(p);
                            }
                        }
                        for (Shape s : shapes) {
                            if (s.selected) {
                                tempShapes.add(s);
                            }
                        }
                    }
                } else if (isDrawMode()) {
                    // Add vertex on click
                    int worldX = startX - offsetX;
                    int worldY = startY - offsetY;

                    if (currentMode == ToolMode.DRAW_POLYGON) {
                        if (previewShape == null) {
                            previewShape = new Point2D(worldX, worldY);
                            closeHintPoint = new Point(worldX, worldY);
                        } else {
                            if (previewShape.isPolygon()) {
                                Polygon2D poly = (Polygon2D) previewShape;
                                if (!poly.vertices.isEmpty() && poly.vertices.size() >= 3) {
                                    Point2D firstVertex = poly.vertices.get(0);
                                    if (Math.hypot(worldX - firstVertex.x,
                                            worldY - firstVertex.y) < POLYGON_CLOSE_TOLERANCE) {
                                        // Close polygon, dont add last point and create shape
                                        poly.add(firstVertex.x, firstVertex.y);
                                        shapes.add(poly);
                                        setMode(currentMode);
                                        return;
                                    }
                                }
                                poly.add(worldX, worldY);
                                polygonPreviewLine = null;
                            }
                        }
                    } else {
                        previewShape = new Point2D(worldX, worldY);
                    }
                    repaint();
                }
                requestFocusInWindow();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                int worldX = x - offsetX;
                int worldY = y - offsetY;
                int startWorldX = startX - offsetX;
                int startWorldY = startY - offsetY;

                if (currentMode == ToolMode.PAN) {
                    offsetX += x - lastX;
                    offsetY += y - lastY;
                } else if (currentMode == ToolMode.SELECT) {
                    int rx = Math.min(startX, x);
                    int ry = Math.min(startY, y);
                    int rw = Math.abs(startX - x);
                    int rh = Math.abs(startY - y);
                    selectionRect = new Rectangle(rx, ry, rw, rh);
                } else if (currentMode == ToolMode.EDIT) {
                    if (drag) {
                        int dx = x - lastX;
                        int dy = y - lastY;
                        translate(dx, dy);
                    }
                } else if (currentMode == ToolMode.ROTATE) {
                    if (rotationCenter == null)
                        return;
                    double currentAngle = Math.atan2(y - rotationCenter.y, x - rotationCenter.x);
                    totalRotationAngle = normalizeAngle(currentAngle - startAngle);
                    rotate(totalRotationAngle);
                } else if (currentMode == ToolMode.DRAW_LINE) {
                    previewShape = new Line2D(startWorldX, startWorldY, worldX, worldY);
                } else if (currentMode == ToolMode.DRAW_CIRCLE) {
                    int r = (int) Math.hypot(worldX - startWorldX, worldY - startWorldY);
                    previewShape = new Circle2D(startWorldX, startWorldY, r);
                } else if (currentMode == ToolMode.DRAW_POINT) {
                    previewShape = new Point2D(worldX, worldY);
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
                if (currentMode == ToolMode.DRAW_POLYGON) {
                    if (previewShape != null) {
                        if (previewShape.isPolygon()) {
                            Polygon2D poly = (Polygon2D) previewShape;
                            if (!poly.vertices.isEmpty()) {
                                Point2D lastVertex = poly.vertices.get(poly.vertices.size() - 1);
                                polygonPreviewLine = new Line2D(lastVertex.x, lastVertex.y, worldX, worldY);
                            }
                            if (poly.vertices.size() >= 3) {
                                if (Math.hypot(worldX - closeHintPoint.x,
                                        worldY - closeHintPoint.y) < POLYGON_CLOSE_TOLERANCE) {
                                    showCloseHint = true;
                                } else {
                                    showCloseHint = false;
                                }
                            }
                        } else if (previewShape.isPoint()) {
                            Point2D lastVertex = (Point2D) previewShape;
                            ArrayList<Point2D> verts = new ArrayList<>();
                            verts.add(lastVertex);
                            Polygon2D newPoly = new Polygon2D(verts);
                            previewShape = newPoly;
                            polygonPreviewLine = new Line2D(lastVertex.x, lastVertex.y, worldX, worldY);
                        }
                    }
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDrawMode()) {
                    if (currentMode == ToolMode.DRAW_LINE) {
                        // Avoid addin Point2D when just clicking without dragging
                        if (previewShape.isPoint()) {
                            previewShape = null;
                        }
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
                    // Apply final rotation to main shapes
                    if (rotationCenter != null && !originalShapes.isEmpty()) {
                        ArrayList<Shape> rotatedSelected = new ArrayList<>();
                        for (Shape orig : originalShapes) {
                            Shape rotated = orig.clone();
                            rotated.rotate(rotationCenter, totalRotationAngle);
                            rotatedSelected.add(rotated);
                        }

                        shapes.addAll(rotatedSelected);

                        // Clear rotation state
                        rotationCenter = null;
                        originalShapes.clear();
                        tempShapes.clear();
                        originalRectPoints.clear();
                        totalRotationAngle = 0.0;
                    }
                    setMode(ToolMode.EDIT);
                    if (listener != null)
                        listener.updateToolButtons();
                }
                repaint();
            }
        };

        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (currentMode == ToolMode.DRAW_POLYGON && e.getKeyCode() == KeyEvent.VK_V) {
                    // Create the polygon if when the user presses 'V'
                    if (previewShape != null && previewShape.isPolygon()) {
                        Polygon2D tmp = (Polygon2D) previewShape;
                        if (tmp.vertices != null) {
                            shapes.add(tmp);
                        }
                        setMode(currentMode);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_X) {
                    if (isDrawMode()) {
                        // Cancel current drawing
                        previewShape = null;
                        setMode(currentMode);
                    } else {
                        // Delete selected shapes
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        // Draw all shapes (no highlight for completed shapes)
        for (Shape s : shapes) {
            s.draw(g2, offsetX, offsetY, false);
        }

        // Draw rotated tempShapes during ROTATE mode
        if (currentMode == ToolMode.ROTATE && !tempShapes.isEmpty()) {
            for (Shape s : tempShapes) {
                s.draw(g2, offsetX, offsetY, false);
            }
        }

        // Draw preview shape if exists
        if (previewShape != null) {
            previewShape.draw(g2, offsetX, offsetY, true);
        }

        if (polygonPreviewLine != null) {
            polygonPreviewLine.draw(g2, offsetX, offsetY, true);
        }

        // Draw polygon close hint circle fo closing the polygon
        if (showCloseHint && closeHintPoint != null) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2f));
            int cx = closeHintPoint.x + offsetX;
            int cy = closeHintPoint.y + offsetY;
            int radius = 8;
            g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g2.setStroke(new BasicStroke(1));
        }

        // Draw selection rectangle or rotated rectangle
        if (selectionRect != null) {
            g2.setColor(Color.BLACK);
            if (currentMode == ToolMode.EDIT) {
                g2.setStroke(new BasicStroke(3f));
            } else {
                float[] dash = { 8f, 4f };
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
            }
            if (rotatedRect[0] != null) {
                Polygon poly = new Polygon();
                for (int i = 0; i < 4; i++) {
                    poly.addPoint(rotatedRect[i].x + offsetX, rotatedRect[i].y + offsetY);
                }
                g2.draw(poly);
            } else {
                g2.draw(selectionRect);
            }
        }
    }
}
