package edu.up.isgc.cg.raytracer.cameras;

import edu.up.isgc.cg.raytracer.math.Ray;
import edu.up.isgc.cg.raytracer.math.Vector3D;

import static java.lang.Math.*;

/**
 * The type Fisheye camera.
 *
 * <p>Unlike {@link PerspertiveCamera}, where distance from the center of the screen maps
 * to a straight-line offset on the image plane, here it maps directly to the *angle* away
 * from the forward direction — the defining trait of a fisheye lens, and what makes
 * straight lines bow outward near the edges. This is a "full-frame" fisheye rather than
 * the classic circular one: it fills the whole rectangular image, so the corners (farther
 * from center than the top/bottom/left/right edges are) end up showing an even wider
 * angle than {@link #getFovDegrees()} — exactly like a real full-frame fisheye lens.</p>
 *
 * @author Claude (Anthropic)
 */
public class FisheyeCamera extends Camera {

    private double fovRadians;

    /**
     * Instantiates a new Fisheye camera.
     *
     * @param position     the position
     * @param yawDegrees   the yaw degrees
     * @param pitchDegrees the pitch degrees
     * @param nearPlane    the near plane
     * @param farPlane     the far plane
     * @param fovDegrees   the field of view, in degrees, reached at the edge (not corner) of the frame
     */
    public FisheyeCamera(Vector3D position, double yawDegrees, double pitchDegrees,
                          double nearPlane, double farPlane, double fovDegrees) {
        super(position, toRadians(yawDegrees), toRadians(pitchDegrees), nearPlane, farPlane);
        setFovDegrees(fovDegrees);
    }

    /**
     * Gets the field of view, in degrees, reached at the edge of the frame (the corners
     * go wider still — see the class doc).
     *
     * @return the fov degrees
     */
    public double getFovDegrees() {
        return toDegrees(fovRadians);
    }

    /**
     * Sets the field of view, in degrees.
     *
     * @param fovDegrees the fov degrees
     */
    public void setFovDegrees(double fovDegrees) {
        this.fovRadians = toRadians(fovDegrees);
    }

    /**
     * @param u the horizontal screen coordinate, in [-1, 1]
     * @param v the vertical screen coordinate, in [-1, 1]
     *          Builds a ray whose angle away from the forward direction is proportional
     *          to distance from the middle of the screen, then rotates it by yaw/pitch
     *          like every other camera.
     * @return ray
     */
    @Override
    public Ray makeRay(double u, double v) {
        double r = sqrt(u * u + v * v);
        double theta = min(PI, r * (fovRadians / 2.0)); // clamped: never point past straight backward
        double phi = atan2(v, u);
        Vector3D local = new Vector3D(sin(theta) * cos(phi), sin(theta) * sin(phi), cos(theta));
        Vector3D direction = local.rotateYP(getYawRadians(), getPitchRadians());
        return new Ray(getPosition(), direction);
    }
}
