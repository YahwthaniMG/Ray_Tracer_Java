# Ray Tracer (Java)

A recursive ray tracer written in plain Java — no rendering libraries, no build-tool
dependencies, no engine underneath it — paired with a Swing-based scene editor that lets
you build a scene visually, preview it in real time, and render it (as a single image or
as an animated sequence exported to video) instead of hand-writing scene setup code.

Made for **UP** (Universidad Panamericana) as a Computer Graphics course project, and
extended well past the original assignment as an ongoing personal/portfolio project.

## Why this project exists

The starting point was a typical computer-graphics course assignment: implement a
recursive ray tracer from first principles (vectors, rays, ray–object intersection,
Phong shading, recursive reflection/refraction) with every scene hand-written in Java
code and every render kicked off from a menu-driven console dialog. That original engine
is still in here, working, and untouched in spirit (`Raytracer.java`, `RenderController`'s
core shading math, `Sphere`/`Plane`/`Model3D`/`Triangle`, the camera and light hierarchy).

What changed is everything *around* it: instead of describing a scene in code and
re-running the program to see it, the project grew a real scene editor — place objects,
cameras and lights with the mouse, move/rotate/scale them, tune their materials, save and
reload the scene, and render it — and then grew further into a small animation tool,
letting a scene's cameras/objects/lights move over time and rendering that out as a video.
The goal throughout has been to keep learning and rebuilding the *engine* itself (the
math, the shading model, the performance) while making it actually pleasant to *use*.

## Two ways to run it

**The scene editor (recommended)** — `edu.up.isgc.cg.raytracer.ui.RayTracerApp`. A GUI:
build a scene visually, preview it, animate it, render it.

**The legacy batch renderer** — `edu.up.isgc.cg.raytracer.Raytracer`. The original,
dialog-driven flow: a couple of `JOptionPane` prompts pick a hard-coded demo scene and a
resolution, then it renders straight to a PNG under `Renders/`. Kept as-is since it's the
original coursework deliverable, not because it's the recommended way to use the project.

### Requirements

- JDK **17 or newer** (the code uses `record`, added in Java 16).
- No external libraries and no build tool (Maven/Gradle) — just `javac`/`java`, or open
  the folder in IntelliJ IDEA (there's a `.iml` module file) or VS Code (a `.vscode/`
  launch config is included, pointed at `RayTracerApp`).
