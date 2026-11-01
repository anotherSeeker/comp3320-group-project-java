package engine;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public class Shader {
    private int ID;

    public boolean init(String vertexSource, String fragmentSource) {
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSource);
        glCompileShader(vertexShader);

        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.out.println("VERTEX");
            System.err.println(glGetShaderInfoLog(vertexShader, 1024));
            return false;
        }

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);

        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.out.println("FRAGMENT");
            System.err.println(glGetShaderInfoLog(fragmentShader, 1024));
            return false;
        }

        ID = glCreateProgram();
        glAttachShader(ID, vertexShader);
        glAttachShader(ID, fragmentShader);
        glLinkProgram(ID);

        return true;
    }

    public boolean initCompute(String computeSource) {
        int computeShader = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(computeShader, computeSource);
        glCompileShader(computeShader);

        if (glGetShaderi(computeShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.out.println("COMPUTE");
            System.err.println(glGetShaderInfoLog(computeShader, 1024));
            return false;
        }

        ID = glCreateProgram();
        glAttachShader(ID, computeShader);
        glLinkProgram(ID);

        if (glGetProgrami(ID, GL_LINK_STATUS) == GL_FALSE) {
            System.out.println("PROGRAM");
            System.err.println(glGetProgramInfoLog(ID));
            return false;
        }

        return true;
    }

    public void setMatrix4(String uniformName, Matrix4f matrix) {
        MemoryStack stack = MemoryStack.stackPush();
        FloatBuffer floatBuffer = stack.mallocFloat(16);

        matrix.get(floatBuffer);

        glUniformMatrix4fv(glGetUniformLocation(ID, uniformName), false, floatBuffer);

        stack.pop();
    }

     public void setUniform(String uniformName, Matrix4f[] matrices) {
        MemoryStack stack = MemoryStack.stackPush();
            int length = matrices != null ? matrices.length : 0;
            FloatBuffer floatBuffer = stack.mallocFloat(16 * length);
            for (int i = 0; i < length; i++) {
                matrices[i].get(16 * i, floatBuffer);
            }
            glUniformMatrix4fv(glGetUniformLocation(ID, uniformName), false, floatBuffer);
        stack.pop();
    }

    public void setInt(String uniformName, int value) {
        glUniform1i(glGetUniformLocation(ID, uniformName), value);
    }

    public void setFloat(String uniformName, float value) {
        glUniform1f(glGetUniformLocation(ID, uniformName), value);
    }

    public void setVector3(String uniformName, Vector3f vector) {
        glUniform3f(glGetUniformLocation(ID, uniformName), vector.x, vector.y, vector.z);
    }

    public void setTexture(String uniformName, Texture texture, int textureSlot) {
        texture.use(textureSlot);
        glUniform1i(glGetUniformLocation(ID, uniformName), textureSlot);
    }

    public void use() {
        glUseProgram(ID);
    }

    public void delete() {
        glDeleteProgram(ID);
    }

    public void dispatchCompute(int groupsX, int groupsY, int groupsZ) {
        glDispatchCompute(groupsX, groupsY, groupsZ);
        glMemoryBarrier(GL_ALL_BARRIER_BITS);
    }

}
