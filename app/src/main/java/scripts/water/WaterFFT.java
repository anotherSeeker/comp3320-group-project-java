package scripts.water;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.joml.Vector2f;
import org.joml.Vector3f;
import java.util.ArrayList;
import static org.lwjgl.opengl.GL30.GL_RG;
import static org.lwjgl.opengl.GL30.GL_RGB;

import engine.AssetManager;

// import static org.lwjgl.opengl.GL11.*;
// import static org.lwjgl.opengl.GL15.*;
// import static org.lwjgl.opengl.GL20.*;
// import static org.lwjgl.opengl.GL30.*;

public class WaterFFT 
{
    //physical parameters for the ocean
    public static double gravity = 9.81;
    public static double waterDensity = 1000;
    public static double surfaceTensionCoefficient = 0.074;

    //I picked depth wholly at random, windspeed was an loose example stated in the wikiwaves page on jonswap kind of as a throwaway line but that's good enough for me
    public static double depth = 50; //H or sometimes D: depth of the water, I believe technical constraints mean we can't have a different one for each point on the surface? we'd need to recalculate a new map for every part of the ocean which is kinda infeasible unless I'm horrifically mistaken
    public static double windSpeed = 20; //U: windspeed, I believe meters per second?
    public static double fetch = 2000; //F: distance to shore from the direction that the wind is blowing from
    public static final double GAMMA_J = 3.3; //gamma: the "peak enhancement factor", from the JONSWAP spectrum, do not ask me I am not an oceanographer

    //acerola/jump trajectory used 4 waves of different length to cascade and hide the tiling properties of sinewaves over large spaces. 
    //acerola used 1024*1024 textures but we're gonna start smaller and scale up
    public static int textureSize = 512;

    //the wavenumbers, kx, ky, and angular frequency / dispersion relation omega
    public static ArrayList<Vector3f> waveVectorList;

    //texture ids for sending to shaders
    public static int noiseTexId;
    public static int waveVectorTexId;
    public static int h0_texId;

    //bytebuffers for creating our textures
    public static ByteBuffer noiseBytes;
    public static FloatBuffer noiseFloats;

    public static ByteBuffer displacementBuffer;
    public static FloatBuffer displacementFloats;

    public static ByteBuffer wavevectorBuffer;
    public static FloatBuffer wavevectorFloats;
    

    public WaterFFT() {}

    public static boolean noiseSetup()
    {
        genGaussianNoise2D();

        noiseTexId = AssetManager.get().createTexture(noiseBytes, textureSize, GL_RG, "gaussianNoise_Water");


        

        h0_texId = AssetManager.get().createTexture(displacementBuffer, textureSize, GL_RGB, "h0_displacement_Water");

        waveVectorTexId = AssetManager.get().createTexture(wavevectorBuffer, textureSize, GL_RG, "h0_displacement_Water");
        return true;
    }



    public static boolean precomputeWavenumbers_kx_ky_omega()
    {
        //thinking about our wavevectors and angular freq / dispersion relation / omega

        //iterate over - (textureSize/2) -> textureSize/2 in both x, and y
        //the i, k values are then our wave vectors and we can plug those values of k in to precalculate our omega value
        //then once we got that we write those three to the rgb of a second texture map, 
        //should be pretty easy now that I've thought it through and am not tired ?
        
        //wait I think we reverse that and iterate from positive to negative, 
        //pos "1,1" should be top left and pos "-1,-1" bottom right so yeah

        waveVectorList = new ArrayList<Vector3f>();

        for (int ky = textureSize/2; ky > -textureSize/2; ky--)
        {
            for (int kx = textureSize/2; kx > -textureSize/2; kx-- )
            {
                //[kx, ky] is a vector pointing in a direction based on the points of the texture, hopefully, unless I'm just that dumb
                //we want them to all have different magnitudes based on distance from the center as well so I'm pretty sure that should be fine?

                double dispersion_omega = dispersionRelation_omega(kx, ky);

                //rgb channels for kx, ky, omega, we still need to calculate h0, but we're precaching these in a texture
                waveVectorList.add(new Vector3f((float) kx, (float) ky, (float) dispersion_omega));
            }
        }
        //put in texture dum dum

        return true;
    }

    public static FloatBuffer genGaussianNoise2D()
    {
        System.out.println("Generating 2D Gaussian Noise values for our waves");

        java.util.Random r = new java.util.Random();
        float noiseX;
        float noiseY;

        //width*height*2*4 for r and g channels as floats (4 bytes)
        int numEntries = textureSize*textureSize*2;
        int bufferSize = numEntries*4;

        noiseBytes = ByteBuffer.allocateDirect(bufferSize);
        noiseFloats = noiseBytes.asFloatBuffer();

        for (int i = 0; i < textureSize*textureSize; i++)
        {
            //values between -1 and 1 normally distributed
            noiseX = (float) (r.nextGaussian());
            noiseY = (float) (r.nextGaussian());

            noiseFloats.put(noiseX);
            noiseFloats.put(noiseY);

            //System.out.println(noiseX + " : " + noiseY);
        }

        noiseBytes.rewind();

        System.out.println("Finished Generating 2D Gaussian Noise values");

        return noiseFloats;
    }

