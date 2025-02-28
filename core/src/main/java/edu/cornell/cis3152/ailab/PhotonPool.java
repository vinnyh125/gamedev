/*
 * PhotonPool.java
 *
 * Like this last lab, this class implements another "particle system" that
 * manages the photons fired.  We do this to limit the amount of memory
 * allocation that takes place while the game is running.
 *
 * This class is a lot more complicated, and shows you some of the steps that we
 * have to take to reduce the amount of memory that we use.  In this game, a
 * photon is destroyed on impact. That means the deletion is no longer in the
 * same order as creation, so we can no longer use a pure queue.
 *
 * However, the number of photons that get deleted by collisions is rare, so we
 * can treat them as a special case.  However, we have a dirty bit that marks a
 * photon as deleted, and we reclaim that size immediately.
 *
 * This class implements Iterable<Photon> so that we can use it in for-each
 * loops. BE VERY CAREFUL with java.util. Those classes are notorious for memory
 * allocation. You will note that, to save memory, we have exactly one iterator
 * that we reused over and over again.  This helps with memory, but it means
 * that this object is not even remotely thread-safe.  As there is only one
 * thread in the game-loop, this is acceptable.
 *
 * Based on original AI Game Lab by Yi Xu and Don Holden, 2007
 *
 * @author:  Walker M. White, Cristian Zaloj
 * @version: 1/24/2025
 */
package edu.cornell.cis3152.ailab;

// LIMIT JAVA.UTIL TO THE INTERFACES
import java.util.Iterator;
import java.util.NoSuchElementException;
import com.badlogic.gdx.graphics.*;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.obj.ModelRef;
import edu.cornell.gdiac.graphics.obj.ObjPipeline;

import static com.badlogic.gdx.Gdx.gl;
import static com.badlogic.gdx.Gdx.gl20;

/**
 * This class provides a pool of pre-allocated photons.
 *
 * This object may be used in for-each loops.  However, IT IS NOT THREAD-SAFE.
 * For memory reaasons, this object is backed by a single iterator object that
 * is reset every single time we start a new for-each loop.
 */
public class PhotonPool implements Iterable<Photon> {
	/** Array implementation of a circular queue. */
	protected Photon[] queue;
	/** Index of head element in the queue */
	protected int head;
	/** Index of tail element in the queue */
	protected int tail;
	/** Number of elements currently in the queue */
	protected int size;

    // Photon properties
	/** The maximum number of photon objects we support */
	private int capacity;

    /** Photon drawing constants */
    private float diameter;
    private float offset;
    private float decay;

    // Drawing constants
    /** OBJ Model for photon. Nonstatic so that we can vary color */
    private ModelRef photonModel;
    /** The 3d rotation for our model */
    private Quaternion rotation;

    /** Firing color for normal tiles */
    private Color normalColor = Color.CYAN;
    /** Firing color for power tiles */
    private Color powerColor  = Color.RED;

	/** The alpha amount to multiply by the age */
	private float alphaFactor;
	/** The base alpha value */
	private float alphaOffset;

	// QUEUE DATA STRUCTURES
	/** Custom iterator so we can use this object in for-each loops */
	private PhotonIterator iterator = new PhotonIterator();

	/**
	 * Creates a (pre-allocated) pool of photons with the given capacity.
	 *
	 * The game will never support more than cap photons on screen at a time.
	 *
	 * @param cap The photon capacity
	 */
	public PhotonPool(JsonValue constants) {
	    // Get the constants first
	    capacity = constants.getInt("capacity");
	    offset = constants.getFloat("offset");
        decay  = constants.getFloat("decay");
        diameter  = constants.getFloat("diameter");

	    alphaFactor = constants.get("alpha").getFloat(1);
        alphaOffset = constants.get("alpha").getFloat(0);

        normalColor = ParserUtils.parseColor(constants.get("base color"), Color.WHITE);
        powerColor  = ParserUtils.parseColor(constants.get("power color"), Color.WHITE);

		queue = new Photon[capacity];
        Photon.setConstants(constants);

		head = 0;
        tail = -1;
        size = 0;

        // "Predeclare" all the photons for efficiency
        for (int ii = 0; ii < queue.length; ii++) {
        	queue[ii] = new Photon();
        }
        rotation = new Quaternion();
	}

	/**
	 * Updates all of the photons in this pool.
	 *
	 * This method should be called once per game loop.  It moves photons forward
	 * and deletes any photons that have gotten too old.
	 */
	public void update() {
		for (Photon p : this) {
			p.age();
			p.getPosition().add(p.getVelocity());
		}

		// Remove dead photons
        while (size > 0 && !queue[head].isAlive()) {
        	// As photons are predeclared, all we have to do is move head forward.
            if (!queue[head].isDirty()) { size--; }
            head = ((head + 1) % queue.length);
        }
	}

