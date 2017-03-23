package com.game.stratego.java;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import com.game.stratego.core.Stratego;

public class StrategoDesktop {
	public static void main (String[] args) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(new Stratego(), config);
		config.width = 960;
		config.height = 620;
	}
}
