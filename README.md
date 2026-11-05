# the sunken castle (comp3320-group-project-java)
Java rewrite of c++ engine

## languages
- java

## libraries
- LWJGL
- Dominion ECS
- IMGUI

## tooling
- gradle


## how to run project

to run the project you can run:
```pwsh
gradle run
```

if you dont have gradle then theres bash and batch scripts that run gradle for you
```pwsh
./gradlew.bat run

# or for macOS/linux
./gradlew :app:run
```

## how to make a script

first create a class that inherits from monoscript inside the `scripts` folder
```java
package scripts;
public class MyGame extends MonoScript {
    @Override
    protected void Start() {
        System.out.println("hello world");
    }

    @Override
    protected void Update(double deltaTime) {

    }
}
```

then register that script to the engine

```java
app.registerScript(new scripts.MyGame());
```
## Local Mac project layout

From this Source folder, use `./gradlew :app:build :app:installDist` to rebuild,
`./gradlew :app:run` to launch, and `./gradlew :app:reefCheck` to verify rendering
and trunk-height collision. The wrapper selects `../JDK/Contents/Home` and keeps
Gradle caches, generated build files and render-check output in `../../Archive`.
The current review app is `../Coral Field Review.app`; it includes its own runtime.
`../Coral Field.app` preserves the earlier build.
See `../START HERE.txt` and `CORAL_FIELD.md` for controls and project details.

## Procedural coral in the current team environment

Contribution branch `nirmit/procedural-coral` merges upstream main `5ff91ab`.
Nirmit approved publishing this branch on 5 November 2026; `main` is unchanged.
The default scene includes the team's current environment and seeded recursive
coral generation. Press R for new shapes/placement, 1/2/3 for density, and G for
growth. Source details and hardware-specific rendering behavior are documented
in `CORAL_FIELD.md`. Run `./gradlew :app:generationCheck` for CPU verification.
