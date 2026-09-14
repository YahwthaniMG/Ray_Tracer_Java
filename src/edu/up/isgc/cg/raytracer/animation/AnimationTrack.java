package edu.up.isgc.cg.raytracer.animation;

import edu.up.isgc.cg.raytracer.math.Vector3D;
import edu.up.isgc.cg.raytracer.tools.SceneProperties;

import java.awt.Color;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * One scene item's keyframes over time, and the interpolation between them.
 *
 * <p>Keyframes are kept in a {@link TreeMap} keyed by time, which gives
 * {@link TreeMap#floorEntry}/{@link TreeMap#ceilingEntry} — exactly what's needed to find
 * "the keyframe just before" and "just after" a given time — for free. Before the first
 * keyframe or after the last, {@link #applyAt} just holds that keyframe's value rather
 * than extrapolating; between two, it linearly interpolates each property the target
 * actually supports (see {@link SceneProperties}), leaving anything unsupported alone.</p>
 *
 * @author Claude (Anthropic)
 */
public class AnimationTrack {

    private final Object target;
    private final TreeMap<Double, Keyframe> keyframes = new TreeMap<>();

    /**
     * Instantiates a new Animation track.
     *
     * @param target the object/camera/light this track animates
     */
    public AnimationTrack(Object target) {
        this.target = target;
    }

    public Object getTarget() {
        return target;
    }

    public boolean isEmpty() {
        return keyframes.isEmpty();
    }

    public Collection<Keyframe> getKeyframes() {
        return keyframes.values();
    }

    /**
     * Snapshots {@link #getTarget()}'s current property values as a keyframe at
     * {@code time}, replacing whatever keyframe (if any) was already exactly there.
     *
     * @param time the time, in seconds
     * @return the keyframe just recorded
     */
    public Keyframe setKeyframe(double time) {
        Keyframe keyframe = new Keyframe(time);
        keyframe.setPosition(SceneProperties.getPosition(target));
        if (SceneProperties.supportsColor(target)) keyframe.setColor(SceneProperties.getColor(target));
        if (SceneProperties.supportsRotation(target)) keyframe.setRotationDegrees(SceneProperties.getRotationDegrees(target));
        if (SceneProperties.supportsScale(target)) keyframe.setScale(SceneProperties.getScale(target));
        if (SceneProperties.supportsAim(target)) keyframe.setAimDegrees(SceneProperties.getAimDegrees(target));
        if (SceneProperties.supportsIntensity(target)) keyframe.setIntensity(SceneProperties.getIntensity(target));
        keyframes.put(time, keyframe);
        return keyframe;
    }

    /** Adds an already-built keyframe (used when reloading a saved animation). */
    public void addKeyframe(Keyframe keyframe) {
        keyframes.put(keyframe.getTime(), keyframe);
    }

    /**
     * Removes whichever keyframe is closest to {@code time}, if one exists within
     * {@code toleranceSeconds} of it.
     *
     * @param time             the time, in seconds
     * @param toleranceSeconds how close a keyframe must be to count as "at" that time
     * @return true if a keyframe was removed
     */
    public boolean removeKeyframeNear(double time, double toleranceSeconds) {
        Double closest = null;
        double bestDelta = Double.MAX_VALUE;
        for (double candidate : keyframes.keySet()) {
            double delta = Math.abs(candidate - time);
            if (delta < bestDelta) {
                bestDelta = delta;
                closest = candidate;
            }
        }
        if (closest != null && bestDelta <= toleranceSeconds) {
            keyframes.remove(closest);
            return true;
        }
        return false;
    }

    /** Applies this track's interpolated state at {@code time} onto {@link #getTarget()}. */
    public void applyAt(double time) {
        if (keyframes.isEmpty()) return;

        Map.Entry<Double, Keyframe> floor = keyframes.floorEntry(time);
        Map.Entry<Double, Keyframe> ceiling = keyframes.ceilingEntry(time);
        Keyframe resolved;
        if (floor == null) {
            resolved = ceiling.getValue();
        } else if (ceiling == null) {
            resolved = floor.getValue();
        } else if (floor.getKey().equals(ceiling.getKey())) {
            resolved = floor.getValue();
        } else {
            double span = ceiling.getKey() - floor.getKey();
            double t = span <= 0 ? 0 : (time - floor.getKey()) / span;
            resolved = interpolate(floor.getValue(), ceiling.getValue(), t);
        }
        apply(resolved);
    }

    private void apply(Keyframe keyframe) {
        if (keyframe.getPosition() != null) SceneProperties.setPosition(target, keyframe.getPosition());
        if (keyframe.getColor() != null) SceneProperties.setColor(target, keyframe.getColor());
        if (keyframe.getRotationDegrees() != null) SceneProperties.setRotationDegrees(target, keyframe.getRotationDegrees());
        if (keyframe.getScale() != null) SceneProperties.setScale(target, keyframe.getScale());
        double[] aim = keyframe.getAimDegrees();
        if (aim != null) SceneProperties.setAimDegrees(target, aim[0], aim[1]);
        if (keyframe.getIntensity() != null) SceneProperties.setIntensity(target, keyframe.getIntensity());
    }

    private static Keyframe interpolate(Keyframe a, Keyframe b, double t) {
        Keyframe result = new Keyframe(a.getTime() + (b.getTime() - a.getTime()) * t);

        result.setPosition(both(a.getPosition(), b.getPosition())
                ? lerp(a.getPosition(), b.getPosition(), t) : either(a.getPosition(), b.getPosition()));
        result.setColor(both(a.getColor(), b.getColor())
                ? lerpColor(a.getColor(), b.getColor(), t) : either(a.getColor(), b.getColor()));
        result.setRotationDegrees(both(a.getRotationDegrees(), b.getRotationDegrees())
                ? lerp(a.getRotationDegrees(), b.getRotationDegrees(), t) : either(a.getRotationDegrees(), b.getRotationDegrees()));
        result.setScale(both(a.getScale(), b.getScale())
                ? a.getScale() + (b.getScale() - a.getScale()) * t : either(a.getScale(), b.getScale()));
        result.setAimDegrees(both(a.getAimDegrees(), b.getAimDegrees())
                ? new double[]{lerpAngleDegrees(a.getAimDegrees()[0], b.getAimDegrees()[0], t),
                        lerpAngleDegrees(a.getAimDegrees()[1], b.getAimDegrees()[1], t)}
                : either(a.getAimDegrees(), b.getAimDegrees()));
        result.setIntensity(both(a.getIntensity(), b.getIntensity())
                ? a.getIntensity() + (b.getIntensity() - a.getIntensity()) * t : either(a.getIntensity(), b.getIntensity()));

        return result;
    }

    private static boolean both(Object a, Object b) {
        return a != null && b != null;
    }

    /** Whichever of the two isn't null — used when only one keyframe captured this property. */
    private static <T> T either(T a, T b) {
        return a != null ? a : b;
    }

    private static Vector3D lerp(Vector3D a, Vector3D b, double t) {
        return new Vector3D(
                a.getX() + (b.getX() - a.getX()) * t,
                a.getY() + (b.getY() - a.getY()) * t,
                a.getZ() + (b.getZ() - a.getZ()) * t);
    }

    private static Color lerpColor(Color a, Color b, double t) {
        return new Color(
                clamp255(a.getRed() + (b.getRed() - a.getRed()) * t),
                clamp255(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                clamp255(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static int clamp255(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }

    /** Interpolates an angle in degrees along the shortest path (350° -> 10° goes +20°, not -340°). */
    private static double lerpAngleDegrees(double a, double b, double t) {
        double delta = ((b - a + 180) % 360 + 360) % 360 - 180;
        return a + delta * t;
    }
}
