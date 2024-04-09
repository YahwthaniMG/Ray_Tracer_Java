/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer.objects;

import edu.up.isgc.cg.raytracer.math.Intersection;
import edu.up.isgc.cg.raytracer.math.Ray;
import edu.up.isgc.cg.raytracer.math.Vector3D;

import static edu.up.isgc.cg.raytracer.math.Vector3D.*;

/**
 * The type Triangle.
 *
 * @author Jafet Rodríguez and Yahwthani Morales
 */
public class Triangle implements IIntersectable {
    private static final double EPSILON = 0.0000001;

    private Vector3D[] vertices;
    private Vector3D[] normals;

    /**
     * Instantiates a new Triangle.
     *
     * @param v1 the v 1
     * @param v2 the v 2
     * @param v3 the v 3
     */
    public Triangle(Vector3D v1, Vector3D v2, Vector3D v3) {
        this(new Vector3D[]{v1, v2, v3}, null);
    }

    /**
     * Instantiates a new Triangle.
     *
     * @param vertices the vertices
     * @param normals  the normals
     */
    public Triangle(Vector3D[] vertices, Vector3D[] normals) {
        setVertices(vertices);
        setNormals(normals);
    }

    /**
     * Get vertices vector 3 d [ ].
     *
     * @return the vector 3 d [ ]
     */
    public Vector3D[] getVertices() {
        return vertices;
    }

    private void setVertices(Vector3D[] vertices) {
        this.vertices = vertices;
    }

    /**
     * Get normals vector 3 d [ ].
     *
     * @return the vector 3 d [ ]
     */
    public Vector3D[] getNormals() {
        return normals;
    }

    /**
     * Sets normals.
     *
     * @param normals the normals
     */
    public void setNormals(Vector3D[] normals) {
        this.normals = normals;
    }

    /**
     *
     * @param ray
     * Gets the intersection of a ray at a point
     * @return Intersection
     */
    @Override
    public Intersection getIntersection(Ray ray) {
        Vector3D[] vert = getVertices();
        Vector3D edge1 = subtract(vert[1], vert[0]);
        Vector3D edge2 = subtract(vert[2], vert[0]);
        Vector3D h = crossProduct(ray.getDirection(), edge2);
        double a = dotProduct(edge1, h);
        if (a > -EPSILON && a < EPSILON) return null;
        double f = 1 / a;
        Vector3D s = subtract(ray.getOrigin(), vert[0]);
        double u = f * dotProduct(s, h);
        if (u < 0 || u > 1) return null;
        Vector3D q = crossProduct(s, edge1);
        double v = f * dotProduct(ray.getDirection(), q);
        if (v < 0 || u + v > 1) return null;
        double t = f * dotProduct(edge2, q);
        if (t <= EPSILON) return null;
        Vector3D P = ray.getPoint(t);
        return new Intersection(ray, null, P, null);
    }
}
