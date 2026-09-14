package edu.up.isgc.cg.raytracer;


import edu.up.isgc.cg.raytracer.cameras.Camera;
import edu.up.isgc.cg.raytracer.lights.Light;
import edu.up.isgc.cg.raytracer.math.Intersection;
import edu.up.isgc.cg.raytracer.math.Ray;
import edu.up.isgc.cg.raytracer.math.Vector3D;
import edu.up.isgc.cg.raytracer.objects.Material;
import edu.up.isgc.cg.raytracer.objects.Object3D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleConsumer;

import static edu.up.isgc.cg.raytracer.math.Vector3D.*;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.lang.Math.max;
import static java.lang.Math.pow;


/**
 * The type Render controller.
 *
 * @author Yahwthani Morales, with Claude (Anthropic)
 */
public final class RenderController {


    /**
     * Render buffered image, using the scene's active camera.
     *
     * @param scene       the scene
     * @param resolution  the resolution
     * @param aspectRatio the aspect ratio
     * @param mode        the secondary ray effect to apply (reflection/refraction/none)
     * @return the buffered image
     */
    public static BufferedImage render(Scene scene, int resolution, double aspectRatio, RenderMode mode) {
        return render(scene, scene.getActiveCamera(), resolution, aspectRatio, mode);
    }

    /**
     * Render buffered image with an explicit camera, regardless of which camera is
     * currently active in the scene. This is what lets a scene hold several cameras
     * (e.g. a few angles you're comparing) while a single render picks just one of them.
     *
     * @param scene       the scene
     * @param camera      the camera to render with
     * @param resolution  the resolution
     * @param aspectRatio the aspect ratio
     * @param mode        the secondary ray effect to apply (reflection/refraction/none)
     * @return the buffered image
     */
    public static BufferedImage render(Scene scene, Camera camera, int resolution, double aspectRatio, RenderMode mode) {
        return render(scene, camera, resolution, aspectRatio, mode, 1, null);
    }

