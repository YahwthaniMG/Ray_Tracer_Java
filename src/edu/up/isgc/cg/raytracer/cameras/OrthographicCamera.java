package edu.up.isgc.cg.raytracer.cameras;


import edu.up.isgc.cg.raytracer.math.Ray;
import edu.up.isgc.cg.raytracer.math.Vector3D;

import static edu.up.isgc.cg.raytracer.math.Vector3D.add;

/**
 * The type Orthographic camera. Unlike {@link PerspertiveCamera}, every ray points in
 * the same direction (no convergence to a single eye point) — only where each ray
 * originates changes across the view plane, which is what gives an orthographic render
 * its "no perspective distortion" look.
 *
 * @author Claude (Anthropic)
 */
public class OrthographicCamera extends Camera {

    private double left;
    private double right;
    private double top;
    private double bottom;

    /**
     * Instantiates a new Orthographic camera.
     *
     * @param position the position
     * @param left     the left edge of the view plane, in world units
     * @param right    the right edge of the view plane, in world units
     * @param top      the top edge of the view plane, in world units
     * @param bottom   the bottom edge of the view plane, in world units
     * @param near     the near
     * @param far      the far
     */
    public OrthographicCamera(Vector3D position, double left, double right, double top, double bottom, double near, double far) {
        super(position, 0, 0, near, far);
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    /** The view plane's edges, in world units — public so scene save/load can persist them (see {@link edu.up.isgc.cg.raytracer.tools.SceneIO}). */
    public double getLeft() { return left; }
    public double getRight() { return right; }
    public double getTop() { return top; }
    public double getBottom() { return bottom; }

    /**
     *
     * @param u the horizontal screen coordinate, in [-1, 1]
     * @param v the vertical screen coordinate, in [-1, 1]
     * Builds a ray whose origin spans the view plane and whose direction is constant —
     * the defining trait of an orthographic (non-perspective) projection.
     * @return ray
     */
    @Override
    public Ray makeRay(double u, double v) {
        double halfWidth = (right - left) / 2.0;
        double halfHeight = (top - bottom) / 2.0;
        Vector3D originOffset = new Vector3D(u * halfWidth, v * halfHeight, 0).rotateYP(getYawRadians(), getPitchRadians());
        Vector3D direction = new Vector3D(0, 0, 1).rotateYP(getYawRadians(), getPitchRadians());
        return new Ray(add(getPosition(), originOffset), direction);
    }

}
