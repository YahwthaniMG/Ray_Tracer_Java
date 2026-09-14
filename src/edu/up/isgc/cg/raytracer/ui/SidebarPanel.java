package edu.up.isgc.cg.raytracer.ui;

import edu.up.isgc.cg.raytracer.RenderMode;
import edu.up.isgc.cg.raytracer.Scene;
import edu.up.isgc.cg.raytracer.cameras.Camera;
import edu.up.isgc.cg.raytracer.cameras.FisheyeCamera;
import edu.up.isgc.cg.raytracer.cameras.OrthographicCamera;
import edu.up.isgc.cg.raytracer.cameras.PerspertiveCamera;
import edu.up.isgc.cg.raytracer.lights.DirectionalLight;
import edu.up.isgc.cg.raytracer.lights.PointLight;
import edu.up.isgc.cg.raytracer.lights.SpotLight;
import edu.up.isgc.cg.raytracer.math.Vector3D;
import edu.up.isgc.cg.raytracer.objects.Plane;
import edu.up.isgc.cg.raytracer.objects.Sphere;
import edu.up.isgc.cg.raytracer.tools.Primitives;
import edu.up.isgc.cg.raytracer.tools.SceneProperties;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static edu.up.isgc.cg.raytracer.tools.OBJReader.getModel3D;

/**
 * Left-hand panel: project name, and palettes for geometry/cameras/lights/OBJ import.
 * A row can be clicked (drops the item at the origin) or dragged onto the viewport
 * (drops it exactly where you release) — both only work in an ortho view (Top/Front/
 * Side); Perspective is reference-only and has no single 2D plane to drop onto, so
 * placement is disabled there (see {@link #setPlacementEnabled}).
 *
 * @author Claude (Anthropic)
 */
public class SidebarPanel extends JPanel {

    private static final int WIDTH = 220;
    private static final Color DEFAULT_OBJECT_COLOR = new Color(210, 210, 215);

    private final Scene scene;
    private final ViewportPanel viewport;
    private final List<JLabel> placementRows = new ArrayList<>();
    private final JPanel importListPanel = new JPanel();

    private final JComboBox<Camera> cameraCombo = new JComboBox<>();
    private final JComboBox<Integer> resolutionCombo = new JComboBox<>(
            new Integer[]{100, 200, 300, 400, 500, 600, 700, 800, 900, 1080, 1156, 1200, 1536, 2160});
    private final JComboBox<String> aspectRatioCombo = new JComboBox<>(
            new String[]{"16/9", "16/10", "1/2", "1/1", "3/4", "6/9"});
    private final JComboBox<RenderMode> modeCombo = new JComboBox<>(RenderMode.values());
    private final JComboBox<Integer> samplesCombo = new JComboBox<>(new Integer[]{1, 4, 9, 16});
    private final JTextField projectNameField = new JTextField("Untitled Scene");

    public SidebarPanel(Scene scene, ViewportPanel viewport) {
        this.scene = scene;
        this.viewport = viewport;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.BG_PANEL);
        setBorder(new MatteBorder(0, 0, 0, 1, Theme.BORDER));

        add(projectNameRow());
        add(geometrySection());
        add(camerasSection());
        add(lightsSection());
        add(importSection());
        add(renderSection());
        add(Box.createVerticalGlue());

