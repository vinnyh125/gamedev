/*
 * Board.java
 *
 * This class keeps track of all the tiles in the game. If a photon hits a ship
 * on a Tile, then that Tile falls away.
 *
 * Because of this gameplay, there clearly has to be a lot of interaction
 * between the Board, Ships, and Photons.  However, this way leads to
 * cyclical references.  As we will discover later in the class, cyclic
 * references are bad, because they lead to components that are too
 * tightly coupled.
 *
 * To address this problem, this project uses a philosophy of "passive"
 * models.  Models do not access the methods or fields of any other
 * Model class.  If we need for two Model objects to interact with
 * one another, this is handled in a controller class. This can get
 * cumbersome at times (particularly in the coordinate transformation
 * methods in this class), but it makes it easier to modify our
 * code in the future.
 *
 * Based on the original AI Game Lab by Yi Xu and Don Holden, 2007
 *
 * @author:  Walker M. White, Cristian Zaloj
 * @version: 1/24/2025
 */
package edu.cornell.cis3152.ailab;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.utils.*;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.obj.*;

/**
 * Class represents a 2D grid of tiles.
 *
 * Most of the work is done by the internal Tile class.  The outer class is
 * really just a container.
 */
public class Board {
    /**
     * The internal tile state
     *
     * Each tile on the board has a set of attributes associated with it.
     * However, no class other than board needs to access them directly.
     * Therefore, we make this an inner class.
     */
    private static class TileState {
        /** Is this a power tile? */
        public boolean power = false;
        /** Is this a goal tiles */
        public boolean goal = false;
        /** Has this tile been visited (used for pathfinding)? */
        public boolean visited = false;
//        /** Is this tile falling */
//        public boolean falling = false;
//        /** How far the tile has fallen */
//        public float fallAmount = 0;
    }

    /** The constants definining the board */
    private JsonValue constants;

    // Instance attributes
    /** The board width (in number of tiles) */
    private int width;
    /** The board height (in number of tiles) */
    private int height;
    /** The tile size */
    private float tileSize;
    /** The tile depth */
    private float tileDepth;
    /** The tile spacing */
    private float tileSpace;
    /** The falling range */
    private float minFall;
    private float maxFall;
    /** The amount to fall each step */
    private float fallRate;

    /** The tile grid (with above dimensions) */
    private TileState[] tiles;

    /** OBJ model for tile. Only need one, since all have same geometry */
    private ModelRef tileModel;
    /** The color for a basic tile */
    private Color tileColor;
    /** The color for a power tile */
    private Color powerColor;

    /** A quaternion for rotations */
    private Quaternion rotation;

    /**
     * Creates a new board from the given set of constants
     *
     * @param constants The board constants
     */
    public Board(JsonValue constants) {
        this.constants = constants;
        width = constants.get("size").getInt(0);
        height = constants.get("size").getInt(1);

        tileSize = constants.getFloat("tile size");
        tileDepth = constants.getFloat("tile depth");
        tileSpace = constants.getFloat("tile space");

        minFall = constants.get("fall range").getInt(0);
        maxFall = constants.get("fall range").getInt(1);
        fallRate = constants.getFloat("fall rate");


        tileColor = ParserUtils.parseColor( constants.get("basic color"), Color.WHITE );
        powerColor = ParserUtils.parseColor( constants.get("power color"), Color.WHITE );

        tiles = new TileState[width * height];
        for (int ii = 0; ii < tiles.length; ii++) {
            tiles[ii] = new TileState();
        }
        rotation = new Quaternion();
        resetTiles();
    }

