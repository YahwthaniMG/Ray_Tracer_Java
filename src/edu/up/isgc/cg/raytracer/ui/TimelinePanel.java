package edu.up.isgc.cg.raytracer.ui;

import edu.up.isgc.cg.raytracer.Scene;
import edu.up.isgc.cg.raytracer.animation.AnimationTrack;
import edu.up.isgc.cg.raytracer.animation.Keyframe;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * The animation timeline: scrub a time slider to preview interpolated keyframes live in
 * the viewport, record/delete a keyframe for whatever's currently selected at the current
 * time, and kick off a full frame-by-frame render of the animation (see
 * {@code edu.up.isgc.cg.raytracer.AnimationRenderer}). Sits directly under the viewport in
 * {@link EditorWindow}, above the single-frame render bar.
 *
 * <p>The {@link JSlider} tracks time in fixed 50ms steps (20 per second) — independent of
 * the export FPS in {@link Scene#getAnimation()}, which only matters when actually
 * rendering frames — so scrubbing/playback stays smooth regardless of what FPS is set for
 * the final export.</p>
 *
 * @author Claude (Anthropic)
 */
public class TimelinePanel extends JPanel {

    private static final int MILLIS_PER_UNIT = 50;

    private final Scene scene;
    private final ViewportPanel viewport;

    private final JSlider timeSlider = new JSlider(0, 100, 0);
    private final KeyframeMarks keyframeMarks = new KeyframeMarks();
    private final JLabel timeLabel = new JLabel();
    private final JSpinner durationField;
    private final JSpinner fpsField;
    private final JButton playButton = new JButton("▶");
    private final JButton setKeyframeButton = new JButton("Set Keyframe");
    private final JButton deleteKeyframeButton = new JButton("Delete Keyframe");
    private final JButton renderAnimationButton = new JButton("Render Animation…");

    private Timer playTimer;
    private Runnable onTimeChanged;
    private Runnable onRenderAnimationRequested;
    private boolean updatingFromModel;

    /**
     * Instantiates a new Timeline panel.
     *
     * @param scene    the scene
     * @param viewport the viewport (repainted on scrub, and asked what's selected for
     *                 Set/Delete Keyframe)
     */
    public TimelinePanel(Scene scene, ViewportPanel viewport) {
        this.scene = scene;
        this.viewport = viewport;

        setLayout(new BorderLayout(0, 4));
        setBackground(Theme.BG_TOOLBAR);
        setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(6, 14, 6, 14)));

        durationField = durationSpinner(scene.getAnimation().getDurationSeconds());
        fpsField = fpsSpinner(scene.getAnimation().getFps());

        durationField.addChangeListener(e -> {
            if (updatingFromModel) return;
            scene.getAnimation().setDurationSeconds(((Number) durationField.getValue()).doubleValue());
            refreshSliderRange();
        });
        fpsField.addChangeListener(e -> {
            if (updatingFromModel) return;
            scene.getAnimation().setFps(((Number) fpsField.getValue()).intValue());
        });

        timeSlider.setOpaque(false);
        timeSlider.addChangeListener(e -> {
            if (updatingFromModel) return;
            applyCurrentTime();
        });

        timeLabel.setForeground(Theme.MUTED);
        timeLabel.setPreferredSize(new Dimension(90, 20));

        playButton.setFocusPainted(false);
        playButton.addActionListener(e -> togglePlay());

        setKeyframeButton.setFocusPainted(false);
        setKeyframeButton.addActionListener(e -> onSetKeyframeClicked());

        deleteKeyframeButton.setFocusPainted(false);
        deleteKeyframeButton.addActionListener(e -> onDeleteKeyframeClicked());

        renderAnimationButton.setFocusPainted(false);
        renderAnimationButton.addActionListener(e -> {
            if (onRenderAnimationRequested != null) onRenderAnimationRequested.run();
        });

        // Two rows rather than one three-region BorderLayout: at the app's minimum window
        // width there isn't room for duration/FPS + the slider + three buttons side by
        // side — BorderLayout would just let WEST/EAST overlap CENTER instead of wrapping,
        // which actually happened here before this was split (garbled, overlapping text).
        // A row of its own guarantees the slider — the one control that actually needs
        // the width — gets it.
        JPanel sliderStack = new JPanel(new BorderLayout());
        sliderStack.setOpaque(false);
        sliderStack.add(timeSlider, BorderLayout.CENTER);
        sliderStack.add(keyframeMarks, BorderLayout.SOUTH);

        JPanel scrubRow = new JPanel(new BorderLayout(8, 0));
        scrubRow.setOpaque(false);
        scrubRow.add(playButton, BorderLayout.WEST);
        scrubRow.add(sliderStack, BorderLayout.CENTER);
        scrubRow.add(timeLabel, BorderLayout.EAST);

        JPanel controlsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        controlsRow.setOpaque(false);
        controlsRow.add(smallLabel("Duration(s)"));
        controlsRow.add(durationField);
        controlsRow.add(smallLabel("FPS"));
        controlsRow.add(fpsField);
        controlsRow.add(Box.createHorizontalStrut(12));
        controlsRow.add(setKeyframeButton);
        controlsRow.add(deleteKeyframeButton);
        controlsRow.add(renderAnimationButton);

        // A plain FlowLayout's *preferred size* always assumes one row (it only actually
        // wraps during layout, once it's already been squeezed into a narrower width by
        // its parent) — nested directly in a BorderLayout that mismatch would starve it of
        // the extra height it needs once it wraps, silently pushing whatever wrapped below
        // the panel's visible bounds. A horizontal scrollbar sidesteps that entirely: this
        // row keeps a fixed height regardless of width, and at the app's minimum window
        // size the least-frequently-needed buttons just scroll into view instead.
        JScrollPane controlsScroll = new JScrollPane(controlsRow,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        controlsScroll.setBorder(null);
        controlsScroll.setOpaque(false);
        controlsScroll.getViewport().setOpaque(false);
        controlsScroll.setPreferredSize(new Dimension(10, controlsRow.getPreferredSize().height + 18));

        add(scrubRow, BorderLayout.NORTH);
        add(controlsScroll, BorderLayout.SOUTH);

        refreshSliderRange();
    }

    /** Called after each scrub/tick so the Inspector can re-read the (possibly now-animated) selection. */
    public void setOnTimeChanged(Runnable onTimeChanged) {
        this.onTimeChanged = onTimeChanged;
    }

    /** Called when "Render Animation…" is clicked — the caller owns the actual render/export flow. */
    public void setOnRenderAnimationRequested(Runnable onRenderAnimationRequested) {
        this.onRenderAnimationRequested = onRenderAnimationRequested;
    }

    /**
     * Repaints the little dots under the slider showing where the currently selected
     * item has keyframes — call whenever the viewport's selection changes, since which
     * item's keyframes to show depends on that.
     */
    public void refreshKeyframeMarks() {
        keyframeMarks.repaint();
    }

    /**
     * Toggles the "Render Animation…" button to "Cancel Animation" and disables the rest
     * of the timeline's controls while a batch render is in progress — a batch render
     * reads every track's keyframes frame by frame over what can be a long stretch of
     * wall-clock time, so scrubbing/editing keyframes out from under it mid-render would
     * be a real (if narrow) way to get a torn/inconsistent result.
     *
     * @param active whether an animation render is currently running
     */
    public void setAnimationRenderActive(boolean active) {
        renderAnimationButton.setText(active ? "Cancel Animation" : "Render Animation…");
        timeSlider.setEnabled(!active);
        playButton.setEnabled(!active);
        durationField.setEnabled(!active);
        fpsField.setEnabled(!active);
        setKeyframeButton.setEnabled(!active);
        deleteKeyframeButton.setEnabled(!active);
        if (active) stopPlaying();
    }

    /** The time the slider is currently at, in seconds. */
    public double getCurrentTimeSeconds() {
        return timeSlider.getValue() * MILLIS_PER_UNIT / 1000.0;
    }

    /**
     * Re-syncs the duration/FPS fields and slider to {@link Scene#getAnimation()} and
     * resets to time 0 — call after the scene's contents are wholesale replaced (New/Load).
     */
    public void refreshFromScene() {
        stopPlaying();
        updatingFromModel = true;
        try {
            durationField.setValue(scene.getAnimation().getDurationSeconds());
            fpsField.setValue(scene.getAnimation().getFps());
            refreshSliderRange();
            timeSlider.setValue(0);
        } finally {
            updatingFromModel = false;
        }
        applyCurrentTime();
    }

    private void refreshSliderRange() {
        double duration = scene.getAnimation().getDurationSeconds();
        int max = Math.max(1, (int) Math.round(duration * 1000 / MILLIS_PER_UNIT));
        boolean wasUpdating = updatingFromModel;
        updatingFromModel = true;
        try {
            timeSlider.setMaximum(max);
            if (timeSlider.getValue() > max) timeSlider.setValue(max);
        } finally {
            updatingFromModel = wasUpdating;
        }
        updateTimeLabel();
        keyframeMarks.repaint(); // the duration changing shifts every mark's fractional position
    }

    private void applyCurrentTime() {
        scene.getAnimation().applyAt(getCurrentTimeSeconds());
        updateTimeLabel();
        viewport.repaint();
        if (onTimeChanged != null) onTimeChanged.run();
    }

    private void updateTimeLabel() {
        timeLabel.setText(String.format("%.2fs / %.2fs", getCurrentTimeSeconds(), scene.getAnimation().getDurationSeconds()));
    }

    private void togglePlay() {
        if (playTimer != null && playTimer.isRunning()) {
            stopPlaying();
            return;
        }
        playButton.setText("⏸");
        playTimer = new Timer(MILLIS_PER_UNIT, e -> {
            int next = timeSlider.getValue() + 1;
            if (next > timeSlider.getMaximum()) next = 0; // loop back to the start
            timeSlider.setValue(next);
        });
        playTimer.start();
    }

    private void stopPlaying() {
        if (playTimer != null) {
            playTimer.stop();
            playTimer = null;
        }
        playButton.setText("▶");
    }

    private void onSetKeyframeClicked() {
        Object selected = viewport.getSelected();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select an object, camera or light first.",
                    "Nothing selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        scene.getAnimation().trackFor(selected).setKeyframe(getCurrentTimeSeconds());
        keyframeMarks.repaint();
    }

    private void onDeleteKeyframeClicked() {
        Object selected = viewport.getSelected();
        if (selected == null) return;
        AnimationTrack track = scene.getAnimation().getTrack(selected);
        if (track == null) return;
        double toleranceSeconds = (MILLIS_PER_UNIT / 2.0) / 1000.0;
        if (track.removeKeyframeNear(getCurrentTimeSeconds(), toleranceSeconds)) {
            applyCurrentTime(); // removing it may change what's interpolated at this instant
            keyframeMarks.repaint();
        }
    }

    private static JLabel smallLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        label.setForeground(Theme.MUTED);
        return label;
    }

    private static JSpinner durationSpinner(double initial) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(initial, 0.5, 600.0, 0.5));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0.0"));
        sizeSpinner(spinner);
        return spinner;
    }

    private static JSpinner fpsSpinner(int initial) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(initial, 1, 120, 1));
        sizeSpinner(spinner);
        return spinner;
    }

    private static void sizeSpinner(JSpinner spinner) {
        Dimension size = new Dimension(64, 24);
        spinner.setPreferredSize(size);
        spinner.setMaximumSize(size);
    }

    /**
     * A thin strip painted directly under the slider showing a dot for each keyframe the
     * currently selected item has, so "Set Keyframe" leaves a visible trace behind
     * instead of only being knowable by scrubbing around and watching values jump.
     */
    private final class KeyframeMarks extends JComponent {
        KeyframeMarks() {
            setOpaque(false);
            setPreferredSize(new Dimension(10, 8));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Object selected = viewport.getSelected();
            if (selected == null) return;
            AnimationTrack track = scene.getAnimation().getTrack(selected);
            if (track == null || track.isEmpty()) return;

            double duration = scene.getAnimation().getDurationSeconds();
            if (duration <= 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.ACCENT);
            int width = getWidth();
            for (Keyframe keyframe : track.getKeyframes()) {
                double fraction = Math.max(0, Math.min(1, keyframe.getTime() / duration));
                int x = (int) Math.round(fraction * (width - 1));
                x = Math.max(3, Math.min(width - 4, x)); // keep a keyframe right at 0 or the end from being clipped in half
                g2.fillOval(x - 3, 1, 6, 6);
            }
            g2.dispose();
        }
    }
}
