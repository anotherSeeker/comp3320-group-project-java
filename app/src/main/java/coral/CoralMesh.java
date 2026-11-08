package coral;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import org.joml.Vector3f;

/** CPU mesh in the engine's position/normal/UV/tangent/bitangent layout. */
public final class CoralMesh {
    public static final int STRIDE = 14;
    private final float[] vertices;
    private final int[] indices;

    CoralMesh(float[] vertices, int[] indices) {
        this.vertices = vertices; this.indices = indices;
        validate();
    }

    public int vertexCount() { return vertices.length / STRIDE; }
    public int triangleCount() { return indices.length / 3; }
    public float[] vertices() { return vertices.clone(); }
    public int[] indices() { return indices.clone(); }

    public void validate() {
        if (vertices.length == 0 || vertices.length % STRIDE != 0 || indices.length == 0 || indices.length % 3 != 0)
            throw new IllegalArgumentException("Expected a nonempty indexed triangle mesh");
        for (float value : vertices)
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Nonfinite vertex attribute");
        for (int index : indices)
            if (index < 0 || index >= vertexCount()) throw new IllegalArgumentException("Index outside vertex array");
    }

    public CoralMesh transformed(Vector3f position, float rotation, float scale) {
        if (!position.isFinite() || !Float.isFinite(rotation) || !Float.isFinite(scale) || scale <= 0)
            throw new IllegalArgumentException("Placement requires finite values and a positive scale");
        float[] result = vertices.clone();
        for (int i = 0; i < result.length; i += STRIDE) {
            Vector3f p = new Vector3f(vertices[i], vertices[i+1], vertices[i+2]);
            p.mul(scale).rotateY(rotation).add(position);
            result[i] = p.x; result[i+1] = p.y; result[i+2] = p.z;
            // Uniform positive scale: inverse-transpose normals normalize to the rotated frame.
            for (int offset : new int[]{3, 8, 11}) {
                Vector3f v = new Vector3f(vertices[i+offset], vertices[i+offset+1], vertices[i+offset+2]);
                v.rotateY(rotation).normalize();
                result[i+offset] = v.x; result[i+offset+1] = v.y; result[i+offset+2] = v.z;
            }
        }
        return new CoralMesh(result, indices.clone());
    }

    public void writeObj(Path path) throws IOException {
        if (path.toAbsolutePath().getParent() != null) Files.createDirectories(path.toAbsolutePath().getParent());
        try (var out = Files.newBufferedWriter(path)) {
            out.write("# Java procedural coral: " + vertexCount() + " vertices, " + triangleCount() + " triangles\n");
            for (int i = 0; i < vertices.length; i += STRIDE)
                out.write(String.format(Locale.ROOT,"v %.9g %.9g %.9g%n",vertices[i],vertices[i+1],vertices[i+2]));
            for (int i = 0; i < vertices.length; i += STRIDE)
                out.write(String.format(Locale.ROOT,"vt %.9g %.9g%n",vertices[i+6],vertices[i+7]));
            for (int i = 0; i < vertices.length; i += STRIDE)
                out.write(String.format(Locale.ROOT,"vn %.9g %.9g %.9g%n",vertices[i+3],vertices[i+4],vertices[i+5]));
            for (int i = 0; i < indices.length; i += 3) {
                int a=indices[i]+1, b=indices[i+1]+1, c=indices[i+2]+1;
                out.write("f "+a+"/"+a+"/"+a+" "+b+"/"+b+"/"+b+" "+c+"/"+c+"/"+c+"\n");
            }
        }
    }

    static final class Builder {
        private float[] vertices = new float[STRIDE * 256];
        private int[] indices = new int[1024];
        private int vertexSize, indexSize;
        int vertexCount() { return vertexSize / STRIDE; }

        void vertex(Vector3f p, Vector3f n, float u, float v, Vector3f t, Vector3f b) {
            if (vertexCount() >= 1_000_000) throw new IllegalArgumentException("Mesh vertex budget exceeded");
            if (vertexSize + STRIDE > vertices.length) vertices = Arrays.copyOf(vertices, vertices.length * 2);
            for (float f : new float[]{p.x,p.y,p.z,n.x,n.y,n.z,u,v,t.x,t.y,t.z,b.x,b.y,b.z}) vertices[vertexSize++] = f;
        }
        void triangle(int a, int b, int c) {
            if (indexSize + 3 > indices.length) indices = Arrays.copyOf(indices, indices.length * 2);
            indices[indexSize++]=a; indices[indexSize++]=b; indices[indexSize++]=c;
        }
        void append(CoralMesh mesh) {
            int offset = vertexCount();
            int size = vertexSize + mesh.vertices.length;
            if (size / STRIDE > 1_000_000) throw new IllegalArgumentException("Mesh vertex budget exceeded");
            if (size > vertices.length) vertices = Arrays.copyOf(vertices, Math.max(size,vertices.length*2));
            System.arraycopy(mesh.vertices,0,vertices,vertexSize,mesh.vertices.length); vertexSize=size;
            size = indexSize + mesh.indices.length;
            if (size > indices.length) indices=Arrays.copyOf(indices,Math.max(size,indices.length*2));
            for (int i : mesh.indices) indices[indexSize++]=i+offset;
        }
        CoralMesh build() { return new CoralMesh(Arrays.copyOf(vertices,vertexSize),Arrays.copyOf(indices,indexSize)); }
    }
}