    /**
     * Resets the values of all the tiles on screen.
     */
    public void resetTiles() {
        int interval = constants.getInt("power interval");
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                TileState tile = getTileState(x, y);
                tile.power = (x % interval == 0) || (y % interval == 0);
                tile.goal = false;
                tile.visited = false;
//                tile.fallAmount = 0.0f;
//                tile.falling = false;
            }
        }
    }

    /**
     * Returns the tile state for the given position (INTERNAL USE ONLY)
     *
     * Returns null if that position is out of bounds.
     *
     * @return the tile state for the given position
     */
    private TileState getTileState(int x, int y) {
        if (!inBounds(x, y)) {
            return null;
        }
        return tiles[x * height + y];
    }

    /**
     * Returns the number of tiles horizontally across the board.
     *
     * @return the number of tiles horizontally across the board.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the number of tiles vertically across the board.
     *
     * @return the number of tiles vertically across the board.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the size of the tile texture.
     *
     * @return the size of the tile texture.
     */
    public float getTileSize() {
        return tileSize;
    }

    /**
     * Returns the amount of spacing between tiles.
     *
     * @return the amount of spacing between tiles.
     */
    public float getTileSpacing() {
        return tileSpace;
    }


    // Drawing information
    /**
     * Sets the OBj model for each tile.
     *
     * We only need one model, as all tiles look (mostly) the same.
     *
     * @param model the OBJ model for each tile
     */
    public void setTile(ModelRef model) {
        tileModel = model;
    }

    /**
     * Returns the OBj model for each tile
     *
     * We only need one model, as all tiles look (mostly) the same.
     *
     * @return the OBJ model for each tile
     */
    public ModelRef getTile() {
        return tileModel;
    }


    // COORDINATE TRANSFORMS
    // The methods are used by the physics engine to coordinate the
    // Ships and Photons with the board. You should not need them.

    /**
     * Returns true if a screen location is safe (i.e. there is a tile there)
     *
     * @param x The x value in screen coordinates
     * @param y The y value in screen coordinates
     *
     * @return true if a screen location is safe
     */
    public boolean isSafeAtScreen(float x, float y) {
        int bx = screenToBoard(x);
        int by = screenToBoard(y);
        return x >= 0 && y >= 0
                && x < (width * (tileSize + tileSpace) - tileSpace)
                && y < (height * (tileSize + tileSpace) - tileSpace);
//                && !getTileState(bx, by).falling;
    }

    /**
     * Returns true if a tile location is safe (i.e. there is a tile there)
     *
     * @param x The x index for the Tile cell
     * @param y The y index for the Tile cell
     *
     * @return true if a screen location is safe
     */
    public boolean isSafeAt(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
//                && !getTileState(x, y).falling;
    }

    /**
      * Destroys a tile at the given cell location.
      *
      * Destruction only causes the tile to begin to fall. It is not
     * destroyed until it reaches MIN_FATAL_AMOUNT.  This allows any
     * ships on it a little bit of time to escape.
     *
     * @param x The x index for the Tile cell
     * @param y The y index for the Tile cell
     */
    public void destroyTileAt(int x, int y) {
        if (!inBounds(x, y)) {
            return;
        }

//        getTileState(x, y).falling = true;
    }

    /**
     * Returns true if a tile is completely destroyed yet.
     *
     * Destruction only causes the tile to begin to fall. It is not
     * actually destroyed until it reaches the maximum fall range
     *
     * @param x The x index for the Tile cell
     * @param y The y index for the Tile cell
     *
     * @return true if a tile is completely destroyed
     */
    public boolean isDestroyedAt(int x, int y) {
        if (!inBounds(x, y)) {
            return true;
        }

//        return getTileState(x, y).fallAmount >= minFall;
        return false;
    }

    /**
     * Returns true if a tile is a power tile (for weapon firing).
     *
     * @param x The x value in screen coordinates
     * @param y The y value in screen coordinates
     *
     * @return true if a tile is a power tile
     */
    public boolean isPowerTileAtScreen(float x, float y) {
        int tx = screenToBoard(x);
        int ty = screenToBoard(y);
        if (!inBounds(tx, ty)) {
            return false;
        }

        return getTileState(tx, ty).power;
    }

    /**
     * Returns true if a tile is a power tile (for weapon firing).
     *
     * @param x The x index for the Tile cell
     * @param y The y index for the Tile cell
     *
     * @return true if a tile is a power tile
     */
    public boolean isPowerTileAt(int x, int y) {
        if (!inBounds(x, y)) {
            return false;
        }

        return getTileState(x, y).power;
    }

    // GAME LOOP
    // This performs any updates local to the board (e.g. animation)

    /**
     * Updates the state of all of the tiles.
     *
     * All we do is animate falling tiles.
     */
    public void update() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                TileState tile = getTileState(x, y);
