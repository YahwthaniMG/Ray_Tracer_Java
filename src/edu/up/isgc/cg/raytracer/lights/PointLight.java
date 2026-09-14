package edu.up.isgc.cg.raytracer.lights;

import edu.up.isgc.cg.raytracer.ColorRGB;
import edu.up.isgc.cg.raytracer.math.Vector3D;
import static edu.up.isgc.cg.raytracer.math.Vector3D.*;
import java.awt.*;
import static java.lang.Math.pow;

/**
 * The type Point light.
 *
 * @author Claude (Anthropic)
 */
public class PointLight extends Light{
    private Vector3D Position;
    // Assumed physical radius (world units), used only to soften shadow edges — a real
    // bulb/fixture isn't an infinitesimal point, so a shading point can be partially
    // occluded from part of it and fully lit from another part.
    private static final double SHADOW_LIGHT_RADIUS = 0.3;

    /**
     * Instantiates a new Point light.
     *
     * @param position  the position
     * @param color     the color
     * @param intensity the intensity
     */
    public PointLight(Vector3D position, Color color, double intensity) {
        super( color, intensity);
        setPosition(position);
    }

    /**
     * Gets position.
     *
     * @return the position
     */
    public Vector3D getPosition() {
        return Position;
    }

    /**
     * Sets position.
     *
     * @param position the position
     */
    public void setPosition(Vector3D position) {
        this.Position = position;
    }

    /**
     * Gets color.
     *
     * @return the position
     */
    @Override
    public Color getColor(Vector3D point) {
        // Standard inverse-square falloff. This used to be raised to the 2.5 power and
        // divided by an extra 4*PI on top of that — a physically-based-rendering-style
        // radiant-intensity-to-irradiance conversion that doesn't belong in a simple Phong
        // renderer where "intensity" is just an artistic knob. At a typical scene distance
        // of 8 units it made a light of intensity 5 (the sidebar's old default) contribute
        // exactly 0 out of 255 to diffuse/specular shading — effectively turning every
        // point light in every scene into a no-op beyond a few units, which is why
        // everything rendered flat and gray no matter where lights were placed.
        double r2 = pow(magnitude(subtract(getPosition(), point)), 2);
        return ColorRGB.multiply(getColor(), getIntensity() / r2);
    }

    /**
     *
     * @param point
     * Gets the direction vector of the ray
     * @return direction
     */
    @Override
    public Vector3D getDirection(Vector3D point){ return normalize(subtract(getPosition(), point));}

    /** See {@link Light#getDistanceTo}. */
    @Override
    public double getDistanceTo(Vector3D point) {
        return magnitude(subtract(getPosition(), point));
    }

    /**
     * Jitters the sample point across a small disk facing {@code point}, centered on
     * {@link #getPosition()} — see {@link Light#getShadowSampleDirection} and the
     * {@link #SHADOW_LIGHT_RADIUS} field doc.
     */
    @Override
    public Vector3D getShadowSampleDirection(Vector3D point, double offsetU, double offsetV) {
        Vector3D toLight = getDirection(point);
        Vector3D[] tangents = perpendicularTangents(toLight);
        Vector3D jitteredPosition = add(getPosition(), add(
                scalarMultiplication(tangents[0], offsetU * SHADOW_LIGHT_RADIUS),
                scalarMultiplication(tangents[1], offsetV * SHADOW_LIGHT_RADIUS)));
        return normalize(subtract(jitteredPosition, point));
    }
}

