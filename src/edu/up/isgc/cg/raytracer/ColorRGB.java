package edu.up.isgc.cg.raytracer;

import java.awt.*;

/**
 * The type Color rgb.
 */
public final class ColorRGB {

    /**
     * Add color.
     *
     * @param colors the colors
     * @return the color
     */
    public static Color add(Color... colors) {
        int r = 0, g = 0, b = 0;
        for (Color color : colors) {
            r += color.getRed();
            g += color.getGreen();
            b += color.getBlue();
        }
        return new Color(regulator(r), regulator(g), regulator(b));
    }

    /**
     * Multiply color.
     *
     * @param c1 the c 1
     * @param c2 the c 2
     * @return the color
     */
    public static Color multiply(Color c1, Color c2) {
        int r = regulator(c1.getRed() * c2.getRed());
        int g = regulator(c1.getGreen() * c2.getGreen());
        int b = regulator(c1.getBlue() * c2.getBlue());
        return new Color(r, g, b);
    }

    /**
     * Multiply color.
     *
     * @param c the c
     * @param t the t
     * @return the color
     */
    public static Color multiply(Color c, double t) {
        int r = regulator((int) (c.getRed() * t));
        int g = regulator((int) (c.getGreen() * t));
        int b = regulator((int) (c.getBlue() * t));
        return new Color(r, g, b);
    }

    /**
     *
     * @param value
     * Regulates that the RGB values ??are correct
     * @return int
     */
    private static int regulator (int value) {
        if (value > 255) return 255;
        else if (value < 0) return 0;
        return value;
    }
}
