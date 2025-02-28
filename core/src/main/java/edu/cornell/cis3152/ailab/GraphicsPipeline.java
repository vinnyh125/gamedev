/*
 * GraphicsPipeline.java
 *
 * This class is used to draw objects to the screen. Unlike the last lab, we do
 * not have a fully 2d pipeline. This time we are combining 2d graphics with 3d
 * graphics. That requires us to be a little more careful with how we drawing
 * objects on the screen.  This class encapsulates this functionality.
*
 * Needless to say, you should not copy this pipeline for your games. As your
 * games will (likely) be 2D, you should not need all the extra features that
 * this class provides.
 *
 * Based on original AI Game Lab by Yi Xu and Don Holden, 2007
 *
 * @author:  Walker M. White, Cristian Zaloj
 * @version: 1/24/2025
 */
package edu.cornell.cis3152.ailab;

import static com.badlogic.gdx.Gdx.gl;
import static com.badlogic.gdx.Gdx.gl20;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.*;

import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;
import edu.cornell.gdiac.graphics.obj.ObjPipeline;

/**
 * Primary view class for the game, abstracting the basic graphics calls.
 *
 * This graphics pipeline combines both 3D and 2D drawing. As this combination is
 * complicated, and we want to hide the details, we make a lot of design decisions
 * in this class that are not ideal.  Do not use this class as an example of good
 * architecture design.
 *
 * One major difference between this class and SpriteBatch is the dependency
 * direction. In SpriteBatch, the graphics pipeline does not depend on any of
 * the game objects. Instead, it is just designed to draw textures. This class,
 * however, explicitly depends on the game models, as we want to customize the
 * drawing for each object.  We attach the game models, and the pipeline iterates
 * through them. This is closer to the scene graph approach that we use in the
 * advanced class.
 */
public class GraphicsPipeline {
	/** The sub-pipeline for 2d objects (background and font) */
	protected SpriteBatch spriteBatch;
	/** The sub-pipeline for three objects */
    public ObjPipeline objPipeline;

    /** The window width */
    private int width;
    /** The window width */
    private int height;
	/** Orthographic camera for the SpriteBatch layer */
	private OrthographicCamera spriteCamera;
	/** Perspective camera for the ObjPipeline layer */
    private PerspectiveCamera objCamera;

	/** The background background image. */
	private Texture background;
	/** Font object for displaying images */
	private BitmapFont displayFont;
	/** Glyph layout to compute the size */
	private TextLayout displayLayout;

    /** A reference to the game session */
    private GameSession session;

	/** The panning factor for the eye, used when the game first loads */
	private float eyepan;

	// Constants only needed locally.
	// We need this pipeline during asset loading, so will hardcode them
	/** Reverse the y-direction so that it is consistent with SpriteBatch */
	private static final Vector3 UP_REVERSED = new Vector3(0,-1,0);
	/** For managing the camera pan interpolation at the start of the game */
	private static final Interpolation.SwingIn SWING_IN = new Interpolation.SwingIn(0.1f);
	/** Distance from the eye to the target */
	private static final float EYE_DIST  = 400.0f;
	/** Field of view for the perspective in degrees */
	private static final float FOV = 80.0f;
	/** Near distance for perspective clipping */
	private static final float NEAR_DIST = 10.0f;
	/** Far distance for perspective clipping */
	private static final float FAR_DIST  = 500.0f;
	/** Horizontal clipping width */
	private static final int   CLIP_X = 500;
	/** Vertical clipping width */
	private static final int   CLIP_Y = 450;
	/** Multiplicative factors for initial camera pan */
	private static final float INIT_TARGET_PAN = 0.1f;
	private static final float INIT_EYE_PAN = 0.05f;
	/** Message while assets are loading */
	private static final String MESSG_LOAD = "Loading...";


	/** Target for Perspective FOV */
	private Vector3 target;
	/** Eye for Perspective FOV */
	private Vector3 eye;

