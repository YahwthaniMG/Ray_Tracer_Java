/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer.lights;

import edu.up.isgc.cg.raytracer.Scene;
import edu.up.isgc.cg.raytracer.math.Vector3D;
import static edu.up.isgc.cg.raytracer.math.Vector3D.*;

import java.awt.*;

/**
 * The type Directional light.
 *
 * @author Jafet Rodríguez and Yahwthani Morales
 */
public class DirectionalLight extends Light{
    private Vector3D direction;

    /**
     * Instantiates a new Directional light.
     *
     * @param direction the direction
     * @param color     the color
     * @param intensity the intensity
     */
    public DirectionalLight(Vector3D direction, Color color, double intensity) {
        super( color, intensity);
        setDirection(direction);
    }

    private Vector3D getDirection() {
        return direction;
    }

    private void setDirection(Vector3D direction) {
        this.direction = magnitude(direction)== 1 ? direction: normalize(direction);
    }

    /**
     * Gets color.
     *
     * @return the position
     */
    @Override
    public Color getColor(Vector3D point) {
        Color color = getColor();
        int r = (int) (color.getRed() * getIntensity());
        int g = (int) (color.getGreen() * getIntensity());
        int b = (int) (color.getBlue() * getIntensity());
        if (r > 255) r = 255;
        if (g > 255) g = 255;
        if (b > 255) b = 255;
        return new Color(r, g, b);
    }

    /**
     *
     * @param point
     * Gets the direction vector of the ray
     * @return direction
     */
    @Override
    public Vector3D getDirection(Vector3D point){ return negative(getDirection());}
}
