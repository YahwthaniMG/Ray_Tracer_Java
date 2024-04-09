/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer.tools;

import edu.up.isgc.cg.raytracer.math.Vector3D;
import edu.up.isgc.cg.raytracer.objects.Model3D;
import edu.up.isgc.cg.raytracer.objects.Triangle;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.up.isgc.cg.raytracer.math.Vector3D.scalarMultiplication;

/**
 * The type Obj reader.
 *
 * @author Jafet Rodríguez and Yahwthani Morales
 */
public abstract class OBJReader {

    /**
     * Gets model 3 d.
     *
     * @param path     the path
     * @param position the position
     * @param color    the color
     * @param scale    the scale
     * @param rotate   the rotate
     *                 Read the vertices and faces of the objects obj
     * @return the model 3 d
     */
    public static Model3D getModel3D(String path, Vector3D position, Color color, Double scale, Vector3D rotate) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));

            ArrayList<Triangle> triangles = new ArrayList<>();
            ArrayList<Vector3D> vertices = new ArrayList<>();
            ArrayList<Vector3D> normals = new ArrayList<>();
            String line;
            int defaultSmoothingGroup = -1;
            int smoothingGroup = defaultSmoothingGroup;
            HashMap<Integer, ArrayList<Triangle>> smoothingMap = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("v ") || line.startsWith("vn ")) {
                    String[] vertexComponents = line.split("(\\s)+");
                    if (vertexComponents.length >= 4) {
                        double x = Double.parseDouble(vertexComponents[1]);
                        double y = Double.parseDouble(vertexComponents[2]);
                        double z = Double.parseDouble(vertexComponents[3]);
                        if (line.startsWith("v ")) {
                            vertices.add(Vector3D.matrixRotate(Vector3D.scalarMultiplication(new Vector3D(x, y, z), scale), rotate));
                        } else {
                            normals.add(Vector3D.matrixRotate(Vector3D.scalarMultiplication(new Vector3D(x, y, z), scale), rotate));
                        }
                    }
                } else if (line.startsWith("f ")) {
                    String[] faceComponents = line.split("(\\s)+");
                    ArrayList<Integer> faceVertex = new ArrayList<>();
                    ArrayList<Integer> faceNormal = new ArrayList<>();

                    for (int i = 1; i < faceComponents.length; i++) {
                        String[] infoVertex = faceComponents[i].split("/");
                        if (infoVertex.length >= 1) {
                            int vertexIndex = Integer.parseInt(infoVertex[0]);
                            faceVertex.add(vertexIndex);
                        }
                        if (infoVertex.length >= 3) {
                            int normalIndex = Integer.parseInt(infoVertex[2]);
                            faceNormal.add(normalIndex);
                        }
                    }

                    if (faceVertex.size() >= 3) {
                        Vector3D[] triangleVertices = new Vector3D[faceVertex.size()];
                        Vector3D[] triangleVerticesNormals = new Vector3D[faceNormal.size()];

                        for (int i = 0; i < faceVertex.size(); i++) {
                            triangleVertices[i] = (vertices.get(faceVertex.get(i) - 1));
                        }

                        Vector3D[] arrangedTriangleVertices;
                        Vector3D[] arrangedTriangleNormals = null;
                        if (normals.size() > 0 && !faceNormal.isEmpty()) {
                            for (int i = 0; i < faceNormal.size(); i++) {
                                triangleVerticesNormals[i] = (normals.get(faceNormal.get(i) - 1));
                            }
                            arrangedTriangleNormals = new Vector3D[]{triangleVerticesNormals[1], triangleVerticesNormals[0], triangleVerticesNormals[2]};
                        }
                        arrangedTriangleVertices = new Vector3D[]{triangleVertices[1], triangleVertices[0], triangleVertices[2]};


                        Triangle tmpTriangle = new Triangle(arrangedTriangleVertices, arrangedTriangleNormals);
                        triangles.add(tmpTriangle);

                        ArrayList<Triangle> trianglesInMap = smoothingMap.get(smoothingGroup);
                        if (trianglesInMap == null) {
                            trianglesInMap = new ArrayList<>();
                        }
                        trianglesInMap.add(tmpTriangle);

                        if (faceVertex.size() == 4) {
                            arrangedTriangleVertices = new Vector3D[]{triangleVertices[2], triangleVertices[0], triangleVertices[3]};
                            if (arrangedTriangleNormals != null) {
                                arrangedTriangleNormals = new Vector3D[]{triangleVerticesNormals[2], triangleVerticesNormals[0], triangleVerticesNormals[3]};
                            }
                            tmpTriangle = new Triangle(arrangedTriangleVertices, arrangedTriangleNormals);
                            triangles.add(tmpTriangle);
                            trianglesInMap.add(tmpTriangle);
                        }

                        if (smoothingGroup != defaultSmoothingGroup) {
                            smoothingMap.put(smoothingGroup, trianglesInMap);
                        }
                    }
                } else if (line.startsWith("s ")) {
                    String[] smoothingComponents = line.split("(\\s)+");
                    if (smoothingComponents.length > 1) {
                        if (smoothingComponents[1].equals("off")) {
                            smoothingGroup = defaultSmoothingGroup;
                        } else {
                            try {
                                smoothingGroup = Integer.parseInt(smoothingComponents[1]);
                            } catch (NumberFormatException nfe) {
                                smoothingGroup = defaultSmoothingGroup;
                            }
                        }
                    }
                }
            }
            reader.close();

            class NormalPair {
                Vector3D normal;
                int count;

                public NormalPair() {
                    normal = new Vector3D();
                    count = 0;
                }
            }

            //Smooth vertices normals
            for (Integer key : smoothingMap.keySet()) {
                HashMap<Vector3D, NormalPair> vertexMap = new HashMap<>();
                ArrayList<Triangle> trianglesInMap = smoothingMap.get(key);
                for (Triangle triangle : trianglesInMap) {
                    Vector3D[] triangleVertices = triangle.getVertices();
                    Vector3D[] triangleNormals = triangle.getNormals();
                    for (int i = 0; i < triangleVertices.length; i++) {
                        NormalPair normalsVertex = vertexMap.get(triangleVertices[i]);
                        if (normalsVertex == null) {
                            normalsVertex = new NormalPair();
                        }
                        if (triangleNormals.length > 0 && i < triangleNormals.length) {
                            normalsVertex.normal = Vector3D.add(normalsVertex.normal, triangleNormals[i]);
                            normalsVertex.count++;
                        }
                        vertexMap.put(triangleVertices[i], normalsVertex);
                    }
                }
                for (Triangle triangle : trianglesInMap) {
                    Vector3D[] triangleVertices = triangle.getVertices();
                    Vector3D[] triangleNormals = triangle.getNormals();
                    for (int i = 0; i < triangleVertices.length; i++) {
                        NormalPair normalsVertex = vertexMap.get(triangleVertices[i]);
                        triangleNormals[i] = scalarMultiplication(normalsVertex.normal, 1.0 / (double) normalsVertex.count);
                    }
                    triangle.setNormals(triangleNormals);
                }
            }

            return new Model3D(position, color, triangles);
        } catch (FileNotFoundException ex) {
            System.err.println("File not found");
        } catch (IOException ex) {
            System.err.println("Exception found");
        }

        return null;
    }
}








