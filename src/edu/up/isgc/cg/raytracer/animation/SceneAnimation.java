package edu.up.isgc.cg.raytracer.animation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every {@link AnimationTrack} in a scene, keyed by the object/camera/light it animates,
 * plus the overall duration/fps used when rendering it out as a frame sequence. Lives as
 * a field on {@code Scene} (like {@code objects}/{@code cameras}/{@code lights}) so it
 * saves and loads with the rest of the scene — the raytracer itself never reads this; it
 * only matters to whoever is scrubbing the timeline or batch-rendering frames (see
 * {@code edu.up.isgc.cg.raytracer.AnimationRenderer}), both of which just push the
 * interpolated state into the scene's live objects before rendering/repainting.
 *
 * @author Claude (Anthropic)
 */
public class SceneAnimation {

    private final Map<Object, AnimationTrack> tracks = new LinkedHashMap<>();
    private double durationSeconds = 5.0;
    private int fps = 24;

    /** Gets the track for {@code target}, creating an empty one if it doesn't have one yet. */
    public AnimationTrack trackFor(Object target) {
        return tracks.computeIfAbsent(target, AnimationTrack::new);
    }

    /** Gets the track for {@code target}, or {@code null} if it has none. */
    public AnimationTrack getTrack(Object target) {
        return tracks.get(target);
    }

    public Collection<AnimationTrack> getTracks() {
        return tracks.values();
    }

    /** Drops {@code target}'s track entirely — call when the item itself is deleted from the scene. */
    public void removeTrackFor(Object target) {
        tracks.remove(target);
    }

    public boolean hasAnyKeyframes() {
        return tracks.values().stream().anyMatch(track -> !track.isEmpty());
    }

    /** Applies every track's interpolated state at {@code time} onto its target. */
    public void applyAt(double time) {
        for (AnimationTrack track : tracks.values()) track.applyAt(time);
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = Math.max(0.1, durationSeconds);
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = Math.max(1, fps);
    }

    /** Removes every track and resets duration/fps to their defaults. */
    public void clear() {
        tracks.clear();
        durationSeconds = 5.0;
        fps = 24;
    }
}
