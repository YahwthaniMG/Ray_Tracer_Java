package edu.up.isgc.cg.raytracer;


import edu.up.isgc.cg.raytracer.cameras.Camera;
import edu.up.isgc.cg.raytracer.lights.Light;
import edu.up.isgc.cg.raytracer.math.Intersection;
import edu.up.isgc.cg.raytracer.math.Ray;
import edu.up.isgc.cg.raytracer.math.Vector3D;
import edu.up.isgc.cg.raytracer.objects.Object3D;

import java.awt.*;
import java.awt.image.BufferedImage;

import static edu.up.isgc.cg.raytracer.math.Vector3D.*;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.lang.Math.max;
import static java.lang.Math.pow;


/**
 * The type Render controller.
 *
 * @author Yahwthani Morales
 */
public final class RenderController {


    /**
     * Render buffered image.
     *
     * @param scene       the scene
     * @param resolution  the resolution
     * @param aspectRatio the aspect ratio
     * @param OP          the op
     * @return the buffered image
     */
    public static BufferedImage render(Scene scene, int resolution, double aspectRatio, int OP) {
        BufferedImage image = new BufferedImage((int) (resolution * aspectRatio), resolution, TYPE_INT_RGB);
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        for (int x = 0; x < imageWidth; x++) {
            for (int y = 0; y < imageHeight; y++) {
                double[] uv = getScreenCoordinates(x, y, imageWidth, imageHeight);
                Color color = CColor(scene, uv[0], uv[1], OP);
                image.setRGB(x, y, color.getRGB());
            }
        }

        return image;
    }


    /**
     *
     * @param x
     * @param y
     * @param width
     * @param height
     * Gets the specific coordinates of each pixel in the image
     * @return double[]
     */
    private static double[] getScreenCoordinates(int x, int y, int width, int height){
        double u, v;
        if (width > height) {
            u = (double) (x - width / 2 + height / 2) / height * 2 - 1;
            v = -((double) y / height * 2 - 1);
        } else {
            u = (double) x / width * 2 - 1;
            v = -((double) (y - height / 2 + width / 2) / width * 2 - 1);
        }
        return new double[]{u, v};
    }

    /**
     *
     * @param scene
     * @param u
     * @param v
     * @param OP
     * From a coordinate call raycast to get the color of the pixel
     * @return Color
     */
    private static Color CColor(Scene scene, double u, double v, int OP){
        Camera camera = scene.getCamera();
        Ray ray = camera.makeRay(u, v);
        return raycast(ray, scene, null, OP);
    }

    /**
     * Gets the color of the point where it collided
     */
    private static Color raycast(Ray ray, Scene scene, Object3D caster, int OP) {
        Camera camera = scene.getCamera();
        Intersection intersection = getClosestIntersection(ray, scene, caster);
        if (intersection == null) return Color.BLACK;

        Color pixelColor = Color.BLACK;
        Color ambient = getAmbient(intersection);
        pixelColor = ColorRGB.add(pixelColor, ambient);
        Object3D object = intersection.getObject();
        Vector3D P = intersection.getPosition();

        for (Light light : scene.getLights()) {
            Vector3D L = light.getDirection(P);
            Ray rayToLight = new Ray(P, L);
            if (getClosestIntersection(rayToLight, scene, object) != null) continue;
            Color diffuse = getDiffuse(intersection, light);
            Color specular = getSpecular(intersection, camera, light);
            pixelColor = ColorRGB.add(pixelColor, diffuse, specular);
        }
        int x=OP;
        if(x==0) {
            Ray reflectionRay = getReflectionRay(intersection, scene);
            Color reflectionColor = raycast(reflectionRay, scene, object,OP);
            pixelColor = ColorRGB.add(pixelColor, reflectionColor);
        }
        else if(x==1){
            Ray reflactionRay = getReflactionRay(intersection, scene);
            Color reflactionColor = raycast(reflactionRay, scene, object,OP);
            pixelColor = ColorRGB.add(pixelColor, reflactionColor);
        }


        return pixelColor;
    }

