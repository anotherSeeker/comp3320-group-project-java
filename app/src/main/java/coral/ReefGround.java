package coral;
import org.joml.Vector3f;
/** A plain reference floor, not a replacement underwater level. */
public final class ReefGround {
 public static CoralMesh make(){
  CoralMesh.Builder b=new CoralMesh.Builder();
  Vector3f n=new Vector3f(0,1,0),t=new Vector3f(1,0,0),bit=new Vector3f(0,0,-1);
  b.vertex(new Vector3f(-25,-.08f,-25),n,0,0,t,bit);
  b.vertex(new Vector3f(-25,-.08f,25),n,0,25,t,bit);
  b.vertex(new Vector3f(25,-.08f,25),n,25,25,t,bit);
  b.vertex(new Vector3f(25,-.08f,-25),n,25,0,t,bit);
  b.triangle(0,1,2);b.triangle(0,2,3);return b.build();
 }
}
