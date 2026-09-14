package edu.up.isgc.cg.raytracer.tools;

import edu.up.isgc.cg.raytracer.Scene;
import edu.up.isgc.cg.raytracer.animation.AnimationTrack;
import edu.up.isgc.cg.raytracer.animation.Keyframe;
import edu.up.isgc.cg.raytracer.animation.SceneAnimation;
import edu.up.isgc.cg.raytracer.cameras.Camera;
import edu.up.isgc.cg.raytracer.cameras.FisheyeCamera;
import edu.up.isgc.cg.raytracer.cameras.OrthographicCamera;
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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Saves/loads a {@link Scene} — every object/camera/light's position, color, rotation
 * and scale — as a small, human-readable JSON file (see {@link Json}), so a scene built
 * in the editor can be picked back up later instead of starting over from scratch.
 *
 * <p>{@link Model3D} isn't serialized triangle-by-triangle — that would bloat the save
 * file and just duplicate the source {@code .obj} — instead its {@link Model3D#getSource()}
 * (the file it was loaded from, or which built-in primitive it is) is remembered, and on
 * load the mesh is rebuilt from there, then the saved position/rotation/scale are
 * reapplied on top, exactly as editing it live would.</p>
 *
 * @author Claude (Anthropic)
 */
public final class SceneIO {
    private SceneIO() {}

    // ---- Save ----

    /**
     * Save.
     *
     * @param scene       the scene
     * @param projectName the project name shown in the editor's sidebar (round-trips back
     *                    into that field on {@link #loadInto})
     * @param file        where to write it (parent directories are created if needed)
     * @throws IOException if the file couldn't be written
     */
    public static void save(Scene scene, String projectName, File file) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("projectName", projectName);
        root.put("objects", writeObjects(scene));
        root.put("cameras", writeCameras(scene));
        root.put("lights", writeLights(scene));
        root.put("activeCameraIndex", scene.getCameras().indexOf(scene.getActiveCamera()));
        root.put("animation", writeAnimation(scene));

        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        Files.writeString(file.toPath(), Json.write(root), StandardCharsets.UTF_8);
    }

    private static List<Object> writeObjects(Scene scene) {
        List<Object> list = new ArrayList<>();
        for (Object3D object : scene.getObjects()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", object.getName());
            entry.put("position", vector(object.getPosition()));
            entry.put("color", color(object.getColor()));

            if (object instanceof Sphere sphere) {
                entry.put("type", "Sphere");
                entry.put("radius", sphere.getRadius());
            } else if (object instanceof Plane plane) {
                entry.put("type", "Plane");
                entry.put("rotation", vector(plane.getRotation()));
                entry.put("scale", plane.getScale());
            } else if (object instanceof Model3D model) {
                if (model.getSource() == null) continue; // no way to rebuild it without knowing where it came from
                entry.put("type", "Model3D");
                entry.put("source", model.getSource());
                entry.put("rotation", vector(model.getRotation()));
                entry.put("scale", model.getScale());
            } else {
                continue; // unknown Object3D subtype — nothing sensible to reconstruct it from
            }
            entry.put("material", writeMaterial(object.getMaterial()));
            list.add(entry);
        }
        return list;
    }

    private static Map<String, Object> writeMaterial(Material material) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("ambient", material.getAmbient());
        entry.put("diffuse", material.getDiffuse());
        entry.put("specular", material.getSpecular());
        entry.put("shininess", material.getShininess());
        entry.put("reflectivity", material.getReflectivity());
        entry.put("transparency", material.getTransparency());
        entry.put("refractiveIndex", material.getRefractiveIndex());
        return entry;
    }

    private static List<Object> writeCameras(Scene scene) {
        List<Object> list = new ArrayList<>();
        for (Camera camera : scene.getCameras()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", camera.getName());
            entry.put("position", vector(camera.getPosition()));
            entry.put("yawDegrees", Math.toDegrees(camera.getYawRadians()));
            entry.put("pitchDegrees", Math.toDegrees(camera.getPitchRadians()));
            entry.put("nearPlane", camera.getNearPlane());
            entry.put("farPlane", camera.getFarPlane());

            if (camera instanceof PerspertiveCamera perspective) {
                entry.put("type", "Perspective");
                entry.put("fovDegrees", Math.toDegrees(perspective.getVerticalFovRadians()));
            } else if (camera instanceof FisheyeCamera fisheye) {
                entry.put("type", "Fisheye");
                entry.put("fovDegrees", fisheye.getFovDegrees());
            } else if (camera instanceof OrthographicCamera ortho) {
                entry.put("type", "Orthographic");
                entry.put("left", ortho.getLeft());
                entry.put("right", ortho.getRight());
                entry.put("top", ortho.getTop());
                entry.put("bottom", ortho.getBottom());
            } else {
                continue;
            }
            list.add(entry);
        }
        return list;
    }

    private static List<Object> writeLights(Scene scene) {
        List<Object> list = new ArrayList<>();
        for (Light light : scene.getLights()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", light.getName());
            entry.put("color", color(light.getColor()));
            entry.put("intensity", light.getIntensity());

            if (light instanceof PointLight point) {
                entry.put("type", "Point");
                entry.put("position", vector(point.getPosition()));
            } else if (light instanceof SpotLight spot) {
                entry.put("type", "Spot");
                entry.put("position", vector(spot.getPosition()));
                entry.put("direction", vector(spot.getDirection()));
                entry.put("coneAngleDegrees", spot.getConeAngleDegrees());
                entry.put("penumbraDegrees", spot.getPenumbraDegrees());
            } else if (light instanceof DirectionalLight directional) {
                entry.put("type", "Directional");
                entry.put("direction", vector(directional.getDirection()));
                entry.put("position", vector(directional.getPosition())); // editor-only anchor
            } else {
                continue;
            }
            list.add(entry);
        }
        return list;
    }

    private static Map<String, Object> writeAnimation(Scene scene) {
        SceneAnimation animation = scene.getAnimation();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("durationSeconds", animation.getDurationSeconds());
        entry.put("fps", animation.getFps());

        List<Object> tracks = new ArrayList<>();
        for (AnimationTrack track : animation.getTracks()) {
            if (track.isEmpty()) continue;

            Object target = track.getTarget();
            String targetType;
            int targetIndex;
            if (target instanceof Object3D o) {
                targetType = "object";
                targetIndex = scene.getObjects().indexOf(o);
            } else if (target instanceof Camera c) {
                targetType = "camera";
                targetIndex = scene.getCameras().indexOf(c);
            } else if (target instanceof Light l) {
                targetType = "light";
                targetIndex = scene.getLights().indexOf(l);
            } else {
                continue;
            }
            if (targetIndex < 0) continue; // not actually in the scene (shouldn't happen) — skip defensively

            Map<String, Object> trackEntry = new LinkedHashMap<>();
            trackEntry.put("targetType", targetType);
            trackEntry.put("targetIndex", targetIndex);

            List<Object> keyframes = new ArrayList<>();
            for (Keyframe keyframe : track.getKeyframes()) {
                keyframes.add(writeKeyframe(keyframe));
            }
            trackEntry.put("keyframes", keyframes);
            tracks.add(trackEntry);
        }
        entry.put("tracks", tracks);
        return entry;
    }

    private static Map<String, Object> writeKeyframe(Keyframe keyframe) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("time", keyframe.getTime());
        if (keyframe.getPosition() != null) entry.put("position", vector(keyframe.getPosition()));
        if (keyframe.getColor() != null) entry.put("color", color(keyframe.getColor()));
        if (keyframe.getRotationDegrees() != null) entry.put("rotation", vector(keyframe.getRotationDegrees()));
        if (keyframe.getScale() != null) entry.put("scale", keyframe.getScale());
        if (keyframe.getAimDegrees() != null) {
            entry.put("aim", List.of(keyframe.getAimDegrees()[0], keyframe.getAimDegrees()[1]));
        }
        if (keyframe.getIntensity() != null) entry.put("intensity", keyframe.getIntensity());
        return entry;
    }

    private static List<Object> vector(Vector3D v) {
        return List.of(v.getX(), v.getY(), v.getZ());
    }

    private static List<Object> color(Color c) {
        return List.of(c.getRed(), c.getGreen(), c.getBlue());
    }

    // ---- Load ----

    /**
     * Clears {@code target} and repopulates it from the file — the caller keeps the same
     * {@link Scene} reference, so anything already holding onto it (the viewport, the
     * sidebar) sees the new contents without needing to be rewired.
     *
     * @param target the scene to clear and load into
     * @param file   the file to read
     * @return the project name that was saved alongside the scene, or {@code null} if the
     *         file predates that field
     * @throws IOException if the file couldn't be read
     */
    @SuppressWarnings("unchecked")
    public static String loadInto(Scene target, File file) throws IOException {
        String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        Map<String, Object> root = (Map<String, Object>) Json.parse(text);

        target.clear();

        for (Object entry : listOf(root.get("objects"))) {
            Object3D object = readObject((Map<String, Object>) entry);
            if (object != null) target.addObject(object);
        }

        List<Camera> cameras = new ArrayList<>();
        for (Object entry : listOf(root.get("cameras"))) {
            Camera camera = readCamera((Map<String, Object>) entry);
            if (camera != null) {
                cameras.add(camera);
                target.addCamera(camera);
            }
        }

        for (Object entry : listOf(root.get("lights"))) {
            Light light = readLight((Map<String, Object>) entry);
            if (light != null) target.addLight(light);
        }

        int activeIndex = (int) doubleOr(root.get("activeCameraIndex"), -1);
        if (activeIndex >= 0 && activeIndex < cameras.size()) {
            target.setActiveCamera(cameras.get(activeIndex));
        }

        readAnimation(root.get("animation"), target);

        Object projectName = root.get("projectName");
        return projectName instanceof String name ? name : null;
    }

    @SuppressWarnings("unchecked")
    private static void readAnimation(Object raw, Scene target) {
        if (!(raw instanceof Map)) return; // scene saved before animation existed
        Map<String, Object> entry = (Map<String, Object>) raw;
        SceneAnimation animation = target.getAnimation();
        animation.setDurationSeconds(doubleOr(entry.get("durationSeconds"), animation.getDurationSeconds()));
        animation.setFps((int) doubleOr(entry.get("fps"), animation.getFps()));

        for (Object rawTrack : listOf(entry.get("tracks"))) {
            Map<String, Object> trackEntry = (Map<String, Object>) rawTrack;
            String targetType = (String) trackEntry.get("targetType");
            int targetIndex = (int) doubleOr(trackEntry.get("targetIndex"), -1);
            Object resolvedTarget = resolveAnimationTarget(target, targetType, targetIndex);
            if (resolvedTarget == null) continue;

            AnimationTrack track = animation.trackFor(resolvedTarget);
            for (Object rawKeyframe : listOf(trackEntry.get("keyframes"))) {
                track.addKeyframe(readKeyframe((Map<String, Object>) rawKeyframe));
            }
        }
    }

    private static Object resolveAnimationTarget(Scene scene, String targetType, int targetIndex) {
        if (targetType == null || targetIndex < 0) return null;
        return switch (targetType) {
            case "object" -> targetIndex < scene.getObjects().size() ? scene.getObjects().get(targetIndex) : null;
            case "camera" -> targetIndex < scene.getCameras().size() ? scene.getCameras().get(targetIndex) : null;
            case "light" -> targetIndex < scene.getLights().size() ? scene.getLights().get(targetIndex) : null;
            default -> null;
        };
    }

    private static Keyframe readKeyframe(Map<String, Object> entry) {
        Keyframe keyframe = new Keyframe(doubleOr(entry.get("time"), 0));
        if (entry.containsKey("position")) keyframe.setPosition(readVector(entry.get("position")));
        if (entry.containsKey("color")) keyframe.setColor(readColor(entry.get("color")));
        if (entry.containsKey("rotation")) keyframe.setRotationDegrees(readVector(entry.get("rotation")));
        if (entry.containsKey("scale")) keyframe.setScale(doubleOr(entry.get("scale"), 1.0));
        if (entry.containsKey("aim")) {
            List<Object> aim = listOf(entry.get("aim"));
            keyframe.setAimDegrees(new double[]{doubleOr(get(aim, 0), 0), doubleOr(get(aim, 1), 0)});
        }
        if (entry.containsKey("intensity")) keyframe.setIntensity(doubleOr(entry.get("intensity"), 1.0));
        return keyframe;
    }

    private static Object3D readObject(Map<String, Object> entry) {
        String type = (String) entry.get("type");
        Vector3D position = readVector(entry.get("position"));
        Color color = readColor(entry.get("color"));

        Object3D object = switch (type) {
            case "Sphere" -> new Sphere(position, doubleOr(entry.get("radius"), 1.0), color);
            case "Plane" -> {
                Plane plane = new Plane(position.getY(), color);
                plane.setPosition(position);
                plane.setRotation(readVector(entry.get("rotation")));
                plane.setScale(doubleOr(entry.get("scale"), 1.0));
                yield plane;
            }
            case "Model3D" -> {
                Model3D model = reconstructModel3D((String) entry.get("source"), position, color);
                if (model == null) yield null;
                model.setRotation(readVector(entry.get("rotation")));
                model.setScale(doubleOr(entry.get("scale"), 1.0));
                yield model;
            }
            default -> null;
        };

        if (object != null) {
            String name = (String) entry.get("name");
            if (name != null) object.setName(name);
            readMaterial(entry.get("material"), object.getMaterial());
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    private static void readMaterial(Object raw, Material material) {
        if (!(raw instanceof Map)) return; // scene saved before materials existed — keep the defaults
        Map<String, Object> entry = (Map<String, Object>) raw;
        material.setAmbient(doubleOr(entry.get("ambient"), material.getAmbient()));
        material.setDiffuse(doubleOr(entry.get("diffuse"), material.getDiffuse()));
        material.setSpecular(doubleOr(entry.get("specular"), material.getSpecular()));
        material.setShininess(doubleOr(entry.get("shininess"), material.getShininess()));
        material.setReflectivity(doubleOr(entry.get("reflectivity"), material.getReflectivity()));
        material.setTransparency(doubleOr(entry.get("transparency"), material.getTransparency()));
        material.setRefractiveIndex(doubleOr(entry.get("refractiveIndex"), material.getRefractiveIndex()));
    }

    private static Model3D reconstructModel3D(String source, Vector3D position, Color color) {
        if (source == null) return null;
        if (Primitives.PYRAMID_SOURCE.equals(source)) return Primitives.getPyramid(position, color);
        return OBJReader.getModel3D(source, position, color, 1.0, new Vector3D(0, 0, 0));
    }

    private static Camera readCamera(Map<String, Object> entry) {
        String type = (String) entry.get("type");
        Vector3D position = readVector(entry.get("position"));
        double yawDegrees = doubleOr(entry.get("yawDegrees"), 0);
        double pitchDegrees = doubleOr(entry.get("pitchDegrees"), 0);
        double near = doubleOr(entry.get("nearPlane"), 0.1);
        double far = doubleOr(entry.get("farPlane"), 400);

        Camera camera = switch (type) {
            case "Perspective" -> new PerspertiveCamera(position, yawDegrees, pitchDegrees, near, far,
                    doubleOr(entry.get("fovDegrees"), 60));
            case "Fisheye" -> new FisheyeCamera(position, yawDegrees, pitchDegrees, near, far,
                    doubleOr(entry.get("fovDegrees"), 180));
            case "Orthographic" -> {
                // The constructor hardcodes yaw/pitch to 0 (an orthographic camera has no
                // "look at" point) — set the saved aim explicitly afterward instead.
                OrthographicCamera ortho = new OrthographicCamera(position,
                        doubleOr(entry.get("left"), -5), doubleOr(entry.get("right"), 5),
                        doubleOr(entry.get("top"), 5), doubleOr(entry.get("bottom"), -5), near, far);
                ortho.setYawRadians(Math.toRadians(yawDegrees));
                ortho.setPitchRadians(Math.toRadians(pitchDegrees));
                yield ortho;
            }
            default -> null;
        };

        if (camera != null) {
            String name = (String) entry.get("name");
            if (name != null) camera.setName(name);
        }
        return camera;
    }

    private static Light readLight(Map<String, Object> entry) {
        String type = (String) entry.get("type");
        Color color = readColor(entry.get("color"));
        double intensity = doubleOr(entry.get("intensity"), 1.0);

        Light light = switch (type) {
            case "Point" -> new PointLight(readVector(entry.get("position")), color, intensity);
            case "Spot" -> {
                SpotLight spot = new SpotLight(readVector(entry.get("position")), readVector(entry.get("direction")),
                        color, intensity, doubleOr(entry.get("coneAngleDegrees"), 30));
                spot.setPenumbraDegrees(doubleOr(entry.get("penumbraDegrees"), spot.getPenumbraDegrees()));
                yield spot;
            }
            case "Directional" -> {
                DirectionalLight directional = new DirectionalLight(readVector(entry.get("direction")), color, intensity);
                if (entry.containsKey("position")) directional.setPosition(readVector(entry.get("position")));
                yield directional;
            }
            default -> null;
        };

        if (light != null) {
            String name = (String) entry.get("name");
            if (name != null) light.setName(name);
        }
        return light;
    }

    private static Vector3D readVector(Object raw) {
        List<Object> list = listOf(raw);
        return new Vector3D(doubleOr(get(list, 0), 0), doubleOr(get(list, 1), 0), doubleOr(get(list, 2), 0));
    }

    private static Color readColor(Object raw) {
        List<Object> list = listOf(raw);
        return new Color(
                (int) doubleOr(get(list, 0), 200),
                (int) doubleOr(get(list, 1), 200),
                (int) doubleOr(get(list, 2), 200));
    }

    @SuppressWarnings("unchecked")
    private static List<Object> listOf(Object raw) {
        return raw instanceof List ? (List<Object>) raw : List.of();
    }

    private static Object get(List<Object> list, int index) {
        return index < list.size() ? list.get(index) : null;
    }

    private static double doubleOr(Object raw, double fallback) {
        return raw instanceof Number number ? number.doubleValue() : fallback;
    }
}
