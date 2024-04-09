/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer.cameras;

import edu.up.isgc.cg.raytracer.math.Intersection;
import edu.up.isgc.cg.raytracer.math.Ray;
import edu.up.isgc.cg.raytracer.math.Vector3D;
import edu.up.isgc.cg.raytracer.objects.Object3D;

import java.awt.*;

/**
 * The type Camera.
 *
 * @author Jafet Rodríguez
 */
public abstract class Camera{
    private Vector3D position;
    private double yawRadians;
    private double pitchRadians;
    private double nearPlane;
    private double farPlane;

    /**
     * Instantiates a new Camera.
     *
     * @param position     the position
     * @param yawRadians   the yaw radians
     * @param pitchRadians the pitch radians
     * @param nearPlane    the near plane
     * @param farPlane     the far plane
     */
    public Camera(Vector3D position, double yawRadians, double pitchRadians,
                  double nearPlane, double farPlane) {
        setPosition(position);
        setYawRadians(yawRadians);
        setPitchRadians(pitchRadians);
        setNearPlane(nearPlane);
        setFarPlane(farPlane);
    }

    private void setPosition(Vector3D position){this.position= position;}

    /**
     * Get position vector 3 d.
     *
     * @return the vector 3 d
     */
    public Vector3D getPosition(){return position;}

    private void setYawRadians(double yawRadians){
        this.yawRadians=yawRadians;
    }

    /**
     * Get yaw radians double.
     *
     * @return the double
     */
    public double getYawRadians(){
        return yawRadians;
    }

    private void setPitchRadians(double pitchRadians){
        this.pitchRadians=pitchRadians;
    }

    /**
     * Get pitch radians double.
     *
     * @return the double
     */
    public double getPitchRadians(){
        return pitchRadians;
    }

    private void setNearPlane(double nearPlane) {
        this.nearPlane=nearPlane;
    }

    /**
     * Get near plane double.
     *
     * @return the double
     */
    public double getNearPlane(){
        return nearPlane;
    }

    /**
     * Set far plane.
     *
     * @param farPlane the far plane
     */
    public void setFarPlane(double farPlane){
        this.farPlane=farPlane;
    }

    /**
     * Get far plane double.
     *
     * @return the double
     */
    public double getFarPlane(){
        return farPlane;
    }

    /**
     * Make ray ray.
     *
     * @param u the u
     * @param v the v
     * @return the ray
     */
    public abstract Ray makeRay(double u, double v);








}
