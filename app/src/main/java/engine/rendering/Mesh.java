package engine.rendering;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL40.*;

import static org.lwjgl.system.MemoryUtil.*;

public class Mesh {
    int indiceCount = 0;
    public static final int MAX_WEIGHTS = 4;

    int VAO;
    int VBO;
    int EBO;
    int VBOweights;
    int VBObones;

    public Mesh(float[] positions, float[] uvs, float[] normals, float[] tangents, float[] bitangents, int[] indexes, int[] boneIndices, float[] weights) 
    {
        boolean error = false;
        int i = 0;

        //ArrayList<Integer> indices = new ArrayList<>();
        ArrayList<Vertex> vertices = new ArrayList<>();

        try {
            while (i < positions.length)
            {
                Vector3f pos = new Vector3f(positions[i], positions[i+1], positions[i+2]);
                Vector3f normal = new Vector3f(normals[i], normals[i+1], normals[i+2]);
                Vector2f uv = new Vector2f(uvs[i], uvs[i+1]);
                Vector3f tangent = new Vector3f(tangents[i], tangents[i+1], tangents[i+2]);
                Vector3f bitangent = new Vector3f(bitangents[i], bitangents[i+1], bitangents[i+2]);

                Vertex vertex = new Vertex(pos, normal, uv, tangent, bitangent);
                vertices.add(vertex);

                i += 3;
            }
        }
        catch(Exception e)
        {
            System.out.println("Something went wrong while trying to create a mesh from arrays");
            System.out.println(e);
            error = true;
        }

        if (error == false)
        {
            FloatBuffer vertexBuffer = memAllocFloat(vertices.size() * 14);
            for (Vertex vertex : vertices) {
                vertexBuffer.put(vertex.position.x);
                vertexBuffer.put(vertex.position.y);
                vertexBuffer.put(vertex.position.z);

                vertexBuffer.put(vertex.normal.x);
                vertexBuffer.put(vertex.normal.y);
                vertexBuffer.put(vertex.normal.z);

                vertexBuffer.put(vertex.uv.x);
                vertexBuffer.put(vertex.uv.y);

                vertexBuffer.put(vertex.tangent.x);
                vertexBuffer.put(vertex.tangent.y);
                vertexBuffer.put(vertex.tangent.z);

                vertexBuffer.put(vertex.bitangent.x);
                vertexBuffer.put(vertex.bitangent.y);
                vertexBuffer.put(vertex.bitangent.z);
            }

            IntBuffer indexBuffer = memAllocInt(indexes.length);
            indexBuffer.put(indexes);

            FloatBuffer weightsBuffer = MemoryUtil.memCallocFloat(weights.length);
            weightsBuffer.put(weights);

            IntBuffer boneIndicesBuffer = MemoryUtil.memCallocInt(boneIndices.length);
            boneIndicesBuffer.put(boneIndices);

            vertexBuffer.flip();
            indexBuffer.flip();
            weightsBuffer.flip();
            boneIndicesBuffer.flip();

            VAO = glGenVertexArrays();
            VBO = glGenBuffers();
            EBO = glGenBuffers();

            glBindVertexArray(VAO);

            int stride = 14 * Float.BYTES;

            glBindBuffer(GL_ARRAY_BUFFER, VBO);
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, VBO);
            glBufferData(GL_ARRAY_BUFFER, weightsBuffer, GL_STATIC_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, VBO);
            glBufferData(GL_ARRAY_BUFFER, boneIndicesBuffer, GL_STATIC_DRAW);

            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
            glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
            glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * Float.BYTES);
            glVertexAttribPointer(4, 3, GL_FLOAT, false, stride, 11 * Float.BYTES);
            glVertexAttribPointer(5, 4, GL_FLOAT, false, 0, 0);
            glVertexAttribPointer(6, 4, GL_FLOAT, false, 0, 0);

            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);
            glEnableVertexAttribArray(2);
            glEnableVertexAttribArray(3);
            glEnableVertexAttribArray(4);
            glEnableVertexAttribArray(5);
            glEnableVertexAttribArray(6);

            indiceCount = indexBuffer.remaining();

            memFree(weightsBuffer);
            memFree(boneIndicesBuffer);
            memFree(vertexBuffer);
            memFree(indexBuffer);
        }
    }

    //awful garbage function I hate
    public Mesh(float[] positions, float[] uvs, float[] normals, float[] tangents, float[] bitangents, int[] indexes) 
    {
        boolean error = false;
        int i = 0;

        //ArrayList<Integer> indices = new ArrayList<>();
        ArrayList<Vertex> vertices = new ArrayList<>();

        try {
            while (i < positions.length)
            {
                Vector3f pos = new Vector3f(positions[i], positions[i+1], positions[i+2]);
                Vector3f normal = new Vector3f(normals[i], normals[i+1], normals[i+2]);
                Vector2f uv = new Vector2f(uvs[i], uvs[i+1]);
                Vector3f tangent = new Vector3f(tangents[i], tangents[i+1], tangents[i+2]);
                Vector3f bitangent = new Vector3f(bitangents[i], bitangents[i+1], bitangents[i+2]);

                Vertex vertex = new Vertex(pos, normal, uv, tangent, bitangent);
                vertices.add(vertex);

                i += 3;
            }
        }
        catch(Exception e)
        {
            System.out.println("Something went wrong while trying to create a mesh from arrays");
            System.out.println(e);
            error = true;
        }

        if (error == false)
        {
            FloatBuffer vertexBuffer = memAllocFloat(vertices.size() * 14);
            for (Vertex vertex : vertices) {
                vertexBuffer.put(vertex.position.x);
                vertexBuffer.put(vertex.position.y);
                vertexBuffer.put(vertex.position.z);

                vertexBuffer.put(vertex.normal.x);
                vertexBuffer.put(vertex.normal.y);
                vertexBuffer.put(vertex.normal.z);

                vertexBuffer.put(vertex.uv.x);
                vertexBuffer.put(vertex.uv.y);

                vertexBuffer.put(vertex.tangent.x);
                vertexBuffer.put(vertex.tangent.y);
                vertexBuffer.put(vertex.tangent.z);

                vertexBuffer.put(vertex.bitangent.x);
                vertexBuffer.put(vertex.bitangent.y);
                vertexBuffer.put(vertex.bitangent.z);
            }

            IntBuffer indexBuffer = memAllocInt(indexes.length);
            indexBuffer.put(indexes);

            vertexBuffer.flip();
            indexBuffer.flip();

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

            memFree(vertexBuffer);
            memFree(indexBuffer);
        }
    }

    Mesh(ArrayList<Vertex> vertices, ArrayList<Integer> indices) {

        FloatBuffer vertexBuffer = memAllocFloat(vertices.size() * 14);

        for (Vertex vertex : vertices) {
            vertexBuffer.put(vertex.position.x);
            vertexBuffer.put(vertex.position.y);
            vertexBuffer.put(vertex.position.z);

            vertexBuffer.put(vertex.normal.x);
            vertexBuffer.put(vertex.normal.y);
            vertexBuffer.put(vertex.normal.z);

            vertexBuffer.put(vertex.uv.x);
            vertexBuffer.put(vertex.uv.y);

            vertexBuffer.put(vertex.tangent.x);
            vertexBuffer.put(vertex.tangent.y);
            vertexBuffer.put(vertex.tangent.z);

            vertexBuffer.put(vertex.bitangent.x);
            vertexBuffer.put(vertex.bitangent.y);
            vertexBuffer.put(vertex.bitangent.z);
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

    public Mesh(FloatBuffer vertexBuffer, IntBuffer indexBuffer, IntBuffer boneIndicesBuffer, FloatBuffer weightsBuffer) {
        VAO = glGenVertexArrays();
        VBO = glGenBuffers();
        EBO = glGenBuffers();

        VBOweights = glGenBuffers();
        VBObones = glGenBuffers();

        glBindVertexArray(VAO);

        int stride = 14 * Float.BYTES;

        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glVertexAttribPointer(4, 3, GL_FLOAT, false, stride, 11 * Float.BYTES);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, VBOweights);
        glBufferData(GL_ARRAY_BUFFER, weightsBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(5, 4, GL_FLOAT, false, 0, 0);

        glBindBuffer(GL_ARRAY_BUFFER, VBObones);
        glBufferData(GL_ARRAY_BUFFER, boneIndicesBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(6, 4, GL_FLOAT, false, 0, 0);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);
        glEnableVertexAttribArray(4);
        glEnableVertexAttribArray(5);
        glEnableVertexAttribArray(6);

        indiceCount = indexBuffer.remaining();
    }

    public static Mesh Cube() {
        ArrayList<Vertex> vertices = new ArrayList<>(
                List.of(
                        new Vertex(new Vector3f(-10.0f, -10.0f, -10.0f)),
                        new Vertex(new Vector3f(10.0f, -10.0f, -10.0f)),
                        new Vertex(new Vector3f(10.0f, 10.0f, -10.0f)),
                        new Vertex(new Vector3f(-10.0f, 10.0f, -10.0f)),

                        new Vertex(new Vector3f(-10.0f, -10.0f, 10.0f)),
                        new Vertex(new Vector3f(10.0f, -10.0f, 10.0f)),
                        new Vertex(new Vector3f(10.0f, 10.0f, 10.0f)),
                        new Vertex(new Vector3f(-10.0f, 10.0f, 10.0f))
                )
        );
        ArrayList<Integer> indices = new ArrayList<>(
                List.of(
                        4, 5, 6,
                        6, 7, 4,

                        1, 0, 3,
                        3, 2, 1,

                        0, 4, 7,
                        7, 3, 0,

                        5, 1, 2,
                        2, 6, 5,

                        3, 7, 6,
                        6, 2, 3,

                        0, 1, 5,
                        5, 4, 0
                )
        );

        return new Mesh(vertices, indices);
    }

    public static Mesh Quad() {
        //@formatter:off
        ArrayList<Vertex> vertices = new ArrayList<>(
            List.of(
                new Vertex(new Vector3f(-1.0f, -1.0f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f), new Vector2f(0, 0), new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f)),
                new Vertex(new Vector3f(-1.0f, 1.0f, 0.0f),  new Vector3f(0.0f, 0.0f, 1.0f), new Vector2f(0, 1), new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f)),
                new Vertex(new Vector3f(1.0f, 1.0f, 0.0f),   new Vector3f(0.0f, 0.0f, 1.0f), new Vector2f(1, 1), new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f)),
                new Vertex(new Vector3f(1.0f, -1.0f, 0.0f),  new Vector3f(0.0f, 0.0f, 1.0f), new Vector2f(1, 0), new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f))
            )
        );
        //@formatter:on
        ArrayList<Integer> indices = new ArrayList<>(List.of(0, 1, 2, 0, 2, 3));

        return new Mesh(vertices, indices);
    }

    public void bind() {
        glBindVertexArray(VAO);
    }

    public void draw() {
        bind();
        glDrawElements(GL_TRIANGLES, indiceCount, GL_UNSIGNED_INT, 0);
    }

    public void drawParticles () {
        glDrawElements(GL_TRIANGLES, indiceCount, GL_UNSIGNED_INT, 0);
    }

    public void drawPatches() {
        bind();
        glPatchParameteri(GL_PATCH_VERTICES, 3);
        glDrawElements(GL_PATCHES, indiceCount, GL_UNSIGNED_INT, 0);
    }

    public void delete() {
        glDeleteVertexArrays(VAO);
        glDeleteBuffers(VBO);
        glDeleteBuffers(EBO);
        glDeleteBuffers(VBOweights);
        glDeleteBuffers(VBObones);
    }

}
