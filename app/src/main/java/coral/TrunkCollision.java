package coral;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

/** Solid horizontal footprint of the primary trunk only. Contacts never change player height.
 * Only trunk segments overlapping the character vertically can block movement.
 */
public final class TrunkCollision {
    public static final float PLAYER_RADIUS = .34f, PLAYER_HEIGHT = 1.6f;
    private static final float SKIN = .002f, STEP = .05f;
    public record Capsule(Vector3f a, Vector3f b, float radius, float minY, float maxY) { }
    public record Profile(List<Capsule> capsules, float top, int vertexCount) { }
    private List<Capsule> bodies = List.of();
    private final java.util.Map<Long,List<Capsule>> cells = new java.util.HashMap<>();
    private static final float CELL=3;
    private static long cellKey(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    private List<Capsule> nearby(Vector3f p){return cells.getOrDefault(cellKey((int)Math.floor(p.x/CELL),(int)Math.floor(p.z/CELL)),List.of());}

    /** The original generator writes the trunk first, in rings with a UV seam. */
    public static Profile fromMesh(CoralMesh mesh) {
        float[] v = mesh.vertices();
        int ringSize = 1;
        while (ringSize < mesh.vertexCount() && v[ringSize * CoralMesh.STRIDE + 6] != 0) ringSize++;
        if (ringSize < 4 || ringSize >= mesh.vertexCount()) throw new IllegalArgumentException("Missing trunk rings");
        List<Vector3f> centres = new ArrayList<>(); List<Float> radii = new ArrayList<>();
        List<Float> bottoms = new ArrayList<>(), tops = new ArrayList<>();
        float top = -Float.MAX_VALUE; int consumed = 0;
        for (int ring = 0; ; ring++) {
            int start = ring * ringSize;
            if (start + ringSize > mesh.vertexCount()) throw new IllegalArgumentException("Incomplete trunk");
            Vector3f centre = new Vector3f();
            float ringBottom=Float.POSITIVE_INFINITY, ringTop=Float.NEGATIVE_INFINITY;
            for (int i = 0; i < ringSize - 1; i++) {
                int k = (start + i) * CoralMesh.STRIDE;
                centre.add(v[k],v[k+1],v[k+2]); top = Math.max(top,v[k+1]);
                ringBottom=Math.min(ringBottom,v[k+1]); ringTop=Math.max(ringTop,v[k+1]);
            }
            centre.div(ringSize - 1);
            float radius = 0;
            for (int i = 0; i < ringSize - 1; i++) {
                int k = (start + i) * CoralMesh.STRIDE;
                radius = Math.max(radius, centre.distance(v[k],v[k+1],v[k+2]));
            }
            centres.add(centre); radii.add(radius); bottoms.add(ringBottom); tops.add(ringTop); consumed += ringSize;
            if (v[start * CoralMesh.STRIDE + 7] >= 1) break;
        }
        List<Capsule> segments = new ArrayList<>();
        for (int i = 1; i < centres.size(); i++)
            segments.add(new Capsule(centres.get(i-1), centres.get(i), Math.max(radii.get(i-1),radii.get(i)),
                Math.min(bottoms.get(i-1),bottoms.get(i)),Math.max(tops.get(i-1),tops.get(i))));
        return new Profile(List.copyOf(segments),top,consumed);
    }
    /** Rebuilt for all placed specimens and their current growth scale. */
    public void setBodies(List<Capsule> capsules) {
        bodies=List.copyOf(capsules);cells.clear();
        for(Capsule c:bodies){
            float padding=c.radius()+PLAYER_RADIUS+SKIN;
            int x0=(int)Math.floor((Math.min(c.a().x,c.b().x)-padding)/CELL), x1=(int)Math.floor((Math.max(c.a().x,c.b().x)+padding)/CELL);
            int z0=(int)Math.floor((Math.min(c.a().z,c.b().z)-padding)/CELL), z1=(int)Math.floor((Math.max(c.a().z,c.b().z)+padding)/CELL);
            for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)cells.computeIfAbsent(cellKey(x,z),k->new ArrayList<>()).add(c);
        }
    }
    public int bodyCount() { return bodies.size(); }
    public static void place(Profile profile, Vector3f position, float growth, List<Capsule> result) {
        place(profile,position,growth,0,result);
    }
    public static void place(Profile profile,Vector3f position,float growth,float yaw,List<Capsule> result){
        for(Capsule c:profile.capsules())result.add(new Capsule(new Vector3f(c.a()).mul(growth).rotateY(yaw).add(position),
            new Vector3f(c.b()).mul(growth).rotateY(yaw).add(position),c.radius()*growth,position.y+c.minY()*growth,position.y+c.maxY()*growth));
    }

    /** Bounded substeps prevent crossing a thin trunk, including fast diagonal movement. */
    public Vector3f move(Vector3f from, Vector3f displacement) {
        if (!from.isFinite() || !displacement.isFinite()) throw new IllegalArgumentException("Nonfinite movement");
        float length = displacement.length();
        if (length > STEP * 4096) throw new IllegalArgumentException("Movement too large; use a resolved teleport");
        int steps = Math.max(1,(int)Math.ceil(length / STEP));
        Vector3f step = new Vector3f(displacement).div(steps);
        Vector3f p = resolve(from);
        for (int i = 0; i < steps; i++) p = resolve(p.add(step));
        return p;
    }
    /** Also used after regeneration/growth. Separation is strictly horizontal. */
    public Vector3f resolve(Vector3f feet) {
        if (!feet.isFinite()) throw new IllegalArgumentException("Nonfinite position");
        Vector3f p = new Vector3f(feet);
        for (int pass = 0; pass < 32; pass++) {
            boolean changed = false;
            for (Capsule body : nearby(p)) {
                if (!overlapsHeight(p,body)) continue;
                Vector3f normal = horizontalDelta(p, body);
                float distance = normal.length(), minimum = PLAYER_RADIUS + body.radius() + SKIN;
                if (distance >= minimum) continue;
                if (distance < 1e-7f) normal.set(1,0,0); else normal.div(distance);
                // Never apply a vertical correction; rounded caps must not lift the character.
                p.x += (minimum-distance) * normal.x;
                p.z += (minimum-distance) * normal.z;
                changed = true;
            }
            if (!changed) break;
        }
        return p;
    }
    public float clearance(Vector3f feet) {
        float gap = Float.POSITIVE_INFINITY;
        for (Capsule c : bodies)
            if(overlapsHeight(feet,c))gap = Math.min(gap,horizontalDelta(feet,c).length()-PLAYER_RADIUS-c.radius());
        return gap;
    }

    // Bounds come from the actual trunk rings, not an infinitely tall footprint.
    // Test the entire character body, so feet below a raised trunk are handled too.
    private static boolean overlapsHeight(Vector3f feet,Capsule body){
        return feet.y < body.maxY()+SKIN && feet.y+PLAYER_HEIGHT > body.minY()-SKIN;
    }

    // Project each vertically overlapping curved trunk segment onto XZ. This narrow exclusion footprint
    // has no top to stand on and excludes every secondary branch from collision.
    private static Vector3f horizontalDelta(Vector3f p, Capsule body) {
        float dx=body.b().x-body.a().x, dz=body.b().z-body.a().z;
        float lengthSquared=dx*dx+dz*dz;
        float t=lengthSquared>1e-10f ? Math.clamp(((p.x-body.a().x)*dx+(p.z-body.a().z)*dz)/lengthSquared,0,1) : 0;
        return new Vector3f(p.x-body.a().x-t*dx,0,p.z-body.a().z-t*dz);
    }
}
