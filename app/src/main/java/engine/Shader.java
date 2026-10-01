package engine;

import static org.lwjgl.opengl.GL11.GL_FALSE;
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

        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println(glGetShaderInfoLog(vertexShader, 1024));
        }

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);

        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println(glGetShaderInfoLog(fragmentShader, 1024));
        }

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

}
