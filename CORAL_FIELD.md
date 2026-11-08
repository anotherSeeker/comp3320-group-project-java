# Procedural coral in the current team engine

The default scene combines the Enchanted Reef corals with the team's animated Feya,
bubble emitters and shadows. See [FANTASY_CORAL.md](FANTASY_CORAL.md) for the coral controls.

## Original integration snapshot, 5 November 2026

Contribution branch: `nirmit/procedural-coral`.
The reviewed snapshot is preserved at `nirmit/coral-review-2026-11-05` (`89a0f30`).
Upstream: `anotherSeeker/comp3320-group-project-java`, main commit
`5ff91ab9b6b3a3967e82c106764dc81efc614ecd` (fetched 5 November).
The previous integration remains at `nirmit/coral-latest-engine` (`1e31a65`).
Nirmit approved publishing this contribution to its own GitHub branch on 5 November 2026.

The default scene combines the team's current material/glTF environment with
live procedural coral, a controllable character, a ground surface, and follow/overview cameras.
The upstream environment at this commit is a material demonstration, not the finished castle,
underwater terrain, or water surface. Those assets are not in this revision.

## Review and run

On the supplied Apple Silicon Mac, open `../Coral Field Review.app`.
It includes Java and dependencies. `../Coral Field.app` preserves the previous build.
The review app is a snapshot; rebuilding source does not automatically replace either app.

From this source directory, using JDK 26:

```sh
./gradlew :app:build :app:installDist
./gradlew :app:run
./gradlew :app:run --args='--seed 12345'
./gradlew :app:generationCheck
./gradlew :app:reefCheck
./gradlew :app:run --args='--engine-demo --check'
```

Use `gradlew.bat` on Windows. The local Mac wrapper selects `../JDK/Contents/Home`
and stores build output/caches in `../../Archive`. In a normal clone, Gradle uses
its standard locations. First-time builds need dependency downloads; cached builds
can append `--offline`. GPU checks require a graphical session.

## Controls and review walkthrough

- WASD: camera-relative movement; Q/E: down/up; Shift: faster.
- Right drag or arrow keys: orbit; scroll or -/+: zoom.
- H: whole-field overview / follow view.
- 1/2/3 or panel buttons: sparse / normal / dense.
- R: generate a new seed, new coral geometry and new placement.
- G: replay the scale-based growth animation.
- F: water-like branch sway; I: proximity response; V: wireframe.
- Press P to reset the player, U to toggle bubbles in the combined scene, and Esc to close.
- The right panel retains the team's lighting, skybox and HDR controls.

Suggested review: inspect the default scene, switch to overview with H, select
3 for the dense field, regenerate with R, replay growth with G, then return to
follow view and move around the trunks. Try a skybox and HDR exposure change.

Seed 42 has **207 / 426 / 677** colonies in the combined scene. The standalone
layout without environment reservations retains **215 / 448 / 712**.
Five circular reservations keep coral roots away from the four material objects
and the relocated sample helmet. Filtering retains the original positions and
shapes of unaffected colonies. The helmet is moved away from the player spawn
only in the combined scene; `--engine-demo` retains its original placement.

## Procedural generation and rendering

`CoralGenerator` recursively constructs curved, tapered branches from vertex rings
joined by indexed triangles. `ReefLayout` creates 36 seeded shapes: 12 branching,
12 bushy and 12 fan presets, each with full and reduced detail meshes. A jittered
48 × 48 grid supplies placements, scales, yaws, colour tints and animation phases,
with a central clearing, cross paths and environment reservations.

`ReefScene.regenerate()` calls the generator directly and uploads the resulting
vertices and indices. Coral OBJ files are not loaded; R rebuilds geometry and layout
at runtime. Existing albedo textures provide colour. The coral shader uses diffuse
lighting and simple distance fog; it does not use the team's full PBR material maps.

`InstancedMesh` groups colonies by shape/detail. View-frustum and distance culling,
near/far detail and at most 72 batches keep the field manageable. The generated
coral shares the environment depth buffer and HDR target before postprocessing.

The team's latest tessellation and displacement shaders remain active for its
materials. `Shader.hasTessellation()` chooses patch draws for those materials and
triangle draws for the coral player's and ground's non-tessellated shaders.
Shader compilation/link failures are reported with diagnostics.

`TrunkCollision` uses finite-height trunk capsules in a spatial grid. Contacts
correct x/z only, so trunks do not lift the player. Above/below-trunk movement stays
free; small branches are passable. Water/proximity motion is visual deformation.
Growth replay scales an existing shape; it is not a simulation that adds branches
progressively. Environment-prop and camera collision are not implemented.

## Platform behavior

On OpenGL 4.3 hardware, the original compute effects (bloom, fog, colour grading,
vignette) and their feature controls remain available. Their shader source is
unchanged from upstream. This Mac supplies OpenGL 4.1: unsupported compute effects
are disabled in both the API and UI. Lighting, skyboxes, HDR tone mapping,
tessellation/displacement and coral animation still work. Only compatible
non-compute shader version declarations are adapted for macOS.

Host-native LWJGL dependencies and ImGui 1.89.0 support this Apple Silicon build.
The packaged app targets Apple Silicon macOS; other platforms build from source.
The OpenGL 4.3 path and Windows/Linux execution were not runtime-tested here.

## Verified results

On 5 November 2026:

- Full Gradle build and install distribution succeeded (40 Java source files).
- CPU checks passed for valid geometry, repeatable and changed seeds, all presets,
  reduced-detail meshes, density/coverage, spawn/path clearance and environment reservations.
- Existing finite-height collision regressions passed, including 90 generated trunks,
  fast movement, sliding, free movement above/below trunks and overlap recovery.
- Actual OpenGL checks passed for seed regeneration, growth replay, sway, proximity
  deformation, visibility, batching, UI rendering and dense-field collision.
- Combined tessellated and triangle rendering passed; disabling displacement and
  changing HDR exposure produced visible pixel differences.
- Unsupported compute-effect requests safely left the Mac render unchanged.
- The original engine scene passed a separate graphics smoke test.
- The packaged review app launched through macOS and its scene and control panels
  were visually inspected. Automated keyboard interaction could not be completed
  because the UI automation timed out; manually review the controls above.

The final measured dense overview rendered all 677 colonies in 36 batches
(788,580 triangles), about 2.86 ms/frame over 60 GPU-synchronized frames at
2200 × 1600. This is a short local check, not a guaranteed gameplay frame rate.
The project has no JUnit test sources; verification uses generationCheck and reefCheck.

Review screenshots and verification summary are in
`../../Archive/Verification/Coral review 2026-11-05`.