	// CACHE OBJECTS
	/** Projection Matrix */
	private Matrix4 proj;
	/** View Matrix */
	private Matrix4 view;
	/** World Matrix */
	private Matrix4 world;
	/** Temporary Matrix (for Calculations) */
	private Matrix4 tmpMat;

	/** Temporary Vectors */
	private Vector3 tmp0 = new Vector3();
	private Vector3 tmp1 = new Vector3();
	private Vector2 tmp2d;

	/**
	 * Creates a new Graphics pipeline.
	 *
     * This constructor initializes all of the necessary graphics objects. The
     * viewport will be computed from the current window size.
	 */
	public GraphicsPipeline() {
		// Initialize instance attributes
		eyepan  = 0.0f;

		// Create and initialize the two sub-pipelines
		spriteBatch = new SpriteBatch();
		spriteBatch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

        objPipeline = new ObjPipeline();

        resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());

		// Vectors used to move the camera
		eye = new Vector3();
		target = new Vector3();
	}

	/**
     * Disposes any resources that should be garbage collected manually.
     */
    public void dispose() {
		// Dispose what requires a manual deletion.
		spriteBatch.dispose();
    	spriteBatch = null;
        // Currently objPipeline is missing a dispose() for some reason
    }

    /**
	 * Resets the cameras when the game window is resized.
	 *
	 * If you do not call this when the game window is resized, you will get
	 * weird scaling issues.
	 *
	 * @param w The new width
	 * @param h The new height
	 */
    public void resize(int w, int h) {
        width = w;
        height = h;
        if (spriteCamera == null) {
            spriteCamera = new OrthographicCamera();
    	}
        spriteCamera.setToOrtho(false,w,h);
        spriteCamera.update();
		spriteBatch.setProjectionMatrix(spriteCamera.combined);

        if (objCamera == null) {
            objCamera = new PerspectiveCamera(FOV,w,h);
            objCamera.up.set(UP_REVERSED);
            objCamera.near = NEAR_DIST;
            objCamera.far  = FAR_DIST;
            objCamera.update();
        } else {
            objCamera.viewportWidth  = w;
            objCamera.viewportHeight = h;
            objCamera.update();
        }
    }

	/**
	 * Returns the panning factor for the eye value.
	 *
	 * This provides the zoom-in effect at the start of the game. The eyepan is
	 * a value between 0 and 1. When it is 1, the eye is locked into the correct
	 * place to start a game.
	 *
	 * @return The eyepan value in [0,1]
	 */
	public float getEyePan() {
		return eyepan;
	}

    /**
     * Sets the panning factor for the eye value.
     *
	 * This provides the zoom-in effect at the start of the game. The eyepan is
	 * a value between 0 and 1. When it is 1, the eye is locked into the correct
	 * place to start a game.
     *
     * @param value The eyepan value in [0,1]
     */
    public void setEyePan(float value) {
        eyepan = value;
    }

    /**
     * Sets the camera target
     *
     * The target is what the camera is looking at. The camera will always move
     * the eye position to guarantee that its view of the target is perpendicular
     * to the board.
     *
     * Note that x and y are in world space, and are not positions in the board
     * grid.
     *
     * @param x The x-coordinate in world space
     * @param y The y-coordinate in world space
     */
    public void setTarget(float x, float y) {
        target.set(x,y,0);
    }


    /**
	 * Returns the font used to display messages.
	 *
	 * @return the font used to display messages.
	 */
	public BitmapFont getFont() {
		return displayFont;
	}

	/**
	 * Sets the font used to display messages.
	 *
	 * @param font the font used to display messages.
	 */
	public void setFont(BitmapFont font) {
		displayFont = font;
        if (font != null) {
            displayLayout = new TextLayout();
            displayLayout.setAlignment( TextAlign.middleCenter );
            displayLayout.setFont( font );
        }
	}

	/**
	 * Returns the background texture for this pipeline.
	 *
	 * The background fills the screen, and everything is drawn on top of the
	 * background. We do not track the background with the eye position.
	 *
	 * @return the background texture for this pipeline.
	 */
	public Texture getBackground() {
		return background;
	}

	/**
	 * Sets the background texture for this pipeline.
	 *
	 * The background fills the screen, and everything is drawn on top of the
	 * background. We do not track the background with the eye position.
	 *
	 * @param background the background texture for this pipeline.
	 */
	public void setBackground(Texture background) {
		this.background = background;
	}

	/**
	 * Returns the current session attached to the graphics pipeline.
     *
     * This session is the container for all of the OBJ models. This class
     * will pass the OBJ sub-pipeline to these models so that they can
     * manage their own drawing.
     *
     * @return the current session attached to the graphics pipeline.
     */
    public GameSession getSession() {
        return session;
    }

    /**
     * Sets the current session attached to the graphics pipeline.
     *
     * This session is the container for all of the OBJ models. This class
     * will pass the OBJ sub-pipeline to these models so that they can
     * manage their own drawing.
     *
     * @param session   Tge current session attached to the graphics pipeline.
     */
    public void setSession(GameSession session) {
        this.session = session;
    }

	/**
	 * Performs a drawing pass with the camera focused on the target
	 *
	 * If eyepan is not 1, the camera will interpolate between the current
	 * position the target. This method will draw the OBJ models if they
	 * have been assigned. It will also draw the message, if one is provided.
	 * This method is guaranteed to draw the background, no matter what else
	 * is drawn this pass.
	 *
     * @param message   The (optional) message to display
	 */
	public void render(String message) {
		// Clear the screen and depth buffer
		gl20.glClearColor(0, 0, 0, 0);
		gl20.glClearDepthf(1.0f);
		gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        updateCamera();

        if (background != null) {
            spriteBatch.begin();
            spriteBatch.setColor( Color.WHITE );
            spriteBatch.setBlendMode( SpriteBatch.BlendMode.ALPHA_BLEND );
            spriteBatch.draw(background, 0, 0, width, height);
            spriteBatch.end();
        }

        // Process the 3d objects
        if (session != null) {
            objPipeline.begin(objCamera);
            gl20.glEnable( gl20.GL_BLEND );
            gl20.glBlendFuncSeparate( gl20.GL_ONE, gl20.GL_ONE_MINUS_SRC_ALPHA, gl20.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA );

            session.getBoard().draw(objPipeline);
            session.getShips().draw(objPipeline);
            session.getPhotons().draw(objPipeline);
            objPipeline.end();
        }

        // Layout the text
        if (message != null) {
            displayLayout.setText( message );
            displayLayout.layout();

            spriteBatch.begin();
            spriteBatch.setColor( Color.WHITE );
            spriteBatch.setBlendMode( SpriteBatch.BlendMode.ALPHA_BLEND );
            spriteBatch.drawText( displayLayout, width/2, height/2 );
            spriteBatch.end();
        }
	}

    /**
     * Updates the camera to match the current target and eye pan
     */
    private void updateCamera() {
		// Set eye and target positions.
		if (eyepan < 1.0f) {
            tmp0.set(target);
			tmp1.set(tmp0).scl(INIT_TARGET_PAN);
			target.set(tmp1).interpolate(tmp0,eyepan,SWING_IN);

			tmp0.add(0, NEAR_DIST, -EYE_DIST);
			tmp1.set(tmp0).scl(INIT_EYE_PAN);
			eye.set(tmp1).interpolate(tmp0,eyepan,SWING_IN);
		} else {
			eye.set(target).add(0, NEAR_DIST, -EYE_DIST);
		}

        // Position the camera
        objCamera.position.set(eye);
        objCamera.up.set(UP_REVERSED);
        objCamera.lookAt( target );
        objCamera.update();
    }


}
