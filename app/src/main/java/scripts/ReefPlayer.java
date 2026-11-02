package scripts;

import dev.dominion.ecs.api.Entity;
import engine.MonoScript;
import coral.TrunkCollision;
import engine.components.Material;
import engine.components.MaterialEntry;
import engine.components.Transform;
import java.nio.file.Path;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.*;

/** Small swimming controller for the project's existing Feya character model. */
public final class ReefPlayer extends MonoScript {
    private Entity character;
    private final Vector3f position = new Vector3f(0, 0, 0);
    private float facing;
    public TrunkCollision collisions;

    @Override protected void Start() {
        int shader = Assets.LoadShader(Path.of("assets/shaders/coral/object.vert"), Path.of("assets/shaders/coral/object.frag"));
        int white = Assets.LoadSolidTexture(255,255,255,255);
        Material material = Assets.createMaterial(new MaterialEntry().add("SHADER",shader).add("COLOUR",white));
        material.coralTint = new Vector3f(.95f, .70f, .28f);
        character = Scene.createObject(Assets.LoadMesh(Path.of("assets/meshes/character_combined.obj")), material);
        place();
        System.out.println("Character: WASD swim, Q/E down/up, Shift faster, P reset. F water flow, I interaction.");
    }
    public Transform transform() { return character.get(Transform.class); }
    public void moveTo(Vector3f feet) {
        if (!feet.isFinite()) throw new IllegalArgumentException("Player position must be finite");
        position.set(collisions == null ? feet : collisions.resolve(feet)); place();
    }
    public void resolveCollision() {
        if (collisions != null) { position.set(collisions.resolve(position)); place(); }
    }
    /** Used by controls and checks, so both exercise the same collision path. */
    public void moveBy(Vector3f displacement) {
        Vector3f target = new Vector3f(position).add(displacement);
        target.x=Math.clamp(target.x,-23.5f,23.5f); target.z=Math.clamp(target.z,-23.5f,23.5f); target.y=Math.clamp(target.y,0,10);
        if (collisions == null) position.set(target);
        else position.set(collisions.move(position, target.sub(position)));
        place();
    }
    private void place() {
        Transform t = transform();
        t.setPosition(new Vector3f(position));
        t.setOrientationEuler(new Vector3f(0, facing, 0));
        t.model.scale(.45f); // Original character is about 3.6 units tall; display it at 1.6.
    }
    @Override protected void Update(double deltaTime) {
        float dt = (float)Math.clamp(deltaTime, 0, .1);
        if(imgui.ImGui.getIO().getWantCaptureKeyboard())return;
        if (Input.IsKeyDown(GLFW_KEY_P)) { facing=0; moveTo(new Vector3f(0,0,0)); return; }
        Vector3f forward = new Vector3f(0,0,-1).rotate(Scene.cameraTransform.orientation);
        forward.y=0;
        if (forward.lengthSquared() < .0001f) forward.set(0,0,-1);
        forward.normalize();
        Vector3f right = new Vector3f(forward).cross(0,1,0).normalize();
        Vector3f move = new Vector3f();
        if (Input.IsKeyDown(GLFW_KEY_W)) move.add(forward);
        if (Input.IsKeyDown(GLFW_KEY_S)) move.sub(forward);
        if (Input.IsKeyDown(GLFW_KEY_A)) move.sub(right);
        if (Input.IsKeyDown(GLFW_KEY_D)) move.add(right);
        if (Input.IsKeyDown(GLFW_KEY_Q)) move.y-=1;
        if (Input.IsKeyDown(GLFW_KEY_E)) move.y+=1;
        if (move.lengthSquared() > 0) {
            move.normalize();
            if (move.x*move.x + move.z*move.z > .0001f) facing=(float)Math.atan2(-move.x,-move.z);
            moveBy(move.mul(dt * (Input.IsKeyDown(GLFW_KEY_LEFT_SHIFT) ? 4 : 2)));
        }
    }
}
