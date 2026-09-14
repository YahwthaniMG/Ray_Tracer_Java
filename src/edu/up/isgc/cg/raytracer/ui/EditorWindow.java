package edu.up.isgc.cg.raytracer.ui;

import edu.up.isgc.cg.raytracer.AnimationRenderer;
import edu.up.isgc.cg.raytracer.RenderController;
import edu.up.isgc.cg.raytracer.RenderMode;
import edu.up.isgc.cg.raytracer.Scene;
import edu.up.isgc.cg.raytracer.cameras.Camera;
import edu.up.isgc.cg.raytracer.math.Vector3D;
import edu.up.isgc.cg.raytracer.tools.SceneIO;
import edu.up.isgc.cg.raytracer.tools.SceneProperties;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * The scene editor window: sidebar (project name + geometry/camera/light/import/render
 * palettes), a viewport with a Top/Front/Side/Perspective switcher showing the current
 * scene's objects/cameras/lights as shaded solids, and a render bar at the bottom that
 * runs the real {@link RenderController} on a background thread — see
 * {@link #startRender()} — and saves the result under {@code Renders/}.
 *
 * @author Claude (Anthropic)
 */
public class EditorWindow extends JFrame {

    private final Scene scene;
    private final ViewportPanel viewport;
    private final InspectorPanel inspector = new InspectorPanel();
    private final SidebarPanel sidebar;
    private final TimelinePanel timeline;

    private JTextField renderNameField;
    private JProgressBar renderProgress;
    private JButton renderButton;
    private SwingWorker<BufferedImage, Integer> activeRender;
    private SwingWorker<AnimationRenderResult, Integer> activeAnimationRender;

    // Single-step undo for an accidental drag in the viewport — deliberately not a full
    // undo stack (nothing else here is undoable yet): just enough to recover from "oops,
    // I bumped that sphere" via Ctrl+Z, which is what actually happens in practice.
    private Object undoTarget;
    private Vector3D undoPosition;

    /** What a batch animation render produced — see {@link #startAnimationRender()}. */
    private record AnimationRenderResult(boolean cancelled, File output, boolean isVideo) {}

    public EditorWindow() {
        this(new Scene());
    }

    public EditorWindow(Scene scene) {
        super("Ray Tracer - Editor");
        this.scene = scene;
        this.viewport = new ViewportPanel(scene);
        this.sidebar = new SidebarPanel(scene, viewport);
        this.timeline = new TimelinePanel(scene, viewport);
        sidebar.setPlacementEnabled(viewport.getView() != ViewportView.PERSPECTIVE);
        viewport.setOnSelectionChanged(item -> {
            inspector.setSelected(item);
            timeline.refreshKeyframeMarks();
        });
        inspector.setOnChange(viewport::repaint);
        inspector.setOnDeleteRequested(this::onDeleteSelected);
        viewport.setOnObjectMoved(this::recordMoveForUndo);
        timeline.setOnTimeChanged(() -> inspector.setSelected(viewport.getSelected()));
        timeline.setOnRenderAnimationRequested(this::onRenderAnimationButtonClicked);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCloseRequested();
            }
        });
        setSize(1100, 760);
        setMinimumSize(new Dimension(860, 600));
        setLocationRelativeTo(null);
        setJMenuBar(buildMenuBar());
        setContentPane(buildContent());
        installUndoShortcut();
    }

    /** Closing via the window's own X previously exited immediately, discarding any unsaved work with no warning. */
    private void onCloseRequested() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Close the editor? Any unsaved changes will be lost.",
                "Close", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        if (activeRender != null) activeRender.cancel(true);
        if (activeAnimationRender != null) activeAnimationRender.cancel(true);
        dispose();
        System.exit(0);
    }

    /**
     * Ctrl+Z (Cmd+Z on macOS) undoes the last drag-to-move in the viewport — see
     * {@link #recordMoveForUndo}. Bound at the root pane / {@code WHEN_IN_FOCUSED_WINDOW}
     * so it fires no matter which component happens to have keyboard focus.
     */
    private void installUndoShortcut() {
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        KeyStroke undoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMask);
        JRootPane root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(undoKeyStroke, "undoLastMove");
        root.getActionMap().put("undoLastMove", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undoLastMove();
            }
        });
    }

    /** Remembers where {@code target} was right before a drag that just moved it — see {@link #undoLastMove()}. */
    private void recordMoveForUndo(Object target, Vector3D previousPosition) {
        undoTarget = target;
        undoPosition = previousPosition;
    }

    private void undoLastMove() {
        if (undoTarget == null || undoPosition == null) return;
        Object target = undoTarget;
        SceneProperties.setPosition(target, undoPosition);
        undoTarget = null;
        undoPosition = null;
        viewport.repaint();
        if (viewport.getSelected() == target) inspector.setSelected(target);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        styleMenuText(fileMenu);

        JMenuItem newItem = new JMenuItem("New Scene");
        newItem.addActionListener(e -> onNewScene());

        JMenuItem saveItem = new JMenuItem("Save Scene…");
        saveItem.addActionListener(e -> onSaveScene());

        JMenuItem loadItem = new JMenuItem("Load Scene…");
        loadItem.addActionListener(e -> onLoadScene());
        for (JMenuItem item : new JMenuItem[]{newItem, saveItem, loadItem}) styleMenuText(item);

        fileMenu.add(newItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        menuBar.add(fileMenu);
        return menuBar;
    }

    /**
     * Nimbus's menu popups paint their background through the dark Painters registered in
     * {@link RayTracerApp}, but the text color they install on each item is a separate,
     * near-black {@code ColorUIResource} that a plain {@code UIManager.put} override
     * doesn't reach (Synth resolves it through a different path than the background). A
     * client-set, non-UIResource color on the instance itself takes precedence over that,
     * so this is set directly here instead of fought over in UIManager defaults.
     */
    private void styleMenuText(JMenuItem item) {
        item.setForeground(Theme.TEXT);
    }

    private void onNewScene() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Clear the current scene? Unsaved changes will be lost.",
                "New Scene", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        scene.clear();
        sidebar.setProjectName("Untitled Scene");
        afterSceneReplaced();
    }

    private void onSaveScene() {
        String projectName = sidebar.getProjectName();
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Scene (*.json)", "json"));
        File scenesDir = new File("Scenes");
        chooser.setCurrentDirectory(scenesDir.exists() ? scenesDir : new File("."));
        chooser.setSelectedFile(new File(scenesDir, sanitizeFileName(projectName) + ".json"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".json")) {
            file = new File(file.getParentFile(), file.getName() + ".json");
        }
        try {
            SceneIO.save(scene, projectName, file);
            JOptionPane.showMessageDialog(this, "Saved to " + file.getPath(),
                    "Scene saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException failure) {
            JOptionPane.showMessageDialog(this, "Couldn't save the scene: " + failure.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onLoadScene() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Scene (*.json)", "json"));
        File scenesDir = new File("Scenes");
        chooser.setCurrentDirectory(scenesDir.exists() ? scenesDir : new File("."));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            String projectName = SceneIO.loadInto(scene, chooser.getSelectedFile());
            sidebar.setProjectName(projectName);
            afterSceneReplaced();
        } catch (IOException | RuntimeException failure) {
            JOptionPane.showMessageDialog(this, "Couldn't load the scene: " + failure.getMessage(),
                    "Load failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** After the scene's contents are wholesale replaced (New/Load), refresh everything that caches them. */
    private void afterSceneReplaced() {
        viewport.select(null);
        sidebar.refreshCameraList();
        timeline.refreshFromScene();
        undoTarget = null;
        undoPosition = null;
        viewport.repaint();
    }

    private JComponent buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(scrollable(sidebar), BorderLayout.WEST);
        root.add(scrollable(inspector), BorderLayout.EAST);

        JPanel centerColumn = new JPanel(new BorderLayout());
        centerColumn.add(buildViewToolbar(), BorderLayout.NORTH);
        centerColumn.add(viewport, BorderLayout.CENTER);
        centerColumn.add(timeline, BorderLayout.SOUTH);
        root.add(centerColumn, BorderLayout.CENTER);

        root.add(buildRenderBar(), BorderLayout.SOUTH);
        return root;
    }

    /** The sidebar (with the Render section) can easily be taller than the window. */
    private JComponent scrollable(JComponent content) {
        JScrollPane scroll = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JComponent buildViewToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bar.setBackground(Theme.BG_TOOLBAR);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER));

        JLabel hint = new JLabel(viewport.getView().getAxesHint());
        hint.setForeground(Theme.MUTED);
        hint.setBorder(new EmptyBorder(0, 14, 0, 0));

        ButtonGroup group = new ButtonGroup();
        for (ViewportView candidate : ViewportView.values()) {
            JToggleButton button = new JToggleButton(candidate.getLabel());
            button.setSelected(candidate == viewport.getView());
            button.setFocusPainted(false);
            button.addActionListener(e -> {
                viewport.setView(candidate);
                hint.setText(candidate.getAxesHint());
                sidebar.setPlacementEnabled(candidate != ViewportView.PERSPECTIVE);
            });
            group.add(button);
            bar.add(button);
        }
        bar.add(hint);
        return bar;
    }

    private JComponent buildRenderBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(Theme.BG_TOOLBAR);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(8, 14, 8, 14)));

        JLabel nameLabel = new JLabel("Render name:");
        nameLabel.setForeground(Theme.TEXT);

        renderNameField = new JTextField("render_01");
        renderNameField.setPreferredSize(new Dimension(160, 26));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(nameLabel);
        left.add(renderNameField);

        renderProgress = new JProgressBar(0, 100);
        renderProgress.setValue(0);
        renderProgress.setStringPainted(true);
        renderProgress.setString("Idle");

        renderButton = new JButton("Render");
        renderButton.addActionListener(e -> onRenderButtonClicked());

        bar.add(left, BorderLayout.WEST);
        bar.add(renderProgress, BorderLayout.CENTER);
        bar.add(renderButton, BorderLayout.EAST);
        return bar;
    }

    private void onDeleteSelected() {
        Object item = viewport.getSelected();
        if (item == null) return;
        SceneProperties.removeFromScene(scene, item);
        scene.getAnimation().removeTrackFor(item);
        if (undoTarget == item) { // undoing a move on something no longer in the scene would do nothing visible
            undoTarget = null;
            undoPosition = null;
        }
        // A lighter refresh than afterSceneReplaced(): a single deletion shouldn't reset
        // the timeline's scrub position back to 0, only a wholesale New/Load should.
        viewport.select(null);
        sidebar.refreshCameraList();
        viewport.repaint();
    }

    private void onRenderButtonClicked() {
        if (activeRender != null && !activeRender.isDone()) {
            activeRender.cancel(true);
            return;
        }
        if (activeAnimationRender != null && !activeAnimationRender.isDone()) {
            JOptionPane.showMessageDialog(this, "An animation render is already in progress.",
                    "Busy", JOptionPane.WARNING_MESSAGE);
            return;
        }
        startRender();
    }

    /**
     * Runs {@link RenderController#render} on a background thread (a full render can
     * take anywhere from seconds to tens of minutes, and this window's own repaint —
     * dragging, view switches — has to keep working while it does), reporting progress
     * back to {@link #renderProgress} and saving the finished image under {@code Renders/}.
     */
    private void startRender() {
        Camera camera = sidebar.getSelectedRenderCamera();
        if (camera == null) {
            JOptionPane.showMessageDialog(this,
                    "Add a camera to the scene first (the Cameras section in the sidebar).",
                    "No camera to render with", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int resolution = sidebar.getSelectedResolution();
        double aspectRatio = sidebar.getSelectedAspectRatio();
        RenderMode mode = sidebar.getSelectedRenderMode();
        int samples = sidebar.getSelectedSamples();
        String fileName = sanitizeFileName(renderNameField.getText());

        renderButton.setText("Cancel");
        renderProgress.setValue(0);
        renderProgress.setString("Rendering…");

        activeRender = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() {
                return RenderController.render(scene, camera, resolution, aspectRatio, mode, samples,
                        fraction -> publish((int) Math.round(fraction * 100)));
            }

            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) renderProgress.setValue(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                renderButton.setText("Render");
                try {
                    BufferedImage image = get();
                    if (image == null) {
                        renderProgress.setValue(0);
                        renderProgress.setString("Cancelled");
                        return;
                    }
                    File file = new File("Renders", fileName + ".png");
                    ImageIO.write(image, "png", file);
                    renderProgress.setValue(100);
                    renderProgress.setString("Saved to " + file.getPath());
                } catch (CancellationException cancelled) {
                    renderProgress.setValue(0);
                    renderProgress.setString("Cancelled");
                } catch (InterruptedException | ExecutionException | IOException failure) {
                    renderProgress.setValue(0);
                    renderProgress.setString("Render failed: " + failure.getMessage());
                }
            }
        };
        activeRender.execute();
    }

    private void onRenderAnimationButtonClicked() {
        if (activeAnimationRender != null && !activeAnimationRender.isDone()) {
            activeAnimationRender.cancel(true);
            return;
        }
        if (activeRender != null && !activeRender.isDone()) {
            JOptionPane.showMessageDialog(this, "A single-frame render is already in progress.",
                    "Busy", JOptionPane.WARNING_MESSAGE);
            return;
        }
        startAnimationRender();
    }

    /**
     * Renders every frame of the scene's animation (see {@link AnimationRenderer}) on a
     * background thread, saving numbered PNGs under {@code Renders/<name>_frames/}, then
     * assembles them into an mp4 with {@code ffmpeg} if it's available on this machine —
     * otherwise leaves the frame sequence as the deliverable and reports the exact ffmpeg
     * command to run later. Restores the timeline to wherever it was before the render
     * started once it's done, since {@link AnimationRenderer#renderAnimation} scrubs the
     * scene's actual objects through every frame to render them.
     */
    private void startAnimationRender() {
        Camera camera = sidebar.getSelectedRenderCamera();
        if (camera == null) {
            JOptionPane.showMessageDialog(this,
                    "Add a camera to the scene first (the Cameras section in the sidebar).",
                    "No camera to render with", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!scene.getAnimation().hasAnyKeyframes()) {
            JOptionPane.showMessageDialog(this,
                    "Set at least one keyframe first: select something, move the timeline, then click \"Set Keyframe\".",
                    "No animation yet", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int resolution = sidebar.getSelectedResolution();
        double aspectRatio = sidebar.getSelectedAspectRatio();
        RenderMode mode = sidebar.getSelectedRenderMode();
        int samples = sidebar.getSelectedSamples();
        int fps = scene.getAnimation().getFps();
        int totalFrames = AnimationRenderer.frameCount(scene.getAnimation());
        double timeBeforeRender = timeline.getCurrentTimeSeconds();
        String baseName = sanitizeFileName(sidebar.getProjectName());
        File frameDir = new File("Renders", baseName + "_frames");

        timeline.setAnimationRenderActive(true);
        renderProgress.setValue(0);
        renderProgress.setString("Rendering frame 0/" + totalFrames + "…");

        activeAnimationRender = new SwingWorker<>() {
            @Override
            protected AnimationRenderResult doInBackground() throws Exception {
                AnimationRenderer.renderAnimation(scene, camera, resolution, aspectRatio, mode, samples,
                        scene.getAnimation(), frameDir, baseName,
                        fraction -> publish((int) Math.round(fraction * 100)));

                if (Thread.currentThread().isInterrupted()) {
                    return new AnimationRenderResult(true, null, false);
                }
                if (AnimationRenderer.isFfmpegAvailable()) {
                    File mp4 = new File("Renders", baseName + ".mp4");
                    AnimationRenderer.assembleVideo(frameDir, baseName, fps, mp4);
                    return new AnimationRenderResult(false, mp4, true);
                }
                return new AnimationRenderResult(false, frameDir, false);
            }

            @Override
            protected void process(List<Integer> chunks) {
                if (chunks.isEmpty()) return;
                int percent = chunks.get(chunks.size() - 1);
                renderProgress.setValue(percent);
                int doneFrames = Math.round(percent / 100f * totalFrames);
                renderProgress.setString("Rendering frame " + doneFrames + "/" + totalFrames + "…");
            }

            @Override
            protected void done() {
                timeline.setAnimationRenderActive(false);
                scene.getAnimation().applyAt(timeBeforeRender);
                viewport.repaint();
                inspector.setSelected(viewport.getSelected());
                try {
                    AnimationRenderResult result = get();
                    if (result.cancelled()) {
                        renderProgress.setValue(0);
                        renderProgress.setString("Cancelled");
                        return;
                    }
                    if (result.isVideo()) {
                        renderProgress.setValue(100);
                        renderProgress.setString("Video saved to " + result.output().getPath());
                    } else {
                        String hint = AnimationRenderer.ffmpegCommandHint(frameDir, baseName, fps, baseName + ".mp4");
                        renderProgress.setValue(100);
                        renderProgress.setString("Frames saved to " + result.output().getPath() + " (ffmpeg not found)");
                        JOptionPane.showMessageDialog(EditorWindow.this,
                                "Frames saved to " + result.output().getPath() + ".\n\n"
                                        + "ffmpeg wasn't found on this machine, so no .mp4 was made.\n"
                                        + "Install it and run this to make one later:\n\n" + hint,
                                "Frames saved", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (CancellationException cancelled) {
                    renderProgress.setValue(0);
                    renderProgress.setString("Cancelled");
                } catch (InterruptedException | ExecutionException failure) {
                    renderProgress.setValue(0);
                    renderProgress.setString("Animation render failed: " + failure.getMessage());
                }
            }
        };
        activeAnimationRender.execute();
    }

    private static String sanitizeFileName(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        String cleaned = trimmed.replaceAll("[\\\\/:*?\"<>|]", "_");
        return cleaned.isEmpty() ? "render" : cleaned;
    }
}
