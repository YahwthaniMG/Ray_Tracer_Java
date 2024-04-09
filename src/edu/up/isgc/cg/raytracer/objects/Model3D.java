/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer.objects;

import edu.up.isgc.cg.raytracer.math.Intersection;
import edu.up.isgc.cg.raytracer.math.Ray;
import edu.up.isgc.cg.raytracer.math.Vector3D;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

import static edu.up.isgc.cg.raytracer.math.Barycentric.CalculateBarycentricCoordinates;
import static edu.up.isgc.cg.raytracer.math.Vector3D.scalarMultiplication;

/**
 * The type Model 3 d.
 *
 * @author Jafet Rodríguez and Yahwthani Morales
 */
public class Model3D extends Object3D {

    private ArrayList<Triangle> triangles;

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
     * Sets triangles.
     *
     * @param triangles the triangles
     */
    public void setTriangles(ArrayList<Triangle> triangles) {
        Vector3D position = getPosition();
        HashSet<Vector3D> uniqueVertices = new HashSet<>();
        for(Triangle triangle : triangles){
            uniqueVertices.addAll(List.of(triangle.getVertices()));
        }

        for(Vector3D vertex : uniqueVertices){
            vertex.setX(vertex.getX() + position.getX());
            vertex.setY(vertex.getY() + position.getY());
            vertex.setZ(vertex.getZ() + position.getZ());
        }

        this.triangles = triangles;
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
