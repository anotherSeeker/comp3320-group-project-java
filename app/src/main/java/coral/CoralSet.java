package coral;

import java.util.Random;

/** One repeatable set of three meshes. No files, Blender or GPU are needed here. */
public final class CoralSet {
    private CoralSet() { }
    public static CoralMesh[] generate(long seed) {
        Random random = new Random(seed);
        CoralType[] types = {CoralType.BRANCHING, CoralType.BUSHY, CoralType.FAN};
        CoralMesh[] result = new CoralMesh[types.length];
        for (int i = 0; i < types.length; i++) {
            CoralParams p = types[i].parameters(random.nextLong());
            // Keep the familiar presets, with a little extra size and spread variation.
            p.trunkLength *= .85f + random.nextFloat() * .30f;
            p.trunkRadius *= .90f + random.nextFloat() * .20f;
            p.branchSpread += (random.nextFloat() - .5f) * .10f;
            result[i] = CoralGenerator.generate(p);
        }
        return result;
    }
}
