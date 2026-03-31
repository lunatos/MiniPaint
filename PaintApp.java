
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
// import java.io.File;  // removido (não utilizado)
import java.util.ArrayList;

enum ToolMode {
    DRAW_POINT,
    SELECT,
    EDIT,
    PAN
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
        return Math.hypot(mx - (x + offsetX), my - (y + offsetY)) <= 6;
    }
}

class CanvasPanel extends JPanel {
    ArrayList<Point2D> points = new ArrayList<>();
    ToolMode currentMode = ToolMode.DRAW_POINT;
    Rectangle selectionRect = null;
    int startX, startY;
    int offsetX = 0, offsetY = 0; // usado ao arrastar painel no modo PAN
    boolean drag = false; // usado ao arrastar retangulo no modo EDIT

    public CanvasPanel() {
        setBackground(Color.WHITE);

        MouseAdapter mouse = new MouseAdapter() {

            int lastX, lastY;

            @Override
            public void mousePressed(MouseEvent e) {
                startX = e.getX();
                startY = e.getY();
                lastX = startX;
                lastY = startY;

                // comportamento por modo:
                if (currentMode == ToolMode.EDIT) {
                    if (selectionRect != null) {
                        if (selectionRect.contains(startX, startY)) {
                            drag = true;
                        } else {
                            // entrar em SELECT para começar nova seleção
                            currentMode = ToolMode.SELECT;
                            selectionRect = null;
                            for (Point2D p : points)
                                p.selected = false;
                            drag = false;
                        }
                    }
                } else if (currentMode == ToolMode.SELECT) {
                    // iniciar criação de um novo rect de seleção
                    selectionRect = new Rectangle(startX, startY, 0, 0);
                    for (Point2D p : points)
                        p.selected = false;
                    drag = false;
                } else {
                    // outros modos limpam seleção/rect
                    selectionRect = null;
                    for (Point2D p : points)
                        p.selected = false;
                    drag = false;
                }

                // garantir que o painel tenha foco para receber teclas (Delete)
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
                        // usar helper centralizado para translação dos pontos selecionados
                        translateSelected(dx, dy);
                    }
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
                        currentMode = ToolMode.EDIT;
                    }
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
                // tecla 'X' remove todos os pontos selecionados
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

    public void setMode(ToolMode mode) {
        this.currentMode = mode;
    }

    public void translateSelected(int dx, int dy) {
        if (selectionRect != null) {
            selectionRect.x += dx;
            selectionRect.y += dy;
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

    public void rotateSelected(double angleDegrees) {
        Point center = getCenterSelected();
        if (center == null)
            return;

        double angle = Math.toRadians(angleDegrees);

        for (Point2D p : points) {
            if (p.selected) {
                int x = p.x - center.x;
                int y = p.y - center.y;

                int xr = (int) (x * Math.cos(angle) - y * Math.sin(angle));
                int yr = (int) (x * Math.sin(angle) + y * Math.cos(angle));

                p.x = xr + center.x;
                p.y = yr + center.y;
            }
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
            Stroke oldStroke = g2.getStroke();
            Color oldColor = g2.getColor();

            if (currentMode != ToolMode.EDIT) {
                float[] dash = { 6f, 6f };
                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
            } else {
                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(2f));
            }
            g2.draw(selectionRect);

            g2.setStroke(oldStroke);
            g2.setColor(oldColor);
        }
    }
}

public class PaintApp {

    // Método que carrega a fonte FontAwesome a partir de resources/fonts
    // e retorna uma instância derivada no tamanho pedido. Se falhar, retorna
    // uma fonte padrão (SansSerif) como fallback.
    private static Font loadFont(float size) {
        try (java.io.InputStream is = PaintApp.class.getResourceAsStream("/fonts/fa-solid-900.otf")) {
            if (is == null) {
                System.err.println("Font resource not found: /fonts/fa-solid-900.otf");
                return new Font("SansSerif", Font.PLAIN, (int) size);
            }
            Font f = Font.createFont(Font.TRUETYPE_FONT, is);
            return f.deriveFont(size);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("SansSerif", Font.PLAIN, (int) size);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Mini Paint - Computação Gráfica");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        CanvasPanel canvas = new CanvasPanel();

        JPanel tools = new JPanel();
        tools.setPreferredSize(new Dimension(800, 60));
        tools.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        Font faFont = loadFont(18f);
        JButton btnPoint = new JButton("\uf111");
        JButton btnSelect = new JButton("\uf245");
        JButton btnPan = new JButton("\uf256");
        btnPoint.setFont(faFont);
        btnSelect.setFont(faFont);
        btnPan.setFont(faFont);

        btnPoint.addActionListener(e -> canvas.setMode(ToolMode.DRAW_POINT));
        btnSelect.addActionListener(e -> canvas.setMode(ToolMode.SELECT));
        btnPan.addActionListener(e -> canvas.setMode(ToolMode.PAN));

        tools.add(btnPoint);
        tools.add(btnSelect);
        tools.add(btnPan);

        frame.setLayout(new BorderLayout());
        frame.add(canvas, BorderLayout.CENTER);
        frame.add(tools, BorderLayout.NORTH);

        frame.setVisible(true);
    }
}