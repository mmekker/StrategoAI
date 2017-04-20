package com.game.stratego.core;

import com.badlogic.gdx.Game;
import com.game.stratego.core.screens.MainMenu;
import com.game.stratego.core.stratego.Match;

public class Stratego extends Game {
	@Override
	public void create () {
		boolean train = true;
		if(!train) {
			this.setScreen(new MainMenu(this));
		}
		else if(train) {
			Match.main(null);
		}
	}

	@Override
	public void render () {
		super.render();
	}
	
	@Override
	public void dispose () {
	}
}
