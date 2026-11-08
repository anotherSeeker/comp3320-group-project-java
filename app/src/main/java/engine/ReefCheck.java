package engine;

import coral.*;
import scripts.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL11.*;

/** Runs actual OpenGL rendering and the same collision path used by the character. */
public final class ReefCheck {
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static int maximumDifference(BufferedImage a,BufferedImage b){
        int max=0;for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++)for(int shift:new int[]{0,8,16})max=Math.max(max,Math.abs(((a.getRGB(x,y)>>shift)&255)-((b.getRGB(x,y)>>shift)&255)));return max;
    }
    private static long difference(BufferedImage a,BufferedImage b){
        long count=0;for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++)if(a.getRGB(x,y)!=b.getRGB(x,y))count++;return count;
    }
    private static void checkEngineIntegration(App app, ReefScene reef) {
        var materials = new java.util.HashMap<engine.components.Material, Float>();
        int triangleObjects = 0;
        for (var object : reef.Scene.ECS.findEntitiesWith(engine.components.GameObject.class)) {
            var material = object.comp().material;
            if (material.shader.hasTessellation()) materials.put(material, material.displacement);
            else triangleObjects++;
        }
        require(!materials.isEmpty() && triangleObjects >= 2,
                "Combined scene must contain tessellated environment objects, player and ground");
        BufferedImage displaced = FrameCapture.read();
        materials.keySet().forEach(material -> material.displacement = 0);
        app.renderFrame(0);
        require(difference(displaced, FrameCapture.read()) > 50, "Environment displacement has no visible effect");
        materials.forEach((material, displacement) -> material.displacement = displacement);
        app.renderFrame(0);

        var pipeline = reef.PostProcess;
        BufferedImage baseline = FrameCapture.read();
        float exposure = pipeline.Exposure;
        pipeline.Exposure = exposure + 1;
        app.renderFrame(0);
        require(difference(baseline, FrameCapture.read()) > 10000, "HDR controls do not affect combined scene");
        pipeline.Exposure = exposure;
        app.renderFrame(0);
        for (int feature : new int[]{engine.rendering.ShaderPipeline.BLOOM,
                engine.rendering.ShaderPipeline.FOG, engine.rendering.ShaderPipeline.COLOUR_GRADE,
                engine.rendering.ShaderPipeline.VIGNETTE}) {
            pipeline.Enable(feature);
            app.renderFrame(0);
            require(glGetError() == GL_NO_ERROR, "Postprocessing feature caused an OpenGL error");
            if (!pipeline.SupportsCompute())
                require(difference(baseline, FrameCapture.read()) == 0, "Unsupported effect altered the scene");
            pipeline.Disable(feature);
        }
        app.renderFrame(0);
        System.out.println("PASS: mixed tessellation/triangle rendering, displacement, HDR controls; compute effects "
                + (pipeline.SupportsCompute() ? "smoke-tested." : "safely disabled on OpenGL 4.1 (4.3 path not runtime-tested)."));
    }

    private static void checkFantasy(App app,ReefScene reef) {
        reef.setFlow(false);reef.setInteraction(false);reef.animationTime(1);app.renderFrame(0);
        BufferedImage luminous=FrameCapture.read();
        reef.PostProcess.CoralGlowIntensity=0;app.renderFrame(0);
        require(difference(luminous,FrameCapture.read())>1000,"Coral bloom has no visible effect");
        reef.PostProcess.CoralGlowIntensity=.75f;
        reef.setSpores(false);app.renderFrame(0);
        require(difference(luminous,FrameCapture.read())>30,"Spores are not visible");
        reef.setSpores(true);reef.setLife(false);app.renderFrame(0);
        require(difference(luminous,FrameCapture.read())>10000,"Bioluminescence toggle has no visible effect");
        reef.setLife(true);
        reef.animationTime(2);app.renderFrame(0);BufferedImage quiet=FrameCapture.read();
        reef.animationTime(1);reef.sendPulse();reef.animationTime(2);app.renderFrame(0);
        require(difference(quiet,FrameCapture.read())>1000,"Travelling coral pulse has no visible effect");
        reef.clearPulse();reef.animationTime(1);app.renderFrame(0);
        require(glGetError()==GL_NO_ERROR,"Fantasy effects left an OpenGL error");
        System.out.println("PASS: Mac-compatible glow, visible spores, bioluminescence toggle and travelling light pulse.");
    }

    public static void run(App app,ReefScene reef,ReefPlayer player,ReefCamera camera,PostProcess effects)throws Exception {
        TrunkCollisionCheck.run();
        require(reef.layoutFor(42,1).equals(reef.layoutFor(42,1)),"Seed is not repeatable");
        for(int variant=0;variant<ReefLayout.VARIANTS;variant++){
            var a=CoralGenerator.generate(ReefLayout.parameters(42,variant,false));
            var b=CoralGenerator.generate(ReefLayout.parameters(42,variant,false));
            require(java.util.Arrays.equals(a.vertices(),b.vertices())&&java.util.Arrays.equals(a.indices(),b.indices()),"Geometry is not seed-repeatable");
        }
        int previous=0;
        for(int d=0;d<3;d++) {
            var layout=reef.layoutFor(42,d);require(layout.size()>previous,"Density does not increase");previous=layout.size();
            int[] quadrants=new int[4];
            for(var c:layout){require(Math.abs(c.x())<24&&Math.abs(c.z())<24,"Outside field");require(c.x()*c.x()+c.z()*c.z()>=16,"Spawn blocked");quadrants[(c.x()>0?1:0)+(c.z()>0?2:0)]++;}
            for(var c:layout)for(var area:reef.clearings())
                require(!area.contains(c.x(),c.z()),"Coral intersects an environment clearing");
            for(int n:quadrants)require(n>30,"A quadrant lacks coral coverage");
            System.out.println("Layout density="+d+" colonies="+layout.size());
        }
        Path out=Path.of(System.getProperty("reef.checkOutput","build/reef-check"));
        reef.animationTime(1);reef.showUI(false);reef.setFlow(false);reef.setInteraction(false);camera.overview(true);
        reef.regenerate(42,1);reef.finishGrowth();app.renderFrame(0);
        app.renderFrame(0);BufferedImage first=FrameCapture.read();FrameCapture.write(first,out.resolve("overview.png"));
        require(reef.visibleCount>reef.count()*.9,"Overview misses field");require(reef.farCount>0&&reef.drawCalls<=72,"Batching/LOD failed");
        reef.regenerate(43,1);reef.finishGrowth();app.renderFrame(0);
        require(difference(first,FrameCapture.read())>10000,"Regeneration did not change the scene");
        reef.regenerate(42,1);reef.finishGrowth();app.renderFrame(0);
        FrameCapture.write(FrameCapture.read(),out.resolve("repeat.png"));
        System.out.println("Repeat pixels changed="+difference(first,FrameCapture.read()));
        // Native GPU interpolation can differ by a few colour levels after buffer reallocation.
        // Geometry and placement above must still match exactly.
        require(maximumDifference(first,FrameCapture.read())<=4 && difference(first,FrameCapture.read())<first.getWidth()*first.getHeight()*.002,"Same seed does not reproduce render");
        camera.overview(false);camera.view(.35f,.42f,9);app.renderFrame(0);
        require(reef.nearCount>0&&reef.farCount>0,"Follow view does not exercise both detail levels");
        FrameCapture.write(FrameCapture.read(),out.resolve("follow.png"));
        checkEngineIntegration(app,reef);
        checkFantasy(app,reef);
        var c=reef.colonies().get(reef.count()/2);
        player.moveTo(c.position().add(0,0,1.3f));
        for(int i=0;i<80;i++) {
            Vector3f direction=new Vector3f(c.position()).sub(player.transform().position);direction.y=0;
            if(direction.lengthSquared()>0)player.moveBy(direction.normalize().mul(.06f));
            require(player.transform().position.y==0,"Trunk lifted character");
            require(reef.collisions.clearance(player.transform().position)>-.001f,"Character inside trunk");
        }
        camera.view(.4f,.35f,5);app.renderFrame(0);
        BufferedImage still=FrameCapture.read();reef.setInteraction(true);app.renderFrame(0);
        require(difference(still,FrameCapture.read())>50,"Proximity deformation not visible");
        reef.setInteraction(false);reef.setFlow(true);app.renderFrame(0);
        require(difference(still,FrameCapture.read())>50,"Water sway not visible");
        BufferedImage waving=FrameCapture.read();reef.animationTime(4);app.renderFrame(0);
        require(difference(waving,FrameCapture.read())>50,"Water movement does not change with time");
        FrameCapture.write(FrameCapture.read(),out.resolve("closeup.png"));
        reef.regenerate(42,2);player.moveTo(new Vector3f());camera.overview(true);app.renderFrame(0);
        BufferedImage growing=FrameCapture.read();reef.finishGrowth();app.renderFrame(0);
        require(difference(growing,FrameCapture.read())>10000,"Growth is not visible");
        for(var colony:reef.colonies()){
            player.moveTo(colony.position().add(0,8,0));
            require(player.transform().position.distance(colony.position().add(0,8,0))<1e-4f,"High-altitude position was deflected");
            float stepX=colony.x()>0?-.2f:.2f; // Keep this check away from the field boundary.
            player.moveBy(new Vector3f(stepX,0,0));
            require(player.transform().position.distance(colony.position().add(stepX,8,0))<1e-4f,"High-altitude movement was blocked");
            Vector3f resolved=reef.collisions.resolve(colony.position());
            require(resolved.y==0&&reef.collisions.clearance(resolved)>-.001f,"Dense field overlap/hash failure");
        }
        player.moveTo(new Vector3f());
        for(int i=0;i<10;i++)app.renderFrame(1.0/60);
        long start=System.nanoTime();
        for(int i=0;i<60;i++){app.renderFrame(1.0/60);glFinish();}
        double ms=(System.nanoTime()-start)/60e6;
        System.out.printf("Dense overview: %d colonies, %d visible, %d draw calls, %d triangles, %.2f ms/frame (GPU synchronized)%n",reef.count(),reef.visibleCount,reef.drawCalls,reef.triangles,ms);
        reef.showUI(true);effects.showUI(true);app.renderFrame(0);FrameCapture.write(FrameCapture.read(),out.resolve("dense-ui.png"));
        require(glGetError()==GL_NO_ERROR,"OpenGL error");
        System.out.println("PASS: field coverage, deterministic geometry/placement, density, near/far batching, regeneration, trunk collisions, proximity response, water sway, GUI and real GPU render.");
    }
}
