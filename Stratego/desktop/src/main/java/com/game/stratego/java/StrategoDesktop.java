package com.game.stratego.java;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import com.game.stratego.core.Stratego;
import com.game.stratego.core.stratego.Match;

public class StrategoDesktop {
	public static void main (String[] args) {
		boolean train = false;
		if(train) {
			Match.main(null);
		}
		else {
			LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
			new LwjglApplication(new Stratego(), config);
			config.width = 960;
			config.height = 620;
		}
	}
}
