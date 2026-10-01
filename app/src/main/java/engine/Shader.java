package engine;

import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public class Shader {
    private int ID;

    public boolean init(String vertexSource, String fragmentSource) {
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSource);
        glCompileShader(vertexShader);

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);

        ID = glCreateProgram();
        glAttachShader(ID, vertexShader);
        glAttachShader(ID, fragmentShader);
        glLinkProgram(ID);

        return true;
    }

    public void setMatrix4(String uniformName, Matrix4f matrix) {
        MemoryStack stack = MemoryStack.stackPush();
        FloatBuffer floatBuffer = stack.mallocFloat(16);

        matrix.get(floatBuffer);

        glUniformMatrix4fv(glGetUniformLocation(ID, uniformName), false, floatBuffer);

        stack.pop();
    }

    public void setTexture(String uniformName, Texture texture) {
        texture.bind();
        glUniform1i(glGetUniformLocation(ID, uniformName), texture.ID);
    }

    public void use() {
        glUseProgram(ID);
    }

    public void delete() {
        glDeleteProgram(ID);
    }

}
