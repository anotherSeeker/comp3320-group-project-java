package engine;

import static org.lwjgl.opengl.GL20.*;

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

    public void use() {
        glUseProgram(ID);
    }

    public void delete() {
        glDeleteShader(ID);
    }
}
