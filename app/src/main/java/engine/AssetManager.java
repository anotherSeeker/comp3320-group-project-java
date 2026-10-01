package engine;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;
import engine.components.Material;

import static org.lwjgl.opengl.GL11.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class AssetManager {
    private Map<Integer, Shader> shaderIndex = new HashMap<>();
    private Map<Integer, Mesh> meshIndex = new HashMap<>();
    private Map<Integer, Texture> textureIndex = new HashMap<>();

    public Integer defaultTexture = "DEFAULT".hashCode();

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

        for (int i = 0; i < width * height; i++) {

        }

        data.flip();

        textureIndex.put(defaultTexture, new Texture(data, width, height, GL_RGBA));
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

        try {
            InputStream inputStream = new FileInputStream(filePath.toString());

            Obj objSource = ObjReader.read(inputStream);
            Obj renderable = ObjUtils.convertToRenderable(objSource);

            IntBuffer indexBuffer = ObjData.getFaceVertexIndices(renderable, 3);
            FloatBuffer positionBuffer = ObjData.getVertices(renderable);
            FloatBuffer normalBuffer = ObjData.getNormals(renderable);
            FloatBuffer uvBuffer = ObjData.getTexCoords(renderable, 2);

            Mesh newMesh = new Mesh(indexBuffer, positionBuffer, normalBuffer, uvBuffer);

            meshIndex.put(ID, newMesh);

        } catch (IOException err) {
            System.out.println(err);
        }

        return ID;
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

            int channelFormat = (channels.get(0) == 4) ? GL_RGBA : GL_RGB;

            Texture texture = new Texture(data, width.get(0), height.get(0), channelFormat);

            textureIndex.put(ID, texture);

            STBImage.stbi_image_free(data);

        }

        return ID;
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

    public Material createMaterial(Integer shaderID, Integer albedo) {
        Material material = new Material();
        material.shader = getShader(shaderID);
        material.albedo = getTexture(albedo);

        return material;
    }

    public void cleanup() {
        for (Mesh mesh : meshIndex.values()) {
            mesh.delete();
        }

        for (Shader shader : shaderIndex.values()) {
            shader.delete();
        }
    }
}
