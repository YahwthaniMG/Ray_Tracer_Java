package edu.up.isgc.cg.raytracer;

import edu.up.isgc.cg.raytracer.animation.SceneAnimation;
import edu.up.isgc.cg.raytracer.cameras.Camera;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.DoubleConsumer;

/**
 * Renders a {@link SceneAnimation} out as a numbered PNG sequence, one frame at a time,
 * then — if {@code ffmpeg} happens to be on this machine — assembles that sequence into an
 * actual {@code .mp4}. {@code ffmpeg} is invoked as an external process ({@link ProcessBuilder}),
 * exactly like any other command-line tool would be; it's not a dependency of this project
 * (no library/jar involved), so nothing here requires it to build or run — only to get an
 * mp4 out of it directly instead of a folder of frames.
 *
 * @author Claude (Anthropic)
 */
public final class AnimationRenderer {
    private AnimationRenderer() {}

    /**
     * How many frames a full render of {@code animation} amounts to (duration × fps,
     * rounded, at least 1).
     *
     * @param animation the animation
     * @return the frame count
     */
    public static int frameCount(SceneAnimation animation) {
        return Math.max(1, (int) Math.round(animation.getDurationSeconds() * animation.getFps()));
    }

    /**
     * Renders every frame of {@code animation} and saves each as
     * {@code outputDir/baseName_0000.png}, {@code _0001.png}, etc. Checks for cancellation
     * between frames (and mid-frame, since {@link RenderController#render} itself
     * cooperates with thread interruption) so a {@code SwingWorker.cancel(true)} on the
     * calling thread stops this promptly instead of finishing every remaining frame.
     *
     * @param scene       the scene
     * @param camera      the camera to render with
     * @param resolution  the resolution
     * @param aspectRatio the aspect ratio
     * @param mode        the secondary ray effect to apply (reflection/refraction/none)
     * @param samples     anti-aliasing/soft-shadow samples per pixel (see {@link RenderController#render})
     * @param animation   the animation to play back, one frame at a time
     * @param outputDir   where to save the frames (created if it doesn't exist)
     * @param baseName    the frame filename prefix
     * @param onProgress  called with the fraction of frames completed so far, or {@code null}
     * @throws IOException if a frame couldn't be written
     */
    public static void renderAnimation(Scene scene, Camera camera, int resolution, double aspectRatio,
                                        RenderMode mode, int samples, SceneAnimation animation,
                                        File outputDir, String baseName, DoubleConsumer onProgress) throws IOException {
        outputDir.mkdirs();
        int totalFrames = frameCount(animation);
        int fps = animation.getFps();

        for (int frame = 0; frame < totalFrames; frame++) {
            if (Thread.currentThread().isInterrupted()) return;

            animation.applyAt(frame / (double) fps);
            BufferedImage image = RenderController.render(scene, camera, resolution, aspectRatio, mode, samples, null);
            if (image == null) return; // cancelled mid-frame

            File file = new File(outputDir, String.format("%s_%04d.png", baseName, frame));
            ImageIO.write(image, "png", file);
            if (onProgress != null) onProgress.accept((frame + 1) / (double) totalFrames);
        }
    }

    /**
     * Whether {@code ffmpeg} can actually be run on this machine.
     *
     * @return true if it can
     */
    public static boolean isFfmpegAvailable() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start();
            drain(process);
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Assembles a rendered frame sequence into an mp4 via {@code ffmpeg}.
     *
     * @param frameDir  the directory {@link #renderAnimation} saved frames into
     * @param baseName  the frame filename prefix used when rendering
     * @param fps       the video's frame rate
     * @param outputMp4 where to write the finished video
     * @return {@code outputMp4}
     * @throws IOException          if ffmpeg couldn't be started or exited with a failure
     * @throws InterruptedException if interrupted while waiting for ffmpeg to finish
     */
    public static File assembleVideo(File frameDir, String baseName, int fps, File outputMp4)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("ffmpeg", "-y",
                "-framerate", String.valueOf(fps),
                "-i", new File(frameDir, baseName + "_%04d.png").getPath(),
                "-c:v", "libx264", "-pix_fmt", "yuv420p",
                outputMp4.getPath());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        drain(process);
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new IOException("ffmpeg exited with code " + exitCode);
        return outputMp4;
    }

    /** The exact command a user could run by hand later if ffmpeg isn't available right now. */
    public static String ffmpegCommandHint(File frameDir, String baseName, int fps, String outputFileName) {
        return String.format("ffmpeg -framerate %d -i \"%s_%%04d.png\" -c:v libx264 -pix_fmt yuv420p \"%s\"",
                fps, new File(frameDir, baseName).getPath(), outputFileName);
    }

    /** ffmpeg writes a lot to its (merged) output stream; not reading it can make it hang instead of exiting. */
    private static void drain(Process process) throws IOException {
        try (InputStream in = process.getInputStream()) {
            byte[] buffer = new byte[4096];
            while (in.read(buffer) != -1) {
                // discarded — only the exit code matters here
            }
        }
    }
}
