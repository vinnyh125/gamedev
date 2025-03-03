/*
 * GameScene.java
 *
 * This class works like GameScene from the last lab. It is different in two
 * important ways. First of all, it handles its own loading (we do not have a
 * separate loading scene). More importantly, it has a custom graphics pipeline
 * and does not just use a SpriteBatch.
 *
 * The design of this class is pretty quick-and-dirty. We do not recommend that
 * you emulate the design of this class for your game.
 *
 * Based on original AI Game Lab by Yi Xu and Don Holden, 2007
 *
 * @author:  Walker M. White, Cristian Zaloj
 * @version: 1/24/2025
 */
package edu.cornell.cis3152.ailab;

import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Bitmap;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.graphics.obj.Material;
import edu.cornell.gdiac.graphics.obj.Model;
import edu.cornell.gdiac.graphics.obj.ModelRef;
import edu.cornell.gdiac.util.Controllers;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.*;
import com.badlogic.gdx.assets.*;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.*;
import com.badlogic.gdx.utils.*;

import edu.cornell.gdiac.util.*;

/**
 * Primary class for controlling the game.
 *
 * This class is the only GameScene for this game. Instead of a separate loading
 * scene, loading is handled in the main scene. As a result, there is no reason
 * for this scene to communicate with the root (via the ScreenListener interface).
 * It also does not need any outside parameters from the root class. So for
 * this lab, THIS class is actually the true root of the game.
 */
public class GameScene implements Screen {
    /**
     * Enumeration defining the game state
     */
    public static enum GameState {
        /** While we are still loading assets */
        LOAD,
        /** After loading, but before we start the game */
        BEFORE,
        /** While we are playing the game */
        PLAY,
        /** When the game has ended, but we are still waiting on animation */
        FINISH,
        /** When the game is over, and all animation is finished */
        AFTER
    }

    /** Message while assets are loading */
    private static final String MESSG_LOAD = "Loading...";

    /** AssetManager to load game assets (textures, sounds, etc.) */
    private AssetDirectory assets;
    /** The collection of strings for messages */
    private JsonValue strings;

    /** Subcontroller for physics (CONTROLLER CLASS) */
    private CollisionController physicsController;
    /** Subcontroller for gameplay (CONTROLLER CLASS) */
    private GameplayController gameplayController;

    /** Container for the game objects (MODEL CLASS) */
    private GameSession session;

    /** Used to draw the game onto the screen (VIEW CLASS) */
    private GraphicsPipeline pipeline;

    /** The current game state (SIMPLE FIELD) */
    private GameState gameState;
    /** How far along (0 to 1) we are in loading process */
    private float  gameLoad;

    /**
     * Constructs a new game scene
     *
     * This method creates the graphics pipeline, but does not yet load
     * any assets.
     */
    public GameScene() {
        gameState = GameState.LOAD;
        gameLoad  = 0.0f;
        pipeline = new GraphicsPipeline();
    }


    /**
     * Called when this screen should release all resources.
     */
    public void dispose() {
        unload();
    }

    /**
     * Restarts the game, laying out all the ships and tiles
     */
    public void resetGame() {
        session = new GameSession(assets);

        // Create the two subcontrollers
        gameplayController = new GameplayController(session);
        physicsController  = new CollisionController(session);

        // Add to the graphics pipeline
        pipeline.setSession( session );
        gameState = GameState.PLAY;

    }

    /**
     * Updates the state of the loading screen.
     *
     * Loading is done when the asset manager is finished and gameLoad == 1.
     */
    public void updateLoad() {
        if (assets.update()) {
            // we are done loading, let's move to another screen!
            if (session == null) {
                // Layout the board, but we are still loading
                resetGame();
                strings = assets.getEntry("strings",JsonValue.class);
                gameState = GameState.LOAD;
            }
            if (gameLoad < 1.0f) {
                gameLoad += 0.01f;
            } else {
                gameState = GameState.BEFORE;
            }
          }
    }

    /**
     * Updates the game logic.
     *
     * This is the primary update loop of the game; called while it is running.
     */
    public void updateGame() {
        // Update the ships and board state
        gameplayController.update();

        // Resolve any collisions
        physicsController.update();

        // if the player ship is dead, end the game with a Game Over:
        ShipList ships = session.getShips();
//        if (gameState == GameState.PLAY) {
//            if (!ships.getPlayer().isActive()) {
//                gameState = GameState.FINISH;
//                SoundEffect s = assets.getEntry("over", SoundEffect.class);
//
//                /** Manager used to cut down on the audio conflicts */
//                SoundEffectManager sounds = SoundEffectManager.getInstance();
//                sounds.play("over", s);
//
//            } else if (ships.numActive() <= 1) {
//                gameState = GameState.FINISH;
//            }
//        } else if (gameState == GameState.FINISH) {
//            if (!ships.getPlayer().isAlive() || ships.numAlive() <= 1) {
//                gameState = GameState.AFTER;
//            }
//        }
    }

