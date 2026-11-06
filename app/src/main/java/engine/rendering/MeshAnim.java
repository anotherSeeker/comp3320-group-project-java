package engine.rendering;

import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class MeshAnim {
    private Mesh mesh;
    private Vector3f aabbMax;
    private Vector3f aabbMin;
    private int[] boneIndices;
    private int materialIdx;
    private float[] weights;

    int VBO;

    public MeshAnim(Mesh mesh, int[] boneIndices, float[] weights) {
        materialIdx = 0;
        this.mesh = mesh;
        this.boneIndices = boneIndices;
        this.weights = weights;
        //this.aabbMin = aabbMin;
        //this.aabbMax = aabbMax;

        VBO = glGenBuffers();

        // Bone weights
        FloatBuffer weightsBuffer = MemoryUtil.memCallocFloat(weights.length);
        weightsBuffer.put(weights).flip();
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, weightsBuffer, GL_STATIC_DRAW);
        glEnableVertexAttribArray(5);
        glVertexAttribPointer(5, 4, GL_FLOAT, false, 0, 0);


        // Bone indices
        IntBuffer boneIndicesBuffer = MemoryUtil.memCallocInt(boneIndices.length);
        boneIndicesBuffer.put(boneIndices).flip();
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, boneIndicesBuffer, GL_STATIC_DRAW);
        glEnableVertexAttribArray(6);
        glVertexAttribPointer(6, 4, GL_FLOAT, false, 0, 0);

        MemoryUtil.memFree(weightsBuffer);
        MemoryUtil.memFree(boneIndicesBuffer);
    }

    public static class AnimatedFrame {
        private Matrix4f[] bonesMatrices;
        private int offset;

        public AnimatedFrame(Matrix4f[] bonesMatrices) {
            this.bonesMatrices = bonesMatrices;
        }

        public void clearData() {
            bonesMatrices = null;
        }

        public Matrix4f[] getBonesMatrices() {
            return bonesMatrices;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }
    }

    public Vector3f getAabbMax() {
        return aabbMax;
    }

    public Vector3f getAabbMin() {
        return aabbMin;
    }

    public int[] getBoneIndices() {
        return boneIndices;
    }

    public int getMaterialIdx() {
        return materialIdx;
    }

    public float[] getWeights() {
        return weights;
    }

    public void setMaterialIdx(int materialIdx) {
        this.materialIdx = materialIdx;
    }

    public Mesh getMesh() {
        return mesh;
    }
    public void delete() {
        glDeleteBuffers(VBO);
        mesh.delete();
    }
}