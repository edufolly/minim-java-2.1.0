package minimjava;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import processing.core.PApplet;

/**
 *
 * @author Eduardo Folly
 */
public class Main {

    public static void main(String[] args) {
        PApplet p = new PApplet();
        Minim minim = new Minim(p);
        AudioPlayer song = minim.loadFile("resource_20.mp3");
        song.play();
        System.out.println("Tocando...");
        while (song.isPlaying()) {
            // Faça alguma coisa.
        }
        System.out.println("Parado!");
        song.close();
    }
}
