package engine;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.nio.FloatBuffer;

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

    Mesh(ArrayList<Vertex> vertices, ArrayList<Integer> indices) {

        FloatBuffer vertexBuffer = memAllocFloat(vertices.size() * 8);

        for (Vertex vertex : vertices) {
            vertexBuffer.put(vertex.position.x);
            vertexBuffer.put(vertex.position.y);
            vertexBuffer.put(vertex.position.z);

            vertexBuffer.put(vertex.normal.x);
            vertexBuffer.put(vertex.normal.y);
            vertexBuffer.put(vertex.normal.z);

            vertexBuffer.put(vertex.uv.x);
            vertexBuffer.put(vertex.uv.y);
        }

        IntBuffer indexBuffer = memAllocInt(indices.size());
        indexBuffer.put(indices.stream().mapToInt(Integer::intValue).toArray());

        vertexBuffer.flip();
        indexBuffer.flip();

        indiceCount = indexBuffer.remaining();

        bindData(vertexBuffer, indexBuffer);

        memFree(indexBuffer);
    }

    Mesh(IntBuffer indexBuffer, FloatBuffer positionBuffer, FloatBuffer normalBuffer, FloatBuffer uvBuffer) {

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
        indiceCount = indexBuffer.remaining();

        bindData(vertexBuffer, indexBuffer);
    }

    private void bindData(FloatBuffer vertexBuffer, IntBuffer indexBuffer) {
        VAO = glGenVertexArrays();
        VBO = glGenBuffers();
        EBO = glGenBuffers();

        glBindVertexArray(VAO);

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
