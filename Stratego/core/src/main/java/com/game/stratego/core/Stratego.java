package com.game.stratego.core;

import com.badlogic.gdx.Game;
import com.game.stratego.core.screens.MainMenu;

public class Stratego extends Game {
	@Override
	public void create () {
		this.setScreen(new MainMenu(this));
	}

	@Override
	public void render () {
		super.render();
	}
	
	@Override
	public void dispose () {
	}
}
