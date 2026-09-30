package engine;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

import static org.lwjgl.system.MemoryUtil.*;

public class Mesh {
    ArrayList<Vertex> vertices;
    ArrayList<Integer> indices;

    int VAO;
    int VBO;
    int EBO;

    Mesh(ArrayList<Vertex> vertices, ArrayList<Integer> indices) {
        VAO = glGenVertexArrays();
        VBO = glGenBuffers();
        EBO = glGenBuffers();

        glBindVertexArray(VAO);

        FloatBuffer vertexBuffer = memAllocFloat(vertices.size() * 3);

        for (Vertex vertex : vertices) {
            vertexBuffer.put(vertex.position.x);
            vertexBuffer.put(vertex.position.y);
            vertexBuffer.put(vertex.position.z);
        }

        IntBuffer indexBuffer = memAllocInt(indices.size());
        indexBuffer.put(indices.stream().mapToInt(Integer::intValue).toArray());

        vertexBuffer.flip();
        indexBuffer.flip();

        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        memFree(vertexBuffer);
        memFree(indexBuffer);

        this.vertices = vertices;
        this.indices = indices;
    }

    public void bind() {
        glBindVertexArray(VAO);
    }

    public void draw() {
        bind();
        glDrawElements(GL_TRIANGLES, indices.size(), GL_UNSIGNED_INT, 0);
    }

    public void delete() {
        glDeleteVertexArrays(VAO);
        glDeleteBuffers(VBO);
        glDeleteBuffers(EBO);
    }

}
