package edu.cornell.cis3152.ailab;

import java.util.LinkedList;
import java.util.List;

public class Player {
    private class CircularBuffer {
        private class PositionAndDirection {
            protected float x;
            protected float y;
            protected int dir;

            public PositionAndDirection(float x, float y, int dir) {
                this.x = x;
                this.y = y;
                this.dir = dir;
            }
        }

        private final PositionAndDirection[] buffer;
        private final int capacity;
        private int head;
        private int size;

        public CircularBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = new PositionAndDirection[capacity];
            this.head = -1;
            this.size = 0;
        }

        public void add(float x, float y, int direction) {
            head = (head + 1) % capacity;
            buffer[head] = new PositionAndDirection(x, y, direction);
            if (size < capacity) {
                size++;
            }
        }

        public PositionAndDirection get(int index) {
            if (index < 0) {
                throw new IndexOutOfBoundsException("Invalid index: " + index);
            }
            int actualIndex = (head - index + capacity) % capacity;
            return buffer[actualIndex];
        }

        public PositionAndDirection getCompanion(int index) {
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
    private final static int DELAY = 12;
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

        CircularBuffer.PositionAndDirection tail = controlBuffer.getCompanion(companions.size() - 1);
        if (tail != null) {
            ship.setX(tail.x);
            ship.setY(tail.y);
        }
    }

    public void removeCompanion(int id) {
        for (Ship s : companions) {
            if (s.getId() == id) {
                companions.remove(s);
                s.setShipType(Ship.SHIPTYPE.COMPANION);
                break;
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
        if (controlCode == 0) {
            // if the head isn't moving no one moves
            for (Ship s : companions) {
                s.update(0);
            }

            return;
        }

        if (this.isAlive()) {
            Ship head = this.getPlayerHead();
            controlBuffer.add(head.getX(), head.getY(), controlCode);
        }

        for (int i = 0; i < companions.size(); i++) {
            Ship s = companions.get(i);
            CircularBuffer.PositionAndDirection prev = controlBuffer.getCompanion(i);
            s.update(prev.dir);

//            System.out.println(Math.pow(s.getX() - prev.x, 2) + Math.pow(s.getY() - prev.y, 2));
            if (Math.pow(s.getX() - prev.x, 2) + Math.pow(s.getY() - prev.y, 2) > 10) {
                s.setX(prev.x);
                s.setY(prev.y);
            }
        }
    }
}
