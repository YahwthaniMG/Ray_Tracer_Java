/**
 * [1968] - [2022] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer.math;


import edu.up.isgc.cg.raytracer.objects.Triangle;
import static edu.up.isgc.cg.raytracer.math.Vector3D.*;

/**
 * The type Barycentric.
 *
 * @author Jafet Rodríguez and Yahwthani Morales
 * @see <a href="https://www.amazon.com/Real-Time-Collision-Detection-Interactive-Technology/dp/1558607323">Christer Ericson's Real-Time Collision Detection</a>
 */
public  final class Barycentric {

    private Barycentric() {
    }

    /**
     * Calculate barycentric coordinates double [ ].
     *
     * @param point    the point
     * @param triangle the triangle
     * @return the double [ ]
     */
// Based on Christer Ericson's Real-Time Collision Detection
    public static double[] CalculateBarycentricCoordinates(Vector3D point, Triangle triangle) {
        double u, v, w;
        Vector3D[] vertices = triangle.getVertices();
        Vector3D a = vertices[0];
        Vector3D b = vertices[1];
        Vector3D c = vertices[2];

        Vector3D v0 = subtract(b, a);
        Vector3D v1 = subtract(c, a);
        Vector3D v2 = subtract(point, a);
        double d00 = dotProduct(v0, v0);
        double d01 = dotProduct(v0, v1);
        double d11 = dotProduct(v1, v1);
        double d20 = dotProduct(v2, v0);
        double d21 = dotProduct(v2, v1);
        double denominator = d00 * d11 - d01 * d01;
        v = (d11 * d20 - d01 * d21) / denominator;
        w = (d00 * d21 - d01 * d20) / denominator;
        u = 1.0 - v - w;

        return new double[]{u, v, w};
    }

}
