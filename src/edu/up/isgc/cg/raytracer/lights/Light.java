package edu.up.isgc.cg.raytracer.lights;


import edu.up.isgc.cg.raytracer.math.Vector3D;

import java.awt.*;

/**
 * The type Light.
 */
public abstract class Light  {

    private double intensity;
    private Color color;

    /**
     * Instantiates a new Light.
     *
     * @param color     the color
     * @param intensity the intensity
     */
    public Light( Color color, double intensity) {
        setColor(color);
        setIntensity(intensity);
    }

    /**
     * Get color color.
     *
     * @return the color
     */
    public Color getColor(){
        return color;
    }
    private void setColor(Color color) {
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

    private void setIntensity(double intensity) {
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
}

