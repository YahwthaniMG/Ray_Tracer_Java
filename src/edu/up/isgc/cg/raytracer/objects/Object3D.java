/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer.objects;

import edu.up.isgc.cg.raytracer.math.Vector3D;

import java.awt.*;

/**
 * The type Object 3 d.
 *
 * @author Jafet Rodríguez
 */
public abstract class Object3D implements IIntersectable{
    private Color color;
    private Vector3D position;


    /**
     * Instantiates a new Object 3 d.
     *
     * @param position the position
     * @param color    the color
     */
    public Object3D(Vector3D position, Color color) {
        setPosition(position);
        setColor(color);
    }


    /**
     * Gets color.
     *
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets color.
     *
     * @param color the color
     */
    protected void setColor(Color color) {
        this.color = color;
    }

    /**
     * Gets position.
     *
     * @return the position
     */
    public Vector3D getPosition() {
        return position;
    }

    /**
     * Sets position.
     *
     * @return the position
     */
    private void setPosition(Vector3D position) {
        this.position = position;
    }

}
