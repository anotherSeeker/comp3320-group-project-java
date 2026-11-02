package coral;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.joml.Vector3f;

/** Repeatable coverage of a configurable area, with a central clearing and cross paths. */
public final class ReefLayout {
    public static final float HALF_EXTENT=24;
    public static final int VARIANTS=36;
    public record Colony(float x,float z,float scale,float yaw,int variant,float phase,float r,float g,float b) {
        public Vector3f position(){return new Vector3f(x,0,z);}
    }
    private ReefLayout(){ }
    public static List<Colony> generate(long seed,int density) {
        if(density<0||density>2) throw new IllegalArgumentException("Density must be 0, 1 or 2");
        int cells=new int[]{16,23,29}[density];
        float spacing=2*HALF_EXTENT/cells;
        Random random=new Random(seed);
        List<Colony> result=new ArrayList<>();
        for(int z=0;z<cells;z++) for(int x=0;x<cells;x++) {
            float px=-HALF_EXTENT+(x+.5f)*spacing+(random.nextFloat()-.5f)*spacing*.32f;
            float pz=-HALF_EXTENT+(z+.5f)*spacing+(random.nextFloat()-.5f)*spacing*.32f;
            // Keep the spawn area and two connected routes clear of solid trunks.
            if(px*px+pz*pz<16 || Math.abs(px)<1.45f || Math.abs(pz)<1.45f || random.nextFloat()<.08f) continue;
            float scale=.48f+random.nextFloat()*.48f;
            result.add(new Colony(px,pz,scale,random.nextFloat()*(float)(Math.PI*2),random.nextInt(VARIANTS),
                random.nextFloat()*6.283185f,.78f+random.nextFloat()*.28f,.78f+random.nextFloat()*.28f,.78f+random.nextFloat()*.28f));
        }
        return List.copyOf(result);
    }
    public static CoralParams parameters(long seed,int variant,boolean lowDetail) {
        if(variant<0 || variant>=VARIANTS)throw new IllegalArgumentException("Variant outside pool");
        Random r=new Random(seed+variant*0x9E3779B97F4A7C15L);
        CoralType type=new CoralType[]{CoralType.BRANCHING,CoralType.BUSHY,CoralType.FAN}[variant%3];
        CoralParams p=type.parameters(r.nextLong());
        p.trunkLength*=.85f+r.nextFloat()*.3f; p.trunkRadius*=.9f+r.nextFloat()*.2f;
        p.branchSpread+=(r.nextFloat()-.5f)*.1f;
        // Same branching RNG/topology: reduce radial/longitudinal sampling only.
        if(lowDetail){p.segments=5;p.ringsPerBranch=4;}
        return p;
    }
}
