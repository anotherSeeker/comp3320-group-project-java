package engine;

public abstract class MonoScript {
    public AssetManager assetManager = AssetManager.get();
    public Scene scene;

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
