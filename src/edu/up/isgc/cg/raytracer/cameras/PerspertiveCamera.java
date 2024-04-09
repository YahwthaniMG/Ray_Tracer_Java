package edu.up.isgc.cg.raytracer.cameras;

import edu.up.isgc.cg.raytracer.math.Ray;
import edu.up.isgc.cg.raytracer.math.Vector3D;
import static edu.up.isgc.cg.raytracer.math.Vector3D.*;
import static java.lang.Math.*;

/**
 * The type Perspertive camera.
 */
public class PerspertiveCamera  extends Camera{

    private double verticalFovRadians;

    /**
     * Instantiates a new Perspertive camera.
     *
     * @param position           the position
     * @param yawDegrees         the yaw degrees
     * @param pitchDegrees       the pitch degrees
     * @param nearPlane          the near plane
     * @param farPlane           the far plane
     * @param verticalFovRadians the vertical fov radians
     */
    public PerspertiveCamera(Vector3D position,  double yawDegrees, double pitchDegrees,
                             double nearPlane, double farPlane, double verticalFovRadians){
         super(position,toRadians(yawDegrees),toRadians(pitchDegrees),nearPlane,farPlane);
         setVerticalFovRadians(toRadians(verticalFovRadians));
    }

    private double getVerticalFovRadians(){
        return verticalFovRadians;
    }

    private void setVerticalFovRadians (double verticalFovRadians){
        this.verticalFovRadians=verticalFovRadians;
    }

    /**
     *
     * @param u
     * @param v
     * Make new rays from a direction and point
     * @return ray
     */
    @Override
    public Ray makeRay (double u, double v){
        Vector3D origin = new Vector3D(0,0,-1/ tan(getVerticalFovRadians() / 2));
        Vector3D direction = normalize(subtract(new Vector3D(u,v,0),origin)).rotateYP(getYawRadians(),getPitchRadians());
        return  new Ray(add(origin,getPosition()),direction);
    }

}


