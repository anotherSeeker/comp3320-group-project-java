package coral;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

/** Geometry/response checks that do not require a graphics context. */
public final class TrunkCollisionCheck {
    private static void require(boolean yes,String message) { if(!yes)throw new IllegalStateException(message); }
    public static void run() {
        TrunkCollision world=new TrunkCollision();
        CoralMesh example=CoralGenerator.makeYCoral();
        TrunkCollision.Profile profile=TrunkCollision.fromMesh(example);
        require(profile.vertexCount()==81 && profile.vertexCount()<example.vertexCount(),"Included child branches in trunk profile");
        List<TrunkCollision.Capsule> bodies=new ArrayList<>();
        TrunkCollision.Profile straight=TrunkCollision.fromMesh(CoralGenerator.makeStraightTube(1.6f,.22f,9,8));
        TrunkCollision.place(straight,new Vector3f(),1,bodies); world.setBodies(bodies);
        Vector3f stopped=world.move(new Vector3f(0,0,2),new Vector3f(0,0,-4));
        require(stopped.z>.4f && world.clearance(stopped)>-.0001f,"Fast movement crossed the trunk");
        Vector3f slide=world.move(new Vector3f(0,0,.6f),new Vector3f(1,0,-.5f));
        require(slide.x>.8f && world.clearance(slide)>-.0001f,"Diagonal movement did not slide around trunk");
        Vector3f above=world.move(new Vector3f(0,2.2f,2),new Vector3f(0,0,-4));
        require(above.distance(0,2.2f,-2)<1e-4f,"Invisible trunk wall above the coral");
        require(world.resolve(new Vector3f(0,1.61f,0)).distance(0,1.61f,0)<1e-5f,"Position just above trunk was pushed sideways");
        Vector3f grazing=world.move(new Vector3f(0,1.59f,2),new Vector3f(0,0,-4));
        require(grazing.z>0 && grazing.y==1.59f,"Partially overlapping feet crossed the trunk");
        Vector3f downward=world.move(new Vector3f(0,3,0),new Vector3f(0,-3,0));
        require(Math.abs(downward.y)<1e-4f && world.clearance(downward)>-.0001f,"Vertical input created a platform instead of staying beside trunk");
        Vector3f inside=world.resolve(new Vector3f());
        require(inside.isFinite() && world.clearance(inside)>-.0001f,"Initial overlap did not resolve");
        // Small branches in the actual Y model are deliberately absent from the collider set.
        bodies.clear(); TrunkCollision.place(profile,new Vector3f(),1,bodies); world.setBodies(bodies);
        Vector3f throughTwigs=world.move(new Vector3f(.8f,1.9f,1),new Vector3f(0,0,-2));
        require(throughTwigs.distance(.8f,1.9f,-1)<1e-4f,"Child branches unexpectedly solid");
        bodies.clear(); TrunkCollision.place(straight,new Vector3f(3.5f,0,0),1,bodies); world.setBodies(bodies);
        require(world.move(new Vector3f(0,0,2),new Vector3f(0,0,-4)).distance(0,0,-2)<1e-4f,"Placement left ghost collider at origin");
        require(world.move(new Vector3f(3.5f,0,2),new Vector3f(0,0,-4)).z>.4f,"Placed trunk did not block");
        bodies.clear(); TrunkCollision.place(straight,new Vector3f(),.1f,bodies); world.setBodies(bodies);
        Vector3f small=world.move(new Vector3f(0,.05f,2),new Vector3f(0,0,-4));
        require(small.z>.34f && small.z<.4f && Math.abs(small.y-.05f)<1e-5f,"Small growing trunk has wrong footprint or lifts player");
        require(world.move(new Vector3f(0,.5f,2),new Vector3f(0,0,-4)).distance(0,.5f,-2)<1e-4f,"Growing trunk blocked above its scaled height");
        bodies.clear();TrunkCollision.place(straight,new Vector3f(3,4,-2),.5f,1.2f,bodies);world.setBodies(bodies);
        require(world.move(new Vector3f(3,1,0),new Vector3f(0,0,-4)).distance(3,1,-4)<1e-4f,"Raised trunk blocked a character entirely below it");
        Vector3f headContact=world.move(new Vector3f(3,2.5f,0),new Vector3f(0,0,-4));
        require(headContact.z>-2 && headContact.y==2.5f,"Character head crossed raised trunk");
        require(world.move(new Vector3f(3,4.81f,0),new Vector3f(0,0,-4)).distance(3,4.81f,-4)<1e-4f,"Transformed trunk blocked above its top");
        // Every generated preset provides just its first tube, and repeated movement stays outside.
        for(long seed=0;seed<30;seed++) for(CoralMesh mesh:CoralSet.generate(seed)) {
            TrunkCollision.Profile trunk=TrunkCollision.fromMesh(mesh);
            require(trunk.vertexCount()<mesh.vertexCount() && trunk.capsules().size()>=6,"Missing/incorrect generated trunk");
            bodies.clear(); TrunkCollision.place(trunk,new Vector3f(),1,bodies); world.setBodies(bodies);
            Vector3f p=new Vector3f(0,0,2);
            for(int i=0;i<80;i++) {
                p=world.move(p,new Vector3f(.002f,0,-.05f));
                require(p.isFinite() && world.clearance(p)>-.001f,"Generated trunk penetration");
                require(p.y==0,"Horizontal movement climbed a generated trunk");
            }
        }
        // Regression: old capsule contacts lifted the player ~0.69 units for seed 7.
        for(CoralMesh mesh:CoralSet.generate(7)) for(float growth:new float[]{.05f,.2f,.5f,1}) {
            bodies.clear(); TrunkCollision.place(TrunkCollision.fromMesh(mesh),new Vector3f(),growth,bodies); world.setBodies(bodies);
            for(float height:new float[]{0,.3f,1,2,4}) {
                Vector3f p=new Vector3f(0,height,2);
                for(int i=0;i<120;i++) {
                    p=world.move(p,new Vector3f(0,0,-.05f));
                    require(p.y==height && world.clearance(p)>-.001f,"No-climb regression at growth "+growth);
                }
            }
            float safeHeight=TrunkCollision.fromMesh(mesh).top()*growth+.01f;
            Vector3f overflight=world.move(new Vector3f(0,safeHeight,2),new Vector3f(0,0,-4));
            require(overflight.distance(0,safeHeight,-2)<1e-4f,"Generated/growing trunk blocked overflight");
            // A new/growing trunk can push sideways, but may never raise or lower the player.
            Vector3f overlap=world.resolve(new Vector3f(0,.4f,0));
            require(overlap.y==.4f && world.clearance(overlap)>-.001f,"Overlap correction changed height");
        }
        world.setBodies(List.of());
        require(world.move(new Vector3f(),new Vector3f(1,2,3)).distance(1,2,3)<1e-4f,"Empty scene blocked movement");
        System.out.println("PASS: trunk-only collision, fast movement, sideways-only sliding, free movement above/below trunks, partial-height contact, passable twigs, overlap recovery, placement/growth, 90 generated trunks.");
    }
}
