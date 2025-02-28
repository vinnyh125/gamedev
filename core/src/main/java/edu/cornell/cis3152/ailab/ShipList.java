/*
 * ShipList.java
 *
 * Like PhotonPool, this class manages a large number of objects in the game,
 * many of which can be deleted. However, since we are never adding new ships
 * to the game -- only taking them away -- this makes this class a lot simpler.
 *
 * Unlike PhotonPool, this method has no update. Updates are different for
 * players and AI ships, so we have embedded that functionality in the
 * subclasses of InputController.
 *
 * This class does have an important similarity to PhotonPool. It implements
 * Iterable<Ship> so that we can use it in for-each loops. BE VERY CAREFUL with
 * java.util.  Those classes are notorious for memory allocation. You will note
 * that, to save memory, we have exactly one iterator that we reused over and
 * over again. This helps with memory, but it means that this object is not even
 * remotely thread-safe. As there is only one thread in the game-loop, this is
 * acceptable.
 *
 * Based on original AI Game Lab by Yi Xu and Don Holden, 2007
 *
 * Author: Walker M. White, Cristian Zaloj
 * LibGDX version, 1/24/2015
 */
package edu.cornell.cis3152.ailab;

//LIMIT JAVA.UTIL TO THE INTERFACES
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.graphics.obj.Material;
import edu.cornell.gdiac.graphics.obj.ModelRef;
import edu.cornell.gdiac.graphics.obj.ObjPipeline;

import static com.badlogic.gdx.Gdx.gl;
import static com.badlogic.gdx.Gdx.gl20;

/**
 * This class provides a list of ships for the game.
 *
 * This object may be used in for-each loops.  However, IT IS NOT THREAD-SAFE.
 * For memory reasons, this object is backed by a single iterator object that
 * is reset every single time we start a new for-each loop.
 */
public class ShipList implements Iterable<Ship> {
	/** OBJ model for enemy. Only need one, since all have same geometry */
    private ModelRef enemyModel;
	/** OBJ model for player. Only need one, since all have same geometry */
    private ModelRef playerModel;
	/** OBJ model for after burner. Nonstatic so that we can vary color */
    private ModelRef fireModel;

	/** The list of ships managed by this object. */
	private Ship[] ships;

	/** The amount of time that has passed since creation (for animation) */
	private float time;
	/** Custom iterator so we can use this object in for-each loops */
	private ShipIterator iterator = new ShipIterator();

	/**
	 * Creates a new ShipList with the given number of ships.
	 *
	 * @param size The number of ships to allocate
	 */
	public ShipList(JsonValue constants) {
        int numEnemies = constants.getInt("num enemies");
        int numCompanions = constants.getInt("num companions");
        Ship.setConstants(constants);
		ships = new Ship[1 + numEnemies + numCompanions];
        ships[0] = new Ship(0, 0, 0);
        ships[0].setShipType(Ship.SHIPTYPE.PLAYER);
        for (int i = 1; i <= numCompanions; i++) {
            ships[i] = new Ship(i, 0, 0);
            ships[i].setShipType(Ship.SHIPTYPE.COMPANION);
        }
        for (int i = numCompanions + 1; i <= numCompanions + numEnemies; i++) {
            ships[i] = new Ship(i, 0, 0);
            ships[i].setShipType(Ship.SHIPTYPE.ENEMY);
        }
	}

	/**
	 * Returns the number of ships in this list
	 *
	 * @return the number of ships in this list
	 */
	public int size() {
		return ships.length;
	}

	/**
	 * Returns the ship for the given (unique) id
	 *
	 * The value given must be between 0 and size-1.
	 *
	 * @return the ship for the given id
	 */
	public Ship get(int id) {
		return ships[id];
	}

	/**
	 * Returns the ship for the player
	 *
	 * @return the ship for the player
	 */
	public Ship getPlayer() {
		return ships[0];
	}

	/**
	 * Returns the number of ships alive at the end of an update.
	 *
	 * @return the number of ships alive at the end of an update.
	 */
	public int numActive() {
		int shipsActive = 0;
		for (Ship s : this) {
			if (s.isActive()) {
				shipsActive++;
			}
		}
		return shipsActive;
	}

	/**
	 * Returns the number of ships alive at the end of an update.
	 *
	 * @return the number of ships alive at the end of an update.
	 */
	public int numAlive() {
		int shipsAlive = 0;
		for (Ship s : this) {
			if (s.isAlive()) {
				shipsAlive++;
			}
		}
		return shipsAlive;
	}

    /**
     * Returns the textured mesh for the player
     *
     * We only need one copy of the mesh, as there is one player.
     *
     * @return the textured mesh for the player
     */
    public ModelRef getPlayerModel() {
        return playerModel;
    }

    /**
     * Sets the textured mesh for the player
     *
     * We only need one copy of the mesh, as there is one player.
     *
     * @param mesh the textured mesh for the player
     */
    public void setPlayerModel(ModelRef model) {
        playerModel = model;
    }

