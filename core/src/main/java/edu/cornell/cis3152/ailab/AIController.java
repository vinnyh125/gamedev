/*
 * AIController.java
 *
 * This class is an inplementation of InputController that uses AI and
 * pathfinding algorithms to determine the choice of input.
 *
 * NOTE: This is the file that you need to modify. You should not need to
 * modify any other files (though you may need to read Board.java heavily).
 *
 * Based on the original AI Game Lab by Yi Xu and Don Holden, 2007
 *
 * @author:  Walker M. White, Cristian Zaloj
 * @version: 1/24/2025
 */
 package edu.cornell.cis3152.ailab;

import com.badlogic.gdx.math.Vector2;

import java.util.*;

/**
 * InputController corresponding to AI control.
 *
 * REMEMBER: As an implementation of InputController you will have access to
 * the control code constants in that interface.  You will want to use them.
 */
public class AIController implements InputController {
    // Constants for chase algorithms
    /** How close a target must be for us to chase it */
    private static final int CHASE_DIST  = 9;
    /** How close a target must be for us to attack it */
    private static final int ATTACK_DIST = 4;

    // Instance Attributes
    /** The ship identifier for this AI controller */
    private int id;
    /** The ship controlled by this AI */
    private Ship ship;
    /** The target ship (to chase or attack). */
    private Ship target;
    /** The state of the game (needed by the AI) */
    private GameSession session;
    /** The ship's next action (may include firing). */
    private int move; // A ControlCode
    /** The number of ticks since we started this controller */
    private long ticks;

    /**
     * Creates an AIController for the ship with the given id.
     *
     * The board and lists of ships should be set later.
     *
     * @param id The unique ship identifier
     */
    public AIController(int id) {
        this.id = id;
        move  = CONTROL_NO_ACTION;
        ticks = 0;
        target = null;
    }

    /**
     * Creates an AIController for the ship with the given id.
     *
     * @param id The unique ship identifier
     * @param session Contains all models for the game including game board and ships
     */
    public AIController(int id, GameSession session) {
        this(id);
        setSession(session);
        target = session.getPlayer().getPlayerHead();
    }

    /**
     * Updates the AI controller to use the given game session
     *
     * @param session   The new session to use
     */
    public void setSession(GameSession session) {
        this.session = session;
        ship = session.getShips().get(id);
    }

    /**
     * Returns a non-ship selection of the player
     *
     * The value returned should be a bitmasked combination of the state ints
     * for controlling the game state (e.g. restarting the game, exiting the
     * program). This method is not implemented by AI opponents.
     *
     * @return a non-ship selection of the player
     */
    public int getSelection() {
        return SELECT_NONE;
    }

    /**
     * Returns the action selected by this InputController
     *
     * The returned int is a bit-vector of more than one possible input option.
     * This is why we do not use an enumeration of Control Codes; Java does not
     & (nicely) provide bitwise operation support for enums.
     *
     * This function tests the environment and uses the FSM to choose the next
     * action of the ship. This function SHOULD NOT need to be modified. It
     * just contains code that drives the functions that you need to implement.
     *
     * @return the action selected by this InputController
     */
    public int getAction() {
        // Increment the number of ticks.
        ticks++;

        // Do not need to rework ourselves every frame. Just every 10 ticks.
        if ((ship.getId() + ticks) % 10 == 0) {
            // Pathfinding
            markGoalTiles();
            move = getMoveAlongPathToGoalTile();
        }

        return move;
    }

    // Pathfinding Code (MODIFY ALL THE FOLLOWING METHODS)

    /**
     * Mark all desirable tiles to move to.
     *
     * This method implements pathfinding through the use of goal tiles. It
     * searches for all desirable tiles to move to (there may be more than
     * one), and marks each one as a goal. Then, the pathfinding method
     * getMoveAlongPathToGoalTile() moves the ship towards the closest one.
     *
     * POSTCONDITION: There is guaranteed to be at least one goal tile
     * when completed.
     */
    private void markGoalTiles() {
        // Clear out previous pathfinding data.
        Board board = session.getBoard();
        board.clearMarks();

        if (session.getPlayer().isAlive()) {
            target = session.getPlayer().getPlayerHead();
            int targetX = session.getBoard().screenToBoard(target.getX());
            int targetY = session.getBoard().screenToBoard(target.getY());
            if (board.isSafeAt(targetX, targetY)) {
                board.setGoal(targetX, targetY);
            }
        }
    }

    private class PositionAndDirection {
        public int x, y, direction;
        public PositionAndDirection(int x, int y, int direction) {
            this.x = x;
            this.y = y;
            this.direction = direction;
        }
    }

    /**
      * Returns a movement direction that moves towards a goal tile.
      *
      * This is one of the longest parts of the assignment. Implement
      * breadth-first search (from 2110) to find the best goal tile to move to.
      * However, just return the movement direction for the next step, not the
      * entire path.
     *
     * The value returned should be a control code. See PlayerController
     * for more information on how to use control codes.
     *
      * @return a movement direction that moves towards a goal tile.
      */
    private int getMoveAlongPathToGoalTile() {
        //#region PUT YOUR CODE HERE
        Board board = session.getBoard();
        // bfs starting from ship location and then return starting direction for the first tile we run into
        int shipX = session.getBoard().screenToBoard(ship.getX());
        int shipY = session.getBoard().screenToBoard(ship.getY());

        if (board.isGoal(shipX, shipY)) {
            return CONTROL_NO_ACTION; // we're already on a goal tile
        }

        Queue<PositionAndDirection> queue = new LinkedList<>();
        if (board.isSafeAt(shipX - 1, shipY)) {
            queue.add(new PositionAndDirection(shipX - 1, shipY, CONTROL_MOVE_LEFT));
            board.setVisited(shipX - 1, shipY);
        }
        if (board.isSafeAt(shipX + 1, shipY)) {
            queue.add(new PositionAndDirection(shipX + 1, shipY, CONTROL_MOVE_RIGHT));
            board.setVisited(shipX + 1, shipY);
        }
        if (board.isSafeAt(shipX, shipY - 1)) {
            queue.add(new PositionAndDirection(shipX, shipY - 1, CONTROL_MOVE_UP));
            board.setVisited(shipX, shipY - 1);
        }
        if (board.isSafeAt(shipX, shipY + 1)) {
            queue.add(new PositionAndDirection(shipX, shipY + 1, CONTROL_MOVE_DOWN));
            board.setVisited(shipX, shipY + 1);
        }

        while (!queue.isEmpty()) {
            PositionAndDirection cur = queue.poll();
            if (board.isGoal(cur.x, cur.y)) {
                // System.out.println("ship" + ship.getId() + ": (" + shipX + ", " + shipY + ") -> (" + cur.x + ", " + cur.y + ")");
                return cur.direction;
            }

            int[] dx = {cur.x, cur.x, cur.x + 1, cur.x - 1};
            int[] dy = {cur.y + 1, cur.y - 1, cur.y, cur.y};
            for (int i = 0; i < 4; i++) {
                if (board.isSafeAt(dx[i], dy[i]) && !board.isVisited(dx[i], dy[i])) {
                    PositionAndDirection next = new PositionAndDirection(dx[i], dy[i], cur.direction);
                    board.setVisited(dx[i], dy[i]);
                    queue.add(next);
                }
            }
        }

        return CONTROL_NO_ACTION;
        //#endregion
    }

    // Add any auxiliary methods or data structures here
    //#region PUT YOUR CODE HERE

    //#endregion
}
