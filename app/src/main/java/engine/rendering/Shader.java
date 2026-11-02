package engine.rendering;

import engine.GraphicsPlatform;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public class Shader {
    private int ID;
    private final java.util.Map<String,Integer> locations = new java.util.HashMap<>();
    private int location(String name) { return locations.computeIfAbsent(name, n -> glGetUniformLocation(ID,n)); }

    public boolean init(String vertexSource, String fragmentSource) {
        vertexSource = GraphicsPlatform.shaderSource(vertexSource);
        fragmentSource = GraphicsPlatform.shaderSource(fragmentSource);
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSource);
        glCompileShader(vertexShader);

        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = glGetShaderInfoLog(vertexShader);
            glDeleteShader(vertexShader);
            throw new IllegalStateException("Vertex shader failed: " + error);
        }

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);

        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = glGetShaderInfoLog(fragmentShader);
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            throw new IllegalStateException("Fragment shader failed: " + error);
        }

        ID = glCreateProgram();
        glAttachShader(ID, vertexShader);
        glAttachShader(ID, fragmentShader);
        glLinkProgram(ID);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        if (glGetProgrami(ID, GL_LINK_STATUS) == GL_FALSE) {
            String error = glGetProgramInfoLog(ID);
            glDeleteProgram(ID);
            ID = 0;
            throw new IllegalStateException("Shader program failed: " + error);
        }

        return true;
    }

    public void setMatrix4(String uniformName, Matrix4f matrix) {
        MemoryStack stack = MemoryStack.stackPush();
        FloatBuffer floatBuffer = stack.mallocFloat(16);

        matrix.get(floatBuffer);

        glUniformMatrix4fv(location(uniformName), false, floatBuffer);

        stack.pop();
    }

    public void setInt(String uniformName, int value) {
        glUniform1i(location(uniformName), value);
    }

    public void setFloat(String uniformName, float value) {
        glUniform1f(location(uniformName), value);
    }

    public void setVector3(String uniformName, Vector3f vector) {
        glUniform3f(location(uniformName), vector.x, vector.y, vector.z);
    }

    public void setTexture(String uniformName, Texture texture, int textureSlot) {
        texture.use(textureSlot);
        glUniform1i(location(uniformName), textureSlot);
    }

    public void use() {
        glUseProgram(ID);
    }

    public void delete() {
        glDeleteProgram(ID);
    }


    public boolean initCompute(String computeSource) {
        if (!org.lwjgl.opengl.GL.getCapabilities().OpenGL43)
            throw new IllegalStateException("Compute postprocessing needs OpenGL 4.3");
        int computeShader = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(computeShader, computeSource);
        glCompileShader(computeShader);
        if (glGetShaderi(computeShader, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = glGetShaderInfoLog(computeShader);
            glDeleteShader(computeShader);
            throw new IllegalStateException("Compute shader failed: " + error);
        }
        ID = glCreateProgram();
        glAttachShader(ID, computeShader);
        glLinkProgram(ID);
        glDeleteShader(computeShader);
        if (glGetProgrami(ID, GL_LINK_STATUS) == GL_FALSE) {
            String error = glGetProgramInfoLog(ID);
            glDeleteProgram(ID);
            ID = 0;
            throw new IllegalStateException("Compute program failed: " + error);
        }
        return true;
    }

    public void dispatchCompute(int groupsX, int groupsY, int groupsZ) {
        glDispatchCompute(groupsX, groupsY, groupsZ);
        glMemoryBarrier(GL_ALL_BARRIER_BITS);
    }
}
