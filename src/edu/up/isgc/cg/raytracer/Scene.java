/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer;

import edu.up.isgc.cg.raytracer.animation.SceneAnimation;
import edu.up.isgc.cg.raytracer.lights.Light;
import edu.up.isgc.cg.raytracer.cameras.Camera;
import edu.up.isgc.cg.raytracer.objects.Object3D;

import java.util.ArrayList;

/**
 * The type Scene.
 *
 * <p>A scene can hold several cameras at once (e.g. a few angles you're comparing while
 * blocking out a shot) — one of them is the "active" camera. The active camera is only
 * a default: it's what the live editor looks through and what a render uses when no
 * camera is explicitly requested, but {@link RenderController#render(Scene, Camera, int, double, RenderMode)}
 * can render with any camera in the scene regardless of which one is active.</p>
 *
 * @author Jafet Rodríguez and Yahwthani Morales, with Claude (Anthropic)
 */
public class Scene {

    private ArrayList<Camera> cameras;
    private Camera activeCamera;
    private ArrayList<Object3D> objects;
    private ArrayList<Light> lights;
    private final SceneAnimation animation = new SceneAnimation();

    private Scene (ArrayList<Camera> cameras, ArrayList<Object3D> objects, ArrayList<Light> lights){
        setObjects(objects);
        setLights(lights);
        setCameras(cameras);
    }

    /**
     * Instantiates a new Scene.
     */
    public Scene() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
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
     * Remove object.
     *
     * @param object the object
     * @return true if the object was in the scene
     */
    public boolean removeObject(Object3D object){
        return getObjects().remove(object);
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
     * Remove light.
     *
     * @param light the light
     * @return true if the light was in the scene
     */
    public boolean removeLight(Light light){
        return getLights().remove(light);
    }

    /**
     * Get every camera currently placed in the scene.
     *
     * @return the array list
     */
    public ArrayList<Camera> getCameras(){
        return cameras;
    }

    private void setCameras(ArrayList<Camera> cameras){
        this.cameras = cameras;
    }

    /**
     * Adds a camera to the scene. The first camera ever added becomes the active one
     * automatically so a freshly-built scene always has a usable default.
     *
     * @param camera the camera
     */
    public void addCamera(Camera camera){
        if (camera == null || getCameras().contains(camera)) return;
        getCameras().add(camera);
        if (activeCamera == null) setActiveCamera(camera);
    }

    /**
     * Removes a camera from the scene. If it was the active camera, the next available
     * camera (if any) becomes active.
     *
     * @param camera the camera
     * @return true if the camera was in the scene
     */
    public boolean removeCamera(Camera camera){
        boolean removed = getCameras().remove(camera);
        if (removed && camera == activeCamera) {
            activeCamera = getCameras().isEmpty() ? null : getCameras().get(0);
        }
        return removed;
    }

    /**
     * Gets the camera the live editor previews through and that a render falls back to
     * when no camera is explicitly chosen.
     *
     * @return the active camera
     */
    public Camera getActiveCamera(){
        return activeCamera;
    }

    /**
     * Sets which of the scene's cameras is active. Adds it to the scene first if it
     * wasn't already part of it.
     *
     * @param camera the camera
     */
    public void setActiveCamera(Camera camera){
        addCamera(camera);
        this.activeCamera = camera;
    }

    /**
     * Convenience alias for {@link #getActiveCamera()}, kept so existing code (and
     * {@link RenderController}'s internals) can keep asking "the scene's camera" without
     * caring whether there are other cameras parked in the scene.
     *
     * @return the active camera
     */
    public Camera getCamera(){
        return getActiveCamera();
    }

    /**
     * Convenience alias for {@link #setActiveCamera(Camera)}.
     *
     * @param camera the camera
     */
    public void setCamera(Camera camera){
        setActiveCamera(camera);
    }

    /**
     * Removes every object, light and camera from the scene.
     */
    public void clear(){
        getObjects().clear();
        getLights().clear();
        getCameras().clear();
        activeCamera = null;
        animation.clear();
    }

    /**
     * Gets this scene's animation (keyframes over time for whichever objects/cameras/
     * lights have any, plus the overall duration/fps) — see {@link SceneAnimation}. Never
     * {@code null}; a fresh scene's animation simply has no keyframes yet. The raytracer
     * itself never reads this — only the editor's timeline and batch frame renderer do,
     * pushing interpolated values into these same objects before rendering/repainting.
     *
     * @return the animation
     */
    public SceneAnimation getAnimation() {
        return animation;
    }
}
