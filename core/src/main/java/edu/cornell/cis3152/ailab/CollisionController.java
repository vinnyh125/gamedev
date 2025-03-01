/*
 * CollisionController.java
 *
 * Unless you are making a point-and-click adventure game, every single game is
 * going to need some sort of collision detection.  In a later lab, we will see
 * how to do this with a physics engine. For now, we use custom physics.
 *
 * This class is an example of subcontroller. A lot of this functionality could
 * go into GameEngine (which is the primary controller). However, we have
 * factored it out into a separate class because it makes sense as a
 * self-contained subsystem.  Unlike Lab 1, this controller stores a lot
 * information as fields. This is to keep us from having to pass the same
 * parameters over and over again.
 *
 * Based on the original AI Game Lab by Yi Xu and Don Holden, 2007
 *
 * @author:  Walker M. White, Cristian Zaloj
 * @version: 1/24/2025
 */
package edu.cornell.cis3152.ailab;

import java.util.Random;
import com.badlogic.gdx.math.*;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;


/**
 * Class to handle basic collisions in the game.
 *
 * This is the simplest of physics engines.  In later labs, we will see how to
 * work with more interesting engines.
 *
 * As a major subcontroller, this class must have a reference to all the models.
 * We encapsulate all these in the GameSession class. See the documentation of
 * that class for why.
 */
public class CollisionController {
    /** Amount to nudge a ship (in either direction) */
    private static final float NUDGE_AMOUNT = 0.1f;
    /** Number of times to attempt a nudge */
    private static final int NUDGE_LIMIT = 100;

    /** Reference to the game session */
    private GameSession session;

    /** Cache attribute for calculations */
    private Vector2 tmp;
    /** Random number generator for nudging */
    private Random random;

    /**
     * Creates a CollisionController for the given models.
     *
     * @param b The game board
     * @param s The list of ships
     * @param p The active photons
     */
    public CollisionController(GameSession session) {
        this.session = session;
        tmp = new Vector2();
        random = new Random();
    }

    /**
     * Updates all of the ships and photons, moving them forward.
     *
     * This is part of the collision phase, because movement can cause collisions!
     * That is why we do not combine this with the gameply controller. When we
     * study the sense-think-act cycle later, we will see another reason for
     * this design.
     */
    public void update() {
        ShipList ships = session.getShips();
        PhotonPool photons = session.getPhotons();

        // Move live ships when possible.
        for (Ship s : ships) {
            if (s.isActive()) {
                moveIfSafe(s);
            }
        }


       for (Ship s1 : ships) {
           // Test collisions between player and ships.
           if (s1.getShipType() == Ship.SHIPTYPE.PLAYER) {
               for (Ship s2 : ships) {
                   if (s2.getShipType() != Ship.SHIPTYPE.PLAYER) {
                       checkForCollision(s1, s2);
                   }
               }
           }
       }

       for (Ship s1 : ships) {
           // Test collisions between enemy and photons
           if (s1.getShipType() == Ship.SHIPTYPE.ENEMY) {
               for (Photon p : photons) {
                   checkForCollision(s1, p);
               }
           }
       }
    }

    /**
     * Moves the ship according to its velocity
     *
     * This only does something if the new position is safe. Otherwise, this
     * ship stays in place.
     *
     * @param ship The ship to move.
     */
    private void moveIfSafe(Ship ship) {
        Board board = session.getBoard();
        tmp.set(ship.getX(), ship.getY());
        boolean safeBefore = board.isSafeAtScreen(tmp.x, tmp.y);

        // Test add velocity
        tmp.add(ship.getVX(), ship.getVY());
        boolean safeAfter  = board.isSafeAtScreen(tmp.x, tmp.y);

        if (!(safeBefore && !safeAfter)) {
            ship.getPosition().set(tmp);
        }
    }

    /**
     * Keeps nudging the ship until a safe location is found.
     *
     * @param ship The ship to nudge.
     */
    private void safeNudge(Ship ship) {
        Board board = session.getBoard();
        int i = 0;
        int tileX, tileY;
        float xNudge, yNudge;
        do {
            xNudge = random.nextFloat() * 2 * NUDGE_AMOUNT - NUDGE_AMOUNT;
            yNudge = random.nextFloat() * 2 * NUDGE_AMOUNT - NUDGE_AMOUNT;
            ship.setX(ship.getX()+xNudge);
            ship.setY(ship.getY()+yNudge);
            tileX = board.screenToBoard(ship.getX());
            tileY = board.screenToBoard(ship.getY());
        } while (!board.isSafeAt(tileX, tileY) && ++i < NUDGE_LIMIT);
    }


    /**
     * Processes collisions between ships, causing them to either kill player (enemy and player) or add companion
     * (player and companion).
     *
     *
     * @param player The player
     * @param ship The ship
     */
    private void checkForCollision(Ship player, Ship ship) {
        Board board = session.getBoard();

        // Do nothing if either ship is off the board.
        if (!player.isActive() || !ship.isActive()) {
            return;
        }

        // Get the tiles for each ship
        int s1x = board.screenToBoard(player.getX());
        int s1y = board.screenToBoard(player.getY());
        int s2x = board.screenToBoard(ship.getX());
        int s2y = board.screenToBoard(ship.getY());

        // If the two ships occupy the same tile,
        if (s1x == s2x && s1y == s2y) {
            // If the ship is an enemy, kill the player
            if (ship.getShipType() == Ship.SHIPTYPE.ENEMY) {
                session.getPlayer().removeCompanion(player.getId());
            }
            // If the ship is a companion and has enough coins, add the companion to the chain
            else if (ship.getShipType() == Ship.SHIPTYPE.COMPANION && session.getPlayer().getCoins() >= ship.getCost()) {
                session.getPlayer().addCompanion(ship);
                session.getPlayer().setCoins(session.getPlayer().getCoins() - ship.getCost());
            }
        }
    }

    /**
     * Processes collisions between a ship and a photon
     *
     * Recall that when a photon collides with a ship, the ship is destroyed.
     *
     * @param enemy   The enemy
     * @param photon The photon
     */
    private void checkForCollision(Ship enemy, Photon photon) {
        Board board = session.getBoard();
        PhotonPool photons = session.getPhotons();

        // Do nothing if ship is off the board.
        if (!enemy.isActive()) {
            return;
        } else if (enemy.getId() == photon.getSource()) {
            // Our own photon; do nothing.
            return;
        }

        // Get the tiles for ship and photon
        int sx = board.screenToBoard(enemy.getX());
        int sy = board.screenToBoard(enemy.getY());
        int px = board.screenToBoard(photon.getX());
        int py = board.screenToBoard(photon.getY());

        // If the ship and photon occupy the same tile,
        if (sx == px && sy == py) {
            // Have the minion "die" and respawn a new one (just change its position to random corner of board)
            enemy.setX(Math.random() > 0.5 ? 0 : board.boardToScreen(board.getWidth()-1));
            enemy.setY(Math.random() > 0.5 ? 0 : board.boardToScreen(board.getHeight()-1));

            session.getPlayer().setCoins(session.getPlayer().getCoins() + 1);
            photons.destroy(photon);

            // We use a manager to ensure only one sound at a time
            // Otherwise, the game gets LOUD
            SoundEffectManager sounds = SoundEffectManager.getInstance();
            SoundEffect fire = session.getSound( "bump" );
            sounds.play( "bump", fire);
        }
    }

    /**
     * Returns the manhattan distance between two points
     *
     * @return the manhattan distance between two points
     */
    private float manhattan(float x0, float y0, float x1, float y1) {
        return Math.abs(x1 - x0) + Math.abs(y1 - y0);
    }

}
