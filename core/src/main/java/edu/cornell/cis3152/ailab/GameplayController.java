/*
 * GameplayController.java
 *
 * This class processes the primary gameplay.  It reads from either the player
 * input or the AI controller to determine the move for each ship. It then
 * updates the velocity and desired angle for each ship, as well as whether or
 * not it will fire.
 *
 * HOWEVER, this class does not actually do anything that would change the
 * animation state of each ship.  It does not move a ship, or turn it. That is
 * the purpose of the CollisionController.  Our reason for separating these two
 * has to do with the sense-think-act cycle which we will learn about in class.
 *
 * Based on the original AI Game Lab by Yi Xu and Don Holden, 2007
 *
 * @author:  Walker M. White, Cristian Zaloj
 * @version: 1/24/2025
 */
package edu.cornell.cis3152.ailab;

import java.util.Random;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;

/**
 * Class to process AI and player input
 *
 * As a major subcontroller, this class must have a reference to all the models.
 * We encapsulate all these in the GameSession class. See the documentation of
 * that class for why.
 */
public class GameplayController {
    /** How close to the center of the tile we need to be to stop drifting */
    private static final float DRIFT_TOLER = 1.0f;
    /** How fast we drift to the tile center when paused */
    private static final float DRIFT_SPEED = 0.325f;

    /** Reference to the game session */
    private GameSession session;

    /** List of all the input (both player and AI) controllers */
    protected InputController[] controls;
    /** The current player action */
    protected int playerAction;

    /** Random number generator for state initialization */
    private Random random;

    /** Firing angle for normal tiles */
    private static final float NORMAL_ANGLE = 90.0f;
    /** Firing angle for power tiles */
    private static final float POWER_ANGLE = 45.0f;
    /** Half of a circle (for radian conversions) */
    private static final float HALF_CIRCLE = 180.0f;

    /**
     * Creates a GameplayController for the given models.
     *
     * @param session   The game session
     */
    public GameplayController(GameSession session) {
        this.session = session;

        initShipPositions();
        ShipList ships = session.getShips();
        controls = new InputController[ships.size()];
        controls[0] = new PlayerController();
        for(int ii = 1; ii < ships.size(); ii++) {
            if (ships.get(ii).getShipType() == Ship.SHIPTYPE.COMPANION) {
                controls[ii] = new CompanionController(ii, session);
            } else {
                controls[ii] = new AIController(ii, session);
            }
        }
    }

    /**
     * Initializes the ships to new random locations.
     *
     * The player is always at the center of the board.
     */
    private void initShipPositions() {
        Board board = session.getBoard();
        ShipList ships = session.getShips();

        // Set the player position
        float px = board.boardToScreen(board.getWidth() / 2);
        float py = board.boardToScreen(board.getHeight() / 2);
        ships.get(0).getPosition().set(px,py);

        // Create a list of available AI positions
        Vector2[] positions = new Vector2[board.getWidth() * board.getHeight() - 1];
        int ii = 0;
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                // Leave the center space for the player.
                if (x != board.getWidth() / 2 || y != board.getHeight() / 2) {
                    positions[ii++] = new Vector2(x, y);
                }
            }
        }

        // Shuffle positions
        random = new Random();
        Vector2 rTemp = new Vector2();
        for (ii = 0; ii < positions.length; ii++) {
            int jj = random.nextInt(positions.length);
            rTemp.set(positions[ii]);
            positions[ii].set(positions[jj]);
            positions[jj].set(rTemp);
        }

        // Assign positions
        for (ii = 1; ii < ships.size(); ii++) {
            Vector2 tile = positions[ii-1];
            float sx = board.boardToScreen((int)tile.x);
            float sy = board.boardToScreen((int)tile.y);
            ships.get(ii).getPosition().set(sx, sy);
        }
    }

    public int getPlayerSelection() {
        return controls[0].getSelection();
    }

    /**
     * Invokes the controller for this ship.
     *
     * Movement actions are determined, but not committed (e.g. the velocity
     * is updated, but not the position). New weapon firing action is processed
     * but photon collisions are not.
     */
    public void update() {
        // Adjust for drift and remove dead ships
        Board board = session.getBoard();
        ShipList ships = session.getShips();
        for (Ship s : ships) {
            float x =  board.centerOffset(s.getX());
            float y =  board.centerOffset(s.getY());
            s.adjustForDrift(x,y);
            checkForDeath(s);

            if (!s.isFalling() && controls[s.getId()] != null) {
                int action = controls[s.getId()].getAction();
                s.update(action);
                if (s.canFire() && s.getShipType() == Ship.SHIPTYPE.PLAYER) {
                    fireWeapon(s);
                } else {
                    s.coolDown(true);
                }

            } else {
                s.update(InputController.CONTROL_NO_ACTION);
            }
        }

        session.getBoard().update();
        session.getPhotons().update();
    }

    /**
     * Determines if a ship is on a destroyed tile.
     *
     * If so, the ship is killed.
     *
     * @param ship The ship to check
     */
    private void checkForDeath(Ship ship) {
        Board board = session.getBoard();

        // Nothing to do if ship is already dead.
        if (!ship.isActive()) {
            return;
        }

        // Get the tile for the ship
        int tx = board.screenToBoard(ship.getX());
        int ty = board.screenToBoard(ship.getY());

        if (!board.inBounds(tx,ty) || board.isDestroyedAt(tx, ty)) {

            // We use a manager to ensure only one sound at a time
            // Otherwise, the game gets LOUD
            SoundEffectManager sounds = SoundEffectManager.getInstance();
            SoundEffect fire = session.getSound( "fall" );
            sounds.play(  "fall", fire);

            ship.destroy();
        }
    }

    /**
     * Creates photons and udates the ship's cooldown.
     *
     * Firing a weapon requires access to all other models, so we have factored
     * this behavior out of the Ship into the GameplayController.
     */
    private void fireWeapon(Ship ship) {
        Board board = session.getBoard();
        PhotonPool photons = session.getPhotons();

        // Determine the number of photons to create.
        float x = ship.getX();
        float y = ship.getY();

        boolean isPower = board.isPowerTileAtScreen(x,y);
        float angPlus = isPower ? POWER_ANGLE : NORMAL_ANGLE;
        for (float fireAngle = 0.0f; fireAngle < 360.0f; fireAngle += angPlus) {
            float vx = (float) Math.cos(fireAngle * Math.PI / HALF_CIRCLE);
            float vy = (float) Math.sin(fireAngle * Math.PI / HALF_CIRCLE);

            photons.allocate(ship.getId(), x, y, vx, vy, isPower);
        }

        // We use a manager to ensure only one sound per ship
        // Otherwise, the game gets LOUD
        SoundEffectManager sounds = SoundEffectManager.getInstance();
        SoundEffect fire = session.getSound( "fire" );
        sounds.play(  "fire"+ship.getId(), fire);

        // Reset the firing cooldown.
        ship.coolDown(false);
    }
}
