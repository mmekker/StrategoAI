package com.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.game.stratego.Stratego;

public class GameScreen implements Screen, InputProcessor {
	Stratego game;
	ShapeRenderer sr;
	
	public GameScreen(Stratego game) {
		Gdx.input.setInputProcessor(this);
		this.game = game;
		sr = new ShapeRenderer();
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		sr.begin(ShapeType.Filled);
		//Draw top left menu button
		sr.setColor(Color.LIGHT_GRAY);
		sr.rect(0, (Gdx.graphics.getHeight()-27), 40, 27);
		sr.setColor(Color.GRAY);
		sr.rect(10, (Gdx.graphics.getHeight()-10), 20, 4);
		sr.rect(10, (Gdx.graphics.getHeight()-15), 20, 4);
		sr.rect(10, (Gdx.graphics.getHeight()-20), 20, 4);
		//Draw board
		sr.setColor(Color.WHITE);
		sr.rect((Gdx.graphics.getWidth()/2)-250, (Gdx.graphics.getHeight()/2)-250, 501, 502);
		sr.setColor(Color.BLACK);
		for(int x = 0; x < 10; x++) {
			for(int y = 10; y > 0; y--) {
				//Water
				if((x == 2 && y == 5)
						|| (x == 2 && y == 6)
						|| (x == 3 && y == 5)
						|| (x == 3 && y == 6)
						|| (x == 6 && y == 5)
						|| (x == 6 && y == 6)
						|| (x == 7 && y == 5)
						|| (x == 7 && y == 6)) {
					sr.setColor(Color.BLUE);
				}
				else {
					sr.setColor(Color.BLACK);
				}
				sr.rect(((Gdx.graphics.getWidth()/2)-250)+(x+1)+(49*x), ((Gdx.graphics.getHeight()/2)-250)+(y+1)+(49*(y-1)), 49, 49);
			}
		}
		//Draw trays
		sr.setColor(Color.WHITE);
		sr.rect(20, 15, 165, 488);
		sr.setColor(Color.BLACK);
		for(int x = 0; x < 2; x++) {
			for(int y = 6; y > 0; y--) {
				sr.rect((21)+(x+1)+(80*x), (15)+(y+1)+(80*(y-1)), 80, 80);
			}
		}
		sr.setColor(Color.WHITE);
		sr.rect(775, 15, 165, 488);
		sr.setColor(Color.BLACK);
		for(int x = 0; x < 2; x++) {
			for(int y = 6; y > 0; y--) {
				sr.rect((776)+(x+1)+(80*x), (15)+(y+1)+(80*(y-1)), 80, 80);
			}
		}
		sr.end();
	}
	
	@Override
	public void show() {

	}

	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void hide() {

	}

	@Override
	public void dispose() {

	}

	@Override
	public boolean keyDown(int keycode) {return false;}
	@Override
	public boolean keyUp(int keycode) {return false;}
	@Override
	public boolean keyTyped(char character) {return false;}
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {return false;}
	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		//int newScreenY = (Gdx.graphics.getHeight()) - screenY;
		if(screenX > 0 && screenX < 40
				&& screenY > 0 && screenY < 25) {
			game.setScreen(new MainMenu(game));
		}
		return false;
	}
	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {return false;}
	@Override
	public boolean mouseMoved(int screenX, int screenY) {return false;}
	@Override
	public boolean scrolled(int amount) {return false;}

}
