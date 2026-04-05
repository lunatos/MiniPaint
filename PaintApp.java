import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class PaintApp implements CanvasListener {

    private CanvasPanel canvas;
    private JFrame frame;
    private final Map<ToolMode, JButton> modeButtons = new LinkedHashMap<>();
    private final Color highlightColor = new Color(173, 216, 230);

    private static Font loadFont(float size) {
        try (java.io.InputStream is = PaintApp.class.getResourceAsStream("/fonts/fa-solid-900.otf")) {
            if (is == null) {
                return new Font("SansSerif", Font.PLAIN, (int) size);
            }
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
        } catch (Exception e) {
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

        JPanel tools = new JPanel();
        tools.setPreferredSize(new Dimension(800, 40));
        tools.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        Font faFont = loadFont(18f);

        // Define tool buttons
        addToolButton(tools, "ToolMode.DRAW_POINT", "\uf192", "Draw Point", ToolMode.DRAW_POINT, faFont);
        addToolButton(tools, "ToolMode.DRAW_LINE", "--", "Draw Line On Drag", ToolMode.DRAW_LINE, faFont);
        addToolButton(tools, "ToolMode.DRAW_CIRCLE", "\uf111", "Draw Circle On Drag", ToolMode.DRAW_CIRCLE, faFont);
        addToolButton(tools, "ToolMode.DRAW_POLYGON", "\ue4e2", "Draw Polygon On Click", ToolMode.DRAW_POLYGON, faFont);
        addToolButton(tools, "ToolMode.SELECT", "\uf565", "Select On Drag", ToolMode.SELECT, faFont);
        addToolButton(tools, "ToolMode.PAN", "\uf256", "Pan On Drag", ToolMode.PAN, faFont);
        addToolButton(tools, "ToolMode.ROTATE", "\uf01e", "Rotate On Drag", ToolMode.ROTATE, faFont);

        frame.setLayout(new BorderLayout());
        frame.add(canvas, BorderLayout.CENTER);
        frame.add(tools, BorderLayout.NORTH);
        frame.setVisible(true);

        updateToolButtons();
    }

    private void addToolButton(JPanel panel, String key, String text, String tooltip, ToolMode mode, Font font) {
        JButton btn = new JButton(text);
        btn.setFont(font);
        btn.setToolTipText(tooltip);
        btn.addActionListener(e -> canvas.setMode(mode));
        modeButtons.put(mode, btn);
        panel.add(btn);
    }

    @Override
    public void updateToolButtons() {
        Color normal = UIManager.getColor("Button.background");
        ToolMode mode = canvas.getCurrentMode();

        for (Map.Entry<ToolMode, JButton> entry : modeButtons.entrySet()) {
            ToolMode btnMode = entry.getKey();
            boolean isActive = (mode == ToolMode.SELECT || mode == ToolMode.EDIT)
                ? (btnMode == ToolMode.SELECT || btnMode == ToolMode.EDIT)
                : mode == btnMode;
            entry.getValue().setBackground(isActive ? highlightColor : normal);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PaintApp());
    }
}
