package engine;

import static org.lwjgl.assimp.Assimp.aiGetMaterialFloatArray;
import static org.lwjgl.assimp.Assimp.aiGetMaterialTexture;
import static org.lwjgl.assimp.Assimp.aiGetMaterialTextureCount;
import static org.lwjgl.assimp.Assimp.aiReturn_SUCCESS;
import static org.lwjgl.assimp.Assimp.aiTextureType_AMBIENT_OCCLUSION;
import static org.lwjgl.assimp.Assimp.aiTextureType_BASE_COLOR;
import static org.lwjgl.assimp.Assimp.aiTextureType_DIFFUSE;
import static org.lwjgl.assimp.Assimp.aiTextureType_DIFFUSE_ROUGHNESS;
import static org.lwjgl.assimp.Assimp.aiTextureType_LIGHTMAP;
import static org.lwjgl.assimp.Assimp.aiTextureType_METALNESS;
import static org.lwjgl.assimp.Assimp.aiTextureType_NORMALS;
import static org.lwjgl.opengl.GL11.GL_RGBA;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AITexel;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import engine.components.Material;
import engine.components.MaterialEntry;
import engine.components.Transform;
import engine.rendering.Texture;

public class SceneParser {
    private AssetManager assetManager = AssetManager.get();

    private PointerBuffer meshes;
    private PointerBuffer materials;

    private SceneGraph sceneGraph;
    private AIScene scene;

    private Path filepath;
    private Path directory;

    public void loadScene(Path filepath, AIScene scene, SceneGraph sceneGraph) {
        this.meshes = scene.mMeshes();
        this.materials = scene.mMaterials();
        this.scene = scene;
        this.sceneGraph = sceneGraph;

        this.filepath = filepath.toAbsolutePath();
        this.directory = this.filepath.getParent();

        AINode root = scene.mRootNode();

        System.out.printf("Meshes: %s\nMaterials: %s\n", scene.mNumMeshes(), scene.mNumMaterials());

        processNode(root, new Matrix4f());
    }

    private void processNode(AINode node, Matrix4f parentTransform) {

        Matrix4f localTransform = toJoml(node.mTransformation());
        Matrix4f worldTransform = new Matrix4f(parentTransform).mul(localTransform);

        IntBuffer meshIndexes = node.mMeshes();

        for (int i = 0; i < node.mNumMeshes(); i++) {

            int meshIndex = meshIndexes.get(i);
            AIMesh mesh = AIMesh.create(meshes.get(meshIndex));

            int meshID = assetManager.LoadMesh(assetManager.createMesh(mesh), node.mName() + "#" + mesh.mName());

            int materialIndex = mesh.mMaterialIndex();
            AIMaterial assimpMat = AIMaterial.create(materials.get(materialIndex));

            Material material = loadMaterial(assimpMat);

            var entity = sceneGraph.createObject(meshID, material);
            Transform transform = entity.get(Transform.class);
            transform.setModel(worldTransform);
        }

        PointerBuffer children = node.mChildren();

        for (int i = 0; i < node.mNumChildren(); i++) {
            AINode child = AINode.create(children.get(i));
            processNode(child, worldTransform);
        }
    }

    private Matrix4f toJoml(AIMatrix4x4 mat) {
        return new Matrix4f(
                mat.a1(), mat.b1(), mat.c1(), mat.d1(),
                mat.a2(), mat.b2(), mat.c2(), mat.d2(),
                mat.a3(), mat.b3(), mat.c3(), mat.d3(),
                mat.a4(), mat.b4(), mat.c4(), mat.d4()
        );

    }

