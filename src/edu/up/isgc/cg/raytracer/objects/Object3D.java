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
 * @author Jafet Rodríguez, with Claude (Anthropic)
 */
public abstract class Object3D implements IIntersectable{
    private Color color;
    private Vector3D position;
    private String name;
    private Material material = new Material();


    /**
     * Instantiates a new Object 3 d.
     *
     * @param position the position
     * @param color    the color
     */
    public Object3D(Vector3D position, Color color) {
        setPosition(position);
        setColor(color);
        setName(getClass().getSimpleName());
    }

    /**
     * Gets the display name used by the scene editor (defaults to the simple class name).
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name used by the scene editor.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
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
    public void setColor(Color color) {
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
     * Sets position. Subclasses whose geometry is baked in world space at load time
     * (e.g. {@link Model3D}) override this to keep their geometry in sync.
     *
     * @param position the position
     */
    public void setPosition(Vector3D position) {
        this.position = position;
    }

    /**
     * Gets how this object's surface reacts to light — see {@link Material}.
     *
     * @return the material
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Sets the object's material.
     *
     * @param material the material
     */
    public void setMaterial(Material material) {
        this.material = material;
    }

}
