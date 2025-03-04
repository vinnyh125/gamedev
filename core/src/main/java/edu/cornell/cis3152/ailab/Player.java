package edu.cornell.cis3152.ailab;

import java.util.LinkedList;
import java.util.List;

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

        public int getCompanion(int index) {
            return get(index * DELAY);
        }

        public int size() {
            return size;
        }

        public int capacity() {
            return capacity;
        }
    }

    /** Number of instructions to wait before following
     * Also the number of instructions stored per companion
     * */
    private final static int DELAY = 8;
    private final static int MAX_COMPANIONS = 10;

    /** List of the companions */
    private List<Ship> companions;
    private CircularBuffer controlBuffer;

    /** The state of the game */
    private GameSession session;

    private int coins;

    public Player(GameSession session) {
        companions = new LinkedList<>();
        this.session = session;
        this.controlBuffer = new CircularBuffer(MAX_COMPANIONS * DELAY);
        this.coins = 0;
    }

    public boolean isAlive() {
        return !companions.isEmpty();
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getCoins() {
        return coins;
    }

    public void addCompanion(Ship ship) {
        // adds to end of list
        companions.add(ship);
        ship.setShipType(Ship.SHIPTYPE.PLAYER);

        Ship tail = companions.get(companions.size() - 1);
        ship.setX(tail.getX());
        ship.setY(tail.getY());
    }

    public void removeCompanion(int id) {
        for (Ship s : companions) {
            if (s.getId() == id) {
                companions.remove(s);
                s.setAlive(false);
            }
        }

    }

    public Ship getPlayerHead() {
        return companions.get(0);
    }

    public Ship getCompanion(int id) {
        for (Ship s : companions) {
            if (s.getId() == id) {
                return s;
            }
        }
        return null; // not found
    }

    public void update(int controlCode) {
        controlBuffer.add(controlCode);

        // Update head
        for (int i = 0; i < companions.size(); i++) {
            Ship s = companions.get(i);
            int followCode = controlBuffer.getCompanion(i);
            s.update(followCode);
        }
    }
}
