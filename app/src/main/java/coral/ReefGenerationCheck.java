package coral;

import java.util.Arrays;

/** Checks runtime generation without needing an OpenGL window. */
public final class ReefGenerationCheck {
    private ReefGenerationCheck() { }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void validateMesh(CoralMesh mesh) {
        require(mesh.vertexCount() > 0 && mesh.indices().length > 0, "Empty generated coral");
        require(mesh.indices().length % 3 == 0, "Generated faces are not triangles");
        for (float value : mesh.vertices()) require(Float.isFinite(value), "Non-finite vertex");
        for (int index : mesh.indices())
            require(index >= 0 && index < mesh.vertexCount(), "Triangle references a missing vertex");
    }

    public static void run() {
        int changedShapes = 0;
        for (int variant = 0; variant < ReefLayout.VARIANTS; variant++) {
            CoralMesh full = CoralGenerator.generate(ReefLayout.parameters(42, variant, false));
            CoralMesh repeat = CoralGenerator.generate(ReefLayout.parameters(42, variant, false));
            CoralMesh other = CoralGenerator.generate(ReefLayout.parameters(43, variant, false));
            CoralMesh low = CoralGenerator.generate(ReefLayout.parameters(42, variant, true));
            validateMesh(full);
            validateMesh(low);
            require(Arrays.equals(full.vertices(), repeat.vertices())
                    && Arrays.equals(full.indices(), repeat.indices()), "Seed changed geometry");
            if (!Arrays.equals(full.vertices(), other.vertices())) changedShapes++;
            require(low.vertexCount() < full.vertexCount(), "Distant mesh did not reduce detail");
        }
        require(changedShapes > 0, "A new seed did not generate new coral shapes");

        for (long seed : new long[]{42, 43, 12345}) {
            int previousCount = 0;
            for (int density = 0; density < 3; density++) {
                var layout = ReefLayout.generate(seed, density);
                require(layout.equals(ReefLayout.generate(seed, density)), "Placement is not repeatable");
                require(layout.size() > previousCount, "Density did not increase colony count");
                previousCount = layout.size();
                int[] quadrants = new int[4];
                for (var colony : layout) {
                    require(Math.abs(colony.x()) < ReefLayout.HALF_EXTENT
                            && Math.abs(colony.z()) < ReefLayout.HALF_EXTENT, "Colony outside environment");
                    require(colony.position().y == 0, "Root detached from the environment's ground");
                    require(colony.x() * colony.x() + colony.z() * colony.z() >= 16, "Spawn obstructed");
                    require(Math.abs(colony.x()) > 1.4f && Math.abs(colony.z()) > 1.4f, "Walking route obstructed");
                    require(colony.variant() >= 0 && colony.variant() < ReefLayout.VARIANTS, "Missing shape");
                    quadrants[(colony.x() > 0 ? 1 : 0) + (colony.z() > 0 ? 2 : 0)]++;
                }
                for (int count : quadrants) require(count > 20, "Environment lacks coral coverage");
                System.out.printf("Generated seed=%d density=%d colonies=%d%n", seed, density, layout.size());
            }
        }
        require(!ReefLayout.generate(42, 1).equals(ReefLayout.generate(43, 1)), "New seed kept old placement");
        TrunkCollisionCheck.run();
        System.out.println("PASS: algorithmic shape generation, seeded variation, environment placement, density, LOD and height collision.");
    }

    public static void main(String[] args) { run(); }
}
