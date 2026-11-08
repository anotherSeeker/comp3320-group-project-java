package engine;

import coral.FrameCapture;
import java.nio.file.Path;
import org.joml.Vector3f;
import scripts.ReefScene;
import scripts.ReefPlayer;
import scripts.ReefCamera;

/** Reproducible frames of the real renderer, including a propagating coral response. */
public final class FantasyCapture {
    private FantasyCapture() { }
    public static void run(App app,ReefScene reef,ReefPlayer player,ReefCamera camera) throws Exception {
        Path out=Path.of(System.getProperty("reef.checkOutput","build/reef-check"),"fantasy");
        reef.showUI(false);reef.setLife(true);reef.setSpores(true);reef.setFlow(true);
        reef.finishGrowth();player.moveTo(new Vector3f());camera.overview(true);
        camera.view(-.45f,.23f,17);
        reef.animationTime(0);reef.sendPulse();
        for(int frame=0;frame<48;frame++) {
            reef.animationTime(frame*.12f);
            app.renderFrame(0);
            FrameCapture.write(FrameCapture.read(),out.resolve(String.format("frame-%03d.png",frame)));
        }
        reef.clearPulse();reef.animationTime(3);app.renderFrame(0);
        FrameCapture.write(FrameCapture.read(),out.resolve("hero.png"));
        camera.overview(false);camera.view(.35f,.32f,7);app.renderFrame(0);
        FrameCapture.write(FrameCapture.read(),out.resolve("playable.png"));
        System.out.println("PASS: captured 48 fantasy animation frames and two stills in "+out);
    }
}
