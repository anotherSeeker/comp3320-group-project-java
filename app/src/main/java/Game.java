import engine.*;

public class Game {
    public static void main(String[] args) {
        App app = new App();

        if (app.init(800, 600, "sunken castle")) {
            app.registerScript(new scripts.DemoScene());
            app.registerScript(new scripts.Camera());
            app.registerScript(new scripts.PostProcess());
            app.registerScript(new scripts.ParticlesDemoScene());
            app.run();
        }

        app.close();
    }
}
