package edu.up.isgc.cg.raytracer.animation;

import edu.up.isgc.cg.raytracer.math.Vector3D;

import java.awt.Color;

/**
 * A snapshot of one scene item's animatable property values at a single instant — a
 * "pose" at that time. Only the properties the item actually supports are ever set (see
 * {@link edu.up.isgc.cg.raytracer.tools.SceneProperties}); the rest stay {@code null} and
 * are simply skipped wherever a {@link Keyframe} is read.
 *
 * @author Claude (Anthropic)
 */
public class Keyframe {

    private final double time;
    private Vector3D position;
    private Color color;
    private Vector3D rotationDegrees;
    private Double scale;
    private double[] aimDegrees; // {yawDegrees, pitchDegrees}
    private Double intensity;

    /**
     * Instantiates a new Keyframe.
     *
     * @param time the time, in seconds
     */
    public Keyframe(double time) {
        this.time = time;
    }

    public double getTime() {
        return time;
    }

    public Vector3D getPosition() {
        return position;
    }

    public void setPosition(Vector3D position) {
        this.position = position;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Vector3D getRotationDegrees() {
        return rotationDegrees;
    }

    public void setRotationDegrees(Vector3D rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
    }

    public Double getScale() {
        return scale;
    }

    public void setScale(Double scale) {
        this.scale = scale;
    }

    public double[] getAimDegrees() {
        return aimDegrees;
    }

    public void setAimDegrees(double[] aimDegrees) {
        this.aimDegrees = aimDegrees;
    }

    public Double getIntensity() {
        return intensity;
    }

    public void setIntensity(Double intensity) {
        this.intensity = intensity;
    }
}
