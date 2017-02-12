package com.game.stratego;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.game.screens.MainMenu;

public class Stratego extends Game {
	public SpriteBatch batch;
	
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
		batch.dispose();
	}
	
	public void changeScreen(Screen screen){
		this.setScreen(screen);
	}
}
