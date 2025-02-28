/*
 * Ship.java
 *
 * This is a model class representing ships. It is slightly more complex than
 * Photon, in that is it not limited to just setters and getters -- it has an
 * update loop. This is because we want to process some complicated code
 * regarding turning drift. As this code makes no reference to any other object
 * other than the ship, it is safe to put it in this class properly.
 *
 * This is a "passive" model.  It does not access the methods or fields of any
 * other Model class.  It also does not store any other model object as a field.
 * This allows us to keep the models from being tightly coupled.
 *
 * This decision makes a slight complication when it comes to photon ownership.
 * We do not want a ship to be hit by its own photons, so each photon needs an
 * owner. But we do not want to store a ship object in an photon. We solve this
 * problem by storing the ship id in the photon instead. Id numbers are a
 * classic technique for decoupling classes from one another.
 *
 * Based on original AI Game Lab by Yi Xu and Don Holden, 2007
 *
 * Author: Walker M. White, Cristian Zaloj
 * LibGDX version, 1/24/2015
 */
package edu.cornell.cis3152.ailab;

import java.util.Random;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.utils.JsonValue;

/**
 * A model class representing a ship.
 *
 * This class has more interesting methods other than setters and getters, but
 * is still a passive model.
 */
public class Ship {
	// SHARED CONSTANTS FOR SHIP HANDLING
	private static final float FULL_CIRCLE = 360.0f;
	private static final float HALF_CIRCLE = 180.0f;

    /** The ship diameter in pixels */
    private static float SHIP_SIZE;
	/** How far forward this ship can move in a single turn */
	private static float MOVE_SPEED;
	/** How much this ship can turn in a single turn */
	private static float TURN_SPEED;
	/** How long the ship must wait until it can fire its weapon again */
	private static int COOLDOWN;
	/** How far a doomed ship will fall (in z-coords) each turn */
	private static float FALL_RATE;
	/** The minimal z-coordinate before a ship will fall to death */
	private static float MIN_FALL;
	/** The z-coordinate at which the ship is removed from the screen */
	private static float MAX_FALL;
	/** Constants for animating the fall */
    private static float FALL_OFFSET;
    private static float FALL_X_SKEW;
    private static float FALL_Z_SKEW;
	/** For animating turning movement */
	private static float RAND_FACTOR;
	private static float RAND_OFFSET;
	/** How close to the center of the tile we need to be to stop drifting */
	private static float DRIFT_TOLER;
	/** How fast we drift to the tile center when paused */
	private static float DRIFT_SPEED;
	/** The damping factor for deceleration */
	private static float SPEED_DAMP;
	/** An epsilon for float comparison */
	private static float EPSILON;

	/** Static random number generator shared across all ships */
	private static final Random random = new Random();

	// Instance Attributes
	/** A unique identifier; used to decouple classes. */
	private int id;
	/** Ship position */
	private Vector2 position;
	/** Ship velocity */
	private Vector2 velocity;
	/** The current angle of orientation (in degrees) */
	private float angle;
	/** The angle we want to go to (for momentum) */
	private float dstAng;

	/** Boolean to track if we are dead yet */
	private boolean isAlive;
	/** Track how far we have fallen (as a dying ship) */
	public float fallAmount; // How far we've fallen (as a dead ship)
	/** The number of frames until we can fire again */
	private int fireCool;

    /** The current 3d rotation of this ship */
    private Quaternion rotation;
    /** A temporary quaternion for computation */
    private Quaternion tempQuat;

    public static enum SHIPTYPE {
        PLAYER, // part of chain
        COMPANION, // waiting to be collected
        ENEMY,
    }

    private SHIPTYPE type;

