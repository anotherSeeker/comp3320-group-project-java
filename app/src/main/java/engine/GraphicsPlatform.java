package engine;

import java.util.Locale;

public final class GraphicsPlatform {
    public static final boolean MAC = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac");
    private GraphicsPlatform() { }

    // The vertex/fragment and tessellation shaders use 4.1-compatible operations.
    // This does not emulate compute shaders or any future OpenGL 4.3 features.
    public static String shaderSource(String source) {
        return MAC ? source.replaceFirst("#version[ \t]+430(?:[ \t]+core)?", "#version 410 core") : source;
    }
}
