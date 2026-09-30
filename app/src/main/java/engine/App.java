package engine;

import org.joml.Vector3f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class App {
    private long window;

    private ArrayList<Vertex> vertices = new ArrayList<>(List.of(
            new Vertex(new Vector3f(0.5f, -0.5f, 0)),
            new Vertex(new Vector3f(0, 0.5f, 0)),
            new Vertex(new Vector3f(-0.5f, -0.5f, 0))));

    private ArrayList<Integer> indices = new ArrayList<>(List.of(0, 1, 2));

    private Scene scene = new Scene(new Vector3f(0, 0, -5));

    // will remove later
    private String readFile(String filePath) {
        String result = "";

        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            result = reader.readAllLines().stream().collect(Collectors.joining("\n"));
            reader.close();
        } catch (IOException err) {
            System.out.println(err);
        }

        return result;
    }

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

        return true;
    }

    public void run() {

        Mesh mesh = new Mesh(vertices, indices);
        Shader shader = new Shader();

        System.out.println(System.getProperty("user.dir"));

        String vertexSource = readFile("assets/shaders/default.vert");
        String fragmentSource = readFile("assets/shaders/default.frag");

        shader.init(vertexSource, fragmentSource);

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            shader.use();
            mesh.draw();

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
