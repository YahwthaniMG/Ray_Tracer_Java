/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer;

import edu.up.isgc.cg.raytracer.lights.Light;
import edu.up.isgc.cg.raytracer.cameras.Camera;
import edu.up.isgc.cg.raytracer.objects.Object3D;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Scene.
 *
 * @author Jafet Rodríguez and Yahwthani Morales
 */
public class Scene {

    private Camera camera;
    private ArrayList<Object3D> objects;
    private ArrayList<Light> lights;

    private Scene (Camera camera, ArrayList<Object3D> objects, ArrayList<Light> lights){
        setObjects(objects);
        setLights(lights);
        setCamera(camera);
    }

    /**
     * Instantiates a new Scene.
     */
    public Scene() {
        this(null, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Get objects array list.
     *
     * @return the array list
     */
    public ArrayList<Object3D> getObjects(){return objects;}

    private void setObjects(ArrayList<Object3D> objects){
        this.objects = objects;
    }

    /**
     * Add object.
     *
     * @param object the object
     */
    public void addObject(Object3D object){
        getObjects().add(object);
    }

    /**
     * Get lights array list.
     *
     * @return the array list
     */
    public ArrayList<Light> getLights(){
        return lights;
    }

    private void setLights(ArrayList<Light> lights){
        this.lights=lights;
    }

    /**
     * Add light.
     *
     * @param light the light
     */
    public void addLight(Light light){
        getLights().add(light);
    }

    /**
     * Get camera camera.
     *
     * @return the camera
     */
    public Camera getCamera(){
        return camera;
    }

    /**
     * Set camera.
     *
     * @param camera the camera
     */
    public void setCamera(Camera camera){
        this.camera= camera;
    }
}