    /**
     * Sets the simulation constants for a game ship.
     *
     * The purpose of this method is to allow us to define our constants (many
     * of which represent game physics). As these constants are the same for
     * all ships, we make them all static.
     *
     * @param constants The JSON defining the ship constants
     */
    public static void setConstants(JsonValue constants) {
        SHIP_SIZE = constants.getFloat("diameter");
	    MOVE_SPEED = constants.get("speed").getFloat( 0 );
	    TURN_SPEED = constants.get("speed").getFloat( 1 );
	    COOLDOWN = constants.getInt("cooldown");
	    FALL_RATE = constants.getFloat("fall rate");
	    MIN_FALL  = constants.get("fall range").getFloat( 0 );
	    MAX_FALL  = constants.get("fall range").getFloat( 1 );
        FALL_OFFSET = constants.getFloat("fall offset");
        FALL_X_SKEW = constants.get("fall skew").getFloat( 0 );
        FALL_Z_SKEW = constants.get("fall skew").getFloat( 2 );

	    RAND_FACTOR = constants.getFloat("random factor");
	    RAND_OFFSET = RAND_FACTOR/2.0f;
	    DRIFT_TOLER = constants.get("drift").getFloat( 0 );
	    DRIFT_SPEED = constants.get("drift").getFloat( 1 );
	    SPEED_DAMP  = constants.getInt("damping");
        EPSILON = constants.getInt("epsilon");
    }

	/**
	 * Creates ship # id at the given position.
	 *
	 * @param id The unique ship id
	 * @param x The initial x-coordinate of the ship
	 * @param y The initial y-coordinate of the ship
	 */
	public Ship(int id, float x, float y) {
		this.id = id;

		position = new Vector2(x,y);
		velocity = new Vector2();
		angle  = 90.0f;
		dstAng = 90.0f;

		isAlive = true;
		fallAmount = 0;
		fireCool = 0;

		rotation = new Quaternion();
		tempQuat = new Quaternion();
	}

    /**
     * Get type of ship
     */
    public SHIPTYPE getShipType() {
        return type;
    }

    /**
     * Set type of ship
     */
    public void setShipType(SHIPTYPE type) {
        this.type = type;
    }

    /**
	 * Returns the unique ship id number
	 *
	 * @return the unique ship id number
	 */
	public int getId() {
		return id;
	}

	/**
	 * Returns the size of this ship
	 *
	 */
	public float getSize() {
	    return SHIP_SIZE;
	}

	/**
	 * Returns the x-coordinate of the ship position
	 *
	 * @return the x-coordinate of the ship position
	 */
	public float getX() {
		return position.x;
	}

	/**
	 * Sets the x-coordinate of the ship position
	 *
	 * @param value the x-coordinate of the ship position
	 */
	public void setX(float value) {
		position.x = value;
	}

	/**
	 * Returns the y-coordinate of the ship position
	 *
	 * @return the y-coordinate of the ship position
	 */
	public float getY() {
		return position.y;
	}

	/**
	 * Sets the y-coordinate of the ship position
	 *
	 * @param value the y-coordinate of the ship position
	 */
	public void setY(float value) {
		position.y = value;
	}

	/**
	 * Returns the position of this ship.
	 *
	 * This method returns a reference to the underlying ship position vector.
	 * Changes to this object will change the position of the ship.
	 *
	 * @return the position of this ship.
	 */
	public Vector2 getPosition() {
		return position;
	}

	/**
	 * Returns the x-coordinate of the ship velocity
	 *
	 * @return the x-coordinate of the ship velocity
	 */
	public float getVX() {
		return velocity.x;
	}

	/**
	 * Sets the x-coordinate of the ship velocity
	 *
	 * @param value the x-coordinate of the ship velocity
	 */
	public void setVX(float value) {
		velocity.x = value;
	}

	/**
	 * Returns the y-coordinate of the ship velocity
	 *
	 * @return the y-coordinate of the ship velocity
	 */
	public float getVY() {
		return velocity.y;
	}

	/**
	 * Sets the y-coordinate of the ship velocity
	 *
	 * @param value the y-coordinate of the ship velocity
	 */
	public void setVY(float value) {
		velocity.y = value;
	}

	/**
	 * Returns the velocity of this ship.
	 *
	 * This method returns a reference to the underlying ship velocity vector.
	 * Changes to this object will change the velocity of the ship.
	 *
	 * @return the velocity of this ship.
	 */
	public Vector2 getVelocity() {
		return velocity;
	}

