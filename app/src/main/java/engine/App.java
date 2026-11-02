package engine;

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

    private ArrayList<MonoScript> scripts = new ArrayList<>();

    private boolean scriptsStarted, guiReady;
    private int initWidth;
    private int initHeight;

    public boolean init(int width, int height, CharSequence title) {
        return init(width,height,title,true);
    }

    public boolean init(int width,int height,CharSequence title,boolean visible) {
        initWidth = width;
        initHeight = height;
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) return false;
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GraphicsPlatform.MAC ? 1 : 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, visible ? GLFW_TRUE : GLFW_FALSE);

        window = glfwCreateWindow(width, height, title, NULL, NULL);

        if (window == NULL) {
            System.err.println("unable to create window");
            return false;
        }

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

        glClearColor(0.39f, 0.58f, 0.93f, 0.0f);
        glViewport(0, 0, width, height);

        assetManager._init();

        scene = new SceneGraph();
        scene.updateAspectRatio(width, height);

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

    public void startScripts() {
        if (!scriptsStarted) { scripts.forEach(MonoScript::start); scriptsStarted=true; }
    }
    public void renderFrame(double deltaTime) {
        startScripts();
        try(var stack=org.lwjgl.system.MemoryStack.stackPush()) {
            var w=stack.mallocInt(1); var h=stack.mallocInt(1);
            glfwGetFramebufferSize(window,w,h);
            if(w.get(0)==0 || h.get(0)==0) return;
            glViewport(0,0,w.get(0),h.get(0)); scene.updateAspectRatio(w.get(0),h.get(0));
        }
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        imGuiGL3.newFrame(); imGuiGlfw.newFrame(); ImGui.newFrame();
        for(MonoScript script:scripts) script.update(deltaTime);
        scene.draw();
        for(MonoScript script:scripts) script.afterRender();
        ImGui.render(); imGuiGL3.renderDrawData(ImGui.getDrawData());
    }
    public void run() {
        startScripts(); double previous=glfwGetTime();
        while(!glfwWindowShouldClose(window)) {
            double now=glfwGetTime(); renderFrame(now-previous); previous=now;
            glfwSwapBuffers(window); glfwPollEvents();
        }
    }
    public void close() {
        if(window!=NULL) {
            for(MonoScript script:scripts) script.close();
            if(scene!=null) scene.cleanup();
            assetManager.cleanup();
            if(guiReady) { imGuiGL3.shutdown(); imGuiGlfw.shutdown(); ImGui.destroyContext(); }
            org.lwjgl.glfw.Callbacks.glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
        }
        glfwTerminate();
        GLFWErrorCallback callback=glfwSetErrorCallback(null); if(callback!=null) callback.free();
    }
}
