/**
 * [1968] - [2023] Centros Culturales de Mexico A.C / Universidad Panamericana
 * All Rights Reserved.
 */
package edu.up.isgc.cg.raytracer;
import edu.up.isgc.cg.raytracer.cameras.PerspertiveCamera;
import edu.up.isgc.cg.raytracer.lights.PointLight;
import edu.up.isgc.cg.raytracer.math.Vector3D;
import edu.up.isgc.cg.raytracer.objects.Plane;

import static edu.up.isgc.cg.raytracer.tools.OBJReader.getModel3D;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import javax.swing.JOptionPane;


/**
 * The type Raytracer.
 *
 * @author Jafet Rodríguez and Yahwthani Morales
 */
public class Raytracer {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        int OP=2;
        int Res=100;
        String aspRat=(JOptionPane.showInputDialog(null,"Select an aspect ratio:","AspectRatio",
                JOptionPane.PLAIN_MESSAGE,null,new Object[]{"Select","16/9", "16/10", "1/2", "1/1", "3/4","6/9"},
                "Select")).toString();
        String[] ans= (aspRat.split("/"));
        float w=  Integer.parseInt(ans[0]) ;
        float h=  Integer.parseInt(ans[1]) ;
        String Refle_Refra=(JOptionPane.showInputDialog(null,"Select an option:","Reflection and Refraction",
                JOptionPane.PLAIN_MESSAGE,null,new Object[]{"None","Reflection","Refraction"},
                "None")).toString();
        if(Refle_Refra=="Reflection"){
            OP=0;
        } else if (Refle_Refra=="Refraction") {
            OP=1;
        }
        String Resolution=(JOptionPane.showInputDialog(null,"Select a resolution:","Resulution",
                JOptionPane.PLAIN_MESSAGE,null,new Object[]{"100","200","300","400","500","600","700"
                        ,"800","900","1080","1156","1200","1536", "2160"},
                        "100")).toString();
        Res=Integer.parseInt(Resolution);
        System.out.println(LocalDateTime.now());
        BufferedImage image;

        /*
        image = RenderController.render(getTestPROScene(), Res, w / h, OP);
        saveImage(image, "TestPRO_02.png");
        image = RenderController.render(getTestRAPScene(), Res, w / h, OP);
        saveImage(image, "TestRAP.png");

        image = RenderController.render(getFirstScene(), Res, w / h, OP);
        saveImage(image, "YahwthaniMorales_Render01.png");
        image = RenderController.render(getSecondScene(), Res, w / h, OP);
        saveImage(image, "YahwthaniMorales_Render02.png");
        image = RenderController.render(getThirdScene(), Res, w / h, OP);
        saveImage(image, "YahwthaniMorales_Render03.png");
        image = RenderController.render(getFourthScene(), Res, w / h, OP);
        saveImage(image, "YahwthaniMorales_Render04.png");
        image = RenderController.render(getFifthScene(), Res, w / h, OP);
        saveImage(image, "YahwthaniMorales_Render05.png");
        image = RenderController.render(getSixthScene(), Res, w / h, OP);
        saveImage(image, "YahwthaniMorales_Render06.png");

        //image = RenderController.render(getTestScene(), Res, w / h, OP);
        //saveImage(image, "Test01.png");
        */

