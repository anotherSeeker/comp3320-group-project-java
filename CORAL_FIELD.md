# Coral field in the latest Java engine

Based on [anotherSeeker/comp3320-group-project-java](https://github.com/anotherSeeker/comp3320-group-project-java), `main` commit `7dacb44a7897df263815a522a61e60eb0efd143d`, fetched on 2 November 2026. The local integration branch is `nirmit/coral-latest-engine`. Nothing has been pushed to GitHub. The complete pre-update source and a checkpoint branch are retained in Archive/Git history.

## Run and demonstrate

On the supplied Apple Silicon Mac, double-click **Coral Field.app**. Its Java runtime, native libraries and assets are included. It opens at seed 42 with 448 algorithmically generated corals and a controllable character in the current team scene. The team skybox, PBR material objects and glTF sample share the same depth buffer and HDR output as the generated field.

- **WASD** move relative to the camera; **Q/E** down/up; **Shift** faster.
- **Right mouse drag** or **arrow keys** orbit; **scroll** or **-/+** zoom.
- **H** switches between character-follow and whole-field overview.
- **R** creates new coral shapes and a new random layout.
- **1 / 2 / 3** select sparse / normal / dense; the menu buttons do the same.
- **F** toggles flowing-water branch motion; **I** toggles character proximity response.
- **G** replays growth; **V** shows triangle wireframes; **P** resets the character; **Esc** closes.

At seed 42 the density presets contain **215 / 448 / 712** corals. Counts vary with the seed because some sites are omitted. A small central clearing and two connected paths stay open. Trunks block the character only while the character overlaps their height. At trunk height, go around them; above the trunk, move freely across. Small branches remain passable and bend near the character.

Suggested demonstration: start in follow view, walk near a coral, press H to show coverage, press 3 for the dense field, then R to show that generation is live.

## How it works

The existing Java recursive generator builds rings of vertices and joins adjacent rings with triangles to form tapered curved branches. Smaller child branches repeat that process. Its shape algorithm is preserved. For each field seed, the new code makes 36 shapes: 12 branching, 12 bushy, and 12 fan presets. Each has a full-detail and a reduced-detail mesh.

`ReefLayout` divides a 48 x 48 unit area into cells, jitters each centre, leaves paths and a spawn clearing, and assigns a shape, scale, yaw, colour tint and animation phase. The same seed reproduces the same meshes and placements. The ground is a simple flat surface at y=-0.08; coral roots sit at y=0. This is a finite field, not a finished underwater level or streamed terrain. Camera collision with scenery is not implemented.

The engine uploads the generated Java vertices and triangle indices directly to OpenGL. It does not need to write an OBJ every time R is pressed. The original engine OBJ loading remains in use for the existing character. The three existing coral colour textures are reused from the earlier Blender material workflow. No new Blender mesh export is required for runtime generation.

The field uses a simple textured diffuse shader. The engine's original normal, roughness and other material support remains available to its original scene, but the field shader uses albedo, lighting and fog rather than those additional maps. GLSL moves upper branch vertices with smooth spatial waves. The primary trunk and base remain rigid. Nearby vertices also bend away from the character, with a fading movement wake. This is visual vertex animation, not a full fluid or branch physics simulation.

`InstancedMesh` draws many placements of the same shape together. Objects outside the camera's view or beyond 85 units are skipped. Corals within 19 units use the full mesh and deformation-adjusted normals; farther corals use fewer rings and sides and cheaper normal shading. All positions still have collision, including corals outside the view. There are at most 72 coral draw calls (36 shapes x 2 detail levels).

`TrunkCollision` extracts only the first tube of each original mesh, rotates/scales it with the colony, and projects its curved trunk onto the horizontal plane. A spatial grid finds nearby trunk segments. Each segment keeps the minimum/maximum height of its actual mesh rings, transformed with the colony and growth. Segments entirely above or below the character are ignored. Short movement steps prevent tunnelling, and collision correction only changes x/z. This preserves the requested no-climb behavior. When a new or growing trunk overlaps the character, it pushes them sideways.

## Changed files

Paths below are relative to this source folder.

| File(s) | Purpose |
|---|---|
| `app/src/main/java/Game.java` | Starts the coral field; `--seed NUMBER`, `--check`, and `--engine-demo` options. |
| `coral/CoralGenerator.java`, `CoralMesh.java`, `CoralParams.java`, `CoralType.java`, `CoralSet.java` under the Java folder | Existing procedural geometry and preset helpers copied from the prior solid-trunk version. |
| `coral/ReefLayout.java`, `ReefGround.java` | New field placements, shape variation/detail settings, and flat ground. |
| `scripts/ReefScene.java` | Creates/replaces mesh pools, places colonies, handles growth/UI, culls, batches and draws. |
| `engine/rendering/InstancedMesh.java` | Adds a per-instance position/scale/yaw/phase/tint buffer to the existing mesh format. |
| `scripts/ReefPlayer.java`, `ReefCamera.java` | Character movement and follow/overview camera. |
| `scripts/CoralInteraction.java` | Existing proximity/wake tracking reused. |
| `coral/TrunkCollision.java` | Existing no-climb collision plus rotated/scaled placement and a spatial lookup grid. |
| `app/assets/shaders/coral/` | Instanced coral animation/lighting and basic character/ground shaders. |
| `app/assets/textures/coral/` | Three reused coral colour textures. |
| `engine/App.java`, `MonoScript.java` | One-frame rendering, a script render/cleanup hook, correct input/UI initialization and resource cleanup. |
| `engine/SceneGraph.java` | Fixes camera quaternion mutation, preserves the original skybox path, extends clip distance, and supplies shader time. |
| `engine/rendering/Shader.java`, `engine/GraphicsPlatform.java` | Checks shader errors, caches uniform locations, uses compatible GLSL/OpenGL on macOS. |
| `engine/AssetManager.java`, `engine/components/Material.java` | Adds cached plain-colour textures and coral tint. Existing material maps remain intact. |
| `app/build.gradle.kts`, `gradlew` | Host-specific native libraries, macOS first-thread launch, run/check tasks, packaged assets, executable wrapper. ImGui changes from 1.86.10 to 1.89.0 because the old Mac native binary was Intel-only. |
| `engine/ReefCheck.java`, `coral/TrunkCollisionCheck.java`, `FrameCapture.java` | Actual-render and geometry/collision checks, with screenshot capture. |

The ImGui change was verified against the downloaded native binary (both arm64 and x86_64) and the library API. Upstream reference: https://github.com/SpaiR/imgui-java/releases/tag/v1.89.0 .

## Build from the source ZIP

Unzip, open a terminal in the extracted source folder, and use JDK 26 (as required by this engine):

```sh
./gradlew :app:build :app:installDist
./gradlew :app:run
./gradlew :app:run --args='--seed 12345'
./gradlew :app:reefCheck
./gradlew :app:run --args='--engine-demo'
```

On Windows use `gradlew.bat`. Gradle downloads dependencies on the first build. The supplied `.app` is for Apple Silicon macOS; other platforms must build from source. Only this Mac was runtime-tested. The rendering checks need a working graphical session even though their window is hidden.

The source ZIP contains the complete engine, Gradle wrapper, shaders and assets, without development caches, logs, binaries or old drafts. `README.md` includes the upstream guidance and relocated local build paths; this file explains the coral addition. The original material-test scene remains available with `--engine-demo`; combine it with `--check` to smoke-test and exit.

## Earlier coral-demo verification (before the November merge)

The results below describe the previously verified build; they are not new GPU results for the merged renderer.

- Gradle build and distribution completed successfully; the upstream project has no JUnit test source files, so verification uses the explicit `reefCheck` task.
- Repeated seeds produce exactly equal CPU vertices, indices and placements; alternate seeds visibly change the rendered field. Render comparison allows at most four colour levels of GPU interpolation rounding on under 0.2% of pixels.
- All four quadrants have coverage; density increases; the spawn clearing is respected.
- Full and reduced detail paths render successfully, and the overview sees all 712 dense colonies in 36 batches (828,930 triangles).
- Existing collision checks cover 90 generated trunks, fast movement, sliding, passable twigs, growing trunks, overlap correction and free movement above/below the trunk and partial-height contact. Every dense-field root also passes overlap resolution and a high-altitude character-movement regression check.
- GPU image checks confirm visible growth, proximity deformation and water motion changing with time. Menu rendering and OpenGL error checks pass.
- The original engine's material-test scene also renders successfully.
- One measured 2200 x 1600 offscreen dense-overview run averaged about 2.2 ms per frame, synchronized with the GPU, over 60 frames after warm-up. This is a short local rendering measurement, not a guaranteed gameplay frame rate. The normal application enables vertical sync.

## Height-collision correction — 23 October 2026

Trunk collision now has finite vertical bounds. The previous infinite-height footprint could force a character far above a coral to detour. The fix keeps sideways-only correction for real contacts, so horizontal movement still cannot lift the character. Small branches remain passable. Source, regression checks and the packaged application were updated together.

## Organised Mac working folder

The complete packaged app is now `../Coral Field.app`. Build and run from this
Source folder with the commands above. The executable wrapper uses the sibling
`../JDK/Contents/Home` and stores development caches and generated output under
`../../Archive`. Original pre-cleanup source is backed up as
`../../Archive/Backups and records/Current source backup/Latest Engine Source.zip`.
The texture-authoring Blender file is archived under Previews and working files;
the exported textures and runtime models remain in the source assets.

## Integration with current GitHub main — 2 November 2026

The default scene now registers the latest upstream `DemoScene` alongside
`ReefPlayer`, `ReefScene` and `ReefCamera`. `ReefScene.regenerate()` builds its
meshes directly with `CoralGenerator.generate()` and obtains seeded positions
from `ReefLayout.generate()`. These live vertex/index buffers are instanced into
the environment; coral OBJ files are not loaded or required. R changes the seed
and regenerates both geometry and placement. G replays growth.

Coral rendering runs after the environment objects and before the team's HDR
postprocessing. The latest renderer package layout, glTF scene parser, PBR
material API, new assets and Uchimura tone mapping are retained. The supplied
upstream environment is the material-test/glTF scene; this repository revision
does not contain the finished castle or underwater terrain.

On OpenGL 4.3 hardware, the team's compute bloom passes remain enabled. On this
Mac's OpenGL 4.1 context, they are skipped; the same HDR framebuffer and Uchimura
tone mapping still run. Vertex/fragment shader versions are adapted for 4.1.
Compute shaders are not relabelled or emulated.

Use `./gradlew :app:generationCheck` for a graphics-free generation/collision
check, or `./gradlew :app:reefCheck` for the actual GPU checks. `Game
--check-generation` also runs the CPU checks before creating a window.

All 38 Java source files compile against the application's dependency JARs.
CPU checks pass for repeatable geometry, changed seeds, reduced-detail meshes,
three density settings, spawn/path clearance, environment coverage, and the
finite-height collision regressions. Full Gradle execution is blocked in this
automation session by its local-socket restriction. Current GPU verification
status is recorded in Archive/Verification/GitHub update 2026-11-02.
