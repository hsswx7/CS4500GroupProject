package us.deathmarine.luyten;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import java.net.URL;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2.GL_POLYGON;
import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
import static com.jogamp.opengl.GL2ES2.GL_STREAM_DRAW;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL2ES3.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL2ES3.GL_NEAREST;
import static com.jogamp.opengl.GL2ES3.GL_ONE;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE0;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_SWIZZLE_RGBA;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.concurrent.ThreadLocalRandom;

public class DrawMap {
    private double ms_per_day = 300; // How many ms to spend on each day's data point
    private double data_points[][]; // River depth measurements--first axis is the time of the measurement, second axis has an entry for each sample location.
    private double start_time = 0; // The time we began playing the animation
    private boolean now_playing = true; // Playing or paused.
    
    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(1);
    public DrawMap() {
        data_points = new double[10][3];
        for(int i=0; i<data_points.length; ++i)
            for(int s=0; s<3; ++s)
                // Make up a random depth for each day and station
                data_points[i][s]=ThreadLocalRandom.current().nextDouble(5, 25)/25;
        play();
    }
    public void play() {
        start_time=System.currentTimeMillis();
        now_playing=true;
    }
    public void stop() { now_playing=false; }
    protected void setup(GL2 gl, int width, int height) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        // coordinate system origin at lower left with width and height same as the window
        GLU glu = new GLU();
        glu.gluOrtho2D(0.0f, width, height, 0.0f);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glViewport(0, 0, width, height);

        try {
            // The dimensions of the map image must be multiples of four
            URL texture = new URL("file:lagrange.gif");
            //System.out.println((texture==null)?"null texture":"path: "+texture.getPath());

            /* Texture data is an object containing all the relevant information about texture.    */
            TextureData data = TextureIO.newTextureData(gl.getGLProfile(), texture, false, TextureIO.GIF);
            int level = 0;
            
            gl.glGenTextures(1, textureName);
            
            gl.glBindTexture(GL_TEXTURE_2D, textureName.get(0));
            {
                gl.glTexImage2D(GL_TEXTURE_2D,
                                level,
                                data.getInternalFormat(),
                                data.getWidth(), data.getHeight(),
                                data.getBorder(),
                                data.getPixelFormat(), data.getPixelType(),
                                data.getBuffer());
                
                gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
                gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, level);
                
                IntBuffer swizzle = GLBuffers.newDirectIntBuffer(new int[]{GL_RED, GL_GREEN, GL_BLUE, GL_ONE});
                gl.glTexParameterIiv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, swizzle);
            }
            gl.glBindTexture(GL_TEXTURE_2D, 0);
        } catch (MalformedURLException ex) {
             System.out.println("Texture has a malformed URL.");
        } catch (IOException ex) {
            System.out.println("Texture load IO Exception.");
        }
    }

    protected void render(GL2 gl, int width, int height) {
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        gl.glLoadIdentity();

        // Draw a quad textured with the map image.
        gl.glBindTexture(GL_TEXTURE_2D, textureName.get(0));
        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glColor3f(1, 1, 1);
        gl.glBegin(GL2.GL_POLYGON);
        gl.glTexCoord2f(0,0); gl.glVertex2f(0, 0);
        gl.glTexCoord2f(1,0); gl.glVertex2f(width, 0);
        gl.glTexCoord2f(1,1); gl.glVertex2f(width, height);
        gl.glTexCoord2f(0,1); gl.glVertex2f(0, height);
        gl.glEnd();
        gl.glBindTexture(GL_TEXTURE_2D, 0);

        // Draw river
        // The river's course
        double points[][] = {{width*0.9 , height*0.1},
                             {width*0.5 , height*0.4},
                             {width*0.05, height*0.9}};
        // Calculate per-point colors
        double elapsed = System.currentTimeMillis()-start_time;
        int current_frame_index = Math.min(data_points.length-1,
                                           (int)(elapsed/ms_per_day)); 
        double depths[] = data_points[current_frame_index];
        double colors[][] = {{depths[0], depths[0], 1},
                             {depths[1], depths[1], 1},
                             {depths[2], depths[2], 1}};
        gl.glLineWidth(20);
        gl.glBegin(GL2.GL_LINE_STRIP);
        for(int i=0; i<points.length; ++i) {
            gl.glColor3d(colors[i][0],colors[i][1],colors[i][2]);
            gl.glVertex2d(points[i][0],points[i][1]);
        }
        gl.glEnd();
    }
}
