MeshAnimpackage scripts.Animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;

import engine.Mesh;
import engine.components.Material;

import org.joml.Vector4f;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.assimp.Assimp.*;
import engine.*;

public class AnimImporter {

    public static MeshAnim loadModel(String modelId, String modelPath, TextureCache textureCache, MaterialCache materialCache,
                                  boolean animation) {
        return loadModel(modelId, modelPath, textureCache, materialCache, aiProcess_GenSmoothNormals | aiProcess_JoinIdenticalVertices |
                aiProcess_Triangulate | aiProcess_FixInfacingNormals | aiProcess_CalcTangentSpace | aiProcess_LimitBoneWeights |
                aiProcess_GenBoundingBoxes | (animation ? 0 : aiProcess_PreTransformVertices));
    }

    public static MeshAnim loadModel(String modelId, String modelPath, TextureCache textureCache,
                                  MaterialCache materialCache, int flags) {


        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(aiScene.mMaterials().get(i));
            Material material = processMaterial(aiMaterial, modelDir, textureCache);
            materialCache.addMaterial(material);
            materialList.add(material);
        }

        int numMeshes = aiScene.mNumMeshes();
        PointerBuffer aiMeshes = aiScene.mMeshes();
        List<MeshAnim> MeshAnimList = new ArrayList<>();
        List<Bone> boneList = new ArrayList<>();
        for (int i = 0; i < numMeshes; i++) {
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
            MeshAnim MeshAnim = processMesh(aiMesh, boneList);
            int materialIdx = aiMesh.mMaterialIndex();
            if (materialIdx >= 0 && materialIdx < materialList.size()) {
                MeshAnim.setMaterialIdx(materialList.get(materialIdx).getMaterialIdx());
            } else {
                MeshAnim.setMaterialIdx(MaterialCache.DEFAULT_MATERIAL_IDX);
            }
            MeshAnimList.add(MeshAnim);
        }

        return new MeshAnim(modelId, MeshAnimList, animations);
    }

    private static MeshAnim processMesh(AIMesh aiMesh, List<Bone> boneList) {

        return new MeshAnim(vertices, normals, tangents, bitangents, textCoords, indices, animMeshAnim.boneIds,
                animMeshAnim.weights, aabbMin, aabbMax);
    }
}