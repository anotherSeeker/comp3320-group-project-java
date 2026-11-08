package engine.rendering;

import coral.CoralMesh;
import java.nio.FloatBuffer;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

/** Reuses one generated shape for many placements in a single draw call. */
public final class InstancedMesh extends Mesh {
    private final int instances;
    public static final int INSTANCE_FLOATS=11;
    public InstancedMesh(CoralMesh cpu) {
        this(upload(cpu));
    }
    private record Upload(FloatBuffer vertices,java.nio.IntBuffer indices) { }
    private static Upload upload(CoralMesh cpu){
        float[] v=cpu.vertices();int[] i=cpu.indices();
        return new Upload(MemoryUtil.memAllocFloat(v.length).put(v).flip(),MemoryUtil.memAllocInt(i.length).put(i).flip());
    }
    private InstancedMesh(Upload data) {
        super(data.vertices(),data.indices());
        MemoryUtil.memFree(data.vertices());MemoryUtil.memFree(data.indices());
        instances=glGenBuffers();glBindVertexArray(VAO);glBindBuffer(GL_ARRAY_BUFFER,instances);
        int stride=INSTANCE_FLOATS*Float.BYTES;
        glVertexAttribPointer(5,4,GL_FLOAT,false,stride,0);glEnableVertexAttribArray(5);glVertexAttribDivisor(5,1);
        glVertexAttribPointer(6,4,GL_FLOAT,false,stride,4L*Float.BYTES);glEnableVertexAttribArray(6);glVertexAttribDivisor(6,1);
        glVertexAttribPointer(7,3,GL_FLOAT,false,stride,8L*Float.BYTES);glEnableVertexAttribArray(7);glVertexAttribDivisor(7,1);
    }
    public int drawInstances(FloatBuffer data) {
        int count=data.remaining()/INSTANCE_FLOATS;
        if(count==0)return 0;
        bind();glBindBuffer(GL_ARRAY_BUFFER,instances);glBufferData(GL_ARRAY_BUFFER,data,GL_STREAM_DRAW);
        glDrawElementsInstanced(GL_TRIANGLES,indiceCount,GL_UNSIGNED_INT,0,count);
        return count;
    }
    public int triangleCount(){return indiceCount/3;}
    @Override public void delete(){glDeleteBuffers(instances);super.delete();}
}
