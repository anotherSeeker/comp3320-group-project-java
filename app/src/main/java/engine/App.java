package engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.file.Path;
import java.util.ArrayList;

public class App {
    private long window;
    private AssetManager assetManager = new AssetManager();
    private Scene scene = new Scene(new Vector3f(0, 0, -3));

    private ArrayList<MonoScript> scripts = new ArrayList<>();

    public boolean init(int width, int height, CharSequence title) {
        GLFWErrorCallback.createPrint(System.err).set();

        glfwInit();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(width, height, title, NULL, NULL);

        if (window == NULL) {
            System.err.println("unable to create window");
            return false;
        }

        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        glClearColor(0.39f, 0.58f, 0.93f, 0.0f);
        glViewport(0, 0, width, height);
        scene.updateAspectRatio(width, height);

        return true;
    }

    public void registerScript(MonoScript script) {
        scripts.add(script);
    }

    public void run() {

        Integer shaderID = assetManager.loadShader(
                Path.of("assets/shaders/default.vert"),
                Path.of("assets/shaders/default.frag"));

        Integer MeshID = assetManager.loadMesh(Path.of("assets/meshes/uvsphere.obj"));

        Mesh mesh = assetManager.getMesh(MeshID);
        Shader shader = assetManager.getShader(shaderID);

        System.out.println(System.getProperty("user.dir"));

        Matrix4f transformA = new Matrix4f().translate(new Vector3f(-0.75f, 0, 0));
        Matrix4f transformB = new Matrix4f().translate(new Vector3f(0.75f, 0, 0));

        scene.createObject(mesh, shader, transformA);
        scene.createObject(mesh, shader, transformB);

        scripts.stream().forEach(script -> script.Start());

        double currentTime = glfwGetTime();
        double previousTime = glfwGetTime();
        double deltaTime = 0.0;

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            currentTime = glfwGetTime();
            deltaTime = currentTime - previousTime;
            previousTime = currentTime;

            for (int i = 0; i < scripts.size(); i++) {
                scripts.get(i).update(deltaTime);
            }

            scene.draw();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        shader.delete();
        mesh.delete();
    }

    public void close() {
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}