        System.out.println(LocalDateTime.now());
    }

    /**
     * Save image.
     *
     * @param image    the image
     * @param filename the filename
     */
    public static void saveImage(BufferedImage image, String filename){
        try {
            File file = new File("Renders/" + filename);
            ImageIO.write(image, "png", file);
        } catch (IOException e){
            System.err.println("There was an error saving the image."+ e);
        }
    }


    /**
     * Gets first scene.
     *
     * @return the first scene
     */
    public static Scene getFirstScene() {
        Scene scene = new Scene();
        scene.setCamera(new PerspertiveCamera(new Vector3D(0,5,-5),0,20,
                0.1,400,100));
        scene.addObject(new Plane(0,new Color(5,50,255)));

        scene.addObject(getModel3D("OBJS/RowBoat1.obj", new Vector3D(-3,0,0),
                new Color(128, 64, 0), 2.25, new Vector3D(0,0,.08)));
        scene.addObject(getModel3D("OBJS/Steve.obj", new Vector3D(-3.8,1.5,-0.8),
                new Color(255, 255, 255), 0.125, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Cube.obj", new Vector3D(45,15,25),
                new Color(255, 247, 166, 255), 5.0, new Vector3D(-0.5,0.5,0.1)));

        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(18,25,40),
                new Color(255, 255, 68), 0.005, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-12,35,50),
                new Color(255, 255, 68), 0.008, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(28,30,80),
                new Color(255, 255, 68), 0.01, new Vector3D(0,0.1,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-47,38,60),
                new Color(55, 55, 200), 0.011, new Vector3D(0.1,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(38,40,25),
                new Color(155, 155, 255), 0.01, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-32,35,85),
                new Color(255, 155, 200), 0.012, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(78,28,90),
                new Color(255, 255, 250), 0.014, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-77,35,75),
                new Color(255, 155, 0), 0.008, new Vector3D(0,0,0)));
        scene.addLight(new PointLight(new Vector3D(45, 25, 20), Color.WHITE, 5));
        return scene;
    }

    /**
     * Gets second scene.
     *
     * @return the second scene
     */
    public static Scene getSecondScene() {
        Scene scene = new Scene();
        scene.setCamera(new PerspertiveCamera(new Vector3D(0,5,-5),0,20,
                0.1,400,100));
        scene.addObject(new Plane(0,new Color(5,50,255)));

        scene.addObject(getModel3D("OBJS/RowBoat1.obj", new Vector3D(-3,0,0),
                new Color(128, 64, 0), 2.25, new Vector3D(0,0,.08)));
        scene.addObject(getModel3D("OBJS/Steve.obj", new Vector3D(-3.8,1.5,-0.8),
                new Color(255, 255, 255), 0.125, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Cube.obj", new Vector3D(42,15,25),
                new Color(255, 247, 166, 255), 5.0, new Vector3D(-0.5,0.5,0.1)));

        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(15,25,40),
                new Color(255, 255, 68), 0.005, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-15,35,50),
                new Color(255, 255, 68), 0.008, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(25,30,80),
                new Color(255, 255, 68), 0.01, new Vector3D(0,0.1,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-50,38,60),
                new Color(55, 55, 200), 0.011, new Vector3D(0.1,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(35,40,25),
                new Color(155, 155, 255), 0.01, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-35,35,85),
                new Color(255, 155, 200), 0.012, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(75,28,90),
                new Color(255, 255, 250), 0.014, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-80,35,75),
                new Color(255, 155, 0), 0.008, new Vector3D(0,0,0)));
        scene.addLight(new PointLight(new Vector3D(45, 25, 20), Color.WHITE, 5));
        return scene;
    }

    /**
     * Gets third scene.
     *
     * @return the third scene
     */
    public static Scene getThirdScene() {
        Scene scene = new Scene();
        scene.setCamera(new PerspertiveCamera(new Vector3D(0,5,-5),0,20,
                0.1,400,100));
        scene.addObject(new Plane(0,new Color(5,50,255)));

        scene.addObject(getModel3D("OBJS/RowBoat1.obj", new Vector3D(-3,0,0),
                new Color(128, 64, 0), 2.25, new Vector3D(0,0,.08)));
        scene.addObject(getModel3D("OBJS/Steve.obj", new Vector3D(-3.8,1.5,-0.8),
                new Color(255, 255, 255), 0.125, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/dolphin.obj", new Vector3D(11,-3,0),
                new Color(98, 114, 126), 0.125, new Vector3D(0,0,0)));

        scene.addObject(getModel3D("OBJS/Cube.obj", new Vector3D(39,15,25),
                new Color(255, 247, 166, 255), 5.0, new Vector3D(-0.5,0.5,0.1)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(12,25,40),
                new Color(255, 255, 68), 0.005, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-18,35,50),
                new Color(255, 255, 68), 0.008, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(22,30,80),
                new Color(255, 255, 68), 0.01, new Vector3D(0,0.1,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-53,38,60),
                new Color(55, 55, 200), 0.011, new Vector3D(0.1,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(32,40,25),
                new Color(155, 155, 255), 0.01, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-38,35,85),
                new Color(255, 155, 200), 0.012, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(72,28,90),
                new Color(255, 255, 250), 0.014, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-83,35,75),
                new Color(255, 155, 0), 0.008, new Vector3D(0,0,0)));

        scene.addLight(new PointLight(new Vector3D(45, 25, 20), Color.WHITE, 5));
        return scene;
    }

    /**
     * Gets fourth scene.
     *
     * @return the fourth scene
     */
    public static Scene getFourthScene() {
        Scene scene = new Scene();
        scene.setCamera(new PerspertiveCamera(new Vector3D(0,5,-5),0,20,
                0.1,400,100));
        scene.addObject(new Plane(0,new Color(5,50,255)));

        scene.addObject(getModel3D("OBJS/RowBoat1.obj", new Vector3D(-3,0,0),
                new Color(128, 64, 0), 2.25, new Vector3D(0,0,.08)));
        scene.addObject(getModel3D("OBJS/Steve.obj", new Vector3D(-3.8,1.5,-0.8),
                new Color(255, 255, 255), 0.125, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/dolphin.obj", new Vector3D(9,0,0),
                new Color(98, 114, 126), 0.125, new Vector3D(0,0,0)));

        scene.addObject(getModel3D("OBJS/Cube.obj", new Vector3D(38,15,25),
                new Color(255, 247, 166, 255), 5.0, new Vector3D(-0.5,0.5,0.1)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(11,25,40),
                new Color(255, 255, 68), 0.005, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-19,35,50),
                new Color(255, 255, 68), 0.008, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(21,30,80),
                new Color(255, 255, 68), 0.01, new Vector3D(0,0.1,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-54,38,60),
                new Color(55, 55, 200), 0.011, new Vector3D(0.1,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(31,40,25),
                new Color(155, 155, 255), 0.01, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-3,35,85),
                new Color(255, 155, 200), 0.012, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(71,28,90),
                new Color(255, 255, 250), 0.014, new Vector3D(0,0,0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-84,35,75),
                new Color(255, 155, 0), 0.008, new Vector3D(0,0,0)));

        scene.addLight(new PointLight(new Vector3D(45, 25, 20), Color.WHITE, 5));
        return scene;
    }

    /**
     * Gets fifth scene.
     *
     * @return the fifth scene
     */
    public static Scene getFifthScene() {
        Scene scene = new Scene();
        scene.setCamera(new PerspertiveCamera(new Vector3D(0, 5, -5), 0, 20,
                0.1, 400, 100));
        scene.addObject(new Plane(0, new Color(5, 50, 255)));

        scene.addObject(getModel3D("OBJS/RowBoat1.obj", new Vector3D(-3, 0, 0),
                new Color(128, 64, 0), 2.25, new Vector3D(0, 0, .08)));
        scene.addObject(getModel3D("OBJS/Steve.obj", new Vector3D(-3.8, 1.5, -0.8),
                new Color(255, 255, 255), 0.125, new Vector3D(0, 0, 0)));
        scene.addObject(getModel3D("OBJS/dolphin.obj", new Vector3D(9, 1, 0),
                new Color(98, 114, 126), 0.125, new Vector3D(0, 0, 0)));

        scene.addObject(getModel3D("OBJS/Cube.obj", new Vector3D(37, 15, 25),
                new Color(255, 247, 166, 255), 5.0, new Vector3D(-0.5, 0.5, 0.1)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(10, 25, 40),
                new Color(255, 255, 68), 0.005, new Vector3D(0, 0, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-20, 35, 50),
                new Color(255, 255, 68), 0.008, new Vector3D(0, 0, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(20, 30, 80),
                new Color(255, 255, 68), 0.01, new Vector3D(0, 0.1, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-55, 38, 60),
                new Color(55, 55, 200), 0.011, new Vector3D(0.1, 0, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(30, 40, 25),
                new Color(155, 155, 255), 0.01, new Vector3D(0, 0, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-4, 35, 85),
                new Color(255, 155, 200), 0.012, new Vector3D(0, 0, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(70, 28, 90),
                new Color(255, 255, 250), 0.014, new Vector3D(0, 0, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-85, 35, 75),
                new Color(255, 155, 0), 0.008, new Vector3D(0, 0, 0)));

        scene.addLight(new PointLight(new Vector3D(45, 25, 20), Color.WHITE, 5));
        return scene;
    }

    /**
     * Gets sixth scene.
     *
     * @return the sixth scene
     */
    public static Scene getSixthScene() {
        Scene scene = new Scene();
        scene.setCamera(new PerspertiveCamera(new Vector3D(0, 5, -5), 0, 20,
                0.1, 400, 100));
        scene.addObject(new Plane(0, new Color(5, 50, 255)));

        scene.addObject(getModel3D("OBJS/RowBoat1.obj", new Vector3D(-3, 0, 0),
                new Color(128, 64, 0), 2.25, new Vector3D(0, 0, .08)));
        scene.addObject(getModel3D("OBJS/Steve.obj", new Vector3D(-3.8, 1.5, -0.8),
                new Color(255, 255, 255), 0.125, new Vector3D(0, 0, 0)));
        scene.addObject(getModel3D("OBJS/dolphin.obj", new Vector3D(9, 0, 0),
                new Color(98, 114, 126), 0.125, new Vector3D(.5,1 , 1)));

        scene.addObject(getModel3D("OBJS/Cube.obj", new Vector3D(37, 15, 25),
                new Color(255, 247, 166, 255), 5.0, new Vector3D(-0.5, 0.5, 0.1)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(10, 25, 40),
                new Color(255, 255, 68), 0.005, new Vector3D(0, 0, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-20, 35, 50),
                new Color(255, 255, 68), 0.008, new Vector3D(0, 0, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(20, 30, 80),
                new Color(255, 255, 68), 0.01, new Vector3D(0, 0.1, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-55, 38, 60),
                new Color(55, 55, 200), 0.011, new Vector3D(0.1, 0, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(30, 40, 25),
                new Color(155, 155, 255), 0.01, new Vector3D(0, 0, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-4, 35, 85),
                new Color(255, 155, 200), 0.012, new Vector3D(0, 0, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(70, 28, 90),
                new Color(255, 255, 250), 0.014, new Vector3D(0, 0, 0)));
        scene.addObject(getModel3D("OBJS/Star.obj", new Vector3D(-85, 35, 75),
                new Color(255, 155, 0), 0.008, new Vector3D(0, 0, 0)));

        scene.addLight(new PointLight(new Vector3D(45, 25, 20), Color.WHITE, 5));
        return scene;
    }

    /**
     * Gets test scene.
     *
     * @return the test scene
     */
    public static Scene getTestScene() {
        Scene scene = new Scene();
        scene.setCamera(new PerspertiveCamera(new Vector3D(0, 5, -5), 0, 20,
                0.1, 400, 100));
        scene.addObject(new Plane(0, new Color(5, 250, 5)));
        scene.addObject(getModel3D("OBJS/Car.obj", new Vector3D(5, 5, 30),
                new Color(255, 10, 10), 4.0, new Vector3D(0, 1, 0)));
        //scene.addObject(getModel3D("OBJS/Spider.obj", new Vector3D(10, 5, 10),
                //new Color(128, 64, 0), 0.06, new Vector3D(0, 0, 0)));

        scene.addLight(new PointLight(new Vector3D(45, 25, 20), Color.WHITE, 5));
        return scene;
    }

    /**
     * Gets test pro scene.
     *
     * @return the test pro scene
     */
    public static Scene getTestPROScene() {
        Scene scene = new Scene();
        scene.setCamera(new PerspertiveCamera(new Vector3D(0, 5, -5), 0, 20,
                0.1, 400, 100));
        scene.addObject(new Plane(0, new Color(0, 255, 38)));
        scene.addObject(getModel3D("OBJS/ah64d.obj", new Vector3D(0, -8, 45),
                new Color(255, 2, 2), 4.9, new Vector3D(0, 2, 0)));

        scene.addLight(new PointLight(new Vector3D(45, 25, 20), Color.WHITE, 5));
        return scene;
    }

    /**
     * Gets test rap scene.
     *
     * @return the test rap scene
     */
    public static Scene getTestRAPScene() {
        Scene scene = new Scene();
        scene.setCamera(new PerspertiveCamera(new Vector3D(0, 5, -5), 0, 20,
                0.1, 400, 100));
        scene.addObject(new Plane(0, new Color(123, 0, 255)));
        scene.addObject(getModel3D("OBJS/Raptor.obj", new Vector3D(0, 1, 0),
                new Color(255, 255, 255), 0.10, new Vector3D(0, 0, 0)));

        scene.addLight(new PointLight(new Vector3D(45, 25, 20), Color.WHITE, 5));
        return scene;
    }

}
