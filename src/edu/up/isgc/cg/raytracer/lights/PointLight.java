package edu.up.isgc.cg.raytracer.lights;

import edu.up.isgc.cg.raytracer.ColorRGB;
import edu.up.isgc.cg.raytracer.math.Vector3D;
import static edu.up.isgc.cg.raytracer.math.Vector3D.*;
import java.awt.*;
import static java.lang.Math.PI;
import  static java.lang.Math.pow;

/**
 * The type Point light.
 */
public class PointLight extends Light{
    private Vector3D Position;

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

    private void setPosition(Vector3D position) {
        this.Position = position;
    }

    /**
     * Gets color.
     *
     * @return the position
     */
    @Override
    public Color getColor(Vector3D point) {
        double r2 = pow(magnitude(subtract(getPosition(), point)),2.5);
        double denominator= 4* PI * r2;
        return ColorRGB.multiply(getColor(), getIntensity()/denominator);
    }

    /**
     *
     * @param point
     * Gets the direction vector of the ray
     * @return direction
     */
    @Override
    public Vector3D getDirection(Vector3D point){ return normalize(subtract(getPosition(), point));}
}

