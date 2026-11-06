package engine;

import engine.rendering.ShaderPipeline;

public abstract class MonoScript {
    public AssetManager Assets = AssetManager.get();
    public SceneGraph Scene;
    public InputManager Input;
    public ShaderPipeline PostProcess;

    public MonoScript() {

    }

    public void start() {
        Start();
    }

    public void update(double deltaTime) {
        Update(deltaTime);
    }

    protected void Start() {
    }

    protected void Update(double deltaTime) {
    }

}
