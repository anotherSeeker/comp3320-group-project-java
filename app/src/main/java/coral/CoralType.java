package coral;

public enum CoralType {
    BRANCHING, BUSHY, FAN, REEF;

    public CoralParams parameters(long seed) {
        CoralParams p = new CoralParams();
        p.seed = seed;
        switch (this) {
            case BRANCHING, REEF -> {
                p.trunkLength = 1.55f; p.trunkRadius = .23f;
                p.lengthScale = .68f; p.radiusScale = .60f;
                p.branchSpread = .62f; p.bend = .16f; p.waviness = .035f; p.upwardBias = .48f;
            }
            case BUSHY -> {
                p.childrenMin = 3; p.childrenMax = 4;
                p.trunkLength = 1.15f; p.trunkRadius = .25f;
                p.lengthScale = .60f; p.radiusScale = .58f;
                p.branchSpread = .78f; p.bend = .19f; p.waviness = .045f; p.upwardBias = .34f;
                p.ringsPerBranch = 8;
            }
            case FAN -> {
                p.maxDepth = 4; p.childrenMin = 2; p.childrenMax = 2;
                p.trunkLength = 1.35f; p.trunkRadius = .20f;
                p.lengthScale = .70f; p.radiusScale = .61f;
                p.branchSpread = .72f; p.bend = .12f; p.waviness = .025f; p.upwardBias = .60f;
                p.segments = 7;
            }
        }
        return p;
    }
}