    /**
     * Returns the textured mesh for a photon.
     *
     * There is only one mesh, as all photons look the same.
     *
     * @return the textured mesh for a photon.
     */
    public ModelRef getPhotonModel() {
        return photonModel;
    }

    /**
     * Sets the textured mesh for a photon.
     *
     * There is only one mesh, as all photons look the same.
     *
     * @param mesh the textured mesh for a photon.
     */
    public void setPhotonModel(ModelRef model) {
        photonModel = model;
    }

    /**
     * Draws the photons to the given canvas.
     *
     * This method draws all of the photons in this pool. It should be the third drawing
     * pass in the GameEngine.
     *
     * @param canvas the drawing context
     */
    public void draw(ObjPipeline pipeline) {
        gl20.glBlendFuncSeparate( gl20.GL_SRC_ALPHA, gl.GL_ONE, gl20.GL_ONE, gl20.GL_ZERO );
        for (Photon p : this) {
            p.getColor().a = (p.getLifeRatio() * alphaFactor + alphaOffset);
            photonModel.getMaterial().setDiffuseTint(p.getColor());

            float angle = (float)Math.toDegrees( Math.atan2( p.getVY(),p.getVX() ) );
            rotation.set( Vector3.Z, angle );

            // Compute world transform
            float scale = diameter+decay*p.getLifeRatio();
            photonModel.setPosition(p.getX(), p.getY(), offset);
            photonModel.setRotation( rotation );
            photonModel.setScale( scale,scale,scale);

            pipeline.draw( photonModel );
        }
    }

	/**
	 * Allocates a new photon with the given attributes.
	 *
	 * The color object is copied.  This method does not store a reference to the
	 * original color object.
	 *
	 * @param ship The ship that fired the photon
	 * @param x  The initial x-coordinate of the photon
	 * @param y  The initial y-coordinate of the photon
	 * @param vx The x-value of the photon velocity
	 * @param vy The y-value of the photon velocity
	 * @param color The photon tint color
	 */
	public void allocate(int id, float x, float y, float vx, float vy, boolean power) {
		// Check if any room in queue.
		// If maximum is reached, remove the oldest photon.
        if (size == queue.length) {
        	head = ((head + 1) % queue.length);
        	size--;
        }

		Color c = power ? powerColor : normalColor;

        // Add a new photon at the end.
        // Already declared, so just initialize.
        tail = ((tail + 1) % queue.length);
        queue[tail].set(id, x, y, vx, vy, c);
        size++;
	}

	/**
	 * Destroys the giving photon, removing it from the pool.
	 *
	 * A destroyed photon reduces the size so that it is not drawn or used in
	 * collisions.  However, the memory is not reclaimed immediately.  It will
	 * only be reclaimed when we reach it in the queue.
	 *
	 * @param p the photon to destroy
	 */
	public void destroy(Photon p) {
		p.destroy();
		size--;
	}

	/**
	 * Returns a photon iterator, satisfying the Iterable interface.
	 *
	 * This method allows us to use this object in for-each loops.
	 *
	 * @return a photon iterator.
	 */
	public Iterator<Photon> iterator() {
		// Take a snapshot of the current state and return iterator.
		iterator.limit = size;
		iterator.pos = head;
		iterator.cnt = 0;
		return iterator;
	}

	/**
	 * Implementation of a custom iterator.
	 *
	 * Iterators are notorious for making new objects all the time.  We make
	 * a custom iterator to cut down on memory allocation.
	 */
	private class PhotonIterator implements Iterator<Photon> {
		/** The current position in the photon queue */
		public int pos = 0;
		/** The number of photons shown already */
		public int cnt = 0;
		/** The number of photons to iterator over (snapshot to allow deletion) */
		public int limit =0;

		/**
		 * Returns true if there are still items left to iterate.
		 *
		 * @return true if there are still items left to iterate
		 */
		public boolean hasNext() {
			return cnt < limit;
		}

		/**
		 * Returns the next photon.
		 *
		 * While it is safe to delete this photon, it is not safe to delete
		 * other photons while this is running.
		 */
		public Photon next() {
			if (cnt > limit) {
				throw new NoSuchElementException();
			}
			int idx = pos;
			do {
				pos = ((pos+1) % queue.length);
			} while (!queue[pos].isAlive());
			cnt++;
			return queue[idx];
		}
	}
}
