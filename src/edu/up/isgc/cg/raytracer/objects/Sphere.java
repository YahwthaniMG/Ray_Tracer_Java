/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer.objects;

import edu.up.isgc.cg.raytracer.math.Intersection;
import edu.up.isgc.cg.raytracer.math.Ray;
import edu.up.isgc.cg.raytracer.math.Vector3D;

import java.awt.*;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import static edu.up.isgc.cg.raytracer.math.Vector3D.*;

/**
 * The type Sphere.
 *
 * @author Jafet Rodríguez
 */
public class Sphere extends Object3D{
    private double radius;

    private double getRadius() {
        return radius;
    }

    private void setRadius(double radius) {
        this.radius = radius;
    }

    /**
     * Instantiates a new Sphere.
     *
     * @param position the position
     * @param radius   the radius
     * @param color    the color
     */
    public Sphere(Vector3D position, double radius, Color color) {
        super(position, color);
        setRadius(radius);
    }

    /**
     *
     * @param ray
     * Gets the intersection of a ray at a point
     * @return Intersection
     */
    @Override
    public Intersection getIntersection(Ray ray) {
        double t= dotProduct(subtract(getPosition(), ray.getOrigin()), ray.getDirection());
        Vector3D P = ray.getPoint(t);
        double y = magnitude(subtract(getPosition(), P));
        if(y >=getRadius()) return null;
        double x = sqrt(pow(getRadius(),2)-pow(y,2));
        double t1= t-x;
        if(t1 <=0) return null;
        P = ray.getPoint(t1);
        Vector3D N = normalize(subtract(P, getPosition()));
        return new Intersection(ray, this, P, N);
    }
}
