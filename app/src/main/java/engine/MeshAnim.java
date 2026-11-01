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
import static org.lwjgl.opengl.GL30 .*;

import static org.lwjgl.system.MemoryUtil.*;

import engine.Mesh;



public class MeshAnim {
    private Mesh mesh;
    private Vector3f aabbMax;
    private Vector3f aabbMin;
    private int[] boneIndices;
    private int materialIdx;
    private float[] weights;

    public static final int MAX_WEIGHTS = 4;

    public MeshAnim(Mesh mesh, int[] boneIndices, float[] weights) {
        materialIdx = 0;
        this.mesh = mesh;
        this.boneIndices = boneIndices;
        this.weights = weights;
        //this.aabbMin = aabbMin;
        //this.aabbMax = aabbMax;
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
}