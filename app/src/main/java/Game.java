import static org.lwjgl.opengl.GL11.glGetError;

import engine.App;
import engine.GLErrors;

public class Game {
    public static void main(String[] args) {
        App app = new App();

        if (app.init(800, 600, "sunken castle")) {
            GLErrors.printIfError("app.init Game");

            //app.registerScript(new scripts.DemoScene());
            //GLErrors.printIfError("DemoScene Game");
            
            //app.registerScript(new scripts.Feya());
            //GLErrors.printIfError("Feya Game");

            app.registerScript(new scripts.WaterScene());
            GLErrors.printIfError("WaterScene Game");

            app.registerScript(new scripts.Camera());
            GLErrors.printIfError("Camera Game");

            app.run();
        }

        app.close();
    }
}
