/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer.objects;

import edu.up.isgc.cg.raytracer.math.Intersection;
import edu.up.isgc.cg.raytracer.math.Ray;

/**
 * The interface Intersectable.
 *
 * @author Jafet Rodríguez
 */
public interface IIntersectable {
    /**
     * Gets intersection.
     *
     * @param ray the ray
     * @return the intersection
     */
    Intersection getIntersection(Ray ray);
}
