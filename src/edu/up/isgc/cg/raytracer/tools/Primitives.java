package edu.up.isgc.cg.raytracer.tools;

import edu.up.isgc.cg.raytracer.math.Vector3D;
import edu.up.isgc.cg.raytracer.objects.Model3D;
import edu.up.isgc.cg.raytracer.objects.Triangle;

import java.awt.Color;
import java.util.ArrayList;

import static edu.up.isgc.cg.raytracer.math.Vector3D.*;

/**
 * Small procedurally-built meshes for shapes the project has no bundled .obj file for
 * (there's no Pyramid.obj or Torus.obj under OBJS/). Only a square pyramid is provided —
 * a torus needs a real parametric mesh (dozens of segments) that isn't worth building
 * for the editor's geometry palette right now.
 *
 * @author Claude (Anthropic)
 */
public abstract class Primitives {

    /**
     * Gets a square pyramid, apex pointing up, centered on {@code position}.
     *
     * @param position the position
     * @param color    the color
     * @return the pyramid
     */
    public static Model3D getPyramid(Vector3D position, Color color) {
        double halfBase = 1.0;
        double height = 1.5;

        Vector3D apex = new Vector3D(0, height, 0);
        Vector3D a = new Vector3D(-halfBase, 0, -halfBase);
        Vector3D b = new Vector3D(halfBase, 0, -halfBase);
        Vector3D c = new Vector3D(halfBase, 0, halfBase);
        Vector3D d = new Vector3D(-halfBase, 0, halfBase);

        ArrayList<Triangle> triangles = new ArrayList<>();
        triangles.add(flatTriangle(a, b, apex));
        triangles.add(flatTriangle(b, c, apex));
        triangles.add(flatTriangle(c, d, apex));
        triangles.add(flatTriangle(d, a, apex));
        triangles.add(flatTriangle(c, b, a)); // base, split in two
        triangles.add(flatTriangle(d, c, a));

        Model3D model = new Model3D(position, color, triangles);
        model.setSource(PYRAMID_SOURCE);
        return model;
    }

    /** The {@link Model3D#getSource} tag {@link edu.up.isgc.cg.raytracer.tools.SceneIO} looks for to rebuild a pyramid on load. */
    public static final String PYRAMID_SOURCE = "primitive:pyramid";

    /** A flat-shaded triangle: same normal (from the winding order) on all 3 vertices. */
    private static Triangle flatTriangle(Vector3D v1, Vector3D v2, Vector3D v3) {
        Vector3D normal = normalize(crossProduct(subtract(v2, v1), subtract(v3, v1)));
        return new Triangle(new Vector3D[]{v1, v2, v3}, new Vector3D[]{normal, normal, normal});
    }
}
