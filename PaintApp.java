import javax.swing.*;
import java.awt.*;
import javax.swing.UIManager;

public class PaintApp implements CanvasListener {

    private CanvasPanel canvas;
    private JPanel tools;
    private JButton btnPoint;
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
        btnPoint = new JButton("\uf111");
        btnSelect = new JButton("\uf245");
        btnPan = new JButton("\uf256");
        btnRotate = new JButton("\uf01e");
        btnPoint.setFont(faFont);
        btnSelect.setFont(faFont);
        btnPan.setFont(faFont);
        btnRotate.setFont(faFont);

        btnPoint.addActionListener(e -> {
            canvas.clearSelectionForDrawing();
            canvas.setMode(ToolMode.DRAW_POINT);
            canvas.repaint();
        });
        btnSelect.addActionListener(e -> {
            canvas.setMode(ToolMode.SELECT);
            canvas.repaint();
        });
        btnPan.addActionListener(e -> {
            canvas.setMode(ToolMode.PAN);
            canvas.repaint();
        });
        btnRotate.addActionListener(e -> {
            canvas.setMode(ToolMode.ROTATE);
            canvas.repaint();
        });

        tools.add(btnPoint);
        tools.add(btnSelect);
        tools.add(btnPan);
        tools.add(btnRotate);

        frame.setLayout(new BorderLayout());
        frame.add(canvas, BorderLayout.CENTER);
        frame.add(tools, BorderLayout.NORTH);

        frame.setVisible(true);

        updateToolButtons();
    }

    public void updateToolButtons() {
        if (canvas == null)
            return;
        Color highlight = new Color(173, 216, 230);
        Color normal = UIManager.getColor("Button.background");

        btnPoint.setBackground(canvas.getCurrentMode() == ToolMode.DRAW_POINT ? highlight : normal);
        btnSelect.setBackground(canvas.getCurrentMode() == ToolMode.SELECT ? highlight
                : canvas.getCurrentMode() == ToolMode.EDIT ? highlight : normal);
        btnPan.setBackground(canvas.getCurrentMode() == ToolMode.PAN ? highlight : normal);
        btnRotate.setBackground(canvas.getCurrentMode() == ToolMode.ROTATE ? highlight : normal);

        // Tooltips
        btnPoint.setToolTipText("Draw Point");
        btnSelect.setToolTipText("Select");
        btnPan.setToolTipText("Pan");
        btnRotate.setToolTipText("Rotate");

        btnPoint.repaint();
        btnSelect.repaint();
        btnPan.repaint();
        btnRotate.repaint();
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PaintApp());
    }
}