    /**
     * Gets the intersection
     */
    private static Intersection getClosestIntersection(Ray ray, Scene scene, Object3D caster){
        double nearPlane = scene.getCamera().getNearPlane();
        double farPlane = scene.getCamera().getFarPlane();
        Intersection closestIntersection = null;
        for (Object3D object : scene.getObjects()) {
            if (object.equals(caster)) continue;
            Intersection intersection = object.getIntersection(ray);
            if (intersection == null) continue;
            double distance = intersection.getDistanceFrom(ray.getOrigin());
            if (caster == null && distance <= nearPlane || distance >= farPlane)
                continue;
            if (closestIntersection == null) closestIntersection = intersection;
            else if (distance < closestIntersection.getDistanceFrom(ray.getOrigin())) {
                closestIntersection = intersection;
            }
        }
        return closestIntersection;
    }

    /**
     *
     * @param intersection
     * Gets the color of the intersection of the object
     * @return Color
     */
    private static Color getAmbient(Intersection intersection){
        Color objectColor = intersection.getObject().getColor();
        double ambient = 0.3;
        return ColorRGB.multiply(objectColor, ambient);
    }

    /**
     *
     * @param intersection
     * @param light
     * Gets the color of the object and makes it a diffuse
     * @return Color
     */
    private static Color getDiffuse(Intersection intersection, Light light){
        Vector3D N = intersection.getNormal();
        Vector3D P = intersection.getPosition();
        Vector3D L = light.getDirection(P);
        Color lightColor = light.getColor(P);
        Color objectColor = intersection.getObject().getColor();
        Color diffuseColor = ColorRGB.multiply(objectColor, 0.1);
        return ColorRGB.multiply(ColorRGB.add(diffuseColor, lightColor), max(0, dotProduct(N, L)));

    }

    private static Color getSpecular (Intersection intersection, Camera camera, Light light){
        Vector3D P = intersection.getPosition();
        Vector3D V = normalize(subtract(camera.getPosition(), P));
        Vector3D L = light.getDirection(P);
        Vector3D H = normalize(add(L, V));
        Vector3D N = intersection.getNormal();
        Color objectColor = intersection.getObject().getColor();
        Color lightColor = light.getColor(P);
        Color specularColor = ColorRGB.multiply(objectColor, 1.4);
        double phongExponent = 120;
        double shininess = pow(dotProduct(N, H), phongExponent);
        return ColorRGB.multiply(ColorRGB.multiply(specularColor, lightColor), shininess);
    }

    /**
     *
     * @param intersection
     * @param scene
     * Generates a reflection ray, to obtain the color to be reflected
     * @return Ray
     */
    private static Ray getReflectionRay(Intersection intersection, Scene scene) {
        Camera camera = scene.getCamera();
        Vector3D N = intersection.getNormal();
        Vector3D P = intersection.getPosition();
        Vector3D D = normalize(subtract(P, camera.getPosition()));
        Vector3D R = subtract(D, scalarMultiplication(N, 1.8 * dotProduct(D, N)));
        return new Ray(P, R);
    }

    /**
     *
     * @param intersection
     * @param scene
     * Generates a refracting ray, to obtain the color it reflects
     * @return Ray
     */
    private static Ray getReflactionRay(Intersection intersection, Scene scene) {
        Camera camera = scene.getCamera();
        Vector3D P = intersection.getPosition();
        Vector3D D = normalize(subtract(camera.getPosition(),P));
        Vector3D N = intersection.getNormal();
        double n=.8;
        double CosI=-dotProduct(N,D);
        double SinT2=(n*n) * (1-(CosI*CosI));
        Vector3D R = subtract(D, scalarMultiplication(N, 1.8 * dotProduct(D, N)));
        if(SinT2>1.0)return new Ray(P,R);
        double CosT=Math.sqrt(1.0-SinT2);
        Vector3D aux=scalarMultiplication(D,n);
        double aux2=(n*(CosI-CosT));
        Vector3D aux3=scalarMultiplication(N,aux2);
        return new Ray(aux, aux3);
    }
}
