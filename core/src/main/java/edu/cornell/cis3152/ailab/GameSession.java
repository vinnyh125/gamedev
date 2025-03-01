/*
 * GameSession.java
 *
 * This class is what we call a level container. It is a container for all our
 * model classes. The models -- Board, PhotonSet, and ShipList -- all have to
 * level somewhere.  A natural place would be as fields in GameScene. But we
 * have multiple sub-controllers and they have to access all the models as well.
 * Instead of constantly passing around the three models, we just put them all
 * in this class and pass them around once. This is is a common design pattern
 * for managing models.
 *
 * Based on original AI Game Lab by Yi Xu and Don Holden, 2007
 *
 * @author:  Walker M. White, Cristian Zaloj
 * @version: 1/24/2025
 */
package edu.cornell.cis3152.ailab;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.graphics.obj.*;

/**
 * This class represents a single session of the game.
 *
 * This is a container class that stores all of the model objects in that game.
 * These types of classes are useful, because they allow us to pass about the
 * game state (which is defined as all of the data in all of the models) about
 * as a single object.  In addition, they make it easy to reset or reload the
 * game.
 *
 * We also put the asset directory in this class as the same classes that need
 * to interact with the models often need to interact with the game assets.
 */
public class GameSession {
    /** A reference to the asset directory (for on demand asssets) */
    private AssetDirectory assets;

    // Location and animation information for game objects (MODEL CLASSES)
    /** The grid of tiles (MODEL CLASS) */
    private Board board;
    /** The ship objects (MODEL CLASS) */
    private ShipList ships;
    private Player player;
    /** Collection of photons on screen. (MODEL CLASS) */
    private PhotonPool photons;

    /**
     * Creates a new game session
     *
     * This method will call reset() to set up the board.
     *
     * @parama assets   The associated asset directory
     */
    public GameSession(AssetDirectory assets) {
        this.assets = assets;
        reset();
    }

    /**
     * Generates all of the game model objects.
     *
     * This method generates the board and all of the ships. It will use the
     * JSON files in the asset directory to generate these models.
     */
    public void reset() {
        // BOARD
        board = new Board(assets.getEntry("board", JsonValue.class));

        Texture tileImage = assets.getEntry( "tile", Texture.class );
        ModelRef tileModel = new ModelRef(assets.getEntry( "tile", Model.class ));
        tileModel.setMaterial(new Material(tileImage));
        board.setTile( tileModel );

        // SHIPS
        player = new Player(this);
        ships = new ShipList(assets.getEntry("ship", JsonValue.class));

        Texture playerImage = assets.getEntry("ship1", Texture.class);
        Texture enemyImage  = assets.getEntry("ship2", Texture.class);
        ModelRef playerModel = new ModelRef(assets.getEntry( "ship", Model.class ));
        ModelRef enemyModel = new ModelRef(assets.getEntry( "ship", Model.class ));
        ModelRef fireModel = new ModelRef( assets.getEntry( "fire", Model.class ));
        playerModel.setMaterial( new Material(playerImage) );
        enemyModel.setMaterial( new Material(enemyImage) );

        ships.setPlayerModel( playerModel );
        ships.setEnemyModel( enemyModel );
        ships.setFireModel( fireModel );

        // PHOTONS
        photons = new PhotonPool(assets.getEntry("photon", JsonValue.class));

        Texture photonImage = assets.getEntry("photon", Texture.class);
        ModelRef photonModel = new ModelRef( assets.getEntry( "photon", Model.class ) );
        photonModel.setMaterial( new Material(photonImage) );
        photons.setPhotonModel( photonModel );

    }

    /**
     * Returns the game board
     *
     * @return the game board
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Returns the list of ships in the game
     *
     * @return the list of ships in the game
     */
    public ShipList getShips() {
        return ships;
    }

    /**
     * Returns the set of active photons
     *
     * @return the set of active photons
     */
    public PhotonPool getPhotons() {
        return photons;
    }

    /**
     * Returns a sound effect with the given name
     *
     * This method is just a simple look-up in the asset directory.
     *
     * @return a sound effect with the given name
     */
    public SoundEffect getSound(String key) {
        return assets.getEntry( key, SoundEffect.class );
    }

    public Player getPlayer() {
        return player;
    }

}