    public void computeDisplacementMap_h0(Vector2f gaussianNoise, float kx, float ky)
    {
        double omega = dispersionRelation_omega(kx, ky);
        double theta = windWaveDirection_Theta(kx, ky);

        double xi_x = (double) gaussianNoise.x;
        double xi_y = (double) gaussianNoise.y;

        //we pregen our wave numbers and angular frequency and we'll pass them into this individually
        double h_0_r   = (1.0/(Math.sqrt(2))) * xi_x * Math.sqrt(doJONSWAP(omega) * doDirectionalSpread(omega, theta));
        double h_0_i = (1.0/(Math.sqrt(2))) * xi_y * Math.sqrt(doJONSWAP(omega) * doDirectionalSpread(omega, theta));
    }

    public static double dispersionRelation_omega(float kx, float ky)
    {
        double magnitude = waveVectorMagnitude_k(kx, ky);

        double aFreq = Math.sqrt(gravity * magnitude * Math.tanh(magnitude * depth));
        //includes variables for capilliary waves? might look better?
        //double aFreq = Math.sqrt((gravity * magnitude + (surfaceTensionCoefficient / waterDensity) * Math.pow(magnitude, 3))* Math.tanh(magnitude * depth));

        return aFreq;
    }

    public static double waveVectorMagnitude_k(Vector2f k)
    {
        return Math.sqrt(k.x*k.x + k.y*k.y);
    }

    public static double waveVectorMagnitude_k(float kx, float ky)
    {
        return Math.sqrt(kx*kx + ky*ky);
    }

    //angle of the wave as compared to the wind direction
    public static double windWaveDirection_Theta(float kx, float ky)
    {
        //theta = arctan(ky/kx)
        return Math.atan(ky/kx);
    }

    //wavenumbers/vectors kx,ky and angular freq omega
    public double doDirectionalSpread(double omega, double theta)
    {
     //decide which spread functions we want to use   

        return 1.0;
    }

    //jonswap is based on a bunch of physical parameters for the ocean that we can tweak and the angular frequency of the specific wave we're looking at
    public static double doJONSWAP(double omega)
    {
        //peak frequency of the wave spectrum
        double omegaPeak = omega_p_J();
        //amplitude of this wave
        double alpha = amplitudeAlpha_J();
        //I believe this is the surface tension term?
        double sigma = sigma_J(omegaPeak, omega);
        //exponent r
        double r = exponent_r_J(omegaPeak, omega, sigma);


        //the actual equation bits
        double a = (alpha*gravity*gravity) / Math.pow(omega, 5);

        double b = Math.exp(-5/4 * Math.pow((omegaPeak/omega), 4));

        double S_jonswap = a*b*Math.pow(GAMMA_J, r);

        return S_jonswap;
    }

    //I believe that this is the surface tension parameter and that jonswap uses a slightly different one for some reason?
    public static double sigma_J(double peakFrequency, double waveFrequency)
    {
        double alpha = 0.09;
        if (peakFrequency >= waveFrequency)
            alpha = 0.07;

        return alpha;
    }

     //JONSWAP amplitude
    public static double amplitudeAlpha_J()
    {
        //as far as I understand 0.076 is the real surface tension of water? perhaps it differs in ocean conditions? again not an oceanographer
        return 0.076 * Math.pow((windSpeed*windSpeed)/(fetch*gravity), 0.22);
    }

    //e^[stuff] part of the jonswap equation you might not have heard but I'm not an oceanographer
    public static double exponent_r_J(double omegaPeak, double omega, double sigma)
    {
        double a = Math.pow(omega - omegaPeak, 2);
        double b = 2*sigma*sigma*omegaPeak*omegaPeak;

        double c = -(a/b);
        double r = Math.exp(c);

        return r;
    }

    //peak angular frequency
    public static double omega_p_J()
    {
        return (22 * ((gravity*gravity)/(windSpeed * fetch)));
    }

    //something that's a start for figuring out the wavevectors
    //MASK SETTINGS
    // var maskThreshold : float = 2.0;

    // //REFERENCES
    // var mask : Texture2D;

    // function Start(){
    //     GenerateTexture();	
    // }

    // function GenerateTexture(){

    //     mask = new Texture2D(texWidth, texHeight, TextureFormat.RGBA32, true);
    //     var maskCenter : Vector2 = new Vector2(texWidth * 0.5, texHeight * 0.5);
        
    //     for(var y : int = 0; y < texture.height; ++y){
    //         for(var x : int = 0; x < texture.width; ++x){

    //             var distFromCenter : float = Vector2.Distance(maskCenter, new Vector2(x, y));
    //             var maskPixel : float = (0.5 - (distFromCenter / texWidth)) * maskThreshold;
    //             mask.SetPixel(x, y, Color(maskPixel, maskPixel, maskPixel, 1.0));
    //         }
    //     }
    //     mask.Apply();
    // }

}