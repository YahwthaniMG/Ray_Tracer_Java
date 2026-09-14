package edu.up.isgc.cg.raytracer.lights;

import edu.up.isgc.cg.raytracer.ColorRGB;
import edu.up.isgc.cg.raytracer.math.Vector3D;
import static edu.up.isgc.cg.raytracer.math.Vector3D.*;

import java.awt.Color;

import static java.lang.Math.pow;

/**
 * A {@link PointLight} narrowed to a cone: light only reaches points within
 * {@link #getConeAngleDegrees()} of {@link #getDirection()} (the axis it's aimed along),
 * fading smoothly to nothing over the last {@link #getPenumbraDegrees()} of that cone
 * instead of a hard, aliased edge.
 *
 * @author Claude (Anthropic)
 */
public class SpotLight extends Light {

    private Vector3D position;
    private Vector3D direction;
    private double coneAngleDegrees;
    private double penumbraDegrees;
    // Assumed physical radius (world units), used only to soften shadow edges — same
    // reasoning as PointLight's identical constant.
    private static final double SHADOW_LIGHT_RADIUS = 0.3;

    /**
     * Instantiates a new Spot light.
     *
     * @param position         the position
     * @param direction        the direction it's aimed along
     * @param color            the color
     * @param intensity        the intensity
     * @param coneAngleDegrees the half-angle (from the aim direction to the cone's edge) of the beam
     */
    public SpotLight(Vector3D position, Vector3D direction, Color color, double intensity, double coneAngleDegrees) {
        super(color, intensity);
        setPosition(position);
        setDirection(direction);
        setConeAngleDegrees(coneAngleDegrees);
        setPenumbraDegrees(coneAngleDegrees / 4.0);
    }

    /**
     * Gets position.
     *
     * @return the position
     */
    public Vector3D getPosition() {
        return position;
    }

    /**
     * Sets position.
     *
     * @param position the position
     */
    public void setPosition(Vector3D position) {
        this.position = position;
    }

    /**
     * Gets the direction the cone is aimed along (constant, independent of the shaded point).
     *
     * @return the direction
     */
    public Vector3D getDirection() {
        return direction;
    }

    /**
     * Sets the aim direction (normalized automatically if needed).
     *
     * @param direction the direction
     */
    public void setDirection(Vector3D direction) {
        this.direction = magnitude(direction) == 1 ? direction : normalize(direction);
    }

    /**
     * Gets the cone's half-angle, in degrees, from its aim direction to its edge.
     *
     * @return the cone angle degrees
     */
    public double getConeAngleDegrees() {
        return coneAngleDegrees;
    }

    /**
     * Sets the cone's half-angle, in degrees.
     *
     * @param coneAngleDegrees the cone angle degrees
     */
    public void setConeAngleDegrees(double coneAngleDegrees) {
        this.coneAngleDegrees = Math.max(0.5, coneAngleDegrees);
    }

    /**
     * Gets how many degrees, just inside the cone's edge, the light fades smoothly over
     * instead of cutting off sharply.
     *
     * @return the penumbra degrees
     */
    public double getPenumbraDegrees() {
        return penumbraDegrees;
    }

    /**
     * Sets the penumbra width, in degrees.
     *
     * @param penumbraDegrees the penumbra degrees
     */
    public void setPenumbraDegrees(double penumbraDegrees) {
        this.penumbraDegrees = Math.max(0, penumbraDegrees);
    }

    /**
     * Same inverse-square falloff as {@link PointLight}, additionally scaled to zero
     * outside the cone and smoothly ramped over the penumbra just inside its edge.
     *
     * @param point the point
     * @return the color
     */
    @Override
    public Color getColor(Vector3D point) {
        Vector3D towardPoint = normalize(subtract(point, position));
        double cosAngle = Math.max(-1.0, Math.min(1.0, dotProduct(direction, towardPoint)));
        double angleDegrees = Math.toDegrees(Math.acos(cosAngle));

        double innerEdge = Math.max(0, coneAngleDegrees - penumbraDegrees);
        double coneFactor;
        if (angleDegrees >= coneAngleDegrees) {
            coneFactor = 0;
        } else if (angleDegrees <= innerEdge || coneAngleDegrees == innerEdge) {
            coneFactor = 1;
        } else {
            coneFactor = 1 - (angleDegrees - innerEdge) / (coneAngleDegrees - innerEdge);
        }
        if (coneFactor <= 0) return Color.BLACK;

        // Standard inverse-square falloff — see PointLight#getColor for why this isn't
        // raised to the 2.5 power with an extra 4*PI divided in on top of that anymore.
        double r2 = pow(magnitude(subtract(position, point)), 2);
        return ColorRGB.multiply(getColor(), coneFactor * getIntensity() / r2);
    }

    /**
     * @param point the point
     * Gets the direction vector of the ray toward the light — the cone only affects how
     * much light arrives (see {@link #getColor(Vector3D)}), not where the light physically is.
     * @return direction
     */
    @Override
    public Vector3D getDirection(Vector3D point) {
        return normalize(subtract(position, point));
    }

    /** See {@link Light#getDistanceTo}. */
    @Override
    public double getDistanceTo(Vector3D point) {
        return magnitude(subtract(position, point));
    }

    /**
     * Jitters the sample point across a small disk facing {@code point}, centered on
     * {@link #getPosition()} — see {@link Light#getShadowSampleDirection} and
     * {@link PointLight#getShadowSampleDirection}, which this mirrors exactly.
     */
    @Override
    public Vector3D getShadowSampleDirection(Vector3D point, double offsetU, double offsetV) {
        Vector3D toLight = getDirection(point);
        Vector3D[] tangents = perpendicularTangents(toLight);
        Vector3D jitteredPosition = add(position, add(
                scalarMultiplication(tangents[0], offsetU * SHADOW_LIGHT_RADIUS),
                scalarMultiplication(tangents[1], offsetV * SHADOW_LIGHT_RADIUS)));
        return normalize(subtract(jitteredPosition, point));
    }
}
