package edu.up.isgc.cg.raytracer.ui;

import java.awt.Color;

/**
 * Shared color palette for the editor UI so every panel (startup screen, sidebar,
 * toolbar, viewport) stays visually consistent instead of each file inventing its own
 * shades.
 *
 * @author Claude (Anthropic)
 */
final class Theme {
    private Theme() {}

    static final Color BG = new Color(24, 24, 28);
    static final Color BG_PANEL = new Color(28, 28, 32);
    static final Color BG_TOOLBAR = new Color(32, 32, 36);
    static final Color BORDER = new Color(50, 50, 56);
    static final Color TEXT = new Color(235, 235, 240);
    static final Color MUTED = new Color(160, 160, 170);
    static final Color ACCENT = new Color(255, 140, 60);

    // Maya-style axis colors, reused both for the viewport grid and (later) any gizmo.
    static final Color AXIS_X = new Color(220, 70, 70);
    static final Color AXIS_Y = new Color(90, 200, 90);
    static final Color AXIS_Z = new Color(80, 130, 230);

    static final Color GRID_LINE = new Color(50, 50, 54);
    static final Color GRID_LINE_STRONG = new Color(70, 70, 76);
}
