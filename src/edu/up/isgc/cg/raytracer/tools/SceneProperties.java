package edu.up.isgc.cg.raytracer.tools;

import edu.up.isgc.cg.raytracer.Scene;
import edu.up.isgc.cg.raytracer.cameras.Camera;
import edu.up.isgc.cg.raytracer.cameras.FisheyeCamera;
import edu.up.isgc.cg.raytracer.cameras.PerspertiveCamera;
import edu.up.isgc.cg.raytracer.lights.DirectionalLight;
import edu.up.isgc.cg.raytracer.lights.Light;
import edu.up.isgc.cg.raytracer.lights.PointLight;
import edu.up.isgc.cg.raytracer.lights.SpotLight;
import edu.up.isgc.cg.raytracer.math.Vector3D;
import edu.up.isgc.cg.raytracer.objects.Material;
import edu.up.isgc.cg.raytracer.objects.Model3D;
import edu.up.isgc.cg.raytracer.objects.Object3D;
import edu.up.isgc.cg.raytracer.objects.Plane;
import edu.up.isgc.cg.raytracer.objects.Sphere;

import java.awt.Color;

/**
 * {@code Object3D}, {@code Camera} and the {@code Light} subtypes don't share a common
 * base in the core model, but several unrelated parts of the app need to treat them the
 * same way — "what's its position", "move it", "what do I call it": the scene editor's
 * Inspector/Viewport/Sidebar ({@code edu.up.isgc.cg.raytracer.ui}), and the animation
 * system ({@code edu.up.isgc.cg.raytracer.animation}), which needs to read/write these
 * same properties to play back keyframes without caring what concrete type it's animating.
 * Rather than duplicate that instanceof dispatch in each of those places, it lives here
 * once, public, in a package neither side needs to depend on the other to reach.
 *
 * <p>{@code DirectionalLight} has no real position (it lights everything equally
 * regardless of distance) — it carries an editor-only anchor point purely so it has
 * somewhere to draw/click/drag its direction arrow from; the raytracer never reads it.</p>
 *
 * @author Claude (Anthropic)
 */
public final class SceneProperties {
    private SceneProperties() {}

    public static Vector3D getPosition(Object item) {
        if (item instanceof Object3D o) return o.getPosition();
        if (item instanceof Camera c) return c.getPosition();
        if (item instanceof PointLight l) return l.getPosition();
        if (item instanceof SpotLight l) return l.getPosition();
        if (item instanceof DirectionalLight l) return l.getPosition();
        return null;
    }

    public static void setPosition(Object item, Vector3D position) {
        if (item instanceof Object3D o) o.setPosition(position);
        else if (item instanceof Camera c) c.setPosition(position);
        else if (item instanceof PointLight l) l.setPosition(position);
        else if (item instanceof SpotLight l) l.setPosition(position);
        else if (item instanceof DirectionalLight l) l.setPosition(position);
    }

    public static String getName(Object item) {
        if (item instanceof Object3D o) return o.getName();
        if (item instanceof Camera c) return c.getName();
        if (item instanceof Light l) return l.getName();
        return "";
    }

    public static void setName(Object item, String name) {
        if (item instanceof Object3D o) o.setName(name);
        else if (item instanceof Camera c) c.setName(name);
        else if (item instanceof Light l) l.setName(name);
    }

    public static String getTypeLabel(Object item) {
        return item == null ? "" : item.getClass().getSimpleName();
    }

    // ---- Rotation (Model3D rotates its mesh; Plane rotates its normal so it can stand
    // in as a wall/ceiling; Sphere is symmetric so rotating it has no visible effect) ----

    public static boolean supportsRotation(Object item) {
        return item instanceof Model3D || item instanceof Plane;
    }

    /** Rotation in degrees, X/Y/Z. */
    public static Vector3D getRotationDegrees(Object item) {
        if (item instanceof Model3D model) return degrees(model.getRotation());
        if (item instanceof Plane plane) return degrees(plane.getRotation());
        return null;
    }

    public static void setRotationDegrees(Object item, Vector3D rotationDegrees) {
        if (item instanceof Model3D model) model.setRotation(radians(rotationDegrees));
        else if (item instanceof Plane plane) plane.setRotation(radians(rotationDegrees));
    }

    private static Vector3D degrees(Vector3D radians) {
        return new Vector3D(Math.toDegrees(radians.getX()), Math.toDegrees(radians.getY()), Math.toDegrees(radians.getZ()));
    }

    private static Vector3D radians(Vector3D degrees) {
        return new Vector3D(Math.toRadians(degrees.getX()), Math.toRadians(degrees.getY()), Math.toRadians(degrees.getZ()));
    }

    // ---- Scale (Model3D scales its mesh; Sphere scaling multiplies its radius; Plane
    // scaling only resizes the editor's visual patch — it stays infinite to the raytracer) ----

    public static boolean supportsScale(Object item) {
        return item instanceof Model3D || item instanceof Sphere || item instanceof Plane;
    }

    public static double getScale(Object item) {
        if (item instanceof Model3D model) return model.getScale();
        if (item instanceof Sphere sphere) return sphere.getScale();
        if (item instanceof Plane plane) return plane.getScale();
        return 1.0;
    }

    public static void setScale(Object item, double scale) {
        if (item instanceof Model3D model) model.setScale(scale);
        else if (item instanceof Sphere sphere) sphere.setScale(scale);
        else if (item instanceof Plane plane) plane.setScale(scale);
    }

