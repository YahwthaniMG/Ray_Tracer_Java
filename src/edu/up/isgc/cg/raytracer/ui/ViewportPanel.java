package edu.up.isgc.cg.raytracer.ui;

import edu.up.isgc.cg.raytracer.Scene;
import edu.up.isgc.cg.raytracer.cameras.Camera;
import edu.up.isgc.cg.raytracer.lights.DirectionalLight;
import edu.up.isgc.cg.raytracer.lights.Light;
import edu.up.isgc.cg.raytracer.lights.PointLight;
import edu.up.isgc.cg.raytracer.lights.SpotLight;
import edu.up.isgc.cg.raytracer.math.Vector3D;
import edu.up.isgc.cg.raytracer.objects.Model3D;
import edu.up.isgc.cg.raytracer.objects.Object3D;
import edu.up.isgc.cg.raytracer.objects.Plane;
import edu.up.isgc.cg.raytracer.objects.Sphere;
import edu.up.isgc.cg.raytracer.objects.Triangle;
import edu.up.isgc.cg.raytracer.tools.SceneProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static edu.up.isgc.cg.raytracer.math.Vector3D.*;

/**
 * The scene viewport. Draws a grid for whichever view is active — three axis-aligned
 * orthographic views (Top/Front/Side) plus a free-look Perspective view for context —
 * plus every object/camera/light in the {@link Scene} as shaded solid faces (painter's
 * algorithm: faces are depth-sorted per view and painted back-to-front, lit by a fixed
 * "studio" key light independent of the scene's actual lights, purely so shapes read
 * clearly while editing) — and lets the user click one to select it, highlighted with a
 * wireframe overlay and edited via the Inspector panel.
 *
 * <p>The Perspective view uses its own small orbit camera (yaw/pitch/distance around
 * the origin) purely for navigating the editor — that's a separate concept from the
 * scene's actual render {@code Camera} objects, which get their own markers drawn like
 * any other scene content.</p>
 *
 * @author Claude (Anthropic)
 */
public class ViewportPanel extends JPanel {

    private static final int GRID_EXTENT_UNITS = 20;
    private static final Color CAMERA_COLOR = new Color(200, 220, 255);
    private static final Color SELECTION_COLOR = new Color(255, 210, 90);
    private static final double CLICK_DRAG_THRESHOLD_PX = 4;

    // Fixed "studio" light for shading the solid preview — independent of the scene's
    // actual PointLights/DirectionalLights, so the preview never goes dark just because
    // you haven't placed a light yet, and looks the same regardless of your lighting setup.
    private static final Vector3D KEY_LIGHT = normalize(new Vector3D(1, 2, -1));
    private static final double AMBIENT = 0.35;

    private final Scene scene;
    private Object selected;
    private Consumer<Object> onSelectionChanged;
    private BiConsumer<Object, Vector3D> onObjectMoved;

    private ViewportView view = ViewportView.FRONT;

    // Orthographic navigation (pan in screen pixels, zoom in pixels-per-world-unit).
    private double panX = 0;
    private double panY = 0;
    private double pixelsPerUnit = 18;

    // Perspective navigation: orbit camera looking at the origin.
    private double camYaw = Math.toRadians(35);
    // Positive: places the orbit camera above the ground looking down. (A negative
    // default silently put it *below* the ground looking up — invisible with wireframe,
    // since a flat Plane has no backface, but it broke depth-sorting once faces got
    // shaded, because "up" no longer reliably meant "closer to the camera".)
    private double camPitch = Math.toRadians(20);
    private double camDistance = 26;

    // Click-vs-drag disambiguation for the mouse navigation handler.
    private Point pressPoint;
    private Point lastDrag;
    private boolean draggedPastThreshold;

    // Set on mousePressed when the press lands on an object in an ortho view — while
    // non-null, dragging moves this item along the view's 2 visible axes instead of
    // panning the camera. Always null in Perspective (reference-only, no placement).
    private Object dragTarget;
    private Vector3D dragStartPosition; // dragTarget's position when the drag began, for Ctrl+Z

    public ViewportPanel(Scene scene) {
        this.scene = scene;
        setBackground(Theme.BG);
        setPreferredSize(new Dimension(700, 560));
        installNavigation();
    }

