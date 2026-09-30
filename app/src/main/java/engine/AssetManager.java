package engine;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    private AssetManager() {
    }

    private static AssetManager singleton = new AssetManager();

    public static AssetManager get() {
        return singleton;
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

    public Integer loadShader(Path vertexPath, Path fragmentPath) {
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

    public Integer loadMesh(Path filePath) {
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

    public Shader getShader(Integer assetID) {
        return shaderIndex.get(assetID);
    }

    public Mesh getMesh(Integer assetID) {
        return meshIndex.get(assetID);
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