	/**
	 * Returns the current facing angle of the ship
	 *j
	 * This value cannot be changed externally.  It can only
	 * be changed by update()
	 *
	 * @return the current facing angle of the ship
	 */
	public float getAngle() {
		return angle;
	}

	/**
	 * Returns whether or not the ship is alive.
	 *
	 * A ship is dead once it has fallen past MAX_FALL_AMOUNT. A dead ship cannot be
	 * targeted, involved in collisions, or drawn.  For all intents and purposes, it
	 * does not exist.
	 *
	 * @return whether or not the ship is alive
	 */
	public boolean isAlive() {
		return isAlive;
	}

	/**
	 * Returns whether or not the ship is active.
	 *
	 * An inactive ship is one that is either dead or dying.  A ship that has started
	 * to fall, but has not fallen past MAX_FALL_AMOUNT is inactive but not dead.
	 * Inactive ships are drawn but cannot be targeted or involved in collisions.
	 * They are just eye-candy at that point.
	 *
	 * @return whether or not the ship is active
	 */
	public boolean isActive() {
		return isAlive && fallAmount == 0;
	}

	/**
	 * Returns whether or not the ship is falling.
	 *
	 * A ship that has started to fall, but has not fallen past MAX_FALL_AMOUNT is
	 * inactive but not dead.  Inactive ships are drawn but cannot be targeted or
	 * involved in collisions. They are just eye-candy at that point.
	 *
	 * @return whether or not the ship is falling
	 */
	public boolean isFalling() {
		return fallAmount > 0;
	}

	/**
	 * Returns how far the ship has fallen so far.
	 *
	 * This value is used in the tumble animation as the ship falls off-screen.
	 *
	 * @return how far the ship has fallen so far.
	 */
	public float getFallDistance() {
		return fallAmount + FALL_OFFSET;
	}

	/**
	 * Pushes the ship so that it starts to fall.
	 *
	 * This method will not destroy the ship immediately. It will tumble and
	 * fall offscreen before dying. To instantly kill a ship, use setAlive().
	 */
	public void destroy() {
		fallAmount = MIN_FALL;
	}

	/**
	 * Sets whether or not the ship is alive.
	 *
	 * This method should only be used if we need to kill the ship immediately.
	 * The preferred method to get rid of a ship is destroy().
	 *
	 * @param value whether or not the ship is alive.
	 */
	public void setAlive(boolean value) {
		isAlive = value;
	}

	/**
	 * Returns whether or not this ship can fire its weapon.
	 *
	 * @return whether or not this ship can fire its weapon.
	 */
	public boolean canFire() {
		return fireCool <= 0;
	}

	/**
	 * Returns the 3d rotation of this ship
	 *
	 * This method will always recompute the rotation.
	 *
	 * @return the 3d rotation of this ship
	 */
	public Quaternion getRotation() {
        rotation.set( Vector3.X, (float) Math.toDegrees( FALL_X_SKEW * fallAmount ) );
        tempQuat.set( Vector3.Z, angle + (float) Math.toDegrees( FALL_Z_SKEW * fallAmount ) );
        rotation.mul( tempQuat );
        tempQuat.set( Vector3.Y, 180 );
        rotation.mul( tempQuat );
        tempQuat.set( Vector3.X, 90 );
        rotation.mul( tempQuat );
        return rotation;
	}

