# Tanz: underwater lighting, shadows and water surface

My part of The Sunken Castle: underwater lighting, light rays, water surface and
Davies' castle scene for the Java engine. Everything on this branch is **new files
only**; no existing team file is changed.

---

## Step by step: add this to the main project

### Step 1. Merge the branch

1. On GitHub, open **Pull requests → New pull request**.
2. Set **base: `main`** and **compare: this branch** (`tanz-lighting`).
3. GitHub should show **"Able to merge"**. There can't be conflicts, because every file is new.
4. Click **Create pull request**, then **Merge pull request**.
5. On your PC, run `git pull` on `main`.

### Step 2. Check the files are there

After merging, these files should exist:

```
app/src/main/java/scripts/SunkenCastleScene.java
app/src/main/java/scripts/UnderwaterLighting.java
app/src/main/java/scripts/WaterSurface.java
app/src/main/java/scripts/FreeCamera.java          (optional)
app/assets/shaders/underwater.vert
app/assets/shaders/underwater.frag
app/assets/shaders/water_surface.vert
app/assets/shaders/water_surface.frag
app/assets/shaders/water_background.vert
app/assets/shaders/water_background.frag
app/assets/meshes/sunken/sunken_seabed.obj
app/assets/meshes/sunken/sunken_castle.obj
app/assets/meshes/sunken/sunken_landscape.obj
app/assets/meshes/sunken/sunken_extras.obj
```

`WaterSurface` also uses the team's existing `app/assets/meshes/uvsphere.obj`, which is already in `main`.

### Step 3. Turn the scripts on in `Game.java`

This is the only existing file that needs changing. Replace the `registerScript` lines with these, **in this order**:

```java
import engine.*;

public class Game {
    public static void main(String[] args) {
        App app = new App();

        if (app.init(800, 600, "sunken castle")) {
            app.registerScript(new scripts.FreeCamera());          // or scripts.Camera
            app.registerScript(new scripts.SunkenCastleScene());   // instead of scripts.DemoScene
            app.registerScript(new scripts.PostProcess());
            app.registerScript(new scripts.UnderwaterLighting());  // must be AFTER PostProcess
            app.registerScript(new scripts.WaterSurface());
            app.run();
        }

        app.close();
    }
}
```

Why the order matters:
- **The camera first:** `SunkenCastleScene` moves the camera to the castle after the camera script has set its start position.
- **`UnderwaterLighting` after `PostProcess`:** `PostProcess` sets the light and fog every frame from its debug window, so `UnderwaterLighting` has to run after it for its values to be used.

Optional changes:
- **`DemoScene` and `Feya`:** leave them out, because the demo spheres and helmet would sit inside the castle. They can be added back.
- **`Camera` instead of `FreeCamera`:** `FreeCamera` adds arrow keys to turn a full 360°. `scripts.Camera` also works.

### Step 4. Run it

```powershell
.\gradlew.bat run
```

You should see:
- Davies' castle under water, with soft shadows, caustics on the sand and walls, and light rays.
- Looking up: the rippled water surface, with the sky visible straight overhead.
- The tower tops above the water line, with a bright line where they leave the water.
- A window called **"underwater lighting"**. Untick "underwater lighting on" to compare with the normal look.

**Controls (`FreeCamera`):**
- **Arrow keys:** turn and look up or down.
- **Right mouse + drag:** look around.
- **W A S D:** move.
- **E / Q:** up or down.
- **Shift:** move slowly.

### Step 5. If something goes wrong

| What you see | Cause | Fix |
|---|---|---|
| `could not load model ...sunken_...obj` | The `.obj` files are missing | Check that `app/assets/meshes/sunken/` was merged (Step 2) |
| Red text starting `VERTEX` or `FRAGMENT` mentioning `underwater`, `water_surface` or `water_background` | Shader compile error | Send the full text to Tanz |
| `error C7011 ... vec4 to vec3` | `anim.vert` lines 62–64 (the animated character shader), not this branch. It happens on NVIDIA | Unrelated; it doesn't stop the game |
| Light and fog sliders in the debug window do nothing | `UnderwaterLighting` overrides them while it's on | Untick "underwater lighting on" |
| Everything looks foggy | `fog.glsl` uses `near = 0.1` in `getDepth()`, but the camera uses `0.01`, so the fog is about 3x too thick | Lower "water density", or change `fog.glsl` to `0.01` |
| The game is slow | Light rays (24 steps per pixel) | Lower the "light rays" slider, or set `RAY_STEPS` lower in the three water shaders |

---

## What each file does

