/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer.math;

import edu.up.isgc.cg.raytracer.objects.Object3D;

import static edu.up.isgc.cg.raytracer.math.Vector3D.magnitude;
import static edu.up.isgc.cg.raytracer.math.Vector3D.subtract;

/**
 * The type Intersection.
 *
 * @author Jafet Rodríguez
 */
public class Intersection {
    private Ray ray;
    private Object3D object;
    private Vector3D position;
    private Vector3D normal;

    /**
     * Instantiates a new Intersection.
     *
     * @param ray      the ray
     * @param object   the object
     * @param position the position
     * @param normal   the normal
     */
    public Intersection(Ray ray, Object3D object,Vector3D position,Vector3D normal) {
        setRay(ray);
        setObject(object);
        setPosition(position);
        setNormal(normal);
    }


    /**
     * Gets ray.
     *
     * @return the ray
     */
    public Ray getRay() {
        return ray;
    }
    private void setRay(Ray ray) {
        this.ray = ray;
    }

    /**
     * Gets normal.
     *
     * @return the normal
     */
    public Vector3D getNormal() {
        return normal;
    }

    /**
     * Sets normal.
     *
     * @param normal the normal
     */
    public void setNormal(Vector3D normal) {
        this.normal = normal;
    }

    /**
     * Gets position.
     *
     * @return the position
     */
    public Vector3D getPosition() {
        return position;
    }

    private void setPosition(Vector3D position) {
        this.position = position;
    }

    /**
     * Gets object.
     *
     * @return the object
     */
    public Object3D getObject() {
        return object;
    }

    /**
     * Sets object.
     *
     * @param object the object
     */
    public void setObject(Object3D object) {
        this.object = object;
    }

    /**
     * Get distance from double.
     *
     * @param point the point
     * @return the double
     */
    public double getDistanceFrom(Vector3D point){
        return magnitude(subtract(getPosition(),point));
    }

}
