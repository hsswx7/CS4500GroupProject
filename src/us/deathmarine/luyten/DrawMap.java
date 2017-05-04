package us.deathmarine.luyten;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.IntBuffer;

import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_SWIZZLE_RGBA;


public class DrawMap {
    private double ms_per_day = 100; // How many ms to spend on each day's data point
    private double data_points[][]; // River depth measurements--first axis is the time of the measurement, second axis has an entry for each sample location.
    private double start_time = 0; // The time we began playing the animation
    private boolean now_playing = true; // Playing or paused.

    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(1);
    private TextureData mapTexture;

    // Public API
    public DrawMap() {
        data_points = new double[][]{{15, 15, 15}};
    }

    public void play() {
        start_time = System.currentTimeMillis();
        now_playing = true;
    }

    public void stop() {
        now_playing = false;
    }

    //Lets reset know when simulation has stoped
    public boolean getPlayingStatus(){
        return now_playing;
    }

    public void setDataPoints(double new_data[][]) {
        data_points = new_data;
    }


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
            mapTexture = TextureIO.newTextureData(gl.getGLProfile(), texture, false, TextureIO.GIF);
            int level = 0;

            gl.glGenTextures(1, textureName);

            gl.glBindTexture(GL_TEXTURE_2D, textureName.get(0));
            {
                gl.glTexImage2D(GL_TEXTURE_2D,
                        level,
                        mapTexture.getInternalFormat(),
                        mapTexture.getWidth(), mapTexture.getHeight(),
                        mapTexture.getBorder(),
                        mapTexture.getPixelFormat(), mapTexture.getPixelType(),
                        mapTexture.getBuffer());

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

    protected void render(GL2 gl, int canvaswidth, int canvasheight) {
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        gl.glLoadIdentity();

        // Draw a quad textured with the map image.
        float textureHeight = mapTexture.getHeight();
        float textureWidth = mapTexture.getWidth();
        float aspect = textureHeight / textureWidth;
        float height = canvasheight;
        float width = height / aspect;

        gl.glBindTexture(GL_TEXTURE_2D, textureName.get(0));
        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glColor3f(1, 1, 1);
        gl.glBegin(GL2.GL_POLYGON);
        gl.glTexCoord2f(0, 0);
        gl.glVertex2f(0, 0);
        gl.glTexCoord2f(1, 0);
        gl.glVertex2f(width, 0);
        gl.glTexCoord2f(1, 1);
        gl.glVertex2f(width, height);
        gl.glTexCoord2f(0, 1);
        gl.glVertex2f(0, height);
        gl.glEnd();
        gl.glBindTexture(GL_TEXTURE_2D, 0);

        // Draw river
        double elapsed = System.currentTimeMillis() - start_time;
        int current_frame_index = Math.min(data_points.length - 1,
                (int) (elapsed / ms_per_day));
        // We're going to interpolate in time between samples
        double tween = (elapsed - current_frame_index * ms_per_day) / ms_per_day;
        double depths[] = data_points[current_frame_index];
        double next_depths[] = data_points[Math.min(current_frame_index + 1, data_points.length - 1)];

        // The river's course
        int course[][] = {{298, 2}, {291, 7}, {284, 10}, {281, 17}, {278, 24}, {277, 33}, {275, 40}, {273, 48}, {270, 56}, {264, 62}, {258, 66}, {252, 70}, {246, 74}, {237, 75}, {228, 76}, {222, 81}, {217, 87}, {213, 93}, {209, 100}, {204, 105}, {198, 109}, {193, 114}, {189, 120}, {183, 127}, {178, 133}, {172, 137}, {165, 140}, {159, 145}, {154, 150}, {150, 157}, {148, 165}, {145, 173}, {141, 180}, {138, 188}, {134, 196}, {129, 202}, {125, 209}, {120, 216}, {114, 222}, {109, 227}, {102, 230}, {94, 233}, {87, 236}, {81, 240}, {74, 243}, {68, 247}, {61, 250}, {56, 255}, {52, 261}, {47, 267}, {41, 271}, {35, 276}, {28, 279}, {23, 284}, {19, 290}, {17, 299}, {16, 308}, {14, 317}, {8, 322}, {4, 328}, {5, 337}, {9, 343}, {9, 344}, {9, 345}, {9, 346}, {9, 347}, {9, 348}, {9, 349}, {9, 350}};

        gl.glLineWidth(20);
        gl.glBegin(GL2.GL_LINE_STRIP);
        for (int i = 0; i < course.length; ++i) {
            int station = (int) Math.floor(3 * (double) i / course.length);
            double shade = ((1 - tween) * depths[station] + tween * next_depths[station]) / 30;
            double redgreen = Math.max(0, 1.75 * shade - 1);
            double blue = shade * 2;
            gl.glColor3d(redgreen, redgreen, blue);
            gl.glVertex2d(width * (course[i][0]) / textureWidth,
                    height * (course[i][1]) / textureHeight);
        }
        gl.glEnd();
    }
}
