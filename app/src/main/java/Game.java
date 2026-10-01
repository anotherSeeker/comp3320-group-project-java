import engine.*;

public class Game {
    public static void main(String[] args) {
        App app = new App();

        if (app.init(800, 600, "sunken castle")) {
            app.registerScript(new scripts.DemoScene());
            app.registerScript(new scripts.Camera());
            app.run();
        }

        app.close();
    }
}
