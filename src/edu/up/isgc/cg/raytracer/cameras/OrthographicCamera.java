package edu.up.isgc.cg.raytracer.cameras;


import edu.up.isgc.cg.raytracer.math.Ray;
import edu.up.isgc.cg.raytracer.math.Vector3D;

/**
 * The type Orthographic camera.
 */
public class OrthographicCamera extends Camera {
    /**
     * Instantiates a new Orthographic camera.
     *
     * @param position the position
     * @param left     the left
     * @param right    the right
     * @param top      the top
     * @param bottom   the bottom
     * @param near     the near
     * @param far      the far
     */
    public OrthographicCamera(Vector3D position, double left, double right, double top, double bottom, double near, double far) {
        super(position, 0, 0, near, far);
    }

    /**
     *
     * @param u
     * @param v
     * Perform a null ray
     * @return ray
     */
    @Override
    public Ray makeRay(double u, double v) {
        return null;
    }

}

