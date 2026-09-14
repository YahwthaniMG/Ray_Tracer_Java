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
 * @author Jafet Rodríguez and Yahwthani Morales, with Claude (Anthropic)
 */
public class DirectionalLight extends Light{
    private Vector3D direction;
    // Editor-only anchor point: a directional light has no real position (it lights
    // everything equally regardless of distance), but the scene editor needs *some* point
    // to draw its direction arrow from and let you click/drag it — the raytracer itself
    // never reads this, only getDirection().
    private Vector3D position = new Vector3D(0, 8, 0);
    // Assumed angular size of the light source (radians) — a real sun/sky isn't a
    // perfectly parallel beam, so jittering the direction itself by a small angle (rather
    // than a world-space position, which wouldn't mean anything for a light with no real
    // position) softens its shadow edges the same way PointLight/SpotLight's do.
    private static final double SHADOW_ANGULAR_RADIUS = 0.03;

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

    /**
     * Gets the editor-only anchor point its direction arrow is drawn from — see the field doc.
     *
     * @return the position
     */
    public Vector3D getPosition() {
        return position;
    }

    /**
     * Sets the editor-only anchor point.
     *
     * @param position the position
     */
    public void setPosition(Vector3D position) {
        this.position = position;
    }

    /**
     * Gets the light's direction vector (constant, independent of the shaded point).
     *
     * @return the direction
     */
    public Vector3D getDirection() {
        return direction;
    }

    /**
     * Sets direction (normalized automatically if needed).
     *
     * @param direction the direction
     */
    public void setDirection(Vector3D direction) {
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

    /**
     * Jitters the direction toward the light by a small angle instead of a position (a
     * directional light has no real position to jitter across) — see
     * {@link Light#getShadowSampleDirection} and {@link #SHADOW_ANGULAR_RADIUS}.
     */
    @Override
    public Vector3D getShadowSampleDirection(Vector3D point, double offsetU, double offsetV) {
        Vector3D toLight = getDirection(point);
        Vector3D[] tangents = perpendicularTangents(toLight);
        Vector3D jittered = add(toLight, add(
                scalarMultiplication(tangents[0], offsetU * SHADOW_ANGULAR_RADIUS),
                scalarMultiplication(tangents[1], offsetV * SHADOW_ANGULAR_RADIUS)));
        return normalize(jittered);
    }
}
