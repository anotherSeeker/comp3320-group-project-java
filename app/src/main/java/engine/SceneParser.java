package engine;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import engine.components.Material;
import engine.components.MaterialEntry;
import engine.components.Transform;

public class SceneParser {
    private AssetManager assetManager = AssetManager.get();

    private PointerBuffer meshes;
    private PointerBuffer materials;

    private SceneGraph sceneGraph;
    private AIScene scene;

    private Path filepath;
    private Path directory;

    public void loadScene(Path filepath, AIScene scene, SceneGraph sceneGraph, Vector3f rootPosition) {
        this.meshes = scene.mMeshes();
        this.materials = scene.mMaterials();
        this.scene = scene;
        this.sceneGraph = sceneGraph;

        this.filepath = filepath.toAbsolutePath();
        this.directory = this.filepath.getParent();

        AINode root = scene.mRootNode();

        Matrix4f rootTransform = new Matrix4f().translate(rootPosition);

        processNode(root, rootTransform);
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
                entry.add("COLOUR", texture);
            }

            texture = loadTexture(assimpMat, aiTextureType_NORMALS);
            if (texture != null) {
                entry.add("NORMAL", texture);
            }

            texture = loadTexture(assimpMat, aiTextureType_LIGHTMAP);
            if (texture != null) {
                entry.add("AMBIENT_OCCLUSION", texture);
            }

            texture = loadTexture(assimpMat, aiTextureType_GLTF_METALLIC_ROUGHNESS);
            if (texture != null) {
                entry.add("METAL", loadTextureChannel(assimpMat, aiTextureType_GLTF_METALLIC_ROUGHNESS, 2, "metallic"));
                entry.add(
                        "ROUGHNESS",
                        loadTextureChannel(assimpMat, aiTextureType_GLTF_METALLIC_ROUGHNESS, 1, "roughness")
                );
            }

            texture = loadTexture(assimpMat, aiTextureType_DISPLACEMENT);
            if (texture != null) {
                entry.add("DISPLACEMENT", texture);
            }

            material = assetManager.createMaterial(entry);

            FloatBuffer value = stack.mallocFloat(1);
            IntBuffer count = stack.ints(1);

            value.put(0, 0);
            count.put(0, 1);

            if (aiGetMaterialFloatArray(assimpMat, "$mat.roughnessFactor", 0, 0, value, count) == aiReturn_SUCCESS) {
                material.roughness = value.get(0);
            }

            value.put(0, 0);
            count.put(0, 1);

            if (aiGetMaterialFloatArray(assimpMat, "$mat.metallicFactor", 0, 0, value, count) == aiReturn_SUCCESS) {
                material.metallic = value.get(0);

            }

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

                ByteBuffer data = STBImage.stbi_load_from_memory(encoded, outWidth, outHeight, channels, 4);
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

    private Integer loadTextureChannel(AIMaterial assimpMat, int textureType, int channel, String suffix) {

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

            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer data = decodeTexture(texturePath.toString(), width, height, channels);

            if (data == null) {
                throw new RuntimeException(
                        "could not decode texture: " + texturePath + " " + STBImage.stbi_failure_reason()
                );
            }

            try {
                int w = width.get(0);
                int h = height.get(0);

                ByteBuffer extracted = BufferUtils.createByteBuffer(w * h);

                for (int i = 0; i < w * h; i++) {

                    extracted.put(data.get(i * 4 + channel));
                }

                extracted.flip();

                String name = filepath + "#" + suffix + "-" + texturePath;

                return assetManager.LoadTexture(extracted, w, h, GL_RED, name);

            } finally {
                STBImage.stbi_image_free(data);
            }
        }

    }

    private ByteBuffer decodeTexture(String texturePath, IntBuffer width, IntBuffer height, IntBuffer channels) {

        if (texturePath.startsWith("*")) {
            int index = Integer.parseInt(texturePath.substring(1));
            PointerBuffer textures = scene.mTextures();

            AITexture embedded = AITexture.create(textures.get(index));

            // gltf files sometimes store compressed textures
            // this is indicated by a height of 0
            if (embedded.mHeight() == 0) {
                ByteBuffer compressed = embedded.pcDataCompressed().duplicate();
                compressed.position(0);
                compressed.limit(embedded.mWidth());

                return STBImage.stbi_load_from_memory(compressed, width, height, channels, 4);
            }

            int w = embedded.mWidth();
            int h = embedded.mHeight();

            AITexel.Buffer texels = embedded.pcData();
            ByteBuffer data = BufferUtils.createByteBuffer(w * h * 4);

            for (int i = 0; i < w * h; i++) {
                AITexel texel = texels.get(i);
                data.put((byte) texel.r());
                data.put((byte) texel.g());
                data.put((byte) texel.b());
                data.put((byte) texel.a());
            }

            data.flip();

            width.put(0, w);
            height.put(0, h);
            channels.put(0, 4);

            return data;
        }

        Path resolved = directory.resolve(texturePath).normalize();

        return STBImage.stbi_load(resolved.toString(), width, height, channels, 4);
    }
}
