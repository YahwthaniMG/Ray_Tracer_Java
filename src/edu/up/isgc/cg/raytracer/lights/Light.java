package edu.up.isgc.cg.raytracer.lights;


import edu.up.isgc.cg.raytracer.math.Vector3D;

import java.awt.*;

/**
 * The type Light.
 *
 * @author Claude (Anthropic)
 */
public abstract class Light  {

    private double intensity;
    private Color color;
    private String name;

    /**
     * Instantiates a new Light.
     *
     * @param color     the color
     * @param intensity the intensity
     */
    public Light( Color color, double intensity) {
        setColor(color);
        setIntensity(intensity);
        setName(getClass().getSimpleName());
    }

    /**
     * Gets the display name used by the scene editor (defaults to the simple class name).
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name used by the scene editor.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get color color.
     *
     * @return the color
     */
    public Color getColor(){
        return color;
    }

    /**
     * Sets color.
     *
     * @param color the color
     */
    public void setColor(Color color) {
        this.color =color;
    }

    /**
     * Gets intensity.
     *
     * @return the intensity
     */
    public double getIntensity() {
        return intensity;
    }

    /**
     * Sets intensity.
     *
     * @param intensity the intensity
     */
    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }

    /**
     * Gets color.
     *
     * @param point the point
     * @return the color
     */
    public abstract Color getColor(Vector3D point);

    /**
     * Gets direction.
     *
     * @param point the point
     * @return the direction
     */
    public abstract Vector3D getDirection(Vector3D point);

    /**
     * Direction from {@code point} toward one sample point on this light's surface, for
     * soft shadows: casting several shadow rays with different {@code offsetU}/
     * {@code offsetV} and averaging how many are blocked gives a soft-edged shadow
     * instead of a hard one, since a real light source isn't an infinitesimal point.
     * The default just returns {@link #getDirection(Vector3D)} unchanged (a point-like
     * light with no soft shadow) — light types wide enough for it to matter override this.
     *
     * @param point   the point being shaded
     * @param offsetU horizontal offset across the light's surface, in [-1, 1]
     * @param offsetV vertical offset across the light's surface, in [-1, 1]
     * @return the direction
     */
    public Vector3D getShadowSampleDirection(Vector3D point, double offsetU, double offsetV) {
        return getDirection(point);
    }

    /**
     * Distance from {@code point} to this light — used to cap the shadow-ray test at the
     * light itself, so a surface *behind* it (from the shaded point's perspective, e.g. a
     * ceiling on the far side of a lamp) doesn't get treated as blocking the light just
     * because it happens to sit further along the same ray. A directional light has no
     * real position (it's effectively infinitely far away in a fixed direction), so the
     * default returns {@link Double#POSITIVE_INFINITY} — nothing is ever "beyond" it;
     * positional lights (point, spot) override this with their actual distance.
     *
     * @param point the point being shaded
     * @return the distance
     */
    public double getDistanceTo(Vector3D point) {
        return Double.POSITIVE_INFINITY;
    }
}

