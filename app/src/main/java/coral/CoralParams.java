package coral;

/** Parameters ported from Nirmit's C++ generator. No graphics context is needed. */
public class CoralParams {
    public int segments = 8, ringsPerBranch = 7, maxDepth = 3;
    public int childrenMin = 2, childrenMax = 3;
    public float trunkLength = 1.7f, trunkRadius = .22f;
    public float lengthScale = .72f, radiusScale = .62f, branchSpread = .65f;
    public float bend = .12f, waviness = .025f, upwardBias = .55f;
    public float lengthJitterMin = .82f, lengthJitterMax = 1.08f;
    public long seed = 42;

    public void validate() {
        if (segments < 3 || segments > 64 || ringsPerBranch < 2 || ringsPerBranch > 64
                || maxDepth < 0 || maxDepth > 6 || childrenMin < 0
                || childrenMax < childrenMin || childrenMax > 5)
            throw new IllegalArgumentException("Invalid ring, recursion or child count");
        for (float v : new float[]{trunkLength, trunkRadius, lengthScale, radiusScale,
                branchSpread, bend, waviness, upwardBias, lengthJitterMin, lengthJitterMax})
            if (!Float.isFinite(v)) throw new IllegalArgumentException("Parameters must be finite");
        if (trunkLength < .12f || trunkRadius < .015f || lengthScale <= 0 || lengthScale > 1
                || radiusScale <= 0 || radiusScale > 1 || branchSpread < 0 || branchSpread > 1
                || bend < 0 || waviness < 0 || upwardBias < 0
                || lengthJitterMin <= 0 || lengthJitterMax < lengthJitterMin)
            throw new IllegalArgumentException("Invalid coral dimensions or variation range");
        long count = 0, level = 1;
        for (int d = 0; d <= maxDepth; d++) { count += level; level *= childrenMax; }
        if (count * ringsPerBranch * (segments + 1) > 300_000)
            throw new IllegalArgumentException("Coral exceeds the 300,000 vertex budget");
    }
}