    public void setView(ViewportView view) {
        this.view = view;
        repaint();
    }

    public ViewportView getView() {
        return view;
    }

    /** Called (with the newly selected item, or {@code null} on deselect) whenever the user clicks in the viewport. */
    public void setOnSelectionChanged(Consumer<Object> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    /**
     * Called once a drag-to-move gesture finishes having actually moved something, with
     * the item and the position it was at right before the drag started — enough for a
     * caller to implement a single-step "undo that move" (e.g. on Ctrl+Z).
     */
    public void setOnObjectMoved(BiConsumer<Object, Vector3D> onObjectMoved) {
        this.onObjectMoved = onObjectMoved;
    }

    private void setSelected(Object item) {
        if (selected == item) return;
        selected = item;
        if (onSelectionChanged != null) onSelectionChanged.accept(item);
        repaint();
    }

    /** Selects an item added from outside (e.g. the sidebar just dropped it in). */
    public void select(Object item) {
        setSelected(item);
    }

    /** The currently selected object/camera/light, or {@code null} if nothing is selected. */
    public Object getSelected() {
        return selected;
    }

    /**
     * Converts a point in this panel's own coordinates to a world position, for
     * dropping a sidebar item exactly where it lands. Only defined for the ortho views
     * — Perspective has no single 2D plane to drop onto, so it returns {@code null}.
     * The depth axis not visible in the current view defaults to 0.
     */
    public Vector3D screenToWorld(Point local) {
        if (view == ViewportView.PERSPECTIVE) return null;
        double cx = getWidth() / 2.0 + panX;
        double cy = getHeight() / 2.0 + panY;
        double h = (local.x - cx) / pixelsPerUnit;
        double v = -(local.y - cy) / pixelsPerUnit;
        return switch (view) {
            case TOP -> new Vector3D(h, 0, v);
            case FRONT -> new Vector3D(h, v, 0);
            case SIDE -> new Vector3D(0, v, h);
            default -> null;
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (view == ViewportView.PERSPECTIVE) {
            drawPerspectiveGrid(g2);
        } else {
            drawOrthographicGrid(g2);
        }
        drawSceneContents(g2);

        g2.setColor(Theme.MUTED);
        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        g2.drawString(view.getLabel().toUpperCase(), 12, 20);
    }

    // ---- Orthographic views (Top / Front / Side) ----

    private void drawOrthographicGrid(Graphics2D g2) {
        int cx = (int) (getWidth() / 2.0 + panX);
        int cy = (int) (getHeight() / 2.0 + panY);

        for (int i = -GRID_EXTENT_UNITS; i <= GRID_EXTENT_UNITS; i++) {
            if (i == 0) continue;
            g2.setColor(Theme.GRID_LINE);
            int offset = (int) Math.round(i * pixelsPerUnit);
            g2.draw(new Line2D.Double(cx + offset, 0, cx + offset, getHeight()));
            g2.draw(new Line2D.Double(0, cy + offset, getWidth(), cy + offset));
        }

        g2.setStroke(new BasicStroke(2f));
        g2.setColor(horizontalAxisColor());
        g2.draw(new Line2D.Double(0, cy, getWidth(), cy));
        g2.setColor(verticalAxisColor());
        g2.draw(new Line2D.Double(cx, 0, cx, getHeight()));
        g2.setStroke(new BasicStroke(1f));
    }

    private Color horizontalAxisColor() {
        return switch (view) {
            case TOP, FRONT -> Theme.AXIS_X;
            case SIDE -> Theme.AXIS_Z;
            default -> Theme.GRID_LINE_STRONG;
        };
    }

    private Color verticalAxisColor() {
        return switch (view) {
            case TOP -> Theme.AXIS_Z;
            case FRONT, SIDE -> Theme.AXIS_Y;
            default -> Theme.GRID_LINE_STRONG;
        };
    }

    /** World coordinate that maps to the screen's horizontal axis in the current view. */
    private double horizontalCoord(double x, double y, double z) {
        return switch (view) {
            case TOP, FRONT -> x;
            case SIDE -> z;
            default -> 0;
        };
    }

    /** World coordinate that maps to the screen's vertical axis in the current view. */
    private double verticalCoord(double x, double y, double z) {
        return switch (view) {
            case TOP -> z;
            case FRONT, SIDE -> y;
            default -> 0;
        };
    }

    private Point2D worldToScreenOrtho(double x, double y, double z) {
        double cx = getWidth() / 2.0 + panX;
        double cy = getHeight() / 2.0 + panY;
        double h = horizontalCoord(x, y, z);
        double v = verticalCoord(x, y, z);
        return new Point2D.Double(cx + h * pixelsPerUnit, cy - v * pixelsPerUnit);
    }

    // ---- Perspective view (free-look, reference only) ----

    private void drawPerspectiveGrid(Graphics2D g2) {
        for (int i = -GRID_EXTENT_UNITS; i <= GRID_EXTENT_UNITS; i++) {
            Color colorAlongX = (i == 0) ? Theme.AXIS_Z : Theme.GRID_LINE;
            drawWorldLine(g2, i, 0, -GRID_EXTENT_UNITS, i, 0, GRID_EXTENT_UNITS, colorAlongX);
            Color colorAlongZ = (i == 0) ? Theme.AXIS_X : Theme.GRID_LINE;
            drawWorldLine(g2, -GRID_EXTENT_UNITS, 0, i, GRID_EXTENT_UNITS, 0, i, colorAlongZ);
        }
    }

    private void drawWorldLine(Graphics2D g2, double x1, double y1, double z1,
                                double x2, double y2, double z2, Color color) {
        Point2D a = worldToScreenPerspective(x1, y1, z1);
        Point2D b = worldToScreenPerspective(x2, y2, z2);
        if (a == null || b == null) return;
        g2.setColor(color);
        g2.draw(new Line2D.Double(a, b));
    }

    /**
     * Projects a world-space point to a screen pixel using the editor's own orbit
     * camera. Returns {@code null} if the point is behind the camera.
     */
    private Point2D worldToScreenPerspective(double x, double y, double z) {
        double viewZ = perspectiveDepth(x, y, z);
        if (viewZ <= 0.1) return null;

        double cosYaw = Math.cos(-camYaw), sinYaw = Math.sin(-camYaw);
        double x1 = x * cosYaw + z * sinYaw;
        double cosPitch = Math.cos(-camPitch), sinPitch = Math.sin(-camPitch);
        double z1 = -x * sinYaw + z * cosYaw;
        double y2 = y * cosPitch - z1 * sinPitch;

        double focal = 380;
        double screenX = (x1 / viewZ) * focal;
        double screenY = (y2 / viewZ) * focal;
        return new Point2D.Double(getWidth() / 2.0 + screenX, getHeight() / 2.0 - screenY);
    }

    /** Distance from the orbit camera along its own forward axis — larger is farther. */
    private double perspectiveDepth(double x, double y, double z) {
        double cosYaw = Math.cos(-camYaw), sinYaw = Math.sin(-camYaw);
        double z1 = -x * sinYaw + z * cosYaw;
        double cosPitch = Math.cos(-camPitch), sinPitch = Math.sin(-camPitch);
        double z2 = y * sinPitch + z1 * cosPitch;
        return z2 + camDistance;
    }

    /** Projects a world point using whichever projection the active view needs. */
    private Point2D project(double x, double y, double z) {
        return view == ViewportView.PERSPECTIVE
                ? worldToScreenPerspective(x, y, z)
                : worldToScreenOrtho(x, y, z);
    }

    private Point2D project(Vector3D v) {
        return project(v.getX(), v.getY(), v.getZ());
    }

    /** "Farther" scalar for painter's-algorithm sorting — larger means farther in every view. */
    private double depthOf(double x, double y, double z) {
        return switch (view) {
            case TOP -> -y;       // viewer above, looking down: lower y is farther
            case FRONT -> z;      // viewer at -Z, looking toward +Z: larger z is farther
            case SIDE -> x;       // viewer at -X, looking toward +X: larger x is farther
            case PERSPECTIVE -> perspectiveDepth(x, y, z);
        };
    }

    // ---- Scene contents: solid shaded faces (painter's algorithm) + camera/light gizmos ----

    /** One shaded triangle, in world space, ready to be projected and painted. */
    private record Face(Vector3D v1, Vector3D v2, Vector3D v3, Vector3D normal, Color color) {}

    private void drawSceneContents(Graphics2D g2) {
        if (scene == null) return;

        List<Face> faces = collectFaces();
        faces.sort(Comparator.comparingDouble((Face f) -> depthOf(
                (f.v1().getX() + f.v2().getX() + f.v3().getX()) / 3.0,
                (f.v1().getY() + f.v2().getY() + f.v3().getY()) / 3.0,
                (f.v1().getZ() + f.v2().getZ() + f.v3().getZ()) / 3.0)).reversed());
        for (Face face : faces) drawFace(g2, face);

        for (Object3D object : scene.getObjects()) {
            drawLabel(g2, object.getPosition(), object.getName());
        }
        for (Light light : scene.getLights()) {
            drawLight(g2, light);
        }
        for (Camera camera : scene.getCameras()) {
            drawCamera(g2, camera);
        }

        drawSelectionOverlay(g2);
    }

    private List<Face> collectFaces() {
        List<Face> faces = new ArrayList<>();
        for (Object3D object : scene.getObjects()) {
            Color color = object.getColor();
            if (object instanceof Model3D model) {
                for (Triangle triangle : model.getTriangles()) {
                    Vector3D[] v = triangle.getVertices();
                    Vector3D[] n = triangle.getNormals();
                    Vector3D normal = (n != null && n.length == 3)
                            ? normalize(Vector3D.add(Vector3D.add(n[0], n[1]), n[2]))
                            : faceNormal(v[0], v[1], v[2]);
                    faces.add(new Face(v[0], v[1], v[2], normal, color));
                }
            } else if (object instanceof Sphere sphere) {
                faces.addAll(sphereFaces(sphere, color));
            } else if (object instanceof Plane plane) {
                faces.addAll(planeFaces(plane, color));
            }
        }
        return faces;
    }

    /**
     * A single pair of triangles spanning the whole plane can't be depth-sorted
     * correctly against objects that sit partway across it — one average centroid
     * can't represent "in front of the sphere here, behind it over there". Cutting the
     * plane into a grid of small quads gives the painter's algorithm something local
     * enough to interleave properly, the same reasoning as tessellating the sphere.
     */
    private List<Face> planeFaces(Plane plane, Color color) {
        int segments = 10;
        Vector3D normal = plane.getNormal();
        Vector3D[] tangents = perpendicularTangents(normal);
        double e = plane.getRenderExtent();
        double step = (2 * e) / segments;
        Vector3D center = plane.getPosition();

        List<Face> faces = new ArrayList<>();
        for (int i = 0; i < segments; i++) {
            double u0 = -e + i * step, u1 = u0 + step;
            for (int j = 0; j < segments; j++) {
                double v0 = -e + j * step, v1 = v0 + step;
                Vector3D a = pointOnPlane(center, tangents, u0, v0);
                Vector3D b = pointOnPlane(center, tangents, u1, v0);
                Vector3D c = pointOnPlane(center, tangents, u1, v1);
                Vector3D d = pointOnPlane(center, tangents, u0, v1);
                faces.add(new Face(a, b, c, normal, color));
                faces.add(new Face(a, c, d, normal, color));
            }
        }
        return faces;
    }

    /**
     * Two vectors spanning the plane, perpendicular to its normal — lets the render
     * patch/tessellation/selection outline work for any orientation, not just
     * horizontal. Picks whichever world axis is least parallel to the normal as a
     * reference so the cross products stay well-defined even for a vertical wall.
     */
    private Vector3D pointOnPlane(Vector3D center, Vector3D[] tangents, double u, double v) {
        return Vector3D.add(center, Vector3D.add(
                scalarMultiplication(tangents[0], u), scalarMultiplication(tangents[1], v)));
    }

    private static Vector3D faceNormal(Vector3D v1, Vector3D v2, Vector3D v3) {
        return normalize(crossProduct(subtract(v2, v1), subtract(v3, v1)));
    }

    /**
     * Sphere has no mesh of its own (it's intersected analytically by the raytracer) —
     * this tessellates one on the fly, purely for the shaded preview. It never touches
     * the actual render geometry.
     */
    private List<Face> sphereFaces(Sphere sphere, Color color) {
        int rings = 10, segments = 16;
        Vector3D center = sphere.getPosition();
        double r = sphere.getRadius();
        Vector3D[][] grid = new Vector3D[rings + 1][segments + 1];
        for (int i = 0; i <= rings; i++) {
            double theta = Math.PI * i / rings;
            for (int j = 0; j <= segments; j++) {
                double phi = 2 * Math.PI * j / segments;
                double x = r * Math.sin(theta) * Math.cos(phi);
                double y = r * Math.cos(theta);
                double z = r * Math.sin(theta) * Math.sin(phi);
                grid[i][j] = new Vector3D(center.getX() + x, center.getY() + y, center.getZ() + z);
            }
        }
        List<Face> faces = new ArrayList<>();
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < segments; j++) {
                Vector3D a = grid[i][j], b = grid[i][j + 1], c = grid[i + 1][j + 1], d = grid[i + 1][j];
                faces.add(sphereFace(a, b, c, center, color));
                faces.add(sphereFace(a, c, d, center, color));
            }
        }
        return faces;
    }

