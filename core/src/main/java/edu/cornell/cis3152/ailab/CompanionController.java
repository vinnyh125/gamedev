package edu.cornell.cis3152.ailab;

import java.io.FileOutputStream;

public class CompanionController implements InputController {
    private int id;
    private int move;
    private long ticks;
    private GameSession session;
    private Ship ship;
    private Ship target; // ship before this in the chain
    private FSMState state;

    private static enum FSMState {
        /** The ship is spawned and waiting to be collected */
        SPAWN,
        /** The ship is in the chain and is following the ship before it */
        FOLLOW,
    }

    public CompanionController(int id) {
        this.id = id;
        move  = CONTROL_NO_ACTION;
        ticks = 0;
    }

    public CompanionController(int id, GameSession session) {
        this(id);
        setSession(session);
    }

    public void setSession(GameSession session) {
        this.session = session;
        ship = session.getShips().get(id);
    }

    @Override
    public int getAction() {
        // Increment the number of ticks.
        ticks++;

        // Do not need to rework ourselves every frame. Just every 10 ticks.
        if ((ship.getId() + ticks) % 10 == 0) {
//            switch (state) {
//            case SPAWN:
//                move = CONTROL_NO_ACTION;
//                break;
//            case FOLLOW:
//                move = CONTROL_MOVE_RIGHT;
//                break;
//            }
            move = CONTROL_NO_ACTION;
        }

        return move;
    }

    @Override
    public int getSelection() {
        return SELECT_NONE;
    }


    private void changeStateIfApplicable() {
        // Next state depends on current state.
        switch (state) {
            case SPAWN:
                // stay here until collected
                if (ship.getShipType() == Ship.SHIPTYPE.PLAYER) {
                    state = FSMState.FOLLOW;
                }
                break;
            case FOLLOW:
                // stay here until dies
                break;
            default:
                assert (false);
                state = FSMState.SPAWN; // If debugging is off
                break;
        }
    }
}
