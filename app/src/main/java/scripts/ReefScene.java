package scripts;

import coral.*;
import engine.*;
import engine.rendering.*;
import engine.components.Material;
import engine.components.MaterialEntry;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.*;
import org.joml.FrustumIntersection;
import org.joml.Vector3f;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

/** Hundreds of placed colonies rendered in batches from a reusable shape pool. */
public final class ReefScene extends MonoScript {
    public ReefPlayer player;
    private final CoralInteraction interaction=new CoralInteraction();
    public final TrunkCollision collisions=new TrunkCollision();
    private final Set<Integer> held=new HashSet<>();
    private List<ReefLayout.Colony> colonies=List.of();
    private Pool[] pools;
    private Shader shader;
    private final Texture[] textures=new Texture[3];
    private long seed;
    private int density=1;
    private boolean flow=true,react=true,wireframe,showUI=true;
    private double growthElapsed=1.8;
    private float growth=1,lastCollisionGrowth=-1;
    public int visibleCount,nearCount,farCount,drawCalls,triangles;
    private record Pool(InstancedMesh near,InstancedMesh far,TrunkCollision.Profile trunk,float height,float radius,
                        FloatBuffer nearInstances,FloatBuffer farInstances) {
        void close(){near.delete();far.delete();memFree(nearInstances);memFree(farInstances);}
    }
    public ReefScene(long seed){this.seed=seed;}
    public void animationTime(float time){Scene.setAnimationTime(time);}
    public long seed(){return seed;}
    public int density(){return density;}
    public int count(){return colonies.size();}
    public List<ReefLayout.Colony> colonies(){return colonies;}
    public int prototypeCount(){return pools.length;}
    public void showUI(boolean enabled){showUI=enabled;}
    public void setFlow(boolean enabled){flow=enabled;}
    public void setInteraction(boolean enabled){react=enabled;}
    public void finishGrowth(){growthElapsed=1.8;updateGrowth(0);}
    @Override protected void Start(){
        int objectShader=Assets.LoadShader(Path.of("assets/shaders/coral/object.vert"),Path.of("assets/shaders/coral/object.frag"));
        shader=Assets.getShader(Assets.LoadShader(Path.of("assets/shaders/coral/reef.vert"),Path.of("assets/shaders/coral/reef.frag")));
        String[] names={"branching","bushy","fan"};
        for(int i=0;i<3;i++)textures[i]=Assets.getTexture(Assets.LoadTexture(Path.of("assets/textures/coral/"+names[i]+".png")));
        CoralMesh floor=ReefGround.make();float[] v=floor.vertices();int[] idx=floor.indices();
        var vb=memAllocFloat(v.length).put(v).flip();var ib=memAllocInt(idx.length).put(idx).flip();
        int floorMesh;
        try{floorMesh=Assets.LoadMesh(new Mesh(vb,ib),"REEF_FLOOR");}finally{memFree(vb);memFree(ib);}
        int white=Assets.LoadSolidTexture(255,255,255,255);
        Material material=Assets.createMaterial(new MaterialEntry().add("SHADER",objectShader).add("COLOUR",white));material.coralTint=new Vector3f(.35f,.42f,.36f);
        Scene.createObject(floorMesh,material);
        Scene.lightDirection=new Vector3f(1,2,1);glClearColor(.035f,.13f,.17f,1);
        player.collisions=collisions;interaction.follow(player.transform());
        regenerate(seed,density);
    }
    public void regenerate(long newSeed,int newDensity){
        List<ReefLayout.Colony> layout=ReefLayout.generate(newSeed,newDensity);
        Pool[] next=new Pool[ReefLayout.VARIANTS];
        try{
            for(int i=0;i<next.length;i++){
                CoralMesh full=CoralGenerator.generate(ReefLayout.parameters(newSeed,i,false));
                CoralMesh low=CoralGenerator.generate(ReefLayout.parameters(newSeed,i,true));
                float height=0,radial=0;float[] vertices=full.vertices();
                for(int k=0;k<vertices.length;k+=CoralMesh.STRIDE){height=Math.max(height,vertices[k+1]);radial=Math.max(radial,vertices[k]*vertices[k]+vertices[k+2]*vertices[k+2]);}
                next[i]=new Pool(new InstancedMesh(full),new InstancedMesh(low),TrunkCollision.fromMesh(full),height,
                    (float)Math.sqrt(radial+height*height*.25f),memAllocFloat(layout.size()*11),memAllocFloat(layout.size()*11));
            }
        }catch(RuntimeException e){for(Pool p:next)if(p!=null)p.close();throw e;}
        if(pools!=null)for(Pool p:pools)p.close();pools=next;colonies=layout;seed=newSeed;density=newDensity;
        growthElapsed=0;lastCollisionGrowth=-1;updateGrowth(0);
        glfwSetWindowTitle(glfwGetCurrentContext(),"Sunken Castle | "+count()+" corals | WASD swim | H overview | R regenerate | Seed "+seed);
        System.out.println("Reef seed="+seed+" density="+density+" colonies="+count()+" variants="+pools.length);
    }
    private void updateGrowth(double dt){
        growthElapsed=Math.min(1.8,growthElapsed+Math.max(0,dt));float t=(float)(growthElapsed/1.8);
        growth=.015f+.985f*t*t*(3-2*t);
        if(growth!=lastCollisionGrowth){
            List<TrunkCollision.Capsule> roots=new ArrayList<>(count()*7);
            for(var c:colonies)TrunkCollision.place(pools[c.variant()].trunk(),c.position(),c.scale()*growth,c.yaw(),roots);
            collisions.setBodies(roots);player.resolveCollision();lastCollisionGrowth=growth;
        }
    }
    private boolean pressed(int key){if(!Input.IsKeyDown(key)){held.remove(key);return false;}return held.add(key);}
    @Override protected void Update(double dt){
        int requested=-1;boolean randomize=false;
        if(!ImGui.getIO().getWantCaptureKeyboard()){
            if(pressed(GLFW_KEY_R))randomize=true;
            if(pressed(GLFW_KEY_1))requested=0;if(pressed(GLFW_KEY_2))requested=1;if(pressed(GLFW_KEY_3))requested=2;
            if(pressed(GLFW_KEY_F))flow=!flow;if(pressed(GLFW_KEY_I))react=!react;
            if(pressed(GLFW_KEY_V))wireframe=!wireframe;
            if(pressed(GLFW_KEY_G)){growthElapsed=0;lastCollisionGrowth=-1;}
            if(pressed(GLFW_KEY_ESCAPE))glfwSetWindowShouldClose(glfwGetCurrentContext(),true);
        }
        if(showUI){
            ImGui.setNextWindowPos(12,12,ImGuiCond.FirstUseEver);ImGui.setNextWindowSize(310,225,ImGuiCond.FirstUseEver);
            ImGui.begin("Coral field");
            ImGui.text(count()+" colonies across 48 x 48 units");ImGui.text("Seed: "+seed);
            if(ImGui.button("Sparse"))requested=0;ImGui.sameLine();if(ImGui.button("Normal"))requested=1;ImGui.sameLine();if(ImGui.button("Dense"))requested=2;
            if(ImGui.button("New random field"))randomize=true;
            if(ImGui.button(flow?"Water: on":"Water: off"))flow=!flow;ImGui.sameLine();if(ImGui.button(react?"Reaction: on":"Reaction: off"))react=!react;
            ImGui.text("WASD swim | Q/E down/up | Shift faster");ImGui.text("H overview | R new field | P reset");
            ImGui.text("Right-drag orbit | Scroll zoom");ImGui.text("Main trunks solid; small branches passable");ImGui.end();
        }
        if(randomize||requested>=0)regenerate(randomize?new Random().nextLong(1_000_000_000L):seed,requested>=0?requested:density);
        updateGrowth(dt);interaction.update(dt);
    }
    @Override protected void LateUpdate(){
        var camera=Scene.cameraTransform.position;var matrix=Scene.computeViewportMatrix();var frustum=new FrustumIntersection(matrix);
        for(Pool p:pools){p.nearInstances().clear();p.farInstances().clear();}
        visibleCount=nearCount=farCount=drawCalls=triangles=0;
        for(var c:colonies){
            Pool p=pools[c.variant()];float distance=camera.distance(c.x(),p.height()*c.scale()*.5f,c.z());
            float radius=p.radius()*c.scale()+.65f;
            if(distance>85 || !frustum.testSphere(c.x(),p.height()*c.scale()*growth*.5f,c.z(),radius))continue;
            boolean near=distance<19;FloatBuffer buffer=near?p.nearInstances():p.farInstances();
            buffer.put(c.x()).put(0).put(c.z()).put(c.scale()).put((float)Math.cos(c.yaw())).put((float)Math.sin(c.yaw())).put(c.phase()).put(0).put(c.r()).put(c.g()).put(c.b());
            visibleCount++;if(near)nearCount++;else farCount++;
        }
        glPolygonMode(GL_FRONT_AND_BACK,wireframe?GL_LINE:GL_FILL);shader.use();
        shader.setMatrix4("CameraMatrix",matrix);shader.setVector3("ViewPosition",camera);shader.setVector3("LightDirection",Scene.lightDirection);
        shader.setFloat("Time",Scene.animationTime());shader.setFloat("Growth",growth);shader.setFloat("SwayStrength",flow?.22f:0);shader.setFloat("SwayPhase",0);
        shader.setVector3("PlayerPosition",interaction.position);shader.setVector3("WakePosition",interaction.wakePosition);shader.setVector3("PlayerVelocity",interaction.velocity);
        shader.setFloat("PlayerRadius",1.6f);shader.setFloat("PlayerHeight",1.6f);shader.setFloat("InteractionStrength",react?.65f:0);
        for(int i=0;i<pools.length;i++){
            Pool p=pools[i];shader.setTexture("Albedo",textures[i%3],4);shader.setFloat("SwayHeight",p.height());shader.setFloat("SwayBaseHeight",p.trunk().top()+.002f);
            p.nearInstances().flip();p.farInstances().flip();
            shader.setFloat("NormalDetail",1);int n=p.near().drawInstances(p.nearInstances());if(n>0){drawCalls++;triangles+=n*p.near().triangleCount();}
            shader.setFloat("NormalDetail",0);int f=p.far().drawInstances(p.farInstances());if(f>0){drawCalls++;triangles+=f*p.far().triangleCount();}
        }
        glPolygonMode(GL_FRONT_AND_BACK,GL_FILL);
    }
    @Override protected void Destroy(){if(pools!=null){for(Pool p:pools)p.close();pools=null;}}
}
