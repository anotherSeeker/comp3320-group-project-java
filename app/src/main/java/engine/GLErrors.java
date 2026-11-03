package engine;

import static org.lwjgl.opengl.GL11.glGetError;

public class GLErrors {
    public static int printIfError(String msg)
    {
        int err = glGetError();
        if (err != 0)
            System.out.println("|Error|\n\t" + msg + "\n\tError code: "+err);

        return err;
    }
}
