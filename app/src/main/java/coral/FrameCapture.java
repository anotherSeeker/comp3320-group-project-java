package coral;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public final class FrameCapture {
    private FrameCapture() { }
    public static BufferedImage read() {
        int[] viewport=new int[4]; glGetIntegerv(GL_VIEWPORT,viewport);
        int width=viewport[2], height=viewport[3];
        ByteBuffer pixels=memAlloc(width*height*4);
        try {
            glFinish();
            glReadPixels(0,0,width,height,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
            BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
                int i=(y*width+x)*4;
                int color=0xff000000 | (pixels.get(i)&255)<<16 | (pixels.get(i+1)&255)<<8 | pixels.get(i+2)&255;
                image.setRGB(x,height-1-y,color);
            }
            return image;
        } finally { memFree(pixels); }
    }
    public static void write(BufferedImage image, Path path) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        if (!ImageIO.write(image,"png",path.toFile())) throw new IOException("PNG writer unavailable");
    }
}
