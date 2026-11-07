package engine;

import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;

public class GLErrors {
    public static int printIfError(String msg)
    {
        int err = glGetError();
        if (err != 0)
            System.out.println("|Error|\n\t" + msg + "\n\tError code: "+err);

        return err;
    }

    public static int programlogIfError(int ID)
    {
        String out = glGetProgramInfoLog(ID,1024);
        int size = out.length();
        if (size > 0)
        {
            System.out.println(out);
        }
         return size;
    }

    public static int shaderlogIfError(int ID)
    {
        String out = glGetShaderInfoLog(ID,1024);
        int size = out.length();
        if (size > 0)
        {
            System.out.println(out);
        }
        return size;
    }
}
