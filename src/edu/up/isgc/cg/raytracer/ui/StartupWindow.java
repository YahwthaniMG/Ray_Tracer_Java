package edu.up.isgc.cg.raytracer.ui;

import java.awt.*;
import javax.swing.*;

/**
 * The first screen of the editor: project title, a short blurb about what the ray
 * tracer supports, author credits, and a "Start" button that opens the scene editor.
 *
 * @author Claude (Anthropic)
 */
public class StartupWindow extends JFrame {

    public StartupWindow() {
        super("Ray Tracer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(560, 380);
        setMinimumSize(new Dimension(480, 340));
        setLocationRelativeTo(null);
        setContentPane(buildContent());
    }

    private JComponent buildContent() {
        JPanel root = new JPanel();
        root.setBackground(Theme.BG);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(36, 40, 28, 40));

        JLabel title = new JLabel("RAY TRACER");
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("A software ray tracer written in plain Java");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(Theme.MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(6, 0, 20, 0));

        JTextArea blurb = new JTextArea(
                "Reflection & refraction · point & directional lights · " +
                "perspective & orthographic cameras · OBJ model import");
        styleBlurb(blurb);

        JLabel authors = new JLabel("By Yahwthani Morales & Claude Code & Jafet Rodríguez");
        authors.setFont(new Font("SansSerif", Font.PLAIN, 13));
        authors.setForeground(Theme.MUTED);
        authors.setAlignmentX(Component.LEFT_ALIGNMENT);
        authors.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.setBorder(BorderFactory.createEmptyBorder(28, 0, 0, 0));
        buttonRow.add(buildStartButton());

        root.add(title);
        root.add(subtitle);
        root.add(blurb);
        root.add(authors);
        root.add(Box.createVerticalGlue());
        root.add(buttonRow);
        return root;
    }

    private void styleBlurb(JTextArea blurb) {
        blurb.setEditable(false);
        blurb.setFocusable(false);
        blurb.setLineWrap(true);
        blurb.setWrapStyleWord(true);
        // Nimbus's JTextArea UI delegate ignores setOpaque(false) and paints its own
        // background regardless, so match it to the panel's color explicitly instead.
        blurb.setOpaque(true);
        blurb.setBackground(Theme.BG);
        blurb.setFont(new Font("SansSerif", Font.PLAIN, 13));
        blurb.setForeground(Theme.TEXT);
        blurb.setAlignmentX(Component.LEFT_ALIGNMENT);
        blurb.setMaximumSize(new Dimension(460, 60));
    }

    private JButton buildStartButton() {
        JButton start = new JButton("Start");
        start.setFont(new Font("SansSerif", Font.BOLD, 15));
        start.setForeground(Color.WHITE);
        start.setBackground(Theme.ACCENT);
        start.setOpaque(true);
        start.setFocusPainted(false);
        start.setBorder(BorderFactory.createEmptyBorder(10, 28, 10, 28));
        start.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        start.addActionListener(e -> onStart());
        return start;
    }

    private void onStart() {
        EditorWindow editor = new EditorWindow();
        editor.setVisible(true);
        dispose();
    }
}
