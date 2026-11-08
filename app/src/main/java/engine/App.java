package engine;

import engine.rendering.ShaderPipeline;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import java.util.ArrayList;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class App {
    private long window;
    private final AssetManager assetManager = AssetManager.get();
    private SceneGraph scene;
    private InputManager input;
    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGL3;
    private ShaderPipeline pipeline;
    private final ArrayList<MonoScript> scripts = new ArrayList<>();
    private boolean scriptsStarted, guiReady;
    private int framebufferWidth, framebufferHeight;

    private static boolean particlesEnabled = true;

    public boolean init(int width, int height, CharSequence title) {
        return init(width, height, title, true);
    }

    public boolean init(int width, int height, CharSequence title, boolean visible) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) return false;
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GraphicsPlatform.MAC ? 1 : 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, visible ? GLFW_TRUE : GLFW_FALSE);
        window = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window == NULL) return false;
        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        ImGui.createContext();
        ImGui.getIO().setIniFilename(null);
        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGL3 = new ImGuiImplGl3();
        input = new InputManager(window);
        imGuiGlfw.init(window, true);
        imGuiGL3.init(GraphicsPlatform.MAC ? "#version 410 core" : "#version 430 core");
        guiReady = true;
        glfwSwapInterval(1);
        glClearColor(.035f, .13f, .17f, 1);
        assetManager._init();
        scene = new SceneGraph();
        framebufferWidth = width;
        framebufferHeight = height;
        scene.updateAspectRatio(width, height);
        pipeline = new ShaderPipeline(assetManager.getShader(assetManager.framebufferShader), width, height);
        glfwSetFramebufferSizeCallback(window, (_, w, h) -> resize(w, h));
        glEnable(GL_DEPTH_TEST);
        return true;
    }

    private void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        if (width != framebufferWidth || height != framebufferHeight) {
            pipeline.onResize(width, height);
            framebufferWidth = width;
            framebufferHeight = height;
            scene.updateAspectRatio(width, height);
        }
        glViewport(0, 0, width, height);
    }

    public void registerScript(MonoScript script) {
        script.Assets = assetManager;
        script.Scene = scene;
        script.Input = input;
        script.PostProcess = pipeline;
        scripts.add(script);
    }

    public void startScripts() {
        if (!scriptsStarted) {
            scripts.forEach(MonoScript::start);
            scriptsStarted = true;
        }
    }

    public void renderFrame(double deltaTime) {
        startScripts();
        try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
            var w = stack.mallocInt(1);
            var h = stack.mallocInt(1);
            glfwGetFramebufferSize(window, w, h);
            if (w.get(0) == 0 || h.get(0) == 0) return;
            resize(w.get(0), h.get(0));
        }
        imGuiGL3.newFrame();
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        for (MonoScript script : scripts) script.update(deltaTime);

        glEnable(GL_DEPTH_TEST);
        scene.generateShadows();
        pipeline.bind();
        glClearColor(.035f, .13f, .17f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        scene.draw();
        // Generated coral meshes share the environment's depth buffer and HDR
        // target; draw them before the team's postprocessing and GUI stages.
        for (MonoScript script : scripts) script.afterRender();
        if (particlesEnabled) scene.drawParticles();
        pipeline.Time += (float) deltaTime;
        pipeline.process();
        pipeline.render();
        ImGui.render();
        imGuiGL3.renderDrawData(ImGui.getDrawData());
    }

    public void run() {
        startScripts();
        double previous = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            renderFrame(now - previous);
            previous = now;
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void toggleParticles () {
        App.particlesEnabled = App.particlesEnabled ? false : true;
    }

    public void close() {
        if (window != NULL) {
            for (MonoScript script : scripts) script.close();
            if (scene != null) scene.cleanup();
            if (pipeline != null) pipeline.close();
            assetManager.cleanup();
            if (guiReady) {
                imGuiGL3.shutdown();
                imGuiGlfw.shutdown();
                ImGui.destroyContext();
            }
            org.lwjgl.glfw.Callbacks.glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        }
        glfwTerminate();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) callback.free();
    }
}