| File | What it does |
|---|---|
| `SunkenCastleScene.java` | Loads Davies' castle, seabed, rocks/coral/seaweed and trees with the engine's PBR materials (temporary brick and concrete textures). The materials use `underwater.vert/.frag`. |
| `assets/meshes/sunken/*.obj` | Davies' scene exported from Blender: Y-up and already in place. Each file is merged into one mesh, because `LoadMesh` only reads the first mesh in a file. |
| `underwater.vert/.frag` | The team's `default.frag` (PBR + PCSS shadows) with underwater lighting added. Every addition is marked `TANZ`. |
| `UnderwaterLighting.java` | Sets the engine's sun, fog, bloom and exposure for under water, and sends the time and slider values to the water shaders. Has its own window with sliders, a reset button and an on/off checkbox. |
| `WaterSurface.java` + `water_surface.vert/.frag` | The water surface at 14 m, seen from below. |
| `water_background.vert/.frag` | Deep water all around the camera, so the sky picture doesn't show past the edges of the scene. Created by `WaterSurface`. |
| `FreeCamera.java` | Optional 360° camera: arrow keys turn and look. It also fixes the mouse-look jump, by reading the mouse movement every frame. |

### The physics used

Everything **below the water line (14 m) is lit as under water**, and everything above it as in air.

| Law | Formula | What it does here |
|---|---|---|
| Snell's law | n₁ sin θ₁ = n₂ sin θ₂ (water n = 1.333) | Sunlight bends as it enters the water. From below, things above the water can only be seen within 48.6° of straight up (the critical angle). Outside that, the surface is a mirror (total internal reflection). |
| Fresnel (Schlick) | R = R₀ + (1 − R₀)(1 − cos θ)⁵, R₀ = ((n₁ − n₂)/(n₁ + n₂))² | Part of the light reflects off the surface, on the way in and on the way out. |
| Beer–Lambert | I = I₀ e^(−σd), σ = (0.30, 0.07, 0.05) per metre for red, green and blue | Water absorbs light, red fastest. Applied to the sunlight's path down to each point, the view path to the camera, and the light rays. |

### Effects

- **Caustics:** moving light lines (Voronoi "distance to edge") projected along the sunlight. Only below the water, and only where the sun reaches.
- **Light rays (god rays):** each pixel steps along its line of sight through the water (24 steps). At each step it checks the engine's shadow map, so the castle and trees block the rays, in the same direction as the shadows.
- **Water surface:**
  - ripples
  - Snell's window showing the sky (`PrefilterMap`)
  - a rippled mirror outside the window
  - sun glint
  - fading into the murk with distance
- **Water line:** a thin bright line where the surface touches the castle, with a darker wet band just above it.
- **Lighting:**
  - blue-green ambient light below the water
  - softer shadow edges (PCSS light angle 6° instead of 3°)
  - plain white sun, with the water doing the colouring

### How it fits the engine (no engine changes)

- **The water and background don't block the sun:** every object is drawn into the shadow map, so `WaterSurface` scales its two objects to 0, which makes them collapse to a point in the shadow pass. Their vertex shaders build the full size from the object's position instead.
- **Time and slider values:** material shaders don't get the time or the slider values, so the scripts set those uniforms on the shaders every frame.
- **See-through surface:** the engine has no transparency, so the water surface writes a depth value pushed back 8 m above the water. Things above the water, like the tower tops, are closer and are drawn over it.

---

## Keeping it working later

1. **`underwater.frag` is a copy of `default.frag`.** If `default.frag` changes, copy the change into `underwater.frag` too, or move the `TANZ` parts into `default.frag`.
2. **The water height (14 m) appears in several places:**
   - `WaterSurface.java`: `WATER_HEIGHT`
   - `underwater.frag`: `WATER_LEVEL` and `RAY_WATER_LEVEL`
   - `water_surface.frag` and `water_background.frag`: `RAY_WATER_LEVEL`

   Change them all together.
3. **Other models (for example Feya):** they keep the normal look. To give them the underwater look, give their material `.add("SHADER", <underwater shader ID>)`, as `SunkenCastleScene` does.
4. **Uniform names:** the shaders use the names `SceneGraph` already sends: `Model`, `CameraMatrix`, `ViewPosition`, `LightDirection`, `DiffuseColour`, `LightMatrix`, `ShadowMapComparison`, `PrefilterMap`. If any of these are renamed, the shaders need the same change.

---

## Tested

- **Full game:** ran on Windows (NVIDIA) with `.\gradlew.bat run`.
- **Shaders:** compiled and linked with an OpenGL 4.3 core context.
- **Merged models:** checked; each loads as a single mesh with UVs.

AI use: Claude (Anthropic) was used as a guide while writing these files.