    // ---- Aim (yaw/pitch) — cameras look along it; DirectionalLight/SpotLight are aimed
    // along it. Represented the same way Camera already was (a direction is just
    // Vector3D(0,0,1) rotated by yaw then pitch — see Vector3D#rotateYP), converted back
    // to yaw/pitch analytically for the lights, which store a direction instead. ----

    public static boolean supportsAim(Object item) {
        return item instanceof Camera || item instanceof DirectionalLight || item instanceof SpotLight;
    }

    /** {yawDegrees, pitchDegrees}. */
    public static double[] getAimDegrees(Object item) {
        if (item instanceof Camera camera) {
            return new double[]{Math.toDegrees(camera.getYawRadians()), Math.toDegrees(camera.getPitchRadians())};
        }
        if (item instanceof DirectionalLight light) return directionToYawPitchDegrees(light.getDirection());
        if (item instanceof SpotLight light) return directionToYawPitchDegrees(light.getDirection());
        return null;
    }

    public static void setAimDegrees(Object item, double yawDegrees, double pitchDegrees) {
        if (item instanceof Camera camera) {
            camera.setYawRadians(Math.toRadians(yawDegrees));
            camera.setPitchRadians(Math.toRadians(pitchDegrees));
        } else if (item instanceof DirectionalLight light) {
            light.setDirection(yawPitchDegreesToDirection(yawDegrees, pitchDegrees));
        } else if (item instanceof SpotLight light) {
            light.setDirection(yawPitchDegreesToDirection(yawDegrees, pitchDegrees));
        }
    }

    private static double[] directionToYawPitchDegrees(Vector3D direction) {
        Vector3D d = Vector3D.normalize(direction);
        double pitch = -Math.asin(Math.max(-1.0, Math.min(1.0, d.getY())));
        double yaw = Math.atan2(d.getX(), d.getZ());
        return new double[]{Math.toDegrees(yaw), Math.toDegrees(pitch)};
    }

    private static Vector3D yawPitchDegreesToDirection(double yawDegrees, double pitchDegrees) {
        return new Vector3D(0, 0, 1).rotateYP(Math.toRadians(yawDegrees), Math.toRadians(pitchDegrees));
    }

    // ---- Field of view (only the camera types that have one — Orthographic doesn't) ----

    public static boolean supportsFov(Object item) {
        return item instanceof PerspertiveCamera || item instanceof FisheyeCamera;
    }

    public static double getFovDegrees(Object item) {
        if (item instanceof PerspertiveCamera camera) return Math.toDegrees(camera.getVerticalFovRadians());
        if (item instanceof FisheyeCamera camera) return camera.getFovDegrees();
        return 0;
    }

    public static void setFovDegrees(Object item, double fovDegrees) {
        if (item instanceof PerspertiveCamera camera) camera.setVerticalFovRadians(Math.toRadians(fovDegrees));
        else if (item instanceof FisheyeCamera camera) camera.setFovDegrees(fovDegrees);
    }

    // ---- Color (any Object3D subtype, plus any Light; Camera has none) ----

    public static boolean supportsColor(Object item) {
        return item instanceof Object3D || item instanceof Light;
    }

    public static Color getColor(Object item) {
        if (item instanceof Object3D o) return o.getColor();
        if (item instanceof Light l) return l.getColor();
        return null;
    }

    public static void setColor(Object item, Color color) {
        if (item instanceof Object3D o) o.setColor(color);
        else if (item instanceof Light l) l.setColor(color);
    }

    // ---- Intensity (any Light) ----

    public static boolean supportsIntensity(Object item) {
        return item instanceof Light;
    }

    public static double getIntensity(Object item) {
        return item instanceof Light l ? l.getIntensity() : 0;
    }

    public static void setIntensity(Object item, double intensity) {
        if (item instanceof Light l) l.setIntensity(intensity);
    }

    // ---- Cone (SpotLight only) ----

    public static boolean supportsCone(Object item) {
        return item instanceof SpotLight;
    }

    public static double getConeAngleDegrees(Object item) {
        return item instanceof SpotLight s ? s.getConeAngleDegrees() : 0;
    }

    public static void setConeAngleDegrees(Object item, double degrees) {
        if (item instanceof SpotLight s) s.setConeAngleDegrees(degrees);
    }

    public static double getPenumbraDegrees(Object item) {
        return item instanceof SpotLight s ? s.getPenumbraDegrees() : 0;
    }

    public static void setPenumbraDegrees(Object item, double degrees) {
        if (item instanceof SpotLight s) s.setPenumbraDegrees(degrees);
    }

    // ---- Material (any Object3D — cameras/lights have none) ----

    public static boolean supportsMaterial(Object item) {
        return item instanceof Object3D;
    }

    /** The item's mutable {@link Material}, or {@code null} if it doesn't have one — edit it in place. */
    public static Material getMaterial(Object item) {
        return item instanceof Object3D o ? o.getMaterial() : null;
    }

    // ---- Adding/removing an item from the scene ----

    public static void addToScene(Scene scene, Object item) {
        if (item instanceof Object3D o) scene.addObject(o);
        else if (item instanceof Camera c) scene.addCamera(c);
        else if (item instanceof Light l) scene.addLight(l);
    }

    public static void removeFromScene(Scene scene, Object item) {
        if (item instanceof Object3D o) scene.removeObject(o);
        else if (item instanceof Camera c) scene.removeCamera(c);
        else if (item instanceof Light l) scene.removeLight(l);
    }
}
