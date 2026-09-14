package edu.up.isgc.cg.raytracer;

/**
 * The secondary ray effect applied on top of direct lighting when tracing a scene.
 * Replaces the previous magic {@code int} (0 = reflection, 1 = refraction, other = none)
 * so both the UI and the renderer can refer to it by name.
 *
 * @author Claude (Anthropic)
 */
public enum RenderMode {
    /** No secondary rays: only ambient + direct lighting per intersection. */
    NONE,
    /** Cast a reflection ray off each intersection and add its contribution. */
    REFLECTION,
    /** Cast a refraction ray off each intersection and add its contribution. */
    REFRACTION,
    /** Cast both a reflection and a refraction ray off each intersection. */
    BOTH
}