	/**
	 * Updates this ship position (and weapons fire) according to the control code.
	 *
	 * This method updates the velocity and the weapon status, but it does not change
	 * the position or create photons.  The later interact with other objects (position
	 * can cause collisions) so they are processed in a controller.  Method in a model
	 * object should only modify state of that specific object and no others.
	 *
	 * @param controlCode The movement controlCode (from InputController).
	 */
	public void update(int controlCode) {
		// If we are dead do nothing.
		if (!isAlive) {
			return;
		} else if (fallAmount >= MIN_FALL) {
			// Animate the fall, but quit
			fallAmount += FALL_RATE;
			isAlive = !(fallAmount > MAX_FALL);
			return;
		}

		// Determine how we are moving.
		boolean movingLeft  = (controlCode & InputController.CONTROL_MOVE_LEFT) != 0;
		boolean movingRight = (controlCode & InputController.CONTROL_MOVE_RIGHT) != 0;
		boolean movingUp    = (controlCode & InputController.CONTROL_MOVE_UP) != 0;
		boolean movingDown  = (controlCode & InputController.CONTROL_MOVE_DOWN) != 0;

		// Process movement command.
		if (movingLeft) {
			dstAng = 0.0f;
			velocity.x = -MOVE_SPEED;
			velocity.y = 0;
		} else if (movingRight) {
			dstAng = 180.0f;
			velocity.x = MOVE_SPEED;
			velocity.y = 0;
		} else if (movingUp) {
			dstAng = 90.0f;
			velocity.y = -MOVE_SPEED;
			velocity.x = 0;
		} else if (movingDown) {
			dstAng = 270.0f;
			velocity.y = MOVE_SPEED;
			velocity.x = 0;
		} else {
			// NOT MOVING, SO SLOW DOWN
			velocity.x *= SPEED_DAMP;
			velocity.y *= SPEED_DAMP;
			if (Math.abs(velocity.x) < EPSILON) {
				velocity.x = 0.0f;
			}
			if (Math.abs(velocity.y) < EPSILON) {
				velocity.y = 0.0f;
			}
		}

		updateRotation();
	}

	/**
	 * Updates the ship rotation so that angle gets closer to dstAng
	 *
	 * This allows us to have some delay in rotation, even though movement is
	 * always left-right/up-down.  The result is a much smoother animation.
	 */
	private void updateRotation() {
		// Change angle to get closer to dstAng
		if (angle > dstAng) {
			float angleDifference = angle - dstAng;
			if (angleDifference <= TURN_SPEED) {
				angle = dstAng;
			} else {
				if (angleDifference == HALF_CIRCLE) {
					angleDifference += random.nextFloat()*RAND_FACTOR-RAND_OFFSET;
				}
				if (angleDifference > HALF_CIRCLE) {
					angle += TURN_SPEED;
				} else {
					angle -= TURN_SPEED;
				}
			}
			velocity.setZero();
		} else if (angle < dstAng) {
			float angleDifference = dstAng - angle;
			if (angleDifference <= TURN_SPEED) {
				angle = dstAng;
			} else {
				if (angleDifference == HALF_CIRCLE) {
					angleDifference += random.nextFloat()*RAND_FACTOR-RAND_OFFSET;
				}
				if (angleDifference > HALF_CIRCLE) {
					angle -= TURN_SPEED;
				} else {
					angle += TURN_SPEED;
				}
			}
			velocity.setZero();
		}

		// Get rid of overspins.
		while (angle > FULL_CIRCLE) {
			angle -= FULL_CIRCLE;
		}
		while (angle < 0.0f) {
			angle += FULL_CIRCLE;
		}
	}

	/**
	 * Resets the cool down of the ship weapon.
	 *
	 * If flag is true, the weapon will cool down by one animation frame.
	 * Otherwise it will reset to its maximum cooldown.
	 *
	 * @param flag whether to cooldown or reset
	 */
	public void coolDown(boolean flag) {
		if (flag && fireCool > 0) {
            fireCool--;
		} else if (!flag) {
            fireCool = COOLDOWN;
		}
	}


    /**
	 * Nudges the ship back to the center of a tile if it is not moving.
	 *
	 * @param ship The ship to adjust
	 */
	public void adjustForDrift(float x, float y) {
		// Drift to line up vertically with the grid.
		if (velocity.x == 0.0f) {
			if (x < -DRIFT_TOLER) {
				position.x += DRIFT_SPEED;
			} else if (x > DRIFT_TOLER) {
				position.x -= DRIFT_SPEED;
			}
		}

		// Drift to line up horizontally with the grid.
		if (velocity.y == 0.0f) {
			if (y < -DRIFT_TOLER) {
				position.y += DRIFT_SPEED;
			} else if (y > DRIFT_TOLER) {
				position.y -= DRIFT_SPEED;
			}
		}
	}
}