        resolutionCombo.setSelectedItem(400);
        samplesCombo.setSelectedItem(1);
        refreshCameraList();
    }

    /**
     * Fixes the width but leaves height to whatever the sections actually need — with
     * this many sections (and more likely to be added later) the sidebar routinely
     * needs more vertical space than the window has, so {@link EditorWindow} wraps it in
     * a scroll pane; that only works if this reports its true natural height instead of
     * a hardcoded one.
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension natural = super.getPreferredSize();
        return new Dimension(WIDTH, natural.height);
    }

    /** Toggled by the view-switcher toolbar — placement only works outside Perspective. */
    public void setPlacementEnabled(boolean enabled) {
        for (JLabel row : placementRows) {
            row.setForeground(enabled ? Theme.TEXT : Theme.MUTED);
        }
    }

    private JComponent projectNameRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Theme.BG_PANEL);
        row.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(12, 14, 12, 14)));
        row.setMaximumSize(new Dimension(WIDTH, 46));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        projectNameField.setFont(new Font("SansSerif", Font.BOLD, 14));
        projectNameField.setForeground(Theme.TEXT);
        projectNameField.setBackground(Theme.BG_PANEL);
        projectNameField.setCaretColor(Theme.TEXT);
        projectNameField.setBorder(null);
        row.add(projectNameField, BorderLayout.CENTER);
        return row;
    }

    /** The project name shown at the top of the sidebar — used to suggest a save filename. */
    public String getProjectName() {
        return projectNameField.getText();
    }

    /** Sets the project name field — used when a saved scene is loaded back in. */
    public void setProjectName(String name) {
        projectNameField.setText(name == null || name.isBlank() ? "Untitled Scene" : name);
    }

    // ---- Geometry ----

    private JComponent geometrySection() {
        return section("Geometry",
                prefabRow("Cube", pos -> getModel3D("OBJS/Cube.obj", pos, DEFAULT_OBJECT_COLOR, 1.0, new Vector3D(0, 0, 0))),
                prefabRow("Sphere", pos -> new Sphere(pos, 1.0, DEFAULT_OBJECT_COLOR)),
                prefabRow("Plane", pos -> new Plane(pos.getY(), DEFAULT_OBJECT_COLOR)),
                prefabRow("Pyramid", pos -> Primitives.getPyramid(pos, DEFAULT_OBJECT_COLOR)),
                prefabRow("Torus", pos -> getModel3D("OBJS/Ring.obj", pos, DEFAULT_OBJECT_COLOR, 1.0, new Vector3D(0, 0, 0))));
    }

    // ---- Cameras ----

    private JComponent camerasSection() {
        return section("Cameras",
                prefabRow("Perspective", pos -> new PerspertiveCamera(pos, 0, 0, 0.1, 400, 60)),
                prefabRow("Orthographic", pos -> new OrthographicCamera(pos, -5, 5, 5, -5, 0.1, 400)),
                prefabRow("Fisheye", pos -> new FisheyeCamera(pos, 0, 0, 0.1, 400, 180)));
    }

    // ---- Lights ----

    private JComponent lightsSection() {
        return section("Lights",
                // Point/SpotLight intensity is divided by distance-squared (see
                // PointLight#getColor) — 15 keeps a light a handful of units away from
                // whatever it's lighting well-exposed without blowing out highlights if
                // it ends up placed fairly close.
                prefabRow("Point", pos -> new PointLight(pos, Color.WHITE, 15)),
                prefabRow("Directional", pos -> {
                    DirectionalLight light = new DirectionalLight(new Vector3D(0.3, -1, 0.3), Color.WHITE, 1.0);
                    light.setPosition(pos);
                    return light;
                }),
                prefabRow("Spot", pos -> new SpotLight(pos, new Vector3D(0, -1, 0), Color.WHITE, 15, 30)));
    }

    // ---- Import OBJ ----

    private JComponent importSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BG_PANEL);
        panel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(12, 14, 12, 14)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(WIDTH, 300));

        JLabel title = new JLabel("Import OBJ");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton browse = new JButton("Browse…");
        browse.setAlignmentX(Component.LEFT_ALIGNMENT);
        browse.setFocusPainted(false);
        browse.addActionListener(e -> browseForFile());

        importListPanel.setLayout(new BoxLayout(importListPanel, BoxLayout.Y_AXIS));
        importListPanel.setOpaque(false);
        importListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (File obj : bundledModels()) {
            importListPanel.add(fileRow(obj));
        }

        JScrollPane scroll = new JScrollPane(importListPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setMaximumSize(new Dimension(WIDTH, 180));
        scroll.setPreferredSize(new Dimension(WIDTH, 180));

        panel.add(title);
        panel.add(Box.createVerticalStrut(8));
        panel.add(browse);
        panel.add(Box.createVerticalStrut(8));
        panel.add(scroll);
        return panel;
    }

    private File[] bundledModels() {
        File dir = new File("OBJS");
        // Ring.obj is claimed by the "Torus" geometry button above — hide it here so it
        // doesn't show up twice under two different names.
        File[] files = dir.listFiles((d, name) ->
                name.toLowerCase().endsWith(".obj") && !name.equalsIgnoreCase("Ring.obj"));
        if (files == null) return new File[0];
        java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));
        return files;
    }

    private void browseForFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Wavefront OBJ (*.obj)", "obj"));
        chooser.setCurrentDirectory(new File("OBJS").exists() ? new File("OBJS") : new File("."));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File chosen = chooser.getSelectedFile();
        importListPanel.add(fileRow(chosen));
        importListPanel.revalidate();
        importListPanel.repaint();
    }

    private JComponent fileRow(File file) {
        return prefabRow(file.getName(),
                pos -> getModel3D(file.getPath(), pos, DEFAULT_OBJECT_COLOR, 1.0, new Vector3D(0, 0, 0)));
    }

    // ---- Render settings (the trigger itself — name, progress, Render/Cancel button —
    // lives in EditorWindow's bottom bar; this is just "how", not "go") ----

    private JComponent renderSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BG_PANEL);
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Render");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setBorder(new EmptyBorder(0, 0, 6, 0));

        cameraCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                String text = value instanceof Camera camera ? camera.getName() : "No cameras yet";
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });

        modeCombo.setToolTipText("<html>Forces every surface to fully reflect/refract (Both = both at once),<br>"
                + "for a quick preview. For normal use, set reflectivity/transparency on<br>"
                + "each object's own Material instead (try the Mirror/Glass presets there —<br>"
                + "a material can already have both set at the same time) and leave this on None.</html>");

        samplesCombo.setToolTipText("<html>Anti-aliasing: traces this many rays per pixel (in a grid) and averages<br>"
                + "them, smoothing jagged edges — at the cost of that many times the render time.<br>"
                + "1 = off (fastest).</html>");

        panel.add(title);
        panel.add(labeledCombo("Camera", cameraCombo));
        panel.add(labeledCombo("Resolution", resolutionCombo));
        panel.add(labeledCombo("Aspect", aspectRatioCombo));
        panel.add(labeledCombo("Mode", modeCombo));
        panel.add(labeledCombo("Samples", samplesCombo));
        return panel;
    }

    private JComponent labeledCombo(String labelText, JComboBox<?> combo) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        label.setForeground(Theme.MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo.setMaximumSize(new Dimension(WIDTH - 28, 24));

        row.add(label);
        row.add(combo);
        return row;
    }

    /**
     * Called whenever a camera is added to the scene (also used by {@link EditorWindow}
     * after a scene load/clear, since that replaces the camera list wholesale) so the
     * render camera picker stays current.
     */
    public void refreshCameraList() {
        Camera previous = getSelectedRenderCamera();
        cameraCombo.removeAllItems();
        for (Camera camera : scene.getCameras()) {
            cameraCombo.addItem(camera);
        }
        if (previous != null && scene.getCameras().contains(previous)) {
            cameraCombo.setSelectedItem(previous);
        } else if (scene.getActiveCamera() != null) {
            cameraCombo.setSelectedItem(scene.getActiveCamera());
        }
    }

    /** The camera chosen in the Render section, or {@code null} if the scene has none yet. */
    public Camera getSelectedRenderCamera() {
        Object item = cameraCombo.getSelectedItem();
        return item instanceof Camera camera ? camera : null;
    }

    public int getSelectedResolution() {
        Object item = resolutionCombo.getSelectedItem();
        return item instanceof Integer resolution ? resolution : 400;
    }

    public double getSelectedAspectRatio() {
        Object item = aspectRatioCombo.getSelectedItem();
        String[] parts = (item == null ? "1/1" : item.toString()).split("/");
        return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
    }

    public RenderMode getSelectedRenderMode() {
        Object item = modeCombo.getSelectedItem();
        return item instanceof RenderMode mode ? mode : RenderMode.NONE;
    }

    /** Anti-aliasing samples per pixel chosen in the Render section (1 = off). */
    public int getSelectedSamples() {
        Object item = samplesCombo.getSelectedItem();
        return item instanceof Integer samples ? samples : 1;
    }

    // ---- Shared row plumbing: click-to-add, drag-to-add ----

    private JComponent section(String title, JComponent... rows) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BG_PANEL);
        panel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(12, 14, 12, 14)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLabel.setForeground(Theme.TEXT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Same right-padding reasoning as prefabRow: zero slack lets Nimbus's paint-time
        // glyph measurement clip the last character on a section whose title happens to
        // be the widest thing in it.
        titleLabel.setBorder(new EmptyBorder(0, 0, 6, 6));
        panel.add(titleLabel);

        for (JComponent row : rows) panel.add(row);
        return panel;
    }

    private JLabel prefabRow(String label, ScenePrefab prefab) {
        JLabel row = new JLabel(label);
        row.setFont(new Font("SansSerif", Font.PLAIN, 12));
        row.setForeground(Theme.TEXT);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Right padding matters here, not just cosmetically: Nimbus measures a label's
        // glyphs at paint time slightly differently than the FontMetrics call that sized
        // it, and with zero slack that mismatch clips the row's last character (seen on
        // "Orthographic", "Directional", etc. — confirmed by a pixel-level render check).
        row.setBorder(new EmptyBorder(3, 0, 3, 6));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setToolTipText("Click to add at the origin, or drag it onto the viewport");
        placementRows.add(row);

        MouseAdapter gesture = new MouseAdapter() {
            private boolean dragged;

            @Override
            public void mousePressed(MouseEvent e) {
                dragged = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                dragged = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (viewport.getView() == ViewportView.PERSPECTIVE) return;
                if (dragged) {
                    Point local = pointInside(viewport, e.getLocationOnScreen());
                    if (local == null) return; // released outside the viewport: cancel
                    Vector3D world = viewport.screenToWorld(local);
                    if (world != null) addToScene(prefab.create(world));
                } else {
                    addToScene(prefab.create(new Vector3D(0, 0, 0)));
                }
            }
        };
        row.addMouseListener(gesture);
        row.addMouseMotionListener(gesture);
        return row;
    }

    private JLabel disabledRow(String label, String tooltip) {
        JLabel row = new JLabel(label);
        row.setFont(new Font("SansSerif", Font.PLAIN, 12));
        row.setForeground(Theme.MUTED);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Right padding matters here, not just cosmetically: Nimbus measures a label's
        // glyphs at paint time slightly differently than the FontMetrics call that sized
        // it, and with zero slack that mismatch clips the row's last character (seen on
        // "Orthographic", "Directional", etc. — confirmed by a pixel-level render check).
        row.setBorder(new EmptyBorder(3, 0, 3, 6));
        row.setToolTipText(tooltip);
        return row;
    }

    /** The screen point translated into {@code target}'s local coordinates, or {@code null} if outside it. */
    private Point pointInside(Component target, Point screenPoint) {
        Point origin = target.getLocationOnScreen();
        Point local = new Point(screenPoint.x - origin.x, screenPoint.y - origin.y);
        if (local.x < 0 || local.y < 0 || local.x > target.getWidth() || local.y > target.getHeight()) {
            return null;
        }
        return local;
    }

    private void addToScene(Object item) {
        if (item == null) return;
        SceneProperties.addToScene(scene, item);
        if (item instanceof Camera) refreshCameraList();
        viewport.select(item);
        viewport.repaint();
    }
}