    private Face sphereFace(Vector3D v1, Vector3D v2, Vector3D v3, Vector3D center, Color color) {
        Vector3D centroid = scalarMultiplication(Vector3D.add(Vector3D.add(v1, v2), v3), 1.0 / 3.0);
        Vector3D normal = normalize(subtract(centroid, center));
        return new Face(v1, v2, v3, normal, color);
    }

    private void drawFace(Graphics2D g2, Face face) {
        Point2D p1 = project(face.v1());
        Point2D p2 = project(face.v2());
        Point2D p3 = project(face.v3());
        if (p1 == null || p2 == null || p3 == null) return;

        GeneralPath path = new GeneralPath();
        path.moveTo(p1.getX(), p1.getY());
        path.lineTo(p2.getX(), p2.getY());
        path.lineTo(p3.getX(), p3.getY());
        path.closePath();

        g2.setColor(shade(face.color(), face.normal()));
        g2.fill(path);
    }

    /** Flat shading under a fixed studio key light, independent of the scene's own lights. */
    private static Color shade(Color base, Vector3D normal) {
        double brightness = Math.max(0, dotProduct(normal, KEY_LIGHT));
        double factor = AMBIENT + (1 - AMBIENT) * brightness;
        return new Color(
                clampChannel(base.getRed() * factor),
                clampChannel(base.getGreen() * factor),
                clampChannel(base.getBlue() * factor));
    }

