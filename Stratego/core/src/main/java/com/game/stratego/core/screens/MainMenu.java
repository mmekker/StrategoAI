package com.game.stratego.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.game.stratego.core.Stratego;

public class MainMenu implements Screen, InputProcessor {
	private Stratego game;
	SpriteBatch batch;
	ShapeRenderer sr;
	Texture logo;
	Texture start;
	Texture load;
	Texture rules;
	Texture help;
	Texture settings;
	
	boolean optionHover;
	int selectedOption;
	Color optionColor;
	
	private static int OPTION_HEIGHT = 46;
	private static int OPTION_WIDTH = 200;
	private static int OPTION_OFFSET = 2;
	
	public MainMenu(Stratego game) {
		Gdx.input.setInputProcessor(this);
		this.game = game;
		logo  = new Texture("logo.png");
		start  = new Texture("start.png");
		load  = new Texture("load.png");
		rules  = new Texture("rules.png");
		help  = new Texture("help.png");
		settings  = new Texture("settings.png");
		batch = new SpriteBatch();
		sr = new ShapeRenderer();
		optionHover = false;
		selectedOption = 0;
		optionColor = Color.WHITE;
	}
	
	@Override
	public void show() {

	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		sr.begin(ShapeType.Filled);
		//Draw Rectangles for options
		sr.setColor(optionColor);
		if(optionHover) {
			sr.circle((Gdx.graphics.getWidth()/2)-(OPTION_WIDTH/2)-50, ((Gdx.graphics.getHeight()/2)+10)-(OPTION_HEIGHT*selectedOption)-(OPTION_OFFSET*selectedOption), 5);
		}
		sr.end();
		batch.begin();
		//Write options
		batch.draw(start, (Gdx.graphics.getWidth()/2)-(OPTION_WIDTH/2), ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*0)-(OPTION_OFFSET*0), OPTION_WIDTH, OPTION_HEIGHT);
		batch.draw(load, (Gdx.graphics.getWidth()/2)-(OPTION_WIDTH/2), ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*1)-(OPTION_OFFSET*1), OPTION_WIDTH, OPTION_HEIGHT);
		batch.draw(rules, (Gdx.graphics.getWidth()/2)-((OPTION_WIDTH-50)/2), ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*2)-(OPTION_OFFSET*2), OPTION_WIDTH-50, OPTION_HEIGHT);
		batch.draw(help, (Gdx.graphics.getWidth()/2)-((OPTION_WIDTH-50)/2), ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*3)-(OPTION_OFFSET*3), OPTION_WIDTH-50, OPTION_HEIGHT);
		batch.draw(settings, (Gdx.graphics.getWidth()/2)-(OPTION_WIDTH/2), ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*4)-(OPTION_OFFSET*4), OPTION_WIDTH, OPTION_HEIGHT);
		//Draw logo
		batch.draw(logo, (Gdx.graphics.getWidth()/2)-(723/2), (Gdx.graphics.getHeight()/2), 723, 237);
		batch.end();
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
		batch.dispose();
		sr.dispose();
	}

	
	//Input
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		optionColor = Color.GREEN;
		return true;
	}
	
	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		optionColor = Color.WHITE;
		int newScreenY = (Gdx.graphics.getHeight()) - screenY;
		if(screenX > 118 && screenX < 842) {
			if(newScreenY > ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*0)-(OPTION_OFFSET*0) 
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*0)-(OPTION_OFFSET*0) + OPTION_HEIGHT) {
				//Option 0 selected
				game.setScreen(new GameScreen(game));
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*1)-(OPTION_OFFSET*1) 
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*1)-(OPTION_OFFSET*1) + OPTION_HEIGHT) {
				//Option 1 selected
				
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*2)-(OPTION_OFFSET*2) 
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*2)-(OPTION_OFFSET*2) + OPTION_HEIGHT) {
				//Option 2 selected
				
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*3)-(OPTION_OFFSET*3) 
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*3)-(OPTION_OFFSET*3) + OPTION_HEIGHT) {
				//Option 3 selected
				
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*4)-(OPTION_OFFSET*4) 
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*4)-(OPTION_OFFSET*4) + OPTION_HEIGHT) {
				//Option 4 selected
				
			}
		}
		return true;
	}
	
	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		int newScreenY = (Gdx.graphics.getHeight()) - screenY;
		if(screenX > 200 && screenX < 800) {
			if(newScreenY > ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*0)-(OPTION_OFFSET*0) 
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*0)-(OPTION_OFFSET*0) + OPTION_HEIGHT) {
				selectedOption = 0;
				optionHover = true;
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*1)-(OPTION_OFFSET*1) 
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*1)-(OPTION_OFFSET*1) + OPTION_HEIGHT) {
				selectedOption = 1;
				optionHover = true;
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*2)-(OPTION_OFFSET*2) 
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*2)-(OPTION_OFFSET*2) + OPTION_HEIGHT) {
				selectedOption = 2;
				optionHover = true;
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*3)-(OPTION_OFFSET*3) 
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*3)-(OPTION_OFFSET*3) + OPTION_HEIGHT) {
				selectedOption = 3;
				optionHover = true;
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*4)-(OPTION_OFFSET*4) 
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-20)-(OPTION_HEIGHT*4)-(OPTION_OFFSET*4) + OPTION_HEIGHT) {
				selectedOption = 4;
				optionHover = true;
			}
			else {
				optionHover = false;
			}
		}
		else {
			optionHover = false;
		}
		return true;
	}
	
	@Override
	public boolean scrolled(int amount) {return false;}
	@Override
	public boolean keyDown(int keycode) {return false;}
	@Override
	public boolean keyUp(int keycode) {return false;}
	@Override
	public boolean keyTyped(char character) {return false;}
	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {return false;}

}