    /**
     * Render buffered image, reporting progress (0.0–1.0, as columns finish) and checking
     * for cancellation — for driving a progress bar and a Cancel button from a
     * {@code SwingWorker} without freezing the UI thread. Returns {@code null} if
     * cancelled (the caller's thread was interrupted) before finishing.
     *
     * <p>The image is split into column ranges and traced across a thread pool sized to
     * the machine's core count — each pixel only reads {@code scene}/{@code camera}, never
     * mutates them, and every task writes to its own disjoint columns of {@code image}, so
     * this is safe without any locking. Cancelling the caller's thread (e.g.
     * {@code SwingWorker.cancel(true)}) while this is blocked waiting for the tasks
     * propagates through {@link ExecutorService#invokeAll} as an {@code InterruptedException},
     * which cancels/interrupts whatever tasks are still running or haven't started yet.</p>
     *
     * @param scene       the scene
     * @param camera      the camera to render with
     * @param resolution  the resolution
     * @param aspectRatio the aspect ratio
     * @param mode        the secondary ray effect to apply (reflection/refraction/none)
     * @param samples     anti-aliasing samples per pixel (1 = off, matches the pre-AA
     *                    behavior exactly; otherwise rounded down to the nearest perfect
     *                    square and traced on a centered subpixel grid — e.g. 4 traces a
     *                    2×2 grid, 9 a 3×3 grid — and averaged)
     * @param onProgress  called with the fraction complete as each column finishes, or {@code null}
     * @return the buffered image, or {@code null} if the render was cancelled
     */
    public static BufferedImage render(Scene scene, Camera camera, int resolution, double aspectRatio,
                                        RenderMode mode, int samples, DoubleConsumer onProgress) {
        BufferedImage image = new BufferedImage((int) (resolution * aspectRatio), resolution, TYPE_INT_RGB);
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int sampleGrid = Math.max(1, (int) Math.sqrt(Math.max(1, samples)));

        int threadCount = Math.max(1, Runtime.getRuntime().availableProcessors());
        // More, smaller chunks than threads so a thread that finishes an easy (cheap to
        // shade) chunk early can pick up another one instead of sitting idle while a
        // neighbor works through a chunk full of reflective/refractive geometry.
        int chunkSize = Math.max(1, imageWidth / (threadCount * 4));
        AtomicInteger columnsDone = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int start = 0; start < imageWidth; start += chunkSize) {
            int fromX = start;
            int toX = Math.min(imageWidth, start + chunkSize);
            tasks.add(() -> {
                for (int x = fromX; x < toX; x++) {
                    if (Thread.currentThread().isInterrupted()) return null;
                    for (int y = 0; y < imageHeight; y++) {
                        Color color = renderPixel(scene, camera, x, y, imageWidth, imageHeight, mode, sampleGrid);
                        image.setRGB(x, y, color.getRGB());
                    }
                    int done = columnsDone.incrementAndGet();
                    if (onProgress != null) onProgress.accept((double) done / imageWidth);
                }
                return null;
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        try {
            for (Future<Void> result : executor.invokeAll(tasks)) {
                result.get();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            cancelled.set(true);
        } catch (ExecutionException failure) {
            throw new RuntimeException("Render failed", failure.getCause());
        } finally {
            executor.shutdownNow();
        }

        return cancelled.get() ? null : image;
    }


    /**
     * Renders one pixel, optionally as the average of several sub-pixel samples for
     * anti-aliasing. {@code sampleGrid == 1} traces a single ray through {@code (x, y)}
     * itself — bit-for-bit what this did before anti-aliasing existed; any larger grid
     * traces {@code sampleGrid * sampleGrid} rays through the centers of a subpixel grid
     * spanning the pixel and averages their color.
     */
    private static Color renderPixel(Scene scene, Camera camera, int x, int y, int imageWidth, int imageHeight,
                                      RenderMode mode, int sampleGrid) {
        // The same grid also sets how many shadow rays soften each light's shadow edges
        // (see raycast/shadowVisibility) — one "quality" dial instead of a second control
        // nobody would know how to weigh against this one.
        if (sampleGrid <= 1) {
            double[] uv = getScreenCoordinates(x, y, imageWidth, imageHeight);
            return CColor(scene, camera, uv[0], uv[1], mode, sampleGrid);
        }

        long r = 0, g = 0, b = 0;
        int sampleCount = sampleGrid * sampleGrid;
        for (int sy = 0; sy < sampleGrid; sy++) {
            for (int sx = 0; sx < sampleGrid; sx++) {
                double offsetX = (sx + 0.5) / sampleGrid;
                double offsetY = (sy + 0.5) / sampleGrid;
                double[] uv = getScreenCoordinates(x + offsetX, y + offsetY, imageWidth, imageHeight);
                Color sample = CColor(scene, camera, uv[0], uv[1], mode, sampleGrid);
                r += sample.getRed();
                g += sample.getGreen();
                b += sample.getBlue();
            }
        }
        return new Color((int) (r / sampleCount), (int) (g / sampleCount), (int) (b / sampleCount));
    }

    /**
     *
     * @param x
     * @param y
     * @param width
     * @param height
     * Gets the specific coordinates of each pixel in the image
     * @return double[]
     */
    private static double[] getScreenCoordinates(double x, double y, int width, int height){
        double u, v;
        if (width > height) {
            u = (x - width / 2 + height / 2) / height * 2 - 1;
            v = -(y / height * 2 - 1);
        } else {
            u = x / width * 2 - 1;
            v = -((y - height / 2 + width / 2) / width * 2 - 1);
        }
        return new double[]{u, v};
    }

    /**
     *
     * @param scene
     * @param camera
     * @param u
     * @param v
     * @param mode
     * From a coordinate call raycast to get the color of the pixel
     * @return Color
     */
    private static Color CColor(Scene scene, Camera camera, double u, double v, RenderMode mode, int shadowGrid){
        Ray ray = camera.makeRay(u, v);
        return raycast(ray, scene, camera, null, mode, 0, shadowGrid);
    }

    /**
     * How many reflection/refraction bounces to follow before just stopping (only
     * shading the last hit directly) instead of casting yet another secondary ray.
     * Without this, two facing reflective surfaces — e.g. a floor and a plane rotated
     * to face it, or any mirror "hallway" — bounce a ray back and forth forever, which
     * doesn't just take a long time to render, it blows the call stack with
     * {@code StackOverflowError} because each bounce is a nested recursive call.
     */
    private static final int MAX_BOUNCE_DEPTH = 8;

    /**
     * Gets the color of the point where it collided
     */
    private static Color raycast(Ray ray, Scene scene, Camera camera, Object3D caster, RenderMode mode, int depth, int shadowGrid) {
        Intersection intersection = getClosestIntersection(ray, scene, camera, caster);
        if (intersection == null) return Color.BLACK;

        Object3D object = intersection.getObject();
        Material material = object.getMaterial();
        Vector3D P = intersection.getPosition();
        // The direction back toward wherever this ray came from, for the specular
        // highlight. For the primary ray that's the camera (kept exactly as before, since
        // the camera's actual eye point sits at a small fixed offset from its stored
        // position — see PerspertiveCamera#makeRay — so this isn't quite equivalent to
        // just negating the ray's direction); for a reflection/refraction bounce it's the
        // previous surface, which -ray.getDirection() gives directly.
        Vector3D viewDirection = depth == 0
                ? normalize(subtract(camera.getPosition(), P))
                : normalize(negative(ray.getDirection()));

        Color ownShading = getAmbient(intersection);
        for (Light light : scene.getLights()) {
            double visibility = shadowVisibility(P, object, light, scene, camera, shadowGrid);
            if (visibility <= 0) continue;
            Color diffuse = getDiffuse(intersection, light);
            Color specular = getSpecular(intersection, viewDirection, light);
            ownShading = ColorRGB.add(ownShading, ColorRGB.multiply(diffuse, visibility), ColorRGB.multiply(specular, visibility));
        }

        // A material's own reflectivity/transparency drives secondary rays regardless of
        // the render-wide Mode; Mode's REFLECTION/REFRACTION/BOTH additionally forces
        // *every* surface to bounce at full strength, as a quick "make it all mirror/
        // glass" preview that doesn't require touching each object's material. BOTH forces
        // both at once — a material can likewise have reflectivity and transparency set
        // simultaneously (e.g. a slightly reflective pane of glass), which already worked
        // without needing the Mode combo at all.
        double reflectWeight = 0, transparentWeight = 0;
        if (depth < MAX_BOUNCE_DEPTH) {
            boolean forceReflection = mode == RenderMode.REFLECTION || mode == RenderMode.BOTH;
            boolean forceRefraction = mode == RenderMode.REFRACTION || mode == RenderMode.BOTH;
            reflectWeight = material.getReflectivity() > 0
                    ? material.getReflectivity() : (forceReflection ? 1.0 : 0.0);
            transparentWeight = material.getTransparency() > 0
                    ? material.getTransparency() : (forceRefraction ? 1.0 : 0.0);
        }

        // Reflection/refraction are layered on top of the surface's own shading at their
        // material's weight, same as this renderer already did (just render-wide before,
        // now per-material) — a true energy-conserving blend (fading ownShading out as
        // reflectivity/transparency rise) looks worse here in practice, since this
        // raytracer has no environment/sky color: a highly reflective object with nothing
        // in particular to reflect would just go dark instead of showing its own color.
        Color pixelColor = ownShading;

        if (reflectWeight > 0) {
            Ray reflectionRay = getReflectionRay(ray, intersection);
            Color reflectionColor = raycast(reflectionRay, scene, camera, object, mode, depth + 1, shadowGrid);
            pixelColor = ColorRGB.add(pixelColor, ColorRGB.multiply(reflectionColor, reflectWeight));
        }
        if (transparentWeight > 0) {
            Ray refractionRay = getRefractionRay(ray, intersection, material.getRefractiveIndex());
            Color refractionColor = raycast(refractionRay, scene, camera, object, mode, depth + 1, shadowGrid);
            pixelColor = ColorRGB.add(pixelColor, ColorRGB.multiply(refractionColor, transparentWeight));
        }

        return pixelColor;
    }

    /**
     * How much of a light reaches {@code point} — 1.0 fully lit, 0.0 fully in shadow, or
     * something in between for a point partly occluded from an area light's surface.
     *
     * <p>{@code shadowGrid <= 1} (anti-aliasing off) casts exactly one shadow ray at
     * the light's true center, bit-for-bit the hard all-or-nothing shadow test this had
     * before soft shadows existed. Otherwise it casts a grid of rays across
     * {@link Light#getShadowSampleDirection} (a small assumed disk for Point/SpotLight, a
     * small angular spread for DirectionalLight) and returns the fraction that reach it
     * unobstructed — the same sample count already chosen for anti-aliasing, since both
     * are "how much extra work am I willing to pay for a cleaner image" knobs.</p>
     *
     * <p>Every shadow ray is capped at {@link Light#getDistanceTo} — without that cap, a
     * surface beyond the light along the same ray (e.g. a ceiling above a lamp, from the
     * point of view of the floor under it) would wrongly count as blocking it, since
     * nothing previously stopped the search at the light itself.</p>
     */
    private static double shadowVisibility(Vector3D P, Object3D caster, Light light, Scene scene, Camera camera, int shadowGrid) {
        double maxDistance = light.getDistanceTo(P);
        if (shadowGrid <= 1) {
            Ray rayToLight = new Ray(P, light.getDirection(P));
            return getClosestIntersection(rayToLight, scene, camera, caster, maxDistance) == null ? 1.0 : 0.0;
        }

        int total = shadowGrid * shadowGrid;
        int visible = 0;
        for (int sy = 0; sy < shadowGrid; sy++) {
            for (int sx = 0; sx < shadowGrid; sx++) {
                double offsetU = (sx + 0.5) / shadowGrid * 2 - 1;
                double offsetV = (sy + 0.5) / shadowGrid * 2 - 1;
                Vector3D L = light.getShadowSampleDirection(P, offsetU, offsetV);
                Ray rayToLight = new Ray(P, L);
                if (getClosestIntersection(rayToLight, scene, camera, caster, maxDistance) == null) visible++;
            }
        }
        return (double) visible / total;
    }

    /**
     * Gets the intersection, searching no farther than the camera's far plane.
     */
    private static Intersection getClosestIntersection(Ray ray, Scene scene, Camera camera, Object3D caster){
        return getClosestIntersection(ray, scene, camera, caster, camera.getFarPlane());
    }

    /**
     * Gets the closest intersection along {@code ray}, ignoring anything at or beyond
     * {@code maxDistance} — for a primary/secondary ray that's the camera's far plane
     * (effectively "no limit" for how this renderer is used); for a shadow ray it's the
     * light's own distance (see {@link #shadowVisibility}), so nothing past the light
     * itself is mistaken for something blocking it.
     */
    private static Intersection getClosestIntersection(Ray ray, Scene scene, Camera camera, Object3D caster, double maxDistance){
        double nearPlane = camera.getNearPlane();
        Intersection closestIntersection = null;
        for (Object3D object : scene.getObjects()) {
            if (object.equals(caster)) continue;
            Intersection intersection = object.getIntersection(ray);
            if (intersection == null) continue;
            double distance = intersection.getDistanceFrom(ray.getOrigin());
            if (caster == null && distance <= nearPlane || distance >= maxDistance)
                continue;
            if (closestIntersection == null) closestIntersection = intersection;
            else if (distance < closestIntersection.getDistanceFrom(ray.getOrigin())) {
                closestIntersection = intersection;
            }
        }
        return closestIntersection;
    }

    /**
     *
     * @param intersection
     * Gets the color of the intersection of the object
     * @return Color
     */
    private static Color getAmbient(Intersection intersection){
        Color objectColor = intersection.getObject().getColor();
        double ambient = intersection.getObject().getMaterial().getAmbient();
        return ColorRGB.multiply(objectColor, ambient);
    }

    /**
     *
     * @param intersection
     * @param light
     * Gets the color of the object and makes it a diffuse
     * @return Color
     */
    private static Color getDiffuse(Intersection intersection, Light light){
        Vector3D N = intersection.getNormal();
        Vector3D P = intersection.getPosition();
        Vector3D L = light.getDirection(P);
        Color lightColor = light.getColor(P);
        Color objectColor = intersection.getObject().getColor();
        double diffuse = intersection.getObject().getMaterial().getDiffuse();
        Color diffuseColor = ColorRGB.multiply(objectColor, diffuse);
        return ColorRGB.multiply(ColorRGB.add(diffuseColor, lightColor), max(0, dotProduct(N, L)));

    }

    private static Color getSpecular (Intersection intersection, Vector3D viewDirection, Light light){
        Vector3D V = viewDirection;
        Vector3D P = intersection.getPosition();
        Vector3D L = light.getDirection(P);
        Vector3D H = normalize(add(L, V));
        Vector3D N = intersection.getNormal();
        Color objectColor = intersection.getObject().getColor();
        Color lightColor = light.getColor(P);
        Material material = intersection.getObject().getMaterial();
        Color specularColor = ColorRGB.multiply(objectColor, material.getSpecular());
        double phongExponent = material.getShininess();
        double shininess = pow(dotProduct(N, H), phongExponent);
        return ColorRGB.multiply(ColorRGB.multiply(specularColor, lightColor), shininess);
    }

    /**
     * Generates a mirror-reflection ray off {@code intersection}'s surface for whatever
     * ray actually hit it (not necessarily the primary ray from the camera — see
     * {@code raycast}), using the standard {@code R = D - 2(D·N)N} formula.
     *
     * @param incomingRay  the ray that produced this intersection
     * @param intersection the intersection
     * @return the reflection ray
     */
    private static Ray getReflectionRay(Ray incomingRay, Intersection intersection) {
        Vector3D N = intersection.getNormal();
        Vector3D P = intersection.getPosition();
        Vector3D D = normalize(incomingRay.getDirection());
        Vector3D R = subtract(D, scalarMultiplication(N, 2 * dotProduct(D, N)));
        return new Ray(P, R);
    }

    /**
     * Generates a refracted (transmitted) ray through {@code intersection}'s surface,
     * bent according to Snell's law. Works whether the ray is entering the object (its
     * material's refractive index vs. air) or exiting it (flips the normal and inverts
     * the index ratio so the same formula applies from either side) — necessary since a
     * transparent object is crossed by a ray twice, once going in and once coming back
     * out. Falls back to a plain reflection on total internal reflection (when the
     * bend angle has no real solution).
     *
     * @param incomingRay     the ray that produced this intersection
     * @param intersection    the intersection
     * @param refractiveIndex the material's index of refraction
     * @return the refraction ray
     */
    private static Ray getRefractionRay(Ray incomingRay, Intersection intersection, double refractiveIndex) {
        Vector3D P = intersection.getPosition();
        Vector3D N = intersection.getNormal();
        Vector3D D = normalize(incomingRay.getDirection());

        double cosI = -dotProduct(N, D);
        Vector3D normal = N;
        double eta = 1.0 / refractiveIndex;
        if (cosI < 0) { // exiting the material rather than entering it
            normal = negative(N);
            cosI = -cosI;
            eta = refractiveIndex;
        }

        double sinT2 = eta * eta * (1 - cosI * cosI);
        if (sinT2 > 1.0) { // total internal reflection: no transmitted ray exists
            return getReflectionRay(incomingRay, intersection);
        }
        double cosT = Math.sqrt(1.0 - sinT2);
        Vector3D T = add(scalarMultiplication(D, eta), scalarMultiplication(normal, eta * cosI - cosT));
        return new Ray(P, T);
    }
}
