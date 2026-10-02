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

        this(vertexBuffer, indexBuffer);

        memFree(vertexBuffer);
        memFree(indexBuffer);
    }

    public Mesh(FloatBuffer vertexBuffer, IntBuffer indexBuffer) {
        VAO = glGenVertexArrays();
        VBO = glGenBuffers();
        EBO = glGenBuffers();

        glBindVertexArray(VAO);

        int stride = 14 * Float.BYTES;

        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glVertexAttribPointer(4, 3, GL_FLOAT, false, stride, 11 * Float.BYTES);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);
        glEnableVertexAttribArray(4);

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
