package edu.up.isgc.cg.raytracer.ui;

import edu.up.isgc.cg.raytracer.math.Vector3D;

/**
 * What a sidebar palette row creates when clicked or dropped — a factory bound to a
 * world position (the origin for a click, or wherever it was dropped on the viewport).
 * Returns an {@code Object3D}, {@code Camera} or {@code Light}; {@code SceneProperties}
 * knows how to add whichever of those it gets to the {@code Scene}.
 *
 * @author Claude (Anthropic)
 */
@FunctionalInterface
interface ScenePrefab {
    Object create(Vector3D position);
}
