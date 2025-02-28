/*
 * GDXRoot.java
 *
 * This is the primary class file for running the game. It is the "static main"
 * of LibGDX.  In the previous lab, we uses multiple scenes: one for loading and
 * one for the game. In this example (because our graphics pipeline is so
 * unusual), we only have one scene.
 *
 * Note that we still need to have a Screen/Scene.  The game class will not
 * work without one.
 *
 * Based on original AI Game Lab by Yi Xu and Don Holden, 2007
 *
 * @author:  Walker M. White, Cristian Zaloj
 * @version: 1/24/2025
 */
package edu.cornell.cis3152.ailab;

import com.badlogic.gdx.Game;

/**
 * Root class for a LibGDX.
 *
 * This class is technically not the ROOT CLASS. Each platform has another class
 * above this (e.g. PC games use DesktopLauncher) which serves as the true root.
 * However, those classes are unique to each platform, while this class is the
 * same across all plaforms. In addition, this functions as the root class all
 * intents and purposes, and you would draw it as a root class in an architecture
 * specification.
 */
public class GDXRoot extends Game {
	/**
	 * Called when the Application is first created.
	 *
	 * This is a weird work-around because of some transparency issues in 3D.
	 */
	public void create () {
		setScreen(new GameScene());
	}
}
