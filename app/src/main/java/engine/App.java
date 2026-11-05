package engine;

import engine.rendering.*;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.util.ArrayList;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

public class App {
    private long window;
    private AssetManager assetManager = AssetManager.get();
    private SceneGraph scene;
    private InputManager input;

    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGL3;

    private ShaderPipeline pipeline;

    private ArrayList<MonoScript> scripts = new ArrayList<>();

    private int initWidth;
    private int initHeight;

<<<<<<< HEAD
    private static boolean particlesEnabled = true;

=======
>>>>>>> origin/main
    public boolean init(int width, int height, CharSequence title) {
        initWidth = width;
        initHeight = height;
        GLFWErrorCallback.createPrint(System.err).set();

        glfwInit();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

        window = glfwCreateWindow(width, height, title, NULL, NULL);

        if (window == NULL) {
            System.err.println("unable to create window");
            return false;
        }

        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        ImGui.createContext();
        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGL3 = new ImGuiImplGl3();

        imGuiGlfw.init(window, true);
        imGuiGL3.init("#version 430 core");

        glClearColor(0.39f, 0.58f, 0.93f, 0.0f);
        glViewport(0, 0, width, height);

        assetManager._init();

        scene = new SceneGraph();
        scene.updateAspectRatio(width, height);

        input = new InputManager(window);
        pipeline = new ShaderPipeline(
                assetManager.getShader(assetManager.framebufferShader),
                width,
                height
        );

        glfwSetFramebufferSizeCallback(window, (_, new_width, new_height) -> {
            glViewport(0, 0, new_width, new_height);
            pipeline.onResize(new_width, new_height);
            scene.updateAspectRatio(new_width, new_height);
        });

        glEnable(GL_DEPTH_TEST);

        return true;
    }

    public void registerScript(MonoScript script) {
        script.Assets = assetManager;
        script.Scene = scene;
        script.Input = input;
        script.PostProcess = pipeline;
        scripts.add(script);
    }

    public void run() {
        glViewport(0, 0, initWidth, initHeight);
        scripts.stream().forEach(script -> script.Start());

        double currentTime = glfwGetTime();
        double previousTime = glfwGetTime();
        double deltaTime = 0.0;

        while (!glfwWindowShouldClose(window)) {
            currentTime = glfwGetTime();
            deltaTime = currentTime - previousTime;
            previousTime = currentTime;

            imGuiGlfw.newFrame();
            ImGui.newFrame();

            for (int i = 0; i < scripts.size(); i++) {
                scripts.get(i).update(deltaTime);
            }

            glEnable(GL_DEPTH_TEST);
            pipeline.bind();

            glClearColor(0.39f, 0.58f, 0.93f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            scene.draw();

<<<<<<< HEAD
            if (particlesEnabled) {
                scene.drawParticles();
            }

=======
>>>>>>> origin/main
            pipeline.Time += (float) deltaTime;

            pipeline.process();
            pipeline.render();

            ImGui.render();
            imGuiGL3.renderDrawData(ImGui.getDrawData());

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void toggleParticles () {
        App.particlesEnabled = App.particlesEnabled ? false : true;
    }

    public void close() {
        assetManager.cleanup();

        imGuiGL3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();

        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}
