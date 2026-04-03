import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

interface CanvasListener {
    void updateToolButtons();
}

class CanvasPanel extends JPanel {
    CanvasListener listener;
    ArrayList<Point2D> points = new ArrayList<>();
    ToolMode currentMode = ToolMode.DRAW_POINT;
    Rectangle selectionRect = null;
    int startX, startY;
    int offsetX = 0, offsetY = 0;
    boolean drag = false;
    Point rotationCenter = null;
    double startAngle = 0;
    ArrayList<Point> originalRectPoints = new ArrayList<>();
    ArrayList<Point> originalPoints = new ArrayList<>();
    ArrayList<Point2D> tempPoints = new ArrayList<>();
    Point[] rotatedRect = new Point[4];

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
                    if (selectionRect != null) {
                        if (selectionRect.contains(startX, startY)) {
                            drag = true;
                        } else {
                            restartSelection();
                        }
                    }
                } else if (currentMode == ToolMode.SELECT) {
                    rotatedRect = new Point[4];
                    selectionRect = new Rectangle(startX, startY, 0, 0);
                    for (Point2D p : points)
                        p.selected = false;
                    drag = false;
                } else if (currentMode == ToolMode.ROTATE) {
                    rotationCenter = getCenterSelected();

                    if (rotationCenter != null) {
                        startAngle = Math.atan2(startY - rotationCenter.y + offsetY,
                                startX - rotationCenter.x + offsetX);

                        originalPoints.clear();
                        originalRectPoints.clear();
                        tempPoints.clear();

                        for (int i = 0; i < 4; i++) {
                            switch (i) {
                                case 0:
                                    originalRectPoints.add(new Point(selectionRect.x, selectionRect.y));
                                    break;
                                case 1:
                                    originalRectPoints
                                            .add(new Point(selectionRect.x + selectionRect.width, selectionRect.y));
                                    break;
                                case 2:
                                    originalRectPoints.add(new Point(selectionRect.x + selectionRect.width,
                                            selectionRect.y + selectionRect.height));
                                    break;
                                case 3:
                                    originalRectPoints
                                            .add(new Point(selectionRect.x, selectionRect.y + selectionRect.height));
                                    break;
                            }
                        }
                        for (Point2D p : points) {
                            if (p.selected) {
                                originalPoints.add(new Point(p.x, p.y));
                                tempPoints.add(p);
                            }
                        }
                    }
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
                    double angleDiff = normalizeAngle(currentAngle - startAngle);

                    rotate(angleDiff);
                }

                lastX = x;
                lastY = y;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentMode == ToolMode.DRAW_POINT) {
                    int px = e.getX() - offsetX;
                    int py = e.getY() - offsetY;
                    points.add(new Point2D(px, py));
                } else if (currentMode == ToolMode.SELECT) {
                    if (selectionRect != null) {
                        for (Point2D p : points) {
                            int px = p.x + offsetX;
                            int py = p.y + offsetY;
                            p.selected = selectionRect.contains(px, py);
                        }
                        setMode(ToolMode.EDIT);
                    }
                } else if (currentMode == ToolMode.ROTATE) {
                    setMode(ToolMode.SELECT);
                    if (listener != null) listener.updateToolButtons();
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
                if (e.getKeyCode() == KeyEvent.VK_X) {
                    for (int i = points.size() - 1; i >= 0; i--) {
                        if (points.get(i).selected) {
                            points.remove(i);
                        }
                    }
                    selectionRect = null;
                    currentMode = ToolMode.SELECT;
                    repaint();
                }
            }
        });
    }

    public void restartSelection() {
        clearSelectionForDrawing();
        currentMode = ToolMode.SELECT;
        repaint();
    }

    public void clearSelectionForDrawing() {
        for (Point2D p : points)
            p.selected = false;
        selectionRect = null;
        rotatedRect = new Point[4];
        repaint();
    }

    public ToolMode getCurrentMode() {
        return currentMode;
    }

    public void setMode(ToolMode mode) {
        this.currentMode = mode;
        if (listener != null) listener.updateToolButtons();
    }

    public void translate(int dx, int dy) {
        if (selectionRect != null) {
            selectionRect.translate(dx, dy);
        }

        for (Point2D p : points) {
            if (p.selected) {
                p.x += dx;
                p.y += dy;
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
        for (int i = 0; i < 4; i++) {
            Point orig = originalRectPoints.get(i);
            if (orig != null) {
                double x = orig.x - rotationCenter.x;
                double y = orig.y - rotationCenter.y;

                double xr = x * Math.cos(angle) - y * Math.sin(angle);
                double yr = x * Math.sin(angle) + y * Math.cos(angle);

                rotatedRect[i] = new Point((int) Math.round(xr + rotationCenter.x),
                        (int) Math.round(yr + rotationCenter.y));
            }
        }

        for (int i = 0; i < tempPoints.size(); i++) {
            Point2D p = tempPoints.get(i);
            Point orig = originalPoints.get(i);

            double x = orig.x - rotationCenter.x;
            double y = orig.y - rotationCenter.y;

            double xr = x * Math.cos(angle) - y * Math.sin(angle);
            double yr = x * Math.sin(angle) + y * Math.cos(angle);

            p.x = (int) Math.round(xr + rotationCenter.x);
            p.y = (int) Math.round(yr + rotationCenter.y);
        }
        repaint();
    }

    public void scaleSelected(double sx, double sy) {
        Point center = getCenterSelected();
        if (center == null)
            return;

        for (Point2D p : points) {
            if (p.selected) {
                int x = p.x - center.x;
                int y = p.y - center.y;

                x *= sx;
                y *= sy;

                p.x = (int) x + center.x;
                p.y = (int) y + center.y;
            }
        }
        repaint();
    }

    public void reflectX() {
        Point center = getCenterSelected();
        if (center == null)
            return;

        for (Point2D p : points) {
            if (p.selected) {
                p.y = center.y - (p.y - center.y);
            }
        }
        repaint();
    }

    public void reflectY() {
        Point center = getCenterSelected();
        if (center == null)
            return;

        for (Point2D p : points) {
            if (p.selected) {
                p.x = center.x - (p.x - center.x);
            }
        }
        repaint();
    }

    public void reflectXY() {
        reflectX();
        reflectY();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (Point2D p : points) {
            p.draw(g, offsetX, offsetY);
        }

        if (selectionRect != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.GRAY);

            if (currentMode == ToolMode.EDIT) {
                g2.setStroke(new BasicStroke(2f));
            } else {
                float[] dash = { 6f, 6f };
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                if (currentMode == ToolMode.ROTATE) {
                    g2.setColor(Color.BLUE);
                }
            }
            
            if(rotatedRect[0] != null) {
                Polygon poly = new Polygon();
                for (int i = 0; i < 4; i++) {
                    poly.addPoint(rotatedRect[i].x, rotatedRect[i].y);
                }
                g2.draw(poly);
            } else {
                g2.draw(selectionRect);
            }
        }
    }
}

