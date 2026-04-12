import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class PaintApp implements CanvasListener {

    private CanvasPanel canvas;
    private JFrame frame;
    private final Map<ToolMode, JButton> modeButtons = new LinkedHashMap<>();
    private final Color highlightColor = new Color(173, 216, 230);
    private JButton reflectXBtn, reflectYBtn, reflectXYBtn;
    private boolean useDDAMode = true;
    private JButton rasterizeBtn;

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
        addToolButton(tools, "\uf192", "Draw Point", ToolMode.DRAW_POINT, faFont);
        addToolButton(tools, "--", "Draw Line On Drag", ToolMode.DRAW_LINE, faFont);
        addToolButton(tools, "\uf111", "Draw Circle On Drag", ToolMode.DRAW_CIRCLE, faFont);
        addToolButton(tools, "\ue4e2", "Draw Polygon On Click", ToolMode.DRAW_POLYGON, faFont);
        addToolButton(tools, "\uf065", "Select On Drag", ToolMode.SELECT, faFont);
        addToolButton(tools, "\uf256", "Pan On Drag", ToolMode.PAN, faFont);
        addToolButton(tools, "\uf2f1", "Rotate On Drag", ToolMode.ROTATE, faFont);
        addToolButton(tools, "\uf31e", "Scale On Drag", ToolMode.SCALE, faFont);

        // Reflection buttons
        tools.add(new JSeparator(JSeparator.VERTICAL));

        reflectXBtn = new JButton("\u0058");
        reflectXBtn.setFont(faFont);
        reflectXBtn.setToolTipText("Reflect X");
        reflectXBtn.addActionListener(e -> canvas.reflectX());
        tools.add(reflectXBtn);

        reflectYBtn = new JButton("\u0059");
        reflectYBtn.setFont(faFont);
        reflectYBtn.setToolTipText("Reflect Y");
        reflectYBtn.addActionListener(e -> canvas.reflectY());
        tools.add(reflectYBtn);

        reflectXYBtn = new JButton("\u0058 \u0059");
        reflectXYBtn.setFont(faFont);
        reflectXYBtn.setToolTipText("Reflect XY");
        reflectXYBtn.addActionListener(e -> canvas.reflectXY());
        tools.add(reflectXYBtn);

        // Rasterization toggle button
        tools.add(new JSeparator(JSeparator.VERTICAL));
        rasterizeBtn = new JButton("DDA");
        rasterizeBtn.setFont(faFont);
        rasterizeBtn.setToolTipText("Toggle Rasterization Algorithm: DDA (click to switch to Bresenham)");
        rasterizeBtn.addActionListener(e -> {
            useDDAMode = !useDDAMode;
            rasterizeBtn.setText(useDDAMode ? "DDA" : "Bres");
            rasterizeBtn.setToolTipText(useDDAMode ?
                "Toggle Rasterization Algorithm: DDA (click to switch to Bresenham)" :
                "Toggle Rasterization Algorithm: Bresenham (click to switch to DDA)");
        });
        tools.add(rasterizeBtn);

        frame.setLayout(new BorderLayout());
        frame.add(canvas, BorderLayout.CENTER);
        frame.add(tools, BorderLayout.NORTH);
        frame.setVisible(true);

        updateToolButtons();
    }

    private void addToolButton(JPanel panel, String text, String tooltip, ToolMode mode, Font font) {
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

    public boolean isUseDDAMode() {
        return useDDAMode;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PaintApp());
    }
}
