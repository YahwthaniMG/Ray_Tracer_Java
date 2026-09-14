package edu.up.isgc.cg.raytracer.objects;

import edu.up.isgc.cg.raytracer.math.Intersection;
import edu.up.isgc.cg.raytracer.math.Ray;
import edu.up.isgc.cg.raytracer.math.Vector3D;

import java.awt.*;

import static edu.up.isgc.cg.raytracer.math.Vector3D.*;

/**
 * The type Plane. Mathematically infinite — a ray either hits it or doesn't, there's no
 * edge. Originally always horizontal (normal fixed at (0,1,0), a fixed "ground"), it now
 * carries a rotatable normal so it can stand in as a wall or ceiling too.
 *
 * <p>{@link #setScale} does not (can't) change how far it actually extends for the
 * raytracer — it only changes how large a patch the scene editor draws/lets you click,
 * purely so a wall-sized plane doesn't look like a tiny floor tile while you're
 * positioning it. See {@link edu.up.isgc.cg.raytracer.ui.ViewportPanel} for that patch.</p>
 *
 * @author Claude (Anthropic)
 */
public class Plane extends Object3D implements IIntersectable{

    private static final double EPSILON = 1e-9;
    private static final double BASE_RENDER_EXTENT = 10;

    private final Vector3D baseNormal = new Vector3D(0, 1, 0);
    private Vector3D normal = baseNormal;
    private Vector3D rotation = new Vector3D(0, 0, 0);
    private double scale = 1.0;

    /**
     * Instantiates a new Plane, horizontal at height y (its normal starts as (0,1,0);
     * rotate it to use it as a wall or ceiling instead).
     *
     * @param y     the y
     * @param color the color
     */
    public Plane(double y, Color color){
        super(new Vector3D(0,y,0), color);
    }

    /**
     * Gets the plane's current normal (after {@link #getRotation}'s been applied).
     *
     * @return the normal
     */
    public Vector3D getNormal() {
        return normal;
    }

    /**
     * Gets the plane's rotation, in radians, away from horizontal (0,1,0).
     *
     * @return the rotation
     */
    public Vector3D getRotation() {
        return rotation;
    }

    /**
     * Sets the plane's rotation, in radians, away from horizontal — e.g. 90° around X
     * or Z turns a floor into a wall.
     *
     * @param rotation the rotation
     */
    public void setRotation(Vector3D rotation) {
        this.rotation = rotation;
        this.normal = normalize(matrixRotate(baseNormal, rotation));
    }

    /**
     * Gets the editor-only visual scale (1.0 = the default patch size).
     *
     * @return the scale
     */
    public double getScale() {
        return scale;
    }

    /**
     * Sets the editor-only visual scale — see the class doc: this cannot make the
     * raytracer's actual (infinite) plane any bigger or smaller.
     *
     * @param scale the scale
     */
    public void setScale(double scale) {
        this.scale = Math.max(0.05, scale);
    }

    /**
     * Gets how large a patch (half-width, in world units) the editor should draw —
     * derived from {@link #getScale}.
     *
     * @return the render extent
     */
    public double getRenderExtent() {
        return BASE_RENDER_EXTENT * scale;
    }

    /**
     *
     * @param ray
     * Gets the intersection of a ray at a point, using the general ray/plane formula
     * (works for any normal, not just the original horizontal-only case).
     * @return Intersection
     */
    @Override
    public Intersection getIntersection(Ray ray){
        double denominator = dotProduct(ray.getDirection(), normal);
        if (Math.abs(denominator) < EPSILON) return null; // ray parallel to the plane
        double t = dotProduct(subtract(getPosition(), ray.getOrigin()), normal) / denominator;
        if (t <= 0 || !Double.isFinite(t)) return null;
        Vector3D P = ray.getPoint(t);
        return new Intersection(ray, this, P, normal);
    }


}
