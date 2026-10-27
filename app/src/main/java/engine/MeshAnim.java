package engine;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

import static org.lwjgl.system.MemoryUtil.*;



public class MeshAnim {

    private Vector3f aabbMax;
    private Vector3f aabbMin;
    private float[] bitangents;
    private int[] boneIndices;
    private int[] indices;
    private int materialIdx;
    private float[] normals;
    private float[] positions;
    private float[] tangents;
    private float[] textCoords;
    private float[] weights;

    public MeshAnim(float[] positions, float[] normals, float[] tangents, float[] bitangents,
                    float[] textCoords, int[] indices, int[] boneIndices, float[] weights,
                    Vector3f aabbMin, Vector3f aabbMax) {
        materialIdx = 0;
        this.positions = positions;
        this.normals = normals;
        this.tangents = tangents;
        this.bitangents = bitangents;
        this.textCoords = textCoords;
        this.indices = indices;
        this.boneIndices = boneIndices;
        this.weights = weights;
        this.aabbMin = aabbMin;
        this.aabbMax = aabbMax;
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

    public float[] getBitangents() {
        return bitangents;
    }

    public int[] getBoneIndices() {
        return boneIndices;
    }

    public int[] getIndices() {
        return indices;
    }

    public int getMaterialIdx() {
        return materialIdx;
    }

    public float[] getNormals() {
        return normals;
    }

    public float[] getPositions() {
        return positions;
    }

    public float[] getTangents() {
        return tangents;
    }

    public float[] getTextCoords() {
        return textCoords;
    }

    public float[] getWeights() {
        return weights;
    }

    public void setMaterialIdx(int materialIdx) {
        this.materialIdx = materialIdx;
    }
}
