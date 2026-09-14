package edu.up.isgc.cg.raytracer.ui;

/**
 * Which face of the "cube" the viewport is currently looking through — the Maya-style
 * approach: Top/Front/Side are orthographic, so dragging in them maps directly to
 * exactly two world axes with no perspective ambiguity. Perspective is a free-look view
 * kept only for visual context (composition, not precise placement).
 *
 * @author Claude (Anthropic)
 */
public enum ViewportView {
    TOP("Top", "drag object: move X·Z · drag empty: pan (switch view for Y)"),
    FRONT("Front", "drag object: move X·Y · drag empty: pan (switch view for Z)"),
    SIDE("Side", "drag object: move Z·Y · drag empty: pan (switch view for X)"),
    PERSPECTIVE("Perspective", "free look — reference only, no placement here");

    private final String label;
    private final String axesHint;

    ViewportView(String label, String axesHint) {
        this.label = label;
        this.axesHint = axesHint;
    }

    public String getLabel() {
        return label;
    }

    public String getAxesHint() {
        return axesHint;
    }
}