//                if (tile.falling && tile.fallAmount <= maxFall) {
//                    tile.fallAmount += fallRate;
//                }
            }
        }
    }

    /**
     * Draws the board to the given canvas.
     *
     * This method draws all of the tiles in this board. It should be the first drawing
     * pass in the GameEngine.
     *
     * @param canvas the drawing context
     */
    public void draw(ObjPipeline pipeline) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                drawTile(x, y, pipeline);
            }
        }
    }

    /**
     * Draws the individual tile at position (x,y).
     *
     * Fallen tiles are not drawn.
     *
     * @param x The x index for the Tile cell
     * @param y The y index for the Tile cell
     */
    private void drawTile(int x, int y, ObjPipeline pipeline) {
        TileState tile = getTileState(x, y);

        // Don't draw tile if it's fallen off the screen
//        if (tile.fallAmount >= maxFall) {
//            return;
//        }

        // Compute drawing coordinates
        float sx = boardToScreen(x);
        float sy = boardToScreen(y);
//        float sz = tile.fallAmount;
//        float a = 0.1f * tile.fallAmount;

        // Position the tile
        // World transform components
//        rotation.set(Vector3.Z,(float)Math.toDegrees( a ));
//        tileModel.setPosition(sx,sy,sz);
        tileModel.setPosition(sx,sy,0);
        tileModel.setRotation(rotation);
        tileModel.setScale(tileSize/2, tileSize/2, tileDepth);

        // You can modify the following to change a tile's highlight color.
        // tileColor corresponds to no highlight.
        ///////////////////////////////////////////////////////
        tileModel.getMaterial().setDiffuseTint(tileColor);
        if (tile.power) {
            tileModel.getMaterial().setDiffuseTint(powerColor);
        }
        ///////////////////////////////////////////////////////

        // Draw
        pipeline.draw(tileModel);
    }


    // METHODS FOR LAB 2

    // CONVERSION METHODS (OPTIONAL)
    // Use these methods to convert between tile coordinates (int) and
    // world coordinates (float).

    /**
     * Returns the board cell index for a screen position.
     *
     * While all positions are 2-dimensional, the dimensions to
      * the board are symmetric. This allows us to use the same
     * method to convert an x coordinate or a y coordinate to
     * a cell index.
     *
     * @param f Screen position coordinate
     *
     * @return the board cell index for a screen position.
     */
    public int screenToBoard(float f) {
        return (int)(f / (tileSize + tileSpace));
    }

    /**
     * Returns the screen position coordinate for a board cell index.
     *
     * While all positions are 2-dimensional, the dimensions to
      * the board are symmetric. This allows us to use the same
     * method to convert an x coordinate or a y coordinate to
     * a cell index.
     *
     * @param n Tile cell index
     *
     * @return the screen position coordinate for a board cell index.
     */
    public float boardToScreen(int n) {
        return (float) (n + 0.5f) * (tileSize + tileSpace);
    }

    /**
     * Returns the distance to the tile center in screen coordinates.
     *
     * This method is an implicit coordinate transform. It takes a position (either
     * x or y, as the dimensions are symmetric) in screen coordinates, and determines
     * the distance to the nearest tile center.
     *
     * @param f Screen position coordinate
     *
     * @return the distance to the tile center
     */
    public float centerOffset(float f) {
        float paddedTileSize = (tileSize + tileSpace);
        int cell = screenToBoard(f);
        float nearestCenter = (cell + 0.5f) * paddedTileSize;
        return f - nearestCenter;
    }

    // PATHFINDING METHODS (REQUIRED)
    // Use these methods to implement pathfinding on the board.

    /**
     * Returns true if the given position is a valid tile
     *
     * It does not check whether the tile is live or not.  Dead tiles are still valid.
     *
     * @param x The x index for the Tile cell
     * @param y The y index for the Tile cell
     *
     * @return true if the given position is a valid tile
     */
    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /**
     * Returns true if the tile has been visited.
     *
     * A tile position that is not on the board will always evaluate to false.
     *
     * @param x The x index for the Tile cell
     * @param y The y index for the Tile cell
     *
     * @return true if the tile has been visited.
     */
    public boolean isVisited(int x, int y) {
        if (!inBounds(x, y)) {
            return false;
        }

        return getTileState(x, y).visited;
    }

    /**
     * Marks a tile as visited.
     *
     * A marked tile will return true for isVisited(), until a call to clearMarks().
     *
     * @param x The x index for the Tile cell
     * @param y The y index for the Tile cell
     */
    public void setVisited(int x, int y) {
        if (!inBounds(x,y)) {
            Gdx.app.error("Board", "Illegal tile "+x+","+y, new IndexOutOfBoundsException());
            return;
        }
        getTileState(x, y).visited = true;
    }

    /**
     * Returns true if the tile is a goal.
     *
     * A tile position that is not on the board will always evaluate to false.
     *
     * @param x The x index for the Tile cell
     * @param y The y index for the Tile cell
     *
     * @return true if the tile is a goal.
     */
    public boolean isGoal(int x, int y) {
        if (!inBounds(x, y)) {
            return false;
        }

        return getTileState(x, y).goal;
    }

    /**
     * Marks a tile as a goal.
     *
     * A marked tile will return true for isGoal(), until a call to clearMarks().
     *
     * @param x The x index for the Tile cell
     * @param y The y index for the Tile cell
     */
    public void setGoal(int x, int y) {
        if (!inBounds(x,y)) {
            Gdx.app.error("Board", "Illegal tile "+x+","+y, new IndexOutOfBoundsException());
            return;
        }
        getTileState(x, y).goal = true;
    }

    /**
     * Clears all marks on the board.
     *
     * This method should be done at the beginning of any pathfinding round.
     */
    public void clearMarks() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                TileState state = getTileState(x, y);
                state.visited = false;
                state.goal = false;
            }
        }
    }
}
