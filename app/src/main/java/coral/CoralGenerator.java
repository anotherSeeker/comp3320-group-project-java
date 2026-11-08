package coral;

import java.util.Random;
import org.joml.Vector3f;

/** Port of Nirmit's ring/tube/curved recursive branch generator. */
public final class CoralGenerator {
    private static final float PI = (float)Math.PI, TWO_PI = 2 * PI;
    private CoralGenerator() { }

    private static Vector3f helper(Vector3f d) {
        return Math.abs(d.y) > .95f ? new Vector3f(1,0,0) : new Vector3f(0,1,0);
    }
    private static float range(Random rng, float lo, float hi) { return lo + (hi-lo)*rng.nextFloat(); }

    public static CoralMesh generate(CoralParams params) {
        params.validate();
        CoralMesh.Builder mesh = new CoralMesh.Builder();
        grow(mesh,new Vector3f(),new Vector3f(0,1,0),params.trunkLength,params.trunkRadius,0,params,new Random(params.seed));
        return mesh.build();
    }

    private static void grow(CoralMesh.Builder mesh, Vector3f start, Vector3f dir,
            float length, float radius, int depth, CoralParams p, Random rng) {
        if (depth > p.maxDepth || length < .12f || radius < .015f) return;
        Vector3f d = new Vector3f(dir).normalize();
        branch(mesh,start,d,length,radius,p.ringsPerBranch,p.segments,
                range(rng,-p.bend,p.bend),range(rng,-p.bend,p.bend),range(rng,0,p.waviness));
        if (depth == p.maxDepth) return;
        Vector3f end = new Vector3f(d).mul(length*.94f).add(start);
        int children = p.childrenMin + rng.nextInt(p.childrenMax-p.childrenMin+1);
        Vector3f right = helper(d).cross(d).normalize(), forward = new Vector3f(d).cross(right).normalize();
        for (int c=0;c<children;c++) {
            float angle = TWO_PI*c/children + range(rng,-.4f,.4f);
            Vector3f radial = new Vector3f(right).mul((float)Math.cos(angle)).fma((float)Math.sin(angle),forward);
            Vector3f child = new Vector3f(d).mul(1-p.branchSpread).fma(p.branchSpread,radial).add(0,p.upwardBias,0).normalize();
            float childLength = length*p.lengthScale*range(rng,p.lengthJitterMin,p.lengthJitterMax);
            float childRadius = radius*p.radiusScale*range(rng,.90f,1.06f);
            grow(mesh,end,child,childLength,childRadius,depth+1,p,rng);
        }
    }

    public static CoralMesh makeBranch(Vector3f start, Vector3f direction, float length,
            float radius, int rings, int segments, float bendRight, float bendForward, float waviness) {
        if (!start.isFinite() || !direction.isFinite() || direction.lengthSquared() < 1e-10f
                || !Float.isFinite(length) || !Float.isFinite(radius) || length <= 0 || radius <= 0
                || rings < 2 || rings > 64 || segments < 3 || segments > 64
                || !Float.isFinite(bendRight) || !Float.isFinite(bendForward) || !Float.isFinite(waviness))
            throw new IllegalArgumentException("Invalid branch dimensions or direction");
        CoralMesh.Builder mesh = new CoralMesh.Builder();
        branch(mesh,start,new Vector3f(direction).normalize(),length,radius,rings,segments,bendRight,bendForward,waviness);
        return mesh.build();
    }