    private static int clampChannel(double v) {
        return (int) Math.max(0, Math.min(255, v));
    }

    // ---- Selection overlay: wireframe outline + handle, drawn on top of the solid faces ----

    private void drawSelectionOverlay(Graphics2D g2) {
        if (selected == null) return;
        g2.setColor(SELECTION_COLOR);
        g2.setStroke(new BasicStroke(2.2f));

        if (selected instanceof Model3D model) {
            for (Triangle triangle : model.getTriangles()) {
                Vector3D[] v = triangle.getVertices();
                drawWorldSegment(g2, v[0], v[1]);
                drawWorldSegment(g2, v[1], v[2]);
                drawWorldSegment(g2, v[2], v[0]);
            }
            drawSelectionHandle(g2, model.getPosition());
        } else if (selected instanceof Sphere sphere) {
            drawSphereWireframe(g2, sphere);
            drawSelectionHandle(g2, sphere.getPosition());
        } else if (selected instanceof Plane plane) {
            for (Vector3D[] edge : planeEdges(plane)) {
                drawWorldSegment(g2, edge[0], edge[1]);
            }
            drawSelectionHandle(g2, plane.getPosition());
        } else if (selected instanceof Camera || selected instanceof Light) {
            Vector3D pos = SceneProperties.getPosition(selected);
            if (pos != null) drawSelectionHandle(g2, pos);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    /** Three orthogonal great circles — just enough to read as "a sphere" while selected. */
    private void drawSphereWireframe(Graphics2D g2, Sphere sphere) {
        Vector3D center = sphere.getPosition();
        double r = sphere.getRadius();
        int segments = 28;
        drawCircle(g2, center, r, segments, 0);
        drawCircle(g2, center, r, segments, 1);
        drawCircle(g2, center, r, segments, 2);
    }

    private void drawCircle(Graphics2D g2, Vector3D center, double r, int segments, int plane) {
        Vector3D prev = null;
        for (int i = 0; i <= segments; i++) {
            double t = 2 * Math.PI * i / segments;
            double a = r * Math.cos(t);
            double b = r * Math.sin(t);
            Vector3D p = switch (plane) {
                case 0 -> new Vector3D(center.getX() + a, center.getY(), center.getZ() + b);
                case 1 -> new Vector3D(center.getX() + a, center.getY() + b, center.getZ());
                default -> new Vector3D(center.getX(), center.getY() + a, center.getZ() + b);
            };
            if (prev != null) drawWorldSegment(g2, prev, p);
            prev = p;
        }
    }

    private Vector3D[] planeCorners(Plane plane) {
        Vector3D[] tangents = perpendicularTangents(plane.getNormal());
        double e = plane.getRenderExtent();
        Vector3D center = plane.getPosition();
        return new Vector3D[]{
                pointOnPlane(center, tangents, -e, -e), pointOnPlane(center, tangents, e, -e),
                pointOnPlane(center, tangents, e, e), pointOnPlane(center, tangents, -e, e)
        };
    }

    private Vector3D[][] planeEdges(Plane plane) {
        Vector3D[] c = planeCorners(plane);
        return new Vector3D[][]{{c[0], c[1]}, {c[1], c[2]}, {c[2], c[3]}, {c[3], c[0]}};
    }

    private void drawCamera(Graphics2D g2, Camera camera) {
        boolean isSelected = camera == selected;
        Vector3D pos = camera.getPosition();
        Vector3D forward = new Vector3D(0, 0, 1).rotateYP(camera.getYawRadians(), camera.getPitchRadians());
        Vector3D tip = Vector3D.add(pos, Vector3D.scalarMultiplication(forward, 1.6));

        g2.setColor(isSelected ? SELECTION_COLOR : CAMERA_COLOR);
        g2.setStroke(new BasicStroke(isSelected ? 2.4f : 1f));
        drawWorldSegment(g2, pos, tip);
        double s = 0.3;
        drawWorldSegment(g2, offset(pos, s, 0, 0), offset(pos, -s, 0, 0));
        drawWorldSegment(g2, offset(pos, 0, s, 0), offset(pos, 0, -s, 0));
        drawWorldSegment(g2, offset(pos, 0, 0, s), offset(pos, 0, 0, -s));
        g2.setStroke(new BasicStroke(1f));

        drawLabel(g2, pos, camera.getName());
    }

    /**
     * A {@link PointLight} is a starburst at its position; a {@link SpotLight} is that
     * same starburst plus an arrow showing which way its cone is aimed; a
     * {@link DirectionalLight} — no real position, only a direction — is drawn as just
     * the arrow, from its editor-only anchor point (see the field doc on that class).
     */
    private void drawLight(Graphics2D g2, Light light) {
        boolean isSelected = light == selected;
        Color color = isSelected ? SELECTION_COLOR : Theme.ACCENT;
        float strokeWidth = isSelected ? 2.4f : 1f;

        if (light instanceof PointLight pointLight) {
            drawPointLightMarker(g2, pointLight.getPosition(), color, strokeWidth);
            drawLabel(g2, pointLight.getPosition(), light.getName());
        } else if (light instanceof SpotLight spotLight) {
            drawPointLightMarker(g2, spotLight.getPosition(), color, strokeWidth);
            drawLightArrow(g2, spotLight.getPosition(), spotLight.getDirection(), color, strokeWidth);
            drawLabel(g2, spotLight.getPosition(), light.getName());
        } else if (light instanceof DirectionalLight directionalLight) {
            drawLightArrow(g2, directionalLight.getPosition(), directionalLight.getDirection(), color, strokeWidth);
            drawLabel(g2, directionalLight.getPosition(), light.getName());
        }
    }

    private void drawPointLightMarker(Graphics2D g2, Vector3D pos, Color color, float strokeWidth) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(strokeWidth));
        double s = 0.4;
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 4;
            Vector3D d = new Vector3D(Math.cos(angle) * s, Math.sin(angle) * s, 0);
            drawWorldSegment(g2, Vector3D.subtract(pos, d), Vector3D.add(pos, d));
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawLightArrow(Graphics2D g2, Vector3D pos, Vector3D direction, Color color, float strokeWidth) {
        Vector3D tip = Vector3D.add(pos, Vector3D.scalarMultiplication(direction, 1.6));
        g2.setColor(color);
        g2.setStroke(new BasicStroke(strokeWidth));
        drawWorldSegment(g2, pos, tip);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawSelectionHandle(Graphics2D g2, Vector3D worldPos) {
        Point2D p = project(worldPos);
        if (p == null) return;
        g2.setColor(SELECTION_COLOR);
        double r = 5;
        g2.draw(new Ellipse2D.Double(p.getX() - r, p.getY() - r, 2 * r, 2 * r));
    }

    private Vector3D offset(Vector3D base, double dx, double dy, double dz) {
        return Vector3D.add(base, new Vector3D(dx, dy, dz));
    }

    private void drawWorldSegment(Graphics2D g2, Vector3D a, Vector3D b) {
        Point2D pa = project(a);
        Point2D pb = project(b);
        if (pa == null || pb == null) return;
        g2.draw(new Line2D.Double(pa, pb));
    }

    private void drawLabel(Graphics2D g2, Vector3D worldPos, String text) {
        Point2D p = project(worldPos);
        if (p == null) return;
        g2.setColor(Theme.MUTED);
        g2.setFont(getFont().deriveFont(11f));
        g2.drawString(text, (float) (p.getX() + 8), (float) (p.getY() - 8));
    }

    // ---- Hit testing (click-to-select) ----

    private Object hitTest(Point p) {
        if (scene == null) return null;

        // Small, precise markers should win over big meshes when they overlap on screen.
        for (Camera camera : scene.getCameras()) {
            if (hitsPoint(camera.getPosition(), p, 14)) return camera;
        }
        for (Light light : scene.getLights()) {
            Vector3D lightPos = SceneProperties.getPosition(light);
            if (lightPos != null && hitsPoint(lightPos, p, 12)) return light;
        }

        List<Object3D> objects = scene.getObjects();
        for (int i = objects.size() - 1; i >= 0; i--) { // topmost-added first
            Object3D object = objects.get(i);
            if (hitsObject(object, p)) return object;
        }
        return null;
    }

    private boolean hitsPoint(Vector3D world, Point p, double radiusPx) {
        Point2D s = project(world);
        return s != null && s.distance(p) <= radiusPx;
    }

    private boolean hitsObject(Object3D object, Point p) {
        if (object instanceof Sphere sphere) {
            Point2D center = project(sphere.getPosition());
            if (center == null) return false;
            Point2D edge = project(Vector3D.add(sphere.getPosition(), new Vector3D(sphere.getRadius(), 0, 0)));
            double radiusPx = Math.max(edge == null ? 0 : center.distance(edge), 10);
            return center.distance(p) <= radiusPx;
        }
        if (object instanceof Model3D model) {
            return hitsVertexBounds(vertexArray(model), p, 6);
        }
        if (object instanceof Plane plane) {
            return hitsVertexBounds(planeCorners(plane), p, 0);
        }
        return false;
    }

    private Vector3D[] vertexArray(Model3D model) {
        ArrayList<Vector3D> all = new ArrayList<>();
        for (Triangle t : model.getTriangles()) {
            all.add(t.getVertices()[0]);
            all.add(t.getVertices()[1]);
            all.add(t.getVertices()[2]);
        }
        return all.toArray(new Vector3D[0]);
    }

    private boolean hitsVertexBounds(Vector3D[] worldPoints, Point p, double paddingPx) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean any = false;
        for (Vector3D w : worldPoints) {
            Point2D s = project(w);
            if (s == null) continue;
            any = true;
            minX = Math.min(minX, s.getX());
            maxX = Math.max(maxX, s.getX());
            minY = Math.min(minY, s.getY());
            maxY = Math.max(maxY, s.getY());
        }
        if (!any) return false;
        return p.x >= minX - paddingPx && p.x <= maxX + paddingPx
                && p.y >= minY - paddingPx && p.y <= maxY + paddingPx;
    }

    // ---- Mouse navigation: drag to pan (ortho) / orbit (perspective), wheel to zoom; a plain click selects ----

    private void installNavigation() {
        MouseAdapter navigation = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressPoint = e.getPoint();
                lastDrag = e.getPoint();
                draggedPastThreshold = false;

                Object hit = hitTest(e.getPoint());
                if (hit != null) {
                    setSelected(hit);
                    // Perspective is reference-only: hitting an object there still
                    // selects it, but dragging afterward orbits, never moves it.
                    dragTarget = (view != ViewportView.PERSPECTIVE) ? hit : null;
                    dragStartPosition = dragTarget != null ? SceneProperties.getPosition(dragTarget) : null;
                } else {
                    dragTarget = null;
                    dragStartPosition = null;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastDrag == null) return;
                int dx = e.getX() - lastDrag.x;
                int dy = e.getY() - lastDrag.y;
                lastDrag = e.getPoint();
                if (pressPoint != null && pressPoint.distance(e.getPoint()) > CLICK_DRAG_THRESHOLD_PX) {
                    draggedPastThreshold = true;
                }

                if (dragTarget != null) {
                    moveDragTarget(dx, dy);
                } else if (view == ViewportView.PERSPECTIVE) {
                    camYaw += Math.toRadians(dx * 0.4);
                    camPitch = clamp(camPitch - Math.toRadians(dy * 0.4), Math.toRadians(-85), Math.toRadians(85));
                } else {
                    panX += dx;
                    panY += dy;
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // A plain click (no drag) on empty space deselects; on an object it was
                // already selected on press. A real drag either just moved dragTarget
                // (handled live in mouseDragged) or panned/orbited — nothing left to do
                // besides letting the caller know it happened, in case it just moved
                // something by accident and wants a way to undo it.
                if (dragTarget == null && !draggedPastThreshold) {
                    setSelected(hitTest(e.getPoint()));
                } else if (dragTarget != null && draggedPastThreshold && onObjectMoved != null) {
                    onObjectMoved.accept(dragTarget, dragStartPosition);
                }
                dragTarget = null;
                dragStartPosition = null;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                boolean overDraggable = view != ViewportView.PERSPECTIVE && hitTest(e.getPoint()) != null;
                setCursor(Cursor.getPredefinedCursor(overDraggable ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        addMouseListener(navigation);
        addMouseMotionListener(navigation);
        addMouseWheelListener(e -> {
            double factor = Math.pow(1.1, -e.getWheelRotation());
            if (view == ViewportView.PERSPECTIVE) {
                camDistance = clamp(camDistance / factor, 6, 120);
            } else {
                pixelsPerUnit = clamp(pixelsPerUnit * factor, 4, 80);
            }
            repaint();
        });
    }

    /**
     * Moves {@link #dragTarget} along the 2 axes visible in the current ortho view,
     * converting the screen-pixel drag delta with the same scale the grid itself uses.
     */
    private void moveDragTarget(int dxPixels, int dyPixels) {
        Vector3D current = SceneProperties.getPosition(dragTarget);
        if (current == null) return;
        double dh = dxPixels / pixelsPerUnit;
        double dv = -dyPixels / pixelsPerUnit;
        double x = current.getX(), y = current.getY(), z = current.getZ();
        switch (view) {
            case TOP -> { x += dh; z += dv; }
            case FRONT -> { x += dh; y += dv; }
            case SIDE -> { z += dh; y += dv; }
            default -> { return; }
        }
        SceneProperties.setPosition(dragTarget, new Vector3D(x, y, z));
        // Keep the Inspector's fields live if it's showing the thing we're dragging.
        if (onSelectionChanged != null) onSelectionChanged.accept(dragTarget);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