    private Material loadMaterial(AIMaterial assimpMat) {
        Material material;
        MaterialEntry entry = new MaterialEntry();

        try (MemoryStack stack = MemoryStack.stackPush()) {

            Integer texture;

            texture = loadTexture(assimpMat, aiTextureType_BASE_COLOR);
            if (texture == null) {
                entry.add("COLOUR", loadTexture(assimpMat, aiTextureType_DIFFUSE));
            } else {
                entry.add("COLOUR", loadTexture(assimpMat, aiTextureType_BASE_COLOR));
            }

            texture = loadTexture(assimpMat, aiTextureType_NORMALS);
            if (texture != null) {
                entry.add("NORMAL", texture);
            }

            texture = loadTexture(assimpMat, aiTextureType_METALNESS);
            if (texture != null) {
                entry.add("METAL", texture);
            }

            texture = loadTexture(assimpMat, aiTextureType_DIFFUSE_ROUGHNESS);
            if (texture != null) {
                entry.add("ROUGHNESS", texture);
            }

            texture = loadTexture(assimpMat, aiTextureType_AMBIENT_OCCLUSION);
            if (texture == null) {
                entry.add("AMBIENT_OCCLUSION", loadTexture(assimpMat, aiTextureType_LIGHTMAP));
            } else {
                entry.add("AMBIENT_OCCLUSION", loadTexture(assimpMat, aiTextureType_AMBIENT_OCCLUSION));
            }

            material = assetManager.createMaterial(entry);

            FloatBuffer value = stack.mallocFloat(1);
            IntBuffer count = stack.ints(1);

            if (aiGetMaterialFloatArray(assimpMat, "$mat.roughnessFactor", 0, 0, value, count) == aiReturn_SUCCESS) {
                material.roughness = value.get(0);
            }

            value.put(0, 0);
            count.put(0, 1);

            if (aiGetMaterialFloatArray(assimpMat, "$mat.metallicFactor", 0, 0, value, count) == aiReturn_SUCCESS) {
                material.metallic = value.get(0);
            }

            value.put(0, 0);
            count.put(0, 1);

        }

        return material;
    }

    private Integer loadTexture(AIMaterial assimpMat, int textureType) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            AIString path = AIString.calloc(stack);

            if (aiGetMaterialTextureCount(assimpMat, textureType) == 0) {
                return null;
            }

            if (aiGetMaterialTexture(
                    assimpMat,
                    textureType,
                    0,
                    path,
                    (IntBuffer) null,
                    null,
                    null,
                    null,
                    null,
                    null
            ) != aiReturn_SUCCESS) {
                return null;
            }

            String texturePath = path.dataString();

            // embedded textures
            if (texturePath.startsWith("*")) {
                int index = Integer.parseInt(texturePath.substring(1));
                PointerBuffer textures = scene.mTextures();

                AITexture embedded = AITexture.create(textures.get(index));

                return loadEmbbededTexture(embedded, index);
            }

            Path resolved = directory.resolve(texturePath).normalize();

            return assetManager.LoadTexture(resolved);
        }
    }

    private Integer loadEmbbededTexture(AITexture texture, int index) {
        String name = filepath + "#embbeded-" + index;

        int width = texture.mWidth();
        int height = texture.mHeight();

        // gltf files sometimes store compressed textures
        // this is indicated by a height of 0
        if (height == 0) {
            ByteBuffer compressed = texture.pcDataCompressed();

            ByteBuffer encoded = BufferUtils.createByteBuffer(width);
            ByteBuffer source = compressed.duplicate();

            source.limit(source.position() + width);
            encoded.put(source);
            encoded.flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {

                IntBuffer outWidth = stack.mallocInt(1);
                IntBuffer outHeight = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                ByteBuffer data = STBImage.stbi_load_from_memory(compressed, outWidth, outHeight, channels, 4);
                if (data == null) {
                    throw new RuntimeException(
                            "failed to load embedded texture " + STBImage.stbi_failure_reason()
                    );
                }

                try {
                    return assetManager.LoadTexture(data, outWidth.get(0), outHeight.get(0), GL_RGBA, name);
                } finally {
                    STBImage.stbi_image_free(data);
                }
            }
        }

        AITexel.Buffer texels = texture.pcData();
        ByteBuffer data = BufferUtils.createByteBuffer(width * height * 4);

        for (int i = 0; i < width * height; i++) {
            AITexel texel = texels.get(i);
            data.put((byte) texel.r());
            data.put((byte) texel.g());
            data.put((byte) texel.b());
            data.put((byte) texel.a());
        }

        data.flip();

        return assetManager.LoadTexture(data, width, height, GL_RGBA, name);
    }
}
