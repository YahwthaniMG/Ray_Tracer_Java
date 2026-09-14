# Development history

This documents how the project got from a course-assignment ray tracer to what's here
now. The engine core (vectors/rays/intersection math, the original `Sphere`/`Plane`/
`Model3D` objects, the camera and light hierarchy, `Raytracer`'s dialog-driven batch flow)
is the original coursework, by Jafet Rodríguez and Yahwthani Morales. Everything from
"Phase 7" onward was designed and implemented by Claude (Anthropic) through Claude Code,
in collaboration with Yahwthani Morales, who directed the work and reviewed every step.

Phases 0–6 (the initial scene editor: a Swing window, a viewport you could place and drag
objects in, and a first pass at saving a scene to JSON) predate the detailed phase-by-phase
record below and aren't reconstructed here in detail to avoid mis-stating history that
wasn't logged as it happened. Everything from Phase 7 on is accurate to how it was
actually done.

## Phase 7 — Editor quick wins

- Delete the currently-selected object/camera/light from the Inspector.
- Save now suggests a filename based on the project's name (typed in the sidebar) instead
  of a fixed `scene.json`, and that name round-trips through load.

## Phase 8 — Multi-threaded rendering

The pixel loop in `RenderController` is embarrassingly parallel (every pixel only reads
shared scene state, never mutates it), so it was split across an `ExecutorService` sized
to the machine's CPU cores, with progress reporting throttled to avoid flooding the UI
thread and cooperative cancellation on interrupt. Verified with a pixel-diff against the
single-threaded output (zero difference) and a timing comparison on a medium scene.

## Phase 9 — Materials

Reflectivity, transparency, shininess and refractive index were previously fixed
constants in `RenderController`, identical for every object in a scene, with a single
render-wide "mode" combo box deciding whether reflection or refraction happened at all.
This phase introduced `Material` as a real per-object property (with sensible defaults
matching the old fixed constants, so existing scenes render unchanged until a material is
actually edited), an Inspector "Material" section to edit it, and `SceneIO` persistence
for it.

## Phase 10 — Reflection & refraction fixes

Two real bugs were found while wiring materials into the shading path:

1. The refraction ray constructor was combining the two halves of Snell's law
   incorrectly — it used a scaled *direction* vector as the ray's *origin* instead of the
   actual intersection point, so refracted rays started from a physically meaningless
   position.
2. Both reflection and refraction were recomputing the incident direction from the
   *camera's* position rather than the incoming ray's actual direction — correct only for
   the primary ray, and wrong for every bounce after the first (a reflection of a
   reflection, refraction inside refraction, etc.).

Both were fixed, and reflection/refraction became driven by each hit object's `Material`
instead of a single global mode.

## Phase 11 — Lights

- `DirectionalLight` was previously not selectable or editable in the viewport at all.
  It gained the same yaw/pitch "aim" editing already used for cameras, and a viewport
  gizmo.
- Added `SpotLight`: a point light narrowed to a cone, with a soft penumbra at the edge
  instead of a hard cutoff.

## Phase 12 — Cameras

Added `FisheyeCamera` — screen coordinates map to an angle away from the forward
direction (rather than a linear offset on the image plane, as Perspective does), the
defining trait of a fisheye lens.

## Phase 13 — Render effects

- **Anti-aliasing**: configurable supersampling (multiple jittered rays per pixel,
  averaged), exposed as a "Samples" option in the render bar.
- **Soft shadows**: point/spot lights are treated as having a small physical radius, and
  several jittered shadow rays are averaged per shading point instead of one all-or-
  nothing test, producing a soft penumbra instead of a hard-edged shadow.

(A bug where the soft-shadow sample count was accidentally square-rooted twice — once
converting sample count to a grid size, then again inside the shadow function — was caught
because re-running a timing benchmark showed render time had gone up 4–5× with an
*identical* output hash, meaning the extra work wasn't actually doing anything.)

## Phase 14 — Animation and video export

The largest single addition: a `Keyframe`/`AnimationTrack`/`SceneAnimation` model (backed
by a `TreeMap<time, Keyframe>` per animated item, for cheap "surrounding keyframes"
lookups), a `TimelinePanel` in the editor to scrub time, preview interpolated state live,
and record/delete keyframes, and an `AnimationRenderer` that renders the whole timeline
out as a numbered PNG sequence and — if `ffmpeg` is available on the machine — assembles
it into an `.mp4` by shelling out to it as an external process (deliberately not a bundled
dependency). `SceneProperties` was introduced in this phase as the shared "get/set
position, color, aim, ..." dispatch layer the animation system, Inspector and viewport all
needed without depending on each other.

## Post-phase-14 fixes: three reported issues + a self-review

After Phase 14, three specific issues were reported and fixed:

- The Inspector panel had no scroll and could clip its own content on a tall selection
  (e.g. a Material-heavy object) — fixed by having it report its true content height to
  the scroll pane wrapping it, instead of a hardcoded one.
- Setting a keyframe had no visible marker on the timeline — added tick marks on the
  scrub bar, edge-clamped so one at time 0 or the full duration doesn't get clipped off
  the edge of the slider.
- Added a one-level undo (Ctrl+Z) for an accidental drag in the viewport — deliberately
  not a full undo stack, since the request was specifically about recovering from a
  mis-drag, not general undo/redo.

A self-directed review of the whole editor flow also caught that closing the window via
its title-bar close button exited immediately with no unsaved-work warning — fixed with a
confirmation prompt, consistent with the existing "New Scene" confirmation.

## Render quality & UI pass

The last round of work addressed three things at once: the inability to have reflection
*and* refraction simultaneously, disappointing render quality, and a dated/cluttered UI.

**Reflection + refraction together.** Added a fourth `RenderMode.BOTH` (rather than
redesigning `RenderMode` into a pair of booleans, which would have broken the legacy
`Raytracer`'s direct references to the enum's bare constants) so the render bar's mode
combo can force both ray types on for a preview, on top of what each object's `Material`
already independently enables.

**Two real rendering bugs**, found by chasing down why lit scenes looked flat and gray
regardless of where lights were placed:

1. `PointLight`/`SpotLight` falloff used `distance^2.5` divided by an extra `4π` — a
   physically-based radiant-intensity-to-irradiance conversion that doesn't belong in a
   simple Phong renderer, where "intensity" is meant as a plain artistic knob. At a
   typical scene distance of 8 units, it made a light of intensity 5 (the sidebar's old
   default) contribute exactly 0-out-of-255 to shading — every point light in every scene
   was effectively a no-op past a few units. Fixed to the standard `distance^2`, no extra
   divisor.
2. **The bigger one:** shadow rays were only capped at the camera's far plane (400 units),
   never at the light's actual distance. Any surface positioned beyond a light along a
   shading point's direction toward it (e.g. a ceiling above a lamp) was incorrectly
   treated as blocking that light for virtually every point in the scene — which is what
   was actually masking the falloff fix's effect in testing (a debug pixel dump showed
   *every* sampled pixel was exactly the ambient-only color, both before and after fixing
   the falloff formula, which is what gave this away). Fixed by capping each shadow ray's
   search at the light's own distance (`Light.getDistanceTo`), not the camera's far plane.

**UI**, addressed as two separate concrete bugs plus a broader theming pass, rather than
one vague "make it look better":

- Sidebar/Inspector row labels ("Orthographic", "Directional", …) were having their last
  character clipped. The actual cause (found by walking the real component tree, not just
  guessing) wasn't a layout/width problem — every label's bounds exactly matched its own
  text width with zero padding, so Nimbus's paint-time glyph measurement (which differs
  slightly from the layout-time `FontMetrics` measurement used to size the label) clipped
  the last sliver of a glyph. Fixed with a few pixels of right padding on affected labels,
  matching a fix already applied elsewhere in the Inspector for the same reason.
- The whole app kept Nimbus's stock light theme everywhere the hand-styled dark panels
  didn't reach: the File menu bar, the Save/Load file chooser, confirmation dialogs, and
  every spinner/combo/text field embedded in the dark panels rendering as a stark white
  box. Fixed with a proper dark Nimbus palette (recoloring Nimbus's base derived colors),
  plus a separate fix for the File menu specifically, since Nimbus paints
  `JPopupMenu`/`JMenuItem` backgrounds through baked-in gradient `Painter`s that ignore
  plain `UIManager` color overrides, and installs each menu item's text color as a
  `ColorUIResource` that a color override doesn't reach either — worked around with an
  explicit `Painter` override for the background and a direct, non-`UIResource`
  `setForeground` call on each menu item.

Every fix in this pass was checked against an actual rendered/screenshotted result (an
off-screen `printAll()` capture, not just a code read-through) before being considered
done, and the SceneIO round-trip, undo, and animation-persistence checks were re-run
afterward to confirm nothing else regressed.
