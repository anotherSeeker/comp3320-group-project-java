package engine;

public abstract class MonoScript {

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