- Optional: [`ffmpeg`](https://ffmpeg.org/) on your `PATH` if you want the animation
  exporter to assemble an actual `.mp4` instead of leaving you a numbered PNG sequence.

### Running from the command line

```bash
# from the project root
javac -encoding UTF-8 -d out/classes $(find src -name "*.java")

# the editor
java -cp out/classes edu.up.isgc.cg.raytracer.ui.RayTracerApp

# the legacy batch renderer
java -cp out/classes edu.up.isgc.cg.raytracer.Raytracer
```

## Features

### Rendering engine

- Recursive ray tracing with reflection **and** refraction — either, both at once, or
  neither, per material (`Material.reflectivity` / `Material.transparency`), plus a
  render-wide mode toggle in the sidebar for quick previews.
- Phong shading (ambient/diffuse/specular) with per-object `Material`s: shininess,
  reflectivity, transparency and refractive index, with one-click Mirror/Glass/Matte
  presets.
- Point, directional and spot lights, all with accurate inverse-square falloff; spot
  lights have a soft-edged cone (penumbra).
- Soft shadows (area-light jittered shadow sampling) and anti-aliasing (configurable
  supersampling), both optional and tunable from the render bar.
- Perspective, orthographic and fisheye cameras.
- `.obj` mesh import (`OBJReader`), plus a couple of procedurally-built primitives
  (`Primitives`) for shapes with no bundled model.
- Multi-threaded rendering — the pixel grid is split across all available CPU cores.

### Scene editor

- A Maya-style viewport: Top/Front/Side orthographic views for precise placement (drag to
  move, exactly two axes at a time) plus a free-look Perspective view for context, all
  rendered live with a software rasterizer (painter's algorithm) — fast preview shading
  independent of the "real" ray-traced render.
- Click-to-select with a numeric Inspector panel (position/rotation/scale/aim/color/
  intensity/material, depending on what's selected) as the precise complement to dragging
  in the viewport.
- A sidebar palette to drop in geometry, cameras, lights or an imported `.obj`, by
  clicking (drops at the origin) or dragging onto the viewport (drops exactly there).
- Save/load a scene as a small, human-readable JSON file (`SceneIO`, backed by a
  dependency-free hand-rolled JSON reader/writer, `Json`) — including which camera is
  active and the full animation timeline.
- One level of undo (Ctrl+Z) for an accidental drag in the viewport.
- A close-confirmation prompt so an unsaved scene isn't lost by an accidental window close.

### Animation & video export

- Keyframe any object/camera/light's position (and whatever else it supports — color,
  rotation, scale, aim, intensity) at any point on a timeline.
- Scrub the timeline for a live, interpolated preview in the viewport (linear
  interpolation, shortest-angular-path for yaw/pitch so a turn never takes the "long way
  around").
- Render the whole animation out as a numbered PNG sequence, and — if `ffmpeg` is on the
  machine — assemble that sequence straight into an `.mp4` (`AnimationRenderer`); if it
  isn't, you get the frames and the exact `ffmpeg` command to run yourself later.

## Project structure

```text
src/edu/up/isgc/cg/raytracer/
├── Raytracer.java            legacy dialog-driven batch entry point
├── RenderController.java     the actual ray tracer: shading, shadows, reflection/refraction
├── RenderMode.java           render-wide secondary-ray mode (None/Reflection/Refraction/Both)
├── Scene.java                everything in a scene: objects, cameras, lights, animation
├── ColorRGB.java             color math helpers
├── AnimationRenderer.java    batch-renders an animation to a frame sequence (+ ffmpeg)
├── animation/                keyframes, per-item tracks, and the scene-wide animation clock
├── cameras/                  Camera (abstract) + Perspective/Orthographic/Fisheye
├── lights/                   Light (abstract) + Point/Directional/Spot
├── math/                     Vector3D, Ray, Intersection, Barycentric coordinates
├── objects/                  Object3D (abstract) + Sphere/Plane/Triangle/Model3D, Material
├── tools/                    OBJReader, Json, SceneIO, SceneProperties, Primitives
└── ui/                       the Swing scene editor (see below)
```

`edu.up.isgc.cg.raytracer.ui` is its own small application: `RayTracerApp` (entry point,
also responsible for re-theming Nimbus to a dark palette) → `StartupWindow` → `EditorWindow`
(menu bar, `SidebarPanel`, `ViewportPanel`, `InspectorPanel`, `TimelinePanel`, the render
bar). `SceneProperties` is the glue that lets the editor, the Inspector and the animation
system all read/write "position", "color", "aim", etc. on an `Object3D`/`Camera`/`Light`
without those three unrelated hierarchies needing to share a common base type.

Other top-level folders: `OBJS/` (importable `.obj` models), `Scenes/` (saved scene JSON),
`Renders/` (rendered output), `UML/` (a UML diagram of the original engine — predates the
editor/animation system, so it covers the core ray tracer, not the `ui`/`animation`
packages).

## Development process

This project was built in two eras: the original course assignment (the core ray tracer),
and a long, AI-assisted extension (the scene editor, materials, new lights/cameras,
animation and video export, and a full render-quality and UI pass) done in collaboration
with Claude (Anthropic) through Claude Code. See **[CHANGELOG.md](CHANGELOG.md)** for the
phase-by-phase account of that second era, including a couple of real engine bugs found
and fixed along the way (an incorrect light falloff formula, and an unbounded shadow-ray
search that was silently keeping every point light from lighting anything).

## Possible future improvements

- **Environment lighting.** There's no sky/environment color — reflections and refraction
  of empty space currently resolve to plain black, and reflective/refractive surfaces are
  composited additively rather than in an energy-conserving way (deliberately, since
  there's no environment to otherwise ground them). A simple sky color or gradient, and
  switching to a properly energy-conserving blend once there is one, would make mirrors
  and glass read as more physically convincing.
- **An acceleration structure.** Every ray currently tests every object in the scene
  (`O(objects)` per ray); a bounding-volume hierarchy or a simple spatial grid would matter
  a lot once scenes have more than a handful of imported meshes.
- **More primitives and a real torus mesh.** `Primitives` currently only builds a pyramid
  procedurally; a proper parametric torus (and a cone) would round out the geometry
  palette without needing a bundled `.obj`.
- **Texture mapping.** Materials are solid colors only right now — UV coordinates already
  exist on imported meshes' triangles in places, but nothing samples an image through them.
- **Multi-level undo/redo.** Only the last viewport drag can be undone; a real undo stack
  covering property edits, deletes and keyframe changes would be a natural extension of
  the existing single-step undo.
- **Bounding/orbit camera for the Inspector's numeric edits**, and extending the animation
  system's supported properties to materials (e.g., animating a fade from opaque to glass).
- **A packaged build** (Maven/Gradle, plus a proper runnable `.jar`) so the project doesn't
  need `javac`/an IDE to try out.
- **Automated tests.** Verification so far has leaned on ad hoc render/pixel-diff scripts
  written during development rather than a checked-in test suite.

## Authors

- **Jafet Rodríguez** and **Yahwthani Morales** — the original ray tracing engine.
- **Claude (Anthropic)** — the scene editor, materials system, new cameras/lights, render
  effects, animation & video export, and the render-quality/UI work documented in
  [CHANGELOG.md](CHANGELOG.md), built in collaboration with Yahwthani Morales through
  Claude Code.

See individual file headers (`@author`) for who worked on each specific file.
