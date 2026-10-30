package engine;

import engine.components.Material;
import engine.components.MaterialEntry;
import engine.rendering.CubeTexture;
import engine.rendering.Mesh;
import engine.rendering.Shader;
import engine.rendering.Texture;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memFree;

import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.assimp.*;
import static org.lwjgl.assimp.Assimp.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AssetManager {
    private Map<Integer, Shader> shaderIndex = new HashMap<>();
    private Map<Integer, Mesh> meshIndex = new HashMap<>();
    private Map<Integer, Texture> textureIndex = new HashMap<>();
    private Map<Integer, CubeTexture> cubeTextureIndex = new HashMap<>();

    public Integer defaultTexture = "DEFAULT".hashCode();
    public Integer whiteTexture = "WHITE".hashCode();
    public Integer blackTexture = "BLACK".hashCode();
    public Integer normalTexture = "NORMAL".hashCode();

    public Integer cubemapShader = "CUBEMAP".hashCode();
    public Integer skyboxShader = "SKYBOX".hashCode();
    public Integer irradianceShader = "IRRADIANCE".hashCode();
    public Integer prefilterShader = "PREFILTER".hashCode();
    public Integer brdfIntShader = "BRDF_INT".hashCode();

    public Integer framebufferShader = "FRAMEBUFFER".hashCode();
    public Integer blurPostProcess = "BLUR".hashCode();
    public Integer bloomPostProcessA = "BLOOM_A".hashCode();
    public Integer bloomPostProcessB = "BLOOM_B".hashCode();
    public Integer vignettePostProcess = "VIGNETTE".hashCode();
    public Integer colourGradeProcess = "COLOUR_GRADE".hashCode();

    public Integer defaultShader = "DEFAULT".hashCode();

    private AssetManager() {

    }

    private static AssetManager singleton = new AssetManager();

    public static AssetManager get() {
        return singleton;
    }

    public void _init() {
        STBImage.stbi_set_flip_vertically_on_load(true);

        int width = 64;
        int height = 64;

        int checkerSize = 8;

        ByteBuffer data = BufferUtils.createByteBuffer(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isMagenta = ((x / checkerSize) + (y / checkerSize)) % 2 == 0;

                if (isMagenta) {
                    data.put((byte) 0xFF);
                    data.put((byte) 0x00);
                    data.put((byte) 0xFF);
                } else {
                    data.put((byte) 0x00);
                    data.put((byte) 0x00);
                    data.put((byte) 0x00);

                }
                data.put((byte) 0xFF);
            }
        }

        data.flip();

        ByteBuffer whiteData = BufferUtils.createByteBuffer(width * height);

        for (int i = 0; i < width * height; i++) {
            whiteData.put((byte) 0xFF);
        }

        ByteBuffer blackData = BufferUtils.createByteBuffer(width * height);

        for (int i = 0; i < width * height; i++) {
            blackData.put((byte) 0x00);
        }

        ByteBuffer normalData = BufferUtils.createByteBuffer(width * height * 3);

        for (int i = 0; i < width * height; i++) {
            normalData.put((byte) 0x80);
            normalData.put((byte) 0x80);
            normalData.put((byte) 0xFF);
        }

        this.loadInternalShader(
                cubemapShader,
                Path.of("assets/shaders/internal/cubemap.vert"),
                Path.of("assets/shaders/internal/cubemap.frag")
        );

        this.loadInternalShader(
                skyboxShader,
                Path.of("assets/shaders/internal/skybox.vert"),
                Path.of("assets/shaders/internal/skybox.frag")
        );

        this.loadInternalShader(
                irradianceShader,
                Path.of("assets/shaders/internal/cubemap.vert"),
                Path.of("assets/shaders/internal/irradiance.frag")
        );

        this.loadInternalShader(
                prefilterShader,
                Path.of("assets/shaders/internal/cubemap.vert"),
                Path.of("assets/shaders/internal/prefilter.frag")
        );

        this.loadInternalShader(
                brdfIntShader,
                Path.of("assets/shaders/internal/brdfInt.vert"),
                Path.of("assets/shaders/internal/brdfInt.frag")
        );

        this.loadInternalShader(
                framebufferShader,
                Path.of("assets/shaders/internal/framebuffer.vert"),
                Path.of("assets/shaders/internal/framebuffer.frag")
        );

        this.loadInternalShader(
                defaultShader,
                Path.of("assets/shaders/default.vert"),
                Path.of("assets/shaders/default.frag")
        );

        this.loadInternalComputeShader(blurPostProcess, Path.of("assets/shaders/internal/blur.glsl"));
        this.loadInternalComputeShader(bloomPostProcessA, Path.of("assets/shaders/internal/bloomA.glsl"));
        this.loadInternalComputeShader(bloomPostProcessB, Path.of("assets/shaders/internal/bloomB.glsl"));
        this.loadInternalComputeShader(vignettePostProcess, Path.of("assets/shaders/internal/vignette.glsl"));
        this.loadInternalComputeShader(colourGradeProcess, Path.of("assets/shaders/internal/colour_grade.glsl"));

        textureIndex.put(defaultTexture, new Texture(data, width, height, GL_RGBA));
        textureIndex.put(whiteTexture, new Texture(whiteData, width, height, GL_R));
        textureIndex.put(blackTexture, new Texture(blackData, width, height, GL_R));
        textureIndex.put(normalTexture, new Texture(normalData, width, height, GL_RGB));
    }

    private String readFile(Path filePath) {
        String result = "";

        try {
            BufferedReader reader = Files.newBufferedReader(filePath);
            result = reader.readAllLines().stream().collect(Collectors.joining("\n"));
            reader.close();
        } catch (IOException err) {
            System.out.println(err);
        }

        return result;
    }

    private void loadInternalShader(Integer ID, Path vertexPath, Path fragmentPath) {
        String vertexSource = readFile(vertexPath);
        String fragmentSource = readFile(fragmentPath);

        Shader shader = new Shader();
        shader.init(vertexSource, fragmentSource);

        shaderIndex.put(ID, shader);
    }

    private void loadInternalComputeShader(Integer ID, Path computePath) {
        String computeSource = readFile(computePath);

        Shader shader = new Shader();
        shader.initCompute(computeSource);

        shaderIndex.put(ID, shader);
    }

    public Integer LoadShader(Path vertexPath, Path fragmentPath) {
        Integer ID = (vertexPath.toAbsolutePath().toString() + fragmentPath.toAbsolutePath().toString()).hashCode();

        if (shaderIndex.containsKey(ID))
            return ID;

        String vertexSource = readFile(vertexPath);
        String fragmentSource = readFile(fragmentPath);

        Shader shader = new Shader();
        shader.init(vertexSource, fragmentSource);

        shaderIndex.put(ID, shader);

        return ID;
    }

    public Integer LoadMesh(Path filePath) {
        Integer ID = filePath.toAbsolutePath().hashCode();

        if (meshIndex.containsKey(ID))
            return ID;

        AIScene scene = aiImportFile(
                filePath.toString(),
                aiProcess_Triangulate |
                        aiProcess_GenSmoothNormals |
                        aiProcess_JoinIdenticalVertices |
                        aiProcess_CalcTangentSpace
        );

        if (scene == null || scene.mRootNode() == null) {
            throw new RuntimeException("could not load model " + aiGetErrorString());
        }

        PointerBuffer meshbuffer = scene.mMeshes();
        AIMesh assimpMesh = AIMesh.create(meshbuffer.get(0));

        Mesh mesh = createMesh(assimpMesh);

        aiReleaseImport(scene);

        meshIndex.put(ID, mesh);

        return ID;
    }

    public Mesh createMesh(AIMesh assimpMesh) {
        int vertexCount = assimpMesh.mNumVertices();
        int indexCount = assimpMesh.mNumFaces() * 3;

        AIVector3D.Buffer positions = assimpMesh.mVertices();
        AIVector3D.Buffer normals = assimpMesh.mNormals();
        AIVector3D.Buffer uvs = assimpMesh.mTextureCoords(0);
        AIVector3D.Buffer tangents = assimpMesh.mTangents();
        AIVector3D.Buffer bitangents = assimpMesh.mBitangents();

        int vertexSize = 14;

        FloatBuffer vertices = memAllocFloat(vertexCount * vertexSize);
        IntBuffer indices = memAllocInt(indexCount);

        for (int i = 0; i < vertexCount; i++) {
            AIVector3D position = positions.get(i);

            vertices.put(position.x());
            vertices.put(position.y());
            vertices.put(position.z());

            if (normals != null) {
                AIVector3D normal = normals.get(i);
                vertices.put(normal.x());
                vertices.put(normal.y());
                vertices.put(normal.z());
            } else {
                vertices.put(0);
                vertices.put(0);
                vertices.put(0);
            }

            if (uvs != null) {
                AIVector3D uv = uvs.get(i);
                vertices.put(uv.x());
                vertices.put(uv.y());
            } else {
                vertices.put(0);
                vertices.put(0);
            }

            if (tangents != null) {
                AIVector3D tangent = tangents.get(i);
                vertices.put(tangent.x());
                vertices.put(tangent.y());
                vertices.put(tangent.z());
            } else {
                vertices.put(0);
                vertices.put(0);
                vertices.put(0);
            }

            if (bitangents != null) {
                AIVector3D bitangent = bitangents.get(i);
                vertices.put(bitangent.x());
                vertices.put(bitangent.y());
                vertices.put(bitangent.z());
            } else {
                vertices.put(0);
                vertices.put(0);
                vertices.put(0);
            }
        }

        AIFace.Buffer faces = assimpMesh.mFaces();

        for (int i = 0; i < assimpMesh.mNumFaces(); i++) {
            AIFace face = faces.get(i);

            if (face.mNumIndices() != 3) {
                throw new RuntimeException("Mesh wasn't triangulated");
            }

            IntBuffer faceIndices = face.mIndices();
            indices.put(faceIndices.get(0));
            indices.put(faceIndices.get(1));
            indices.put(faceIndices.get(2));
        }

        vertices.flip();
        indices.flip();

        Mesh mesh = new Mesh(vertices, indices);

        memFree(vertices);
        memFree(indices);

        return mesh;
    }

    public Integer LoadMesh(Mesh mesh, String name) {
        Integer ID = name.hashCode();

        if (meshIndex.containsKey(ID))
            return ID;

        meshIndex.put(ID, mesh);

        return ID;
    }

    public Integer LoadTexture(Path filePath) {
        Integer ID = filePath.toAbsolutePath().hashCode();

        if (textureIndex.containsKey(ID))
            return ID;

        try (MemoryStack stack = MemoryStack.stackPush()) {

            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer data = STBImage.stbi_load(filePath.toString(), width, height, channels, 0);
            if (data == null) {
                throw new RuntimeException(
                        "could not load texture " + filePath.toString() + " " + STBImage.stbi_failure_reason()
                );
            }

            int channelFormat = GL_RGBA;

            if (channels.get(0) == 3) {
                channelFormat = GL_RGB;
            } else if (channels.get(0) == 1) {
                channelFormat = GL_RED;
            }

            Texture texture = new Texture(data, width.get(0), height.get(0), channelFormat);

            textureIndex.put(ID, texture);

            STBImage.stbi_image_free(data);

        }

        return ID;
    }

    public Integer LoadTexture(ByteBuffer data, int width, int height, int channelFormat, String name) {

        Integer ID = name.hashCode();

        if (textureIndex.containsKey(ID))
            return ID;

        Texture texture = new Texture(data, width, height, channelFormat);

        textureIndex.put(ID, texture);

        return ID;
    }

    // takes in a .HDR equirectangular texture and converts it to a cube map
    public Integer LoadCubeMap(Path filePath) {
        Integer ID = filePath.toAbsolutePath().hashCode();

        if (cubeTextureIndex.containsKey(ID))
            return ID;

        try (MemoryStack stack = MemoryStack.stackPush()) {

            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            FloatBuffer data = STBImage.stbi_loadf(filePath.toString(), width, height, channels, 0);
            if (data == null) {
                throw new RuntimeException(
                        "could not load cubemap " + filePath.toString() + " " + STBImage.stbi_failure_reason()
                );
            }

            cubeTextureIndex.put(ID, new CubeTexture(data, width.get(0), height.get(0)));

            STBImage.stbi_image_free(data);
        }

        return ID;
    }

    public void LoadIntoScene(Path filePath, SceneGraph sceneGraph, Vector3f rootPosition) {
        AIScene scene = aiImportFile(
                filePath.toString(), aiProcess_Triangulate |
                        aiProcess_GenSmoothNormals |
                        aiProcess_JoinIdenticalVertices |
                        aiProcess_CalcTangentSpace
        );

        if (scene == null || scene.mRootNode() == null) {
            System.out.printf("failed to load file %s:\n%s\n", filePath, aiGetErrorString());
            return;
        }

        SceneParser loader = new SceneParser();

        loader.loadScene(filePath, scene, sceneGraph, rootPosition);

        System.out.printf("loaded file %s\n", filePath);

        aiReleaseImport(scene);

    }

    public Shader getShader(Integer assetID) {
        return shaderIndex.get(assetID);
    }

    public Mesh getMesh(Integer assetID) {
        return meshIndex.get(assetID);
    }

    public Texture getTexture(Integer assetID) {
        return textureIndex.get(assetID);
    }

    public CubeTexture getCubeTexture(Integer assetID) {
        return cubeTextureIndex.get(assetID);
    }

    public Material createMaterial(MaterialEntry materialEntry) {

        Material material = new Material();
        material.shader = getShader(materialEntry.shaderID != null ? materialEntry.shaderID : defaultShader);

        // @formatter:off
        material.albedoMap              = getTexture(materialEntry.albedoID           != null ? materialEntry.albedoID : defaultTexture);
        material.roughnessMap           = getTexture(materialEntry.roughnessID        != null ? materialEntry.roughnessID : blackTexture);
        material.ambientOcclusionMap    = getTexture(materialEntry.ambientOcclusionID != null ? materialEntry.ambientOcclusionID : whiteTexture);
        material.normalMap              = getTexture(materialEntry.normalID           != null ? materialEntry.normalID : normalTexture);
        material.metallicMap            = getTexture(materialEntry.metallicID           != null ? materialEntry.metallicID : blackTexture);
        // @formatter:on

        return material;
    }

    public void cleanup() {
        for (Mesh mesh : meshIndex.values()) {
            mesh.delete();
        }

        for (Shader shader : shaderIndex.values()) {
            shader.delete();
        }

        for (Texture texture : textureIndex.values()) {
            texture.delete();
        }

        for (CubeTexture cubeTexture : cubeTextureIndex.values()) {
            cubeTexture.delete();
        }
    }
}
