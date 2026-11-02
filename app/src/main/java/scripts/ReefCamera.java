package scripts;

import engine.MonoScript;
import engine.components.Transform;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import imgui.ImGui;
import static org.lwjgl.glfw.GLFW.*;

/** Follow the character or inspect the complete coral field from above. */
public final class ReefCamera extends MonoScript {
    public ReefPlayer player;
    private float yaw=.35f,pitch=.42f,distance=9;
    private boolean overview,held;
    @Override protected void Start(){Input.UnlockMouse();apply();}
    public void overview(boolean enabled){overview=enabled;distance=enabled?64:9;pitch=enabled?1.05f:.42f;apply();}
    public void view(float angle,float elevation,float zoom){yaw=angle;pitch=elevation;distance=zoom;apply();}
    private void apply(){
        Vector3f target=overview?new Vector3f(0,1,0):new Vector3f(player.transform().position).add(0,1.1f,0);
        Vector3f eye=new Vector3f((float)Math.sin(yaw)*(float)Math.cos(pitch),(float)Math.sin(pitch),
            (float)Math.cos(yaw)*(float)Math.cos(pitch)).mul(distance).add(target);
        Scene.cameraTransform=new Transform(new Matrix4f().lookAt(eye,target,new Vector3f(0,1,0)).invert());
    }
    @Override protected void Update(double deltaTime){
        float dt=(float)Math.min(deltaTime,.1);
        boolean key=Input.IsKeyDown(GLFW_KEY_H);
        if(key&&!held&&!ImGui.getIO().getWantCaptureKeyboard())overview(!overview);held=key;
        var mouse=Input.GetMouseDelta();
        if(!ImGui.getIO().getWantCaptureMouse()){
            if(Input.IsMouse2Down()){yaw-=mouse.x*.006f;pitch-=mouse.y*.006f;}
            distance-=ImGui.getIO().getMouseWheel()*.8f;
        }
        if(!ImGui.getIO().getWantCaptureKeyboard()){
            if(Input.IsKeyDown(GLFW_KEY_LEFT))yaw-=dt;
            if(Input.IsKeyDown(GLFW_KEY_RIGHT))yaw+=dt;
            if(Input.IsKeyDown(GLFW_KEY_UP))pitch+=dt;
            if(Input.IsKeyDown(GLFW_KEY_DOWN))pitch-=dt;
            if(Input.IsKeyDown(GLFW_KEY_EQUAL))distance-=dt*8;
            if(Input.IsKeyDown(GLFW_KEY_MINUS))distance+=dt*8;
        }
        pitch=Math.clamp(pitch,.12f,1.4f);distance=Math.clamp(distance,3,85);apply();
    }
}
