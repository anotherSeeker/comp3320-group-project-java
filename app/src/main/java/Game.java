import engine.*;
import scripts.*;

public class Game {
    public static void main(String[] args) throws Exception {
        boolean check=false,original=false,generationCheck=false,showcase=false,capture=false;long seed=42;
        for(int i=0;i<args.length;i++) {
            switch(args[i]) {
                case "--check" -> check=true;
                case "--check-generation" -> generationCheck=true;
                case "--engine-demo" -> original=true;
                case "--showcase" -> showcase=true;
                case "--capture" -> capture=true;
                case "--seed" -> seed=Long.parseLong(args[++i]);
                default -> throw new IllegalArgumentException("Unknown option: "+args[i]);
            }
        }
        if(generationCheck){coral.ReefGenerationCheck.run();return;}
        App app=new App();
        try {
            if(!app.init(1100,800,"Coral Field — Latest Engine",!check&&!capture))throw new IllegalStateException("Cannot open graphics window");
            PostProcess effects = new PostProcess();
            effects.showUI(!check&&!showcase&&!capture);
            app.registerScript(effects);
            if(original) {
                app.registerScript(new DemoScene());app.registerScript(new Camera());
                app.registerScript(new Feya());app.registerScript(new Particles());
                if(check){
                    app.startScripts();for(int i=0;i<3;i++)app.renderFrame(1.0/60);
                    coral.FrameCapture.write(coral.FrameCapture.read(),java.nio.file.Path.of(System.getProperty("reef.checkOutput","build/reef-check"),"original-engine.png"));
                    if(org.lwjgl.opengl.GL11.glGetError()!=0)throw new IllegalStateException("Original engine graphics error");
                    System.out.println("PASS: original engine demo renders with tessellated materials and HDR.");
                }else app.run();
            } else {
                DemoScene environment = new DemoScene(true);
                Feya feya = null;
                if (!showcase) {
                    app.registerScript(environment);
                    feya = new Feya();
                    app.registerScript(feya);
                    app.registerScript(new Particles(org.lwjgl.glfw.GLFW.GLFW_KEY_U));
                }
                ReefPlayer player=new ReefPlayer(feya);
                ReefScene reef=new ReefScene(seed,environment.coralClearings());reef.player=player;
                ReefCamera camera=new ReefCamera();camera.player=player;
                app.registerScript(player);app.registerScript(reef);app.registerScript(camera);app.startScripts();
                if(capture)FantasyCapture.run(app,reef,player,camera);
                else if(check)ReefCheck.run(app,reef,player,camera,effects);else app.run();
            }
        } finally {app.close();}
    }
}