    /**
     * Draws the game board while we are still loading
     */
    public void drawLoad() {
        pipeline.setEyePan(gameLoad);

        if (session != null) {
            ShipList ships = session.getShips();
            pipeline.setTarget( session.getPlayer().getPlayerHead().getX(), session.getPlayer().getPlayerHead().getY() );
        }
        pipeline.render(MESSG_LOAD);

    }

    /**
     * Draws the game board when we are playing the game.
     */
    public void drawGame() {

        pipeline.setEyePan(1.0f);

        // Specify what the camera should be centered on (in this case, the head ship)
        pipeline.setTarget(session.getPlayer().getPlayerHead().getX(), session.getPlayer().getPlayerHead().getY());

        ShipList ships = session.getShips();

        // Determine if we have a message
        String message = null;
        switch (gameState) {
            case BEFORE:
                message = strings.getString("start");
                break;
            case FINISH:
            case AFTER:
//                if (!ships.getPlayer().isActive()) {
//                    message = strings.getString("lost")+"\n"+strings.getString("restart");
//                } else {
//                    message = strings.getString("won")+"\n"+strings.getString("restart");
//                }
                break;
            case LOAD:
                message = strings.getString("load")+"\n"+strings.getString("restart");
                break;
            case PLAY:
                // when the player is in PLAY mode, want this to be the message
                message = "Coins: " + session.getPlayer().getCoins();
                break;
        }

        // actually render the message on screen
        pipeline.render(message);
    }

    // SCREEN INTERFACE METHODS

    /**
     * Called when this screen becomes the current screen for a Game.
     *
     * This is the equivalent of create() in Lab 1
     */
    public void show() {
        load();
    }

    /**
     * Called when this screen is no longer the current screen for a Game.
     *
     * When this happens, we should also dispose of all resources.
     */
    public void hide() {
        unload();
    }

    /**
     * Called when the screen should render itself.
     *
     * This is the primary game loop, as mandated by the Screen interface.  We
     * break it up into a bunch of helpers to make it readable.
     *
     * @param delta The time in seconds since the last render.
     */
    public void render(float delta) {
        // Determine what the player wants to do
        int selection = InputController.SELECT_NONE;
        if (gameplayController != null) {
            selection = gameplayController.getPlayerSelection();
        }

        if (gameState == GameState.BEFORE) {
            if ((selection & InputController.SELECT_BEGIN) != 0) {
                gameState = GameState.PLAY;
            }
        } else if (gameState == GameState.PLAY || gameState == GameState.FINISH || gameState == GameState.AFTER) {
            if ((selection & InputController.SELECT_RESET) != 0) {
                resetGame();
                gameState = GameState.PLAY;
            }
        }

        // Now update the game according to the game state
        switch (gameState) {
        case LOAD:
            updateLoad();
            drawLoad();
            break;
        case PLAY:
        case FINISH:
            updateGame();
        case BEFORE:
        case AFTER:
            drawGame();
            break;
        }
    }

    /**
     * Called when the Application is resized.
     *
     * This can happen at any time. We need to resize the graphics pipeline
     * if this happens.
     */
    public void resize(int width, int height) {
        pipeline.resize(width,height);
    }

    /**
     * Called when the Application is paused.
     *
     * This is usually when it's not active or visible on screen.
     */
    public void pause() { }

    /**
     * Called when the Application is resumed from a paused state.
     *
     * This is usually when it regains focus.
     */
    public void resume() {}


    // HELPER FUNCTIONS

    /**
     * Loads all assets for this level
     *
     * This game uses an asset directory, just like the last lab. However,
     * we do not do this as a separate loading scene.  Instead we do everything
     * in this scene. That means we need to bootstrap the game by guaranteeing
     * that some assets are loaded immediately (while we can wait on the others).
     */
    private void load() {
        // Get the few resources we need immediately
        AssetDirectory boot = new AssetDirectory("jsons/loading.json");
        boot.loadAssets();
        boot.finishLoading();

        Texture stars = boot.getEntry("stars",Texture.class);
        pipeline.setBackground(stars);

        BitmapFont font = boot.getEntry("amyn",BitmapFont.class);
        pipeline.setFont(font);

        // These assets get loaded asynchronously
        assets = new AssetDirectory( "jsons/assets.json" );
        assets.loadAssets();
    }

    /**
     * Unloads all assets previously loaded.
     */
    private void unload() {
        pipeline.setBackground(null);
        pipeline.setFont(null);
        assets.unloadAssets();
    }

}

