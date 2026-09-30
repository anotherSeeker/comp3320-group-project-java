package engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AssetManager {
    private Map<Integer, Shader> shaderIndex = new HashMap<>();

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

    public Shader getShader(Integer assetID) {
        return shaderIndex.get(assetID);
    }
}
