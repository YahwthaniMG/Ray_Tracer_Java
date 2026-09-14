/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer.objects;

import edu.up.isgc.cg.raytracer.math.Intersection;
import edu.up.isgc.cg.raytracer.math.Ray;
import edu.up.isgc.cg.raytracer.math.Vector3D;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static edu.up.isgc.cg.raytracer.math.Barycentric.CalculateBarycentricCoordinates;
import static edu.up.isgc.cg.raytracer.math.Vector3D.scalarMultiplication;

/**
 * The type Model 3 d.
 *
 * <p>Unlike {@link Sphere} or {@link Plane}, this object's triangles are baked into
 * world space (they're what {@link #getIntersection} actually tests against) instead of
 * being re-derived from {@code position} each time. To support moving/rotating/scaling
 * it live from the editor without drift, a snapshot of each unique vertex's local-space
 * value (as it was right after {@link edu.up.isgc.cg.raytracer.tools.OBJReader} applied
 * its own load-time scale/rotate) is kept, and the world-space vertices are recomputed
 * from that snapshot — {@code world = position + rotate(local * scale, rotation)} —
 * whenever position, rotation or scale changes. Recomputing from the pristine snapshot
 * (rather than incrementally nudging the live vertices) keeps repeated edits exact.</p>
 *
 * @author Jafet Rodríguez and Yahwthani Morales, with Claude (Anthropic)
 */
public class Model3D extends Object3D {

    private ArrayList<Triangle> triangles;
    private Map<Vector3D, Vector3D> localVertices; // live vertex instance -> its local-space value
    private Vector3D rotation = new Vector3D(0, 0, 0); // additional rotation (radians), on top of load-time rotation
    private double scale = 1.0; // additional uniform scale, on top of load-time scale
    private String source; // where this mesh came from (an .obj path, or a "primitive:" tag) — see getSource

    /**
     * Instantiates a new Model 3 d.
     *
     * @param position  the position
     * @param color     the color
     * @param triangles the triangles
     */
    public Model3D(Vector3D position, Color color, ArrayList<Triangle> triangles) {
        super(position, color);
        setTriangles(triangles);
    }

    /**
     * Gets triangles.
     *
     * @return the triangles
     */
    public ArrayList<Triangle> getTriangles() {
        return triangles;
    }

    /**
     * Sets triangles, capturing their current values as the local-space snapshot that
     * position/rotation/scale edits are applied on top of from now on.
     *
     * @param triangles the triangles
     */
    public void setTriangles(ArrayList<Triangle> triangles) {
        this.triangles = triangles;
        captureLocalSnapshot();
        applyTransform();
    }

    /**
     * Moves the model (recomputes world vertices from the local snapshot).
     *
     * @param position the new position
     */
    @Override
    public void setPosition(Vector3D position) {
        super.setPosition(position);
        applyTransform();
    }

    /**
     * Gets the model's additional rotation, in radians, on top of whatever rotation
     * {@link edu.up.isgc.cg.raytracer.tools.OBJReader} applied when it was loaded.
     *
     * @return the rotation
     */
    public Vector3D getRotation() {
        return rotation;
    }

    /**
     * Sets the model's additional rotation, in radians.
     *
     * @param rotation the rotation
     */
    public void setRotation(Vector3D rotation) {
        this.rotation = rotation;
        applyTransform();
    }

    /**
     * Gets the model's additional uniform scale (1.0 = as loaded).
     *
     * @return the scale
     */
    public double getScale() {
        return scale;
    }

    /**
     * Sets the model's additional uniform scale (1.0 = as loaded).
     *
     * @param scale the scale
     */
    public void setScale(double scale) {
        this.scale = scale;
        applyTransform();
    }

    /**
     * Gets where this mesh was built from: an {@code .obj} file path (see
     * {@link edu.up.isgc.cg.raytracer.tools.OBJReader}), a {@code "primitive:"}-prefixed
     * tag for a procedurally-built shape (see {@link edu.up.isgc.cg.raytracer.tools.Primitives}),
     * or {@code null} if untagged. {@link edu.up.isgc.cg.raytracer.tools.SceneIO} uses this
     * to rebuild the model when a saved scene is loaded, since re-baking triangle-by-triangle
     * into the save file would just duplicate the source file.
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Tags where this mesh was built from — see {@link #getSource}.
     *
     * @param source the source
     */
    public void setSource(String source) {
        this.source = source;
    }

    private void captureLocalSnapshot() {
        localVertices = new LinkedHashMap<>();
        for (Triangle triangle : triangles) {
            for (Vector3D vertex : triangle.getVertices()) {
                localVertices.putIfAbsent(vertex, new Vector3D(vertex.getX(), vertex.getY(), vertex.getZ()));
            }
        }
    }

    private void applyTransform() {
        if (localVertices == null) return;
        Vector3D position = getPosition();
        for (Map.Entry<Vector3D, Vector3D> entry : localVertices.entrySet()) {
            Vector3D live = entry.getKey();
            Vector3D local = entry.getValue();
            Vector3D transformed = Vector3D.matrixRotate(scalarMultiplication(local, scale), rotation);
            Vector3D world = Vector3D.add(position, transformed);
            live.setX(world.getX());
            live.setY(world.getY());
            live.setZ(world.getZ());
        }
    }

    /**
     *
     * @param ray
     * Gets the intersection of a ray at a point
     * @return Intersection
     */
    @Override
    public Intersection getIntersection(Ray ray) {
        Intersection closestIntersection = null;
        for (Triangle triangle : getTriangles()) {
            Intersection intersection = triangle.getIntersection(ray);
            if (intersection == null) continue;
            intersection.setObject(this);
            Vector3D N = new Vector3D();
            double[] uVw = CalculateBarycentricCoordinates(intersection.getPosition(), triangle);
            Vector3D[] normals = triangle.getNormals();
            for(int i = 0; i < uVw.length; i++) {
                N = Vector3D.add(N, scalarMultiplication(normals[i], uVw[i]));
            }
            intersection.setNormal(N);
            double distance = intersection.getDistanceFrom(ray.getOrigin());
            if (closestIntersection == null) closestIntersection = intersection;
            else if (distance < closestIntersection.getDistanceFrom(ray.getOrigin())) {
                closestIntersection = intersection;
            }
        }
        return closestIntersection;
    }
}
