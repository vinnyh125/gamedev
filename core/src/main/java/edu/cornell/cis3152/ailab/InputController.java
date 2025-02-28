/*
 * InputController.java
 *
 * This class provides a uniform interface for the player and the AI. The player
 * controls with an input device, while and AI player controls with AI algorithms.
 * This is a very standard way to set up AI control in a game that can have
 * either AI or human players.
 *
 * Based on original AI Game Lab by Yi Xu and Don Holden, 2007
 *
 * @author:  Walker M. White, Cristian Zaloj
 * @version: 1/24/2025
 */
package edu.cornell.cis3152.ailab;

/**
 * Interface for either player or AI control
 */
public interface InputController {

	// Constants for the control codes
	// We would normally use an enum here, but Java enums do not bitmask nicely
	/** Do not do anything */
	public static final int CONTROL_NO_ACTION  = 0x00;
	/** Move the ship to the left */
	public static final int CONTROL_MOVE_LEFT  = 0x01;
	/** Move the ship to the right */
	public static final int CONTROL_MOVE_RIGHT = 0x02;
	/** Move the ship to the up */
	public static final int CONTROL_MOVE_UP    = 0x04;
	/** Move the ship to the down */
	public static final int CONTROL_MOVE_DOWN  = 0x08;
	/** Fire the ship weapon */
	public static final int CONTROL_FIRE 	   = 0x10;

    // Constants for game state selection
    /** Do not do anything */
    public static final int SELECT_NONE        = 0x00;
    /** Start the game (player ONLY)  */
    public static final int SELECT_BEGIN 	   = 0x01;
    /** Reset the game (player ONLY) */
    public static final int SELECT_RESET 	   = 0x02;
    /** Quit the game (player ONLY) */
    public static final int SELECT_EXIT 	   = 0x03;

	/**
	 * Return the action of this ship (but do not process)
	 *
	 * The value returned must be some bitmasked combination of the static ints
	 * for ship control.  For example, if the ship moves left and fires, it
	 * returns CONTROL_MOVE_LEFT | CONTROL_FIRE
	 *
	 * @return the action of this ship
	 */
	public int getAction();

    /**
     * Returns a non-ship selection of the player
     *
     * The value returned should be a bitmasked combination of the state ints
     * for controlling the game state (e.g. restarting the game, exiting the
     * program). This method is not implemented by AI opponents.
     *
     * @return a non-ship selection of the player
     */
    public int getSelection();
}

