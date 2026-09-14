package edu.up.isgc.cg.raytracer.objects;

/**
 * How a surface reacts to light — every {@link Object3D} has one (defaulting to values
 * that reproduce the fixed shading constants {@link edu.up.isgc.cg.raytracer.RenderController}
 * used before materials existed, so nothing changes visually until a material is actually
 * edited). {@code reflectivity}/{@code transparency}/{@code refractiveIndex} aren't read by
 * the shading formulas yet — they're wired up when reflection/refraction becomes
 * per-material instead of a single render-wide mode.
 *
 * @author Claude (Anthropic)
 */
public class Material {

    private double ambient = 0.3;
    private double diffuse = 0.1;
    private double specular = 1.4;
    private double shininess = 120.0;
    private double reflectivity = 0.0;
    private double transparency = 0.0;
    private double refractiveIndex = 1.5; // ~glass; only matters once transparency > 0

    /** How much of the object's own color shows up regardless of any light (0–1+). */
    public double getAmbient() { return ambient; }
    public void setAmbient(double ambient) { this.ambient = ambient; }

    /** How strongly the object's own color blends into its lit (diffuse) shading. */
    public double getDiffuse() { return diffuse; }
    public void setDiffuse(double diffuse) { this.diffuse = diffuse; }

    /** Brightness of the specular highlight. */
    public double getSpecular() { return specular; }
    public void setSpecular(double specular) { this.specular = specular; }

    /** Phong exponent — higher means a smaller, sharper highlight. */
    public double getShininess() { return shininess; }
    public void setShininess(double shininess) { this.shininess = shininess; }

    /** How mirror-like the surface is (0 = none, 1 = perfect mirror). */
    public double getReflectivity() { return reflectivity; }
    public void setReflectivity(double reflectivity) { this.reflectivity = reflectivity; }

    /** How see-through the surface is (0 = opaque, 1 = fully transparent). */
    public double getTransparency() { return transparency; }
    public void setTransparency(double transparency) { this.transparency = transparency; }

    /** Index of refraction used to bend rays passing through (only matters if transparency > 0). */
    public double getRefractiveIndex() { return refractiveIndex; }
    public void setRefractiveIndex(double refractiveIndex) { this.refractiveIndex = refractiveIndex; }
}
