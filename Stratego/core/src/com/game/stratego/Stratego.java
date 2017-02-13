package com.game.stratego;

import com.badlogic.gdx.Game;
import com.game.screens.MainMenu;

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
