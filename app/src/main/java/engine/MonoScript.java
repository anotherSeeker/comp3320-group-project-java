package engine;

public abstract class MonoScript {
    public AssetManager Assets = AssetManager.get();
    public SceneGraph Scene;
    public InputManager Input;

    public MonoScript() {

    }

    public void start() {
        Start();
    }

    public void update(double deltaTime) {
        Update(deltaTime);
    }

    public void afterRender() { LateUpdate(); }
    public void close() { Destroy(); }
    protected void LateUpdate() { }
    protected void Destroy() { }

    protected void Start() {
    }

    protected void Update(double deltaTime) {
    }

}
