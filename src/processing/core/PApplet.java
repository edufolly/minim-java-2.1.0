package processing.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 *
 * @author Eduardo Folly
 */
public class PApplet {

    private String sketchPath;
    public int height = 100;
    public int width = 100;

    public static void println(String msg) {
        System.out.println(msg);
    }

    public static void println() {
        System.out.println();
    }

    public static void print(String msg) {
        System.out.print(msg);
    }

    public String sketchPath(String where) {
        try {
            if (sketchPath == null) {
                sketchPath = System.getProperty("user.dir");
            }
        } catch (Exception e) {
        }
        try {
            if (new File(where).isAbsolute()) {
                return where;
            }
        } catch (Exception e) {
        }

        return sketchPath + File.separator + where;
    }

    public InputStream createInput(String filename) {
        try {
            return new FileInputStream(filename);
        } catch (FileNotFoundException ex) {
            return null;
        }
    }

    public static double constrain(double amt, double low, double high) {
        return (amt < low) ? low : ((amt > high) ? high : amt);
    }

    public static float constrain(float amt, float low, float high) {
        return (amt < low) ? low : ((amt > high) ? high : amt);
    }

    public static float map(float value,
            float istart, float istop,
            float ostart, float ostop) {
        return ostart + (ostop - ostart) * ((value - istart) / (istop - istart));
    }

    public static float floor(float val) {
        return (float) Math.floor(val);
    }

    public static float pow(float a, float b) {
        return (float) Math.pow(a, b);
    }

    public static float max(float f, float i) {
        return Math.max(f, i);
    }

    public void stroke(int i) {
        return;
    }

    public void strokeWeight(int i) {
        return;
    }

    public void line(int i, float f, int i0, float f0) {
        return;
    }

    public int color(int i, int i0, int i1) {
        return 0;
    }

    public int color(int i) {
        return 0;
    }
}
