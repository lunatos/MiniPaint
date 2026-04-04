import javax.swing.*;
import java.awt.*;
import javax.swing.UIManager;

public class PaintApp implements CanvasListener {

    private CanvasPanel canvas;
    private JPanel tools;
    private JButton btnPoint;
    private JButton btnLine;
    private JButton btnCircle;
    private JButton btnPolygon;
    private JButton btnSelect;
    private JButton btnPan;
    private JButton btnRotate;
    private JFrame frame;

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

    public PaintApp() {
        init();
    }

    private void init() {
        frame = new JFrame("Mini Paint - Computação Gráfica");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        canvas = new CanvasPanel(this);

        tools = new JPanel();
        tools.setPreferredSize(new Dimension(800, 40));
        tools.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        Font faFont = loadFont(18f);
        btnPoint = new JButton("\uf192");
        btnLine = new JButton("--");
        btnCircle = new JButton("\uf111");
        btnPolygon = new JButton("\ue4e2");
        btnSelect = new JButton("\uf565");
        btnPan = new JButton("\uf256");
        btnRotate = new JButton("\uf01e");

        btnPoint.setFont(faFont);
        btnLine.setFont(faFont);
        btnCircle.setFont(faFont);
        btnSelect.setFont(faFont);
        btnPan.setFont(faFont);
        btnRotate.setFont(faFont);
        btnPolygon.setFont(faFont);
        
        btnPoint.setToolTipText("Draw Point");
        btnLine.setToolTipText("Draw Line On Drag");
        btnCircle.setToolTipText("Draw Circle On Drag");
        btnSelect.setToolTipText("Select On Drag");
        btnPan.setToolTipText("Pan On Drag");
        btnRotate.setToolTipText("Rotate On Drag");
        btnPolygon.setToolTipText("Draw Polygon On Click");

        btnPoint.addActionListener(e -> {
            canvas.setMode(ToolMode.DRAW_POINT);
        });
        btnLine.addActionListener(e -> {
            canvas.setMode(ToolMode.DRAW_LINE);
        });
        btnCircle.addActionListener(e -> {
            canvas.setMode(ToolMode.DRAW_CIRCLE);
        });
        btnPolygon.addActionListener(e -> {
            canvas.setMode(ToolMode.DRAW_POLYGON);
        });
        btnSelect.addActionListener(e -> {
            canvas.setMode(ToolMode.SELECT);
        });
        btnPan.addActionListener(e -> {
            canvas.setMode(ToolMode.PAN);
        });
        btnRotate.addActionListener(e -> {
            canvas.setMode(ToolMode.ROTATE);
        });

        tools.add(btnPoint);
        tools.add(btnLine);
        tools.add(btnCircle);
        tools.add(btnPolygon);
        tools.add(btnSelect);
        tools.add(btnPan);
        tools.add(btnRotate);

        frame.setLayout(new BorderLayout());
        frame.add(canvas, BorderLayout.CENTER);
        frame.add(tools, BorderLayout.NORTH);

        frame.setVisible(true);

        updateToolButtons();
    }

    @Override
    public void updateToolButtons() {
        if (canvas == null)
            return;
        Color highlight = new Color(173, 216, 230);
        Color normal = UIManager.getColor("Button.background");

        btnPoint.setBackground(canvas.getCurrentMode() == ToolMode.DRAW_POINT ? highlight : normal);
        btnLine.setBackground(canvas.getCurrentMode() == ToolMode.DRAW_LINE ? highlight : normal);
        btnCircle.setBackground(canvas.getCurrentMode() == ToolMode.DRAW_CIRCLE ? highlight : normal);
        btnPolygon.setBackground(canvas.getCurrentMode() == ToolMode.DRAW_POLYGON ? highlight : normal);
        btnSelect.setBackground(
                (canvas.getCurrentMode() == ToolMode.SELECT || canvas.getCurrentMode() == ToolMode.EDIT) ? highlight
                        : normal);
        btnPan.setBackground(canvas.getCurrentMode() == ToolMode.PAN ? highlight : normal);
        btnRotate.setBackground(canvas.getCurrentMode() == ToolMode.ROTATE ? highlight : normal);

        btnPoint.repaint();
        btnLine.repaint();
        btnCircle.repaint();
        btnSelect.repaint();
        btnPan.repaint();
        btnRotate.repaint();
        btnPolygon.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PaintApp());
    }
}
