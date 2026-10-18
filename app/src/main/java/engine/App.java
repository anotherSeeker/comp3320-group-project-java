package engine;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.util.ArrayList;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

import static imgui.gl3.ImGuiImplGl3.*;
import static imgui.glfw.ImGuiImplGlfw.*;

public class App {
    private long window;
    private AssetManager assetManager = AssetManager.get();
    private SceneGraph scene;
    private InputManager input;

    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGL3;

    private ArrayList<MonoScript> scripts = new ArrayList<>();

    public boolean init(int width, int height, CharSequence title) {
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

        glfwSetFramebufferSizeCallback(window, (_, new_width, new_height) -> {
            glViewport(0, 0, new_width, new_height);

            scene.updateAspectRatio(new_width, new_height);
        });

        glEnable(GL_DEPTH_TEST);

        return true;
    }

    public void registerScript(MonoScript script) {
        script.Assets = assetManager;
        script.Scene = scene;
        script.Input = input;
        scripts.add(script);
    }

    public void run() {
        scripts.stream().forEach(script -> script.Start());

        double currentTime = glfwGetTime();
        double previousTime = glfwGetTime();
        double deltaTime = 0.0;

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            currentTime = glfwGetTime();
            deltaTime = currentTime - previousTime;
            previousTime = currentTime;

            imGuiGlfw.newFrame();
            ImGui.newFrame();

            for (int i = 0; i < scripts.size(); i++) {
                scripts.get(i).update(deltaTime);
            }

            ImGui.begin("hi!!");

            ImGui.end();

            scene.draw();

            ImGui.render();
            imGuiGL3.renderDrawData(ImGui.getDrawData());

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
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
