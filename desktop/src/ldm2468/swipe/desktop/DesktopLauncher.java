package ldm2468.swipe.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import ldm2468.swipe.DesktopGooglePlay;
import ldm2468.swipe.Main;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(540, 960);
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 2);
        Main.googlePlay = new DesktopGooglePlay();
        new Lwjgl3Application(new Main(), config);
    }
}
