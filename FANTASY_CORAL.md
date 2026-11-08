# Enchanted Reef — fantasy coral preview

Branch: `nirmit/coral-fantasy`, based on `nirmit/procedural-coral` commit `52551b5`.

## Play

Open `../Enchanted Reef.app` on this Apple Silicon Mac. Its bundled runtime and assets
match the tested build. It opens the coral showcase with the team's sample props hidden.
The older Coral Field Review.app remains available unchanged.

From source, with JDK 26:

```sh
./gradlew :app:run                          # Coral plus team environment
./gradlew :app:run --args='--showcase'      # Coral showcase
./gradlew :app:run --args='--engine-demo'   # Original team environment
./gradlew :app:run --args='--showcase --capture'
./gradlew :app:generationCheck :app:reefCheck
```

Controls: WASD move, Q/E swim vertically, right-drag or arrows orbit, scroll zoom,
H overview, R new seed, 1/2/3 density, G scale-growth replay, F sway, I proximity,
V wireframe, P reset, Esc close.

New controls:

- **B** sends an expanding light pulse from the player. Nearby colonies brighten
  and bow as the wave passes, then settle back into their idle motion.
- **L** toggles bioluminescence and its glow.
- **O** toggles the drifting luminous spores.
- **Tab** hides/shows the coral panel for a clear view.

## Visual treatment

Three species palettes share the existing procedural geometry: turquoise/jade,
amethyst/pink and coral/amber. The fragment shader layers a dark root-to-crown colour
ramp, textured surface detail, pearly highlights, approximate backlighting, Fresnel
edge lighting, luminous pores, winding veins and bright tips. A phase-offset breathing
cycle and travelling brightness bands animate each colony independently.

Upper branches combine a slow tide with smaller flutter. Trunks retain their rigid
collision shape. Nearby swimmers trigger a local fluorescent response and bending;
the smoothed velocity/wake also affects drifting spores. These are stylized shader
behaviours, not fluid simulation or biological growth.

The pulse moves at six world units per second, weakens with distance and fades out.
The CPU stores its origin/time; the GPU computes its wavefront consistently across
corals and spores. Pressing B starts a fresh pulse at the player's current position.

Five repeatable spores per colony rise, orbit and fade analytically on the GPU.
They render as depth-tested additive point sprites in one draw call, with depth writes
disabled so they cannot occlude opaque scenery. The field still uses instancing,
frustum culling and the existing near/far mesh detail levels.

A dark seabed shader supplies moving caustic-like patterns and fine surface variation.
Coral scenes use a matching deep-water sky tint. The original engine scene retains
its normal skybox, material and postprocessing behavior.

## Glow and compatibility

`CoralGlow` extracts bright HDR values at quarter resolution, applies two horizontal/
vertical Gaussian blur pairs, and adds the result before the team's existing Uchimura
tone mapping. The two temporary textures alternate reads/writes to avoid framebuffer
feedback. Targets resize with the main framebuffer and are released on shutdown.
All these passes use vertex/fragment shaders supported by Mac OpenGL 4.1.

The team's compute bloom/fog/grading/vignette controls still require OpenGL 4.3.
The new coral glow is a separate optional path; it does not enable or emulate those
compute effects. The original scene defaults to zero coral glow.

## Verification — 5 November 2026

Full Gradle build and install distribution passed. CPU generation and finite-height
collision checks passed. The real GPU suite verified:

- deterministic regeneration, density and near/far rendering;
- unchanged trunk collision and player movement guarantees;
- visible glow, spores and bioluminescence toggles;
- a visible propagating pulse and existing proximity/water responses;
- tessellated team materials, displacement and HDR controls;
- original-engine smoke test with coral effects disabled.

Final dense overview: 677 visible colonies, 36 coral mesh batches, 788,580 triangles,
plus 3,385 spores in one extra draw and five reduced-resolution glow passes.
The recorded 60-frame GPU-synchronized run averaged 3.21 ms/frame at 2200 × 1600;
this is a short local measurement, not a guaranteed frame rate.

The `--capture` mode recorded 48 frames of the actual showcase renderer with a fixed
seed, camera and timeline. PNG stills and animated GIF/WebP previews are under
`../../Archive/Verification/Fantasy coral 2026-11-05`.
The preview shows frames captured directly from the running engine.

Limitations: shaders approximate translucency/caustics rather than physically tracing
light; spores are visual effects, not collidable objects; geometry and the scale-growth
animation are inherited. The finished castle/water scene is still absent from upstream.
Only Apple Silicon/macOS was runtime-tested; the OpenGL 4.3 path needs team hardware.
