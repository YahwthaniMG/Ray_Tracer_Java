package edu.up.isgc.cg.raytracer.objects;

import edu.up.isgc.cg.raytracer.math.Intersection;
import edu.up.isgc.cg.raytracer.math.Ray;
import edu.up.isgc.cg.raytracer.math.Vector3D;

import java.awt.*;

/**
 * The type Plane.
 */
public class Plane extends Object3D implements IIntersectable{

    /**
     * Instantiates a new Plane.
     *
     * @param y     the y
     * @param color the color
     */
    public Plane(double y, Color color){
        super(new Vector3D(0,y,0), color);
    }

    /**
     *
     * @param ray
     * Gets the intersection of a ray at a point
     * @return Intersection
     */
    @Override
    public Intersection getIntersection(Ray ray){
        double t= -(ray.getOrigin().getY() - getPosition().getY()) / ray.getDirection().getY();
        if (t <= 0 || !Double.isFinite(t)) return null;
        Vector3D P = ray.getPoint(t);
        return new Intersection(ray, this, P, new Vector3D(0,1,0));
    }


}
