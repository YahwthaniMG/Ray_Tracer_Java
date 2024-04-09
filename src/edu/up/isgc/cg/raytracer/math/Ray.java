/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer.math;

import static edu.up.isgc.cg.raytracer.math.Vector3D.*;

/**
 * The type Ray.
 *
 * @author Jafet Rodríguez and Yahwthani Morales
 */
public class Ray {
    private Vector3D origin;
    private Vector3D direction;

    /**
     * Instantiates a new Ray.
     *
     * @param origin    the origin
     * @param direction the direction
     */
    public Ray(Vector3D origin, Vector3D direction) {
        setOrigin(origin);
        setDirection(direction);
    }

    /**
     * Gets origin.
     *
     * @return the origin
     */
    public Vector3D getOrigin() {
        return origin;
    }

    private void setOrigin(Vector3D origin) {
        this.origin = origin;
    }

    /**
     * Gets direction.
     *
     * @return the direction
     */
    public Vector3D getDirection() {
        return Vector3D.normalize(direction);
    }

    private void setDirection(Vector3D direction) {
        this.direction =magnitude(direction)== 1? direction : normalize(direction);

    }

    /**
     * Get point vector 3 d.
     *
     * @param l the l
     * @return the vector 3 d
     */
    public Vector3D getPoint(double l){
        return add(getOrigin(),scalarMultiplication(getDirection(),l));
    }
}