    private static void branch(CoralMesh.Builder mesh, Vector3f start, Vector3f d,
            float length, float radius, int rings, int segments, float bendR, float bendF, float waviness) {
        int base = mesh.vertexCount(), stride = segments+1;
        Vector3f right = helper(d).cross(d).normalize(), forward = new Vector3f(d).cross(right).normalize();
        for (int r=0;r<rings;r++) {
            float v = (float)r/(rings-1), arc = (float)Math.sin(v*PI), wave = (float)Math.sin(v*TWO_PI);
            Vector3f centre = new Vector3f(d).mul(length*v).add(start)
                    .fma(bendR*arc+waviness*wave,right).fma(bendF*arc+waviness*.5f*wave,forward);
            Vector3f axis = new Vector3f(d).mul(length)
                    .fma(bendR*PI*(float)Math.cos(v*PI)+waviness*TWO_PI*(float)Math.cos(v*TWO_PI),right)
                    .fma(bendF*PI*(float)Math.cos(v*PI)+waviness*PI*(float)Math.cos(v*TWO_PI),forward).normalize();
            // Project the branch's reference axis rather than switching helper axes mid-curve.
            // This avoids sudden ring rotations as a curved tangent crosses the 0.95 threshold.
            Vector3f ringRight = new Vector3f(right).fma(-right.dot(axis),axis);
            if (ringRight.lengthSquared() < 1e-8f) ringRight = helper(axis).cross(axis);
            ringRight.normalize();
            Vector3f ringUp = new Vector3f(axis).cross(ringRight).normalize();
            for (int i=0;i<=segments;i++) {
                // Duplicate the first point at u=1 so normal maps interpolate across the seam correctly.
                float angle = i == segments ? 0 : TWO_PI*i/segments;
                float c = (float)Math.cos(angle), s = (float)Math.sin(angle);
                Vector3f n = new Vector3f(ringRight).mul(c).fma(s,ringUp).normalize();
                Vector3f tangent = new Vector3f(ringRight).mul(-s).fma(c,ringUp).normalize();
                Vector3f bitangent = new Vector3f(n).cross(tangent).normalize();
                Vector3f point = new Vector3f(n).mul(radius*(1-.76f*v)).add(centre);
                mesh.vertex(point,n,(float)i/segments,v,tangent,bitangent);
            }
        }
        for (int r=0;r<rings-1;r++) for (int i=0;i<segments;i++) {
            int a=base+r*stride+i, b=a+stride;
            mesh.triangle(a,a+1,b+1); mesh.triangle(a,b+1,b);
        }
    }

    public static CoralMesh makeStraightTube(float height, float radius, int rings, int segments) {
        return makeBranch(new Vector3f(),new Vector3f(0,1,0),height,radius,rings,segments,0,0,0);
    }

    public static CoralMesh makeYCoral() {
        CoralMesh.Builder b = new CoralMesh.Builder();
        b.append(makeBranch(new Vector3f(),new Vector3f(0,1,0),1.6f,.22f,9,8,.08f,.03f,.015f));
        b.append(makeBranch(new Vector3f(0,1.35f,0),new Vector3f(-.55f,.85f,.15f),1,.13f,7,8,.12f,.05f,.02f));
        b.append(makeBranch(new Vector3f(0,1.35f,0),new Vector3f(.55f,.85f,-.10f),1.1f,.13f,7,8,-.10f,-.04f,.02f));
        return b.build();
    }

    public static CoralMesh makeCube(float size) {
        if (!Float.isFinite(size) || size <= 0) throw new IllegalArgumentException("Invalid cube size");
        float h=size/2;
        float[][] corners={ {-h,-h,h,h,-h,h,h,h,h,-h,h,h}, {h,-h,-h,-h,-h,-h,-h,h,-h,h,h,-h},
                {h,-h,h,h,-h,-h,h,h,-h,h,h,h}, {-h,-h,-h,-h,-h,h,-h,h,h,-h,h,-h},
                {-h,h,h,h,h,h,h,h,-h,-h,h,-h}, {-h,-h,-h,h,-h,-h,h,-h,h,-h,-h,h} };
        Vector3f[] normals={new Vector3f(0,0,1),new Vector3f(0,0,-1),new Vector3f(1,0,0),
                new Vector3f(-1,0,0),new Vector3f(0,1,0),new Vector3f(0,-1,0)};
        CoralMesh.Builder b=new CoralMesh.Builder();
        for(int f=0;f<6;f++) {
            float[] p=corners[f]; int base=b.vertexCount();
            Vector3f t=new Vector3f(p[3]-p[0],p[4]-p[1],p[5]-p[2]).normalize();
            Vector3f bit=new Vector3f(normals[f]).cross(t).normalize();
            for(int i=0;i<4;i++) b.vertex(new Vector3f(p[i*3],p[i*3+1],p[i*3+2]),normals[f],
                    i==1||i==2?1:0,i>=2?1:0,t,bit);
            b.triangle(base,base+1,base+2); b.triangle(base,base+2,base+3);
        }
        return b.build();
    }
}
