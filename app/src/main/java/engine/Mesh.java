package engine;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

import static org.lwjgl.system.MemoryUtil.*;

public class Mesh {
    int indiceCount = 0;

    int VAO;
    int VBO;
    int EBO;

    Mesh(IntBuffer indexBuffer, FloatBuffer positionBuffer, FloatBuffer normalBuffer, FloatBuffer uvBuffer) {
        VAO = glGenVertexArrays();
        VBO = glGenBuffers();
        EBO = glGenBuffers();

        glBindVertexArray(VAO);

        int vertexCount = positionBuffer.remaining() / 3;

        FloatBuffer vertexBuffer = memAllocFloat(vertexCount * 8);

        for (int i = 0; i < vertexCount; i++) {
            vertexBuffer.put(positionBuffer.get(i * 3));
            vertexBuffer.put(positionBuffer.get(i * 3 + 1));
            vertexBuffer.put(positionBuffer.get(i * 3 + 2));

            if (normalBuffer != null && normalBuffer.capacity() >= (i + 1) * 3) {
                vertexBuffer.put(normalBuffer.get(i * 3));
                vertexBuffer.put(normalBuffer.get(i * 3 + 1));
                vertexBuffer.put(normalBuffer.get(i * 3 + 2));
            } else {
                vertexBuffer.put(0.0f).put(0.0f).put(0.0f);
            }

            if (uvBuffer != null && uvBuffer.capacity() >= (i + 1) * 2) {
                vertexBuffer.put(uvBuffer.get(i * 2));
                vertexBuffer.put(uvBuffer.get(i * 2 + 1));
            } else {
                vertexBuffer.put(0.0f).put(0.0f);
            }

        }

        vertexBuffer.flip();

        int stride = 8 * Float.BYTES;

        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        memFree(vertexBuffer);

        indiceCount = indexBuffer.remaining();
    }

    public void bind() {
        glBindVertexArray(VAO);
    }

    public void draw() {
        bind();
        glDrawElements(GL_TRIANGLES, indiceCount, GL_UNSIGNED_INT, 0);
    }

    public void delete() {
        glDeleteVertexArrays(VAO);
        glDeleteBuffers(VBO);
        glDeleteBuffers(EBO);
    }

}
