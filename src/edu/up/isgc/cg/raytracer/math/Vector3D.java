/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer.math;

import static java.lang.Math.*;

/**
 * The type Vector 3 d.
 *
 * @author Jafet Rodríguez
 */
public class Vector3D {

    private double x;
    private double y;
    private double z;

    /**
     * Instantiates a new Vector 3 d.
     *
     * @param x the x
     * @param y the y
     * @param z the z
     */
    public Vector3D(double x, double y, double z) {
        setX(x);
        setY(y);
        setZ(z);
    }

    /**
     * Instantiates a new Vector 3 d.
     *
     * @param d the d
     */
    public Vector3D(double d) {
        this(d, d, d);
    }

    /**
     * Instantiates a new Vector 3 d.
     */
    public Vector3D() {
        this(0);
    }

    /**
     * Gets x.
     *
     * @return the x
     */
    public double getX() {
        return x;
    }

    /**
     * Sets x.
     *
     * @param x the x
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Gets y.
     *
     * @return the y
     */
    public double getY() {
        return y;
    }

    /**
     * Sets y.
     *
     * @param y the y
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Gets z.
     *
     * @return the z
     */
    public double getZ() {
        return z;
    }

    /**
     * Sets z.
     *
     * @param z the z
     */
    public void setZ(double z) {
        this.z = z;
    }

    /**
     * Length double.
     *
     * @return the double
     */
    public double length() {
        return sqrt(dotProduct(this, this));
    }

    @Override
    public String toString() {
        return "Vector3D{" +
                "x=" + getX() +
                ", y=" + getY() +
                ", z=" + getZ() +
                "}";
    }

    /**
     * Dot product double.
     *
     * @param vectorA the vector a
     * @param vectorB the vector b
     * @return the double
     */
    public static double dotProduct(Vector3D vectorA, Vector3D vectorB) {
        return (vectorA.getX() * vectorB.getX()) + (vectorA.getY() * vectorB.getY()) + (vectorA.getZ() * vectorB.getZ());
    }

    /**
     * Cross product vector 3 d.
     *
     * @param vectorA the vector a
     * @param vectorB the vector b
     * @return the vector 3 d
     */
    public static Vector3D crossProduct(Vector3D vectorA, Vector3D vectorB){
        return new Vector3D(
                (vectorA.getY() * vectorB.getZ()) - (vectorA.getZ() * vectorB.getY()),
                (vectorA.getZ() * vectorB.getX()) - (vectorA.getX() * vectorB.getZ()),
                (vectorA.getX() * vectorB.getY()) - (vectorA.getY() * vectorB.getX()));
    }

    /**
     * Magnitude double.
     *
     * @param vectorA the vector a
     * @return the double
     */
    public static double magnitude (Vector3D vectorA){
        return Math.sqrt(dotProduct(vectorA, vectorA));
    }

    /**
     * Add vector 3 d.
     *
     * @param vectorA the vector a
     * @param vectorB the vector b
     * @return the vector 3 d
     */
    public static Vector3D add(Vector3D vectorA, Vector3D vectorB){
        return new Vector3D(vectorA.getX() + vectorB.getX(), vectorA.getY() + vectorB.getY(), vectorA.getZ() + vectorB.getZ());
    }

    /**
     * Subtract vector 3 d.
     *
     * @param vectorA the vector a
     * @param vectorB the vector b
     * @return the vector 3 d
     */
    public static Vector3D subtract(Vector3D vectorA, Vector3D vectorB){
        return new Vector3D(vectorA.getX() - vectorB.getX(), vectorA.getY() - vectorB.getY(), vectorA.getZ() - vectorB.getZ());
    }

    /**
     * Normalize vector 3 d.
     *
     * @param vectorA the vector a
     * @return the vector 3 d
     */
    public static Vector3D normalize(Vector3D vectorA){
        return divide(vectorA, magnitude(vectorA));
    }

    /**
     * Scalar multiplication vector 3 d.
     *
     * @param vectorA the vector a
     * @param scalar  the scalar
     * @return the vector 3 d
     */
    public static Vector3D scalarMultiplication(Vector3D vectorA, double scalar){
        return new Vector3D(vectorA.getX() * scalar, vectorA.getY() * scalar, vectorA.getZ() * scalar);
    }

    /**
     * Negative vector 3 d.
     *
     * @param vector the vector
     * @return the vector 3 d
     */
    public static Vector3D negative(Vector3D vector){
        return new Vector3D(-vector.getX(), -vector.getY(), -vector.getZ());
    }

    /**
     * Rotate yp vector 3 d.
     *
     * @param yaw   the yaw
     * @param pitch the pitch
     * @return the vector 3 d
     */
    public Vector3D rotateYP(double yaw, double pitch) {

        double y = getY() * cos(pitch) - getZ() * sin(pitch);
        double z = getY() * sin(pitch) + getZ() * cos(pitch);
        double x = getX() * cos(yaw) + z * sin(yaw);
        z = -getX() * sin(yaw) + z * cos(yaw);
        return new Vector3D(x, y, z);
    }

    /**
     * Divide vector 3 d.
     *
     * @param a the a
     * @param d the d
     * @return the vector 3 d
     */
    public static Vector3D divide(Vector3D a, double d) {
        return scalarMultiplication(a, 1 / d);
    }

    /**
     * Matrix rotate vector 3 d.
     *
     * @param vector the vector
     * @param v      the v
     * @return the vector 3 d
     */
    public static Vector3D matrixRotate (Vector3D vector, Vector3D v){
        double[] rotatedVector = new double[3];
        double[][] rotationMatrix = new double[3][3];
        double x=v.getX();
        double y=v.getY();
        double z=v.getZ();
        double x1=vector.getX();
        double y1=vector.getY();
        double z1=vector.getZ();
        double [] vector1={x1,y1,z1};

        rotationMatrix[0][0] = Math.cos(y) * Math.cos(z);
        rotationMatrix[0][1] = -Math.cos(y) * Math.sin(y);
        rotationMatrix[0][2] = Math.sin(y);
        rotationMatrix[1][0] = Math.cos(x) * Math.sin(z) + Math.sin(x) * Math.sin(y) * Math.cos(z);
        rotationMatrix[1][1] = Math.cos(x) * Math.cos(z) - Math.sin(x) * Math.sin(y) * Math.sin(z);
        rotationMatrix[1][2] = -Math.sin(x) * Math.cos(y);
        rotationMatrix[2][0] = Math.sin(x) * Math.sin(z) - Math.cos(x) * Math.sin(y) * Math.cos(z);
        rotationMatrix[2][1] = Math.sin(x) * Math.cos(z) + Math.cos(x) * Math.sin(y) * Math.sin(z);
        rotationMatrix[2][2] = Math.cos(x) * Math.cos(y);

        for (int i = 0; i < 3; i++) {
            rotatedVector[i] = 0;
            for (int j = 0; j < 3; j++) {
                rotatedVector[i] += rotationMatrix[i][j] * vector1[j];
            }
        }
        Vector3D RotateV=new Vector3D(rotatedVector[0],rotatedVector[1],rotatedVector[2]);

        return RotateV;
    }
}

