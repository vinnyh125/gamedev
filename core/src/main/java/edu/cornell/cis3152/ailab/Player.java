package edu.cornell.cis3152.ailab;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Player {
    private class CircularBuffer {
        private final int[] buffer;
        private final int capacity;
        private int head;
        private int size;

        public CircularBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = new int[capacity];
            this.head = -1;
            this.size = 0;
        }

        public void add(int value) {
            head = (head + 1) % capacity;
            buffer[head] = value;
            if (size < capacity) {
                size++;
            }
        }

        public int get(int index) {
            if (index < 0) {
                throw new IndexOutOfBoundsException("Invalid index: " + index);
            }
            int actualIndex = (head - index + capacity) % capacity;
            return buffer[actualIndex];
        }

        public int size() {
            return size;
        }

        public int capacity() {
            return capacity;
        }
    }


    // chain of ships controlled by the player
    private class CompanionState {
        public Ship ship;
        public int controlIdx;

        int curTileX;
        int curTileY;
        int prevTileX;
        int prevTileY;

        public CompanionState(Ship ship, int controlIdx) {
            this.ship = ship;
            this.controlIdx = controlIdx;
            curTileX = session.getBoard().screenToBoard(ship.getX());
            curTileY = session.getBoard().screenToBoard(ship.getY());
            prevTileX = curTileX;
            prevTileY = curTileY;
        }
    }
    /** Number of instructions to wait before following
     * Also the number of instructions stored per companion
     * */
    private final static int DELAY = 8;
    private final static int MAX_COMPANIONS = 10;

    /** List of the companions */
    private List<CompanionState> companionStates;
    private CircularBuffer controlBuffer;

    /** The state of the game */
    private GameSession session;

    private int coins;

    public Player(GameSession session) {
        companionStates = new LinkedList<>();
        this.session = session;
        this.controlBuffer = new CircularBuffer(MAX_COMPANIONS * DELAY);
        this.coins = 0;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getCoins() {
        return coins;
    }

    public void addCompanion(Ship ship) {
        // adds to end of list
        int startIndex = companionStates.size() * DELAY;
        companionStates.add(new CompanionState(ship, startIndex));
        ship.setShipType(Ship.SHIPTYPE.PLAYER);

        CompanionState tail = companionStates.get(companionStates.size() - 1);
        ship.setX(tail.ship.getX());
        ship.setY(tail.ship.getY());
    }

    public void removeCompanion(int id) {
        for (CompanionState cs : companionStates) {
            if (cs.ship.getId() == id) {
//                companionStates.remove(cs);
                cs.ship.setAlive(false);
            }
        }

    }

    public Ship getPlayerHead() {
        return companionStates.get(0).ship;
    }

    public Ship getCompanion(int id) {
        for (CompanionState cs : companionStates) {
            if (cs.ship.getId() == id) {
                return cs.ship;
            }
        }
        return null; // not found
    }

    public void update(int controlCode) {
        controlBuffer.add(controlCode);

        // Update head
        for (int i = 0; i < companionStates.size(); i++) {
            CompanionState cs = companionStates.get(i);
            int followCode = controlBuffer.get(cs.controlIdx);
            cs.ship.update(followCode);

            int actualCurTileX = session.getBoard().screenToBoard(cs.ship.getX());
            int actualCurTileY = session.getBoard().screenToBoard(cs.ship.getY());
            if (actualCurTileX != cs.curTileX || actualCurTileY != cs.curTileY) {
                cs.prevTileX = cs.curTileX;
                cs.prevTileY = cs.curTileY;
                cs.curTileX = actualCurTileX;
                cs.curTileY = actualCurTileY;
            }

            // if a ship is too far away from its desired location, just teleport it
            if (i > 0) {
                CompanionState ps = companionStates.get(i - 1);
                int prevCurTileX = session.getBoard().screenToBoard(ps.ship.getX());
                int prevCurTileY = session.getBoard().screenToBoard(ps.ship.getY());

                if (Math.abs(actualCurTileX-prevCurTileX) > 2 || Math.abs(actualCurTileY-prevCurTileY) > 2) {
                    cs.ship.setX(session.getBoard().boardToScreen(prevCurTileX));
                    cs.ship.setY(session.getBoard().boardToScreen(prevCurTileY));
                }
            }
        }
    }
}