    /**
     * Returns the textured mesh for the enemy
     *
     * We only need one copy of the mesh, as all enemies look the same.
     *
     * @return the textured mesh for the enemy
     */
    public ModelRef getEnemyModel() {
        return enemyModel;
    }

    /**
     * Sets the textured mesh for the enemy
     *
     * We only need one copy of the mesh, as all enemies look the same.
     *
     * @param mesh the textured mesh for the enemy
     */
    public void setEnemyModel(ModelRef model) {
        enemyModel = model;
    }

    /**
     * Returns the textured mesh for the afterburner
     *
     * We only need one copy of the mesh, as all ships use the same afterburner.
     *
     * @return the textured mesh for the afterburner
     */
    public ModelRef getFireModel() {
        return fireModel;
    }

    /**
     * Sets the textured mesh for the afterburner
     *
     * We only need one copy of the mesh, as all ships use the same afterburner.
     *
     * @param mesh the textured mesh for the afterburner
     */
    public void setFireModel(ModelRef model) {
        fireModel = model;
    }

    /**
     * Draws the ships to the given graphics pipeline.
     *
     * This method draws all the ships in this list. As this is a 3d pipeline,
     * order does not matter (we use a depth buffer).
     *
     * @param pipeline  the 3d graphics pipeline
     */
    public void draw(ObjPipeline pipeline) {
        // Increment the animation factor
        time += 0.05f;
        gl20.glBlendFuncSeparate( gl20.GL_ONE, gl.GL_ONE_MINUS_SRC_ALPHA, gl20.GL_SRC_ALPHA, gl20.GL_ONE_MINUS_SRC_ALPHA );

        // Draw the ships
        for (Ship s : this) {
            ModelRef model = (s.getId() == 0 ? playerModel : enemyModel);

            // Rotate the ship into position
            float size = s.getSize();
            Quaternion rot = s.getRotation();
            model.setPosition( s.getX(), s.getY(),  s.getFallDistance());
            model.setRotation( rot );
            model.setScale( size, size, size );

            pipeline.draw( model );
        }

        // I hate having the change this, but this is an artifact of the original models
        gl20.glBlendFuncSeparate( gl20.GL_SRC_ALPHA, gl.GL_ONE, gl20.GL_ONE, gl20.GL_ZERO );

        // Draw the after burners
        for (Ship s : this) {

            Material m = fireModel.getModel().getSurface( 0 ).getMaterial();
            m.getDiffuseTint().a = generateNoise(time % 1.0f)*Math.min(1, s.getVelocity().len2());

            // Rotate the afterburner into position
            float size = s.getSize();
            Quaternion rot = s.getRotation();
            fireModel.setPosition( s.getX(), s.getY(), s.getFallDistance());
            fireModel.setRotation( rot );
            fireModel.setScale( size,size,size);

            pipeline.draw(fireModel);
        }

    }

    /**
	 * Generates the Perlin Noise for the after burner
	 *
	 * Cristian came up with these numbers (and did not document them :( ).  I have
	 * no idea what they mean.
	 *
	 * @param fx seed value for random noise.
	 */
	private float generateNoise(float fx) {
        // TODO: Get rid of these magic numbers
		float noise = (float)(188768.0 * Math.pow(fx, 10));
		noise -= (float)(874256.0 * Math.pow(fx, 9));
		noise += (float)(1701310.0 * Math.pow(fx, 8));
		noise -= (float)(1804590.0 * Math.pow(fx, 7));
		noise += (float)(1130570.0 * Math.pow(fx, 6));
		noise -= (float)(422548.0 * Math.pow(fx, 5));
		noise += (float)(89882.7 * Math.pow(fx, 4));
		noise -= (float)(9425.33 * Math.pow(fx, 3));
		noise += (float)(276.413 * fx * fx);
		noise += (float)(14.3214 * fx);
		return noise;
	}

	/**
	 * Returns a ship iterator, satisfying the Iterable interface.
	 *
	 * This method allows us to use this object in for-each loops.
	 *
	 * @return a ship iterator.
	 */
	public Iterator<Ship> iterator() {
		// Take a snapshot of the current state and return iterator.
		iterator.pos = 0;
		return iterator;
	}

	/**
	 * Implementation of a custom iterator.
	 *
	 * Iterators are notorious for making new objects all the time. We make
	 * a custom iterator to cut down on memory allocation.
	 */
	private class ShipIterator implements Iterator<Ship> {
		/** The current position in the ship list */
		public int pos = 0;

		/**
		 * Returns true if there are still items left to iterate.
		 *
		 * @return true if there are still items left to iterate
		 */
		public boolean hasNext() {
			return pos < ships.length;
		}

		/**
		 * Returns the next ship.
		 *
		 * Dead ships are skipped, but inactive ships are not skipped.
		 */
		public Ship next() {
			if (pos >= ships.length) {
				throw new NoSuchElementException();
			}
			int idx = pos;
			do {
				pos++;
			} while (pos < ships.length && !ships[pos].isAlive());
			return ships[idx];
		}
	}
}
