package edu.up.isgc.cg.raytracer.ui;

import javax.swing.*;
import java.awt.Color;

/**
 * Entry point for the new scene-editor UI. This is a separate entry point from
 * {@link edu.up.isgc.cg.raytracer.Raytracer}, which stays as the legacy dialog-driven
 * batch render flow while this UI is being built out phase by phase.
 *
 * @author Claude (Anthropic)
 */
public class RayTracerApp {

    public static void main(String[] args) {
        useNimbusIfAvailable();
        SwingUtilities.invokeLater(() -> new StartupWindow().setVisible(true));
    }

    /**
     * Plain Swing's default look & feel (Metal on most platforms, a native-chrome
     * emulation on Windows) mostly ignores colors set on components like buttons.
     * Nimbus ships in the JDK itself (no external dependency) and actually respects
     * them, so custom-styled components render as intended on any OS.
     */
    private static void useNimbusIfAvailable() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    darkenNimbus();
                    return;
                }
            }
        } catch (Exception ignored) {
            // Fall back to the platform default look & feel.
        }
    }

    /**
     * The hand-built panels (sidebar, inspector, toolbar, viewport) paint themselves in
     * {@link Theme}'s dark palette directly, but everything Nimbus still owns — menus,
     * spinners/combo boxes/text fields sitting inside those dark panels, and whole dialogs
     * like the file chooser and confirm/warning popups — kept rendering in Nimbus's stock
     * light theme, since Nimbus paints through its own gradient {@code Painter}s derived
     * from a handful of base colors rather than reading a component's own background/
     * foreground. {@code setBackground}/{@code setForeground} on the instances (or even
     * per-component keys like "MenuBar.background") has no effect on those painters; the
     * only thing Nimbus's derivation actually keys off is overriding the base colors below
     * before any component is created. This is the standard recolor-Nimbus technique (still
     * the JDK's built-in Nimbus, so no new dependency) rather than a bespoke dark L&F.
     */
    private static void darkenNimbus() {
        UIManager.put("control", Theme.BG_PANEL);
        UIManager.put("nimbusLightBackground", Theme.BG);
        UIManager.put("info", Theme.BG_PANEL);
        UIManager.put("text", Theme.TEXT);
        UIManager.put("controlText", Theme.TEXT);
        UIManager.put("menuText", Theme.TEXT);
        UIManager.put("infoText", Theme.TEXT);
        UIManager.put("nimbusDisabledText", Theme.MUTED);
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("nimbusSelectionBackground", Theme.ACCENT);
        UIManager.put("nimbusFocus", Theme.ACCENT);
        UIManager.put("nimbusBase", new Color(45, 45, 52));
        UIManager.put("nimbusBlueGrey", Theme.BG_TOOLBAR);
        UIManager.put("nimbusBorder", Theme.BORDER);
        UIManager.put("ToolTip.background", Theme.BG_PANEL);
        UIManager.put("ToolTip.foreground", Theme.TEXT);

        // The base-color overrides above darken JMenuBar itself, but PopupMenu/Menu/
        // MenuItem paint their background through Nimbus's built-in gradient Painters,
        // which are baked resources rather than something re-derived from "control" at
        // runtime — plain color keys on them (also tried here, and even setBackground()
        // on live instances) are silently ignored. Supplying our own flat-fill Painter per
        // key is the only background override Nimbus actually consults for these regions.
        // The *text* color on a menu item turned out to need a different fix again — see
        // EditorWindow#styleMenuText, set directly on each item instead of fought over here.
        UIManager.put("PopupMenu[Enabled].backgroundPainter", flatPainter(Theme.BG_PANEL));
        UIManager.put("Menu[Enabled].backgroundPainter", flatPainter(Theme.BG_PANEL));
        UIManager.put("Menu[MouseOver].backgroundPainter", flatPainter(Theme.ACCENT));
        UIManager.put("Menu[Selected].backgroundPainter", flatPainter(Theme.ACCENT));
        UIManager.put("MenuItem[Enabled].backgroundPainter", flatPainter(Theme.BG_PANEL));
        UIManager.put("MenuItem[MouseOver].backgroundPainter", flatPainter(Theme.ACCENT));
        UIManager.put("MenuItem[Selected].backgroundPainter", flatPainter(Theme.ACCENT));
        UIManager.put("Menu.foreground", Theme.TEXT);
        UIManager.put("Separator.foreground", Theme.BORDER);
        UIManager.put("Separator[Enabled].backgroundPainter", flatPainter(Theme.BG_PANEL));
    }

    /** A Nimbus region Painter that just flat-fills with one color, ignoring state/size. */
    private static Painter<JComponent> flatPainter(Color color) {
        return (g, c, w, h) -> {
            g.setColor(color);
            g.fillRect(0, 0, w, h);
        };
    }
}
