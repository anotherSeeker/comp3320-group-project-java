package engine.rendering;

import coral.ReefLayout;
import engine.AssetManager;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.GL_PROGRAM_POINT_SIZE;
import static org.lwjgl.system.MemoryUtil.*;

/** Seeded, depth-tested luminous spores; one draw call and no per-frame CPU simulation. */
public final class CoralMotes {
    private final int vao=glGenVertexArrays(), buffer=glGenBuffers();
    private final Shader shader;
    private int count;

    public CoralMotes() {
        AssetManager assets=AssetManager.get();
        shader=assets.getShader(assets.LoadShader(Path.of("assets/shaders/coral/motes.vert"),
                Path.of("assets/shaders/coral/motes.frag")));
        glBindVertexArray(vao); glBindBuffer(GL_ARRAY_BUFFER,buffer);
        glVertexAttribPointer(0,3,GL_FLOAT,false,20,0); glEnableVertexAttribArray(0);
        glVertexAttribPointer(1,1,GL_FLOAT,false,20,12); glEnableVertexAttribArray(1);
        glVertexAttribPointer(2,1,GL_FLOAT,false,20,16); glEnableVertexAttribArray(2);
    }

    public void regenerate(List<ReefLayout.Colony> colonies,float[] heights,long seed) {
        count=colonies.size()*5;
        var data=memAllocFloat(count*5);
        Random random=new Random(seed ^ 0x5DEECE66DL);
        try {
            for (var c:colonies) for(int i=0;i<5;i++) {
                data.put(c.x()+(random.nextFloat()-.5f)*1.4f)
                    .put(heights[c.variant()]*c.scale()*(.4f+random.nextFloat()*.5f))
                    .put(c.z()+(random.nextFloat()-.5f)*1.4f)
                    .put(random.nextFloat()*10).put(c.variant()%3);
            }
            data.flip(); glBindBuffer(GL_ARRAY_BUFFER,buffer); glBufferData(GL_ARRAY_BUFFER,data,GL_STATIC_DRAW);
        } finally { memFree(data); }
    }

    public void draw(Matrix4f matrix,Vector3f camera,float time,float growth,Vector3f player,
            Vector3f wake,Vector3f velocity,float interaction,Vector3f pulseOrigin,float pulseAge) {
        shader.use(); shader.setMatrix4("CameraMatrix",matrix); shader.setVector3("ViewPosition",camera);
        shader.setFloat("Time",time); shader.setFloat("Growth",growth);
        shader.setVector3("PlayerPosition",player); shader.setVector3("WakePosition",wake);
        shader.setVector3("PlayerVelocity",velocity); shader.setFloat("InteractionStrength",interaction);
        shader.setVector3("PulseOrigin",pulseOrigin); shader.setFloat("PulseAge",pulseAge);
        try(var stack=MemoryStack.stackPush()) {
            var viewport=stack.mallocInt(4); glGetIntegerv(GL_VIEWPORT,viewport);
            shader.setFloat("ViewHeight",viewport.get(3));
        }
        glEnable(GL_PROGRAM_POINT_SIZE); glEnable(GL_BLEND); glBlendFunc(GL_ONE,GL_ONE);
        glDepthMask(false); glBindVertexArray(vao); glDrawArrays(GL_POINTS,0,count);
        glDepthMask(true); glDisable(GL_BLEND); glDisable(GL_PROGRAM_POINT_SIZE);
    }

    public void close() { glDeleteBuffers(buffer); glDeleteVertexArrays(vao); }
}
