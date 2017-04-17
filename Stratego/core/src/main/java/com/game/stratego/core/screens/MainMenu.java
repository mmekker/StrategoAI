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
	Texture start;
	Texture rules;
	Texture help;
	Texture settings;
	Texture background;
	
	boolean optionHover;
	int selectedOption;
	Color optionColor;
	
	private static int OPTION_HEIGHT = 46;
	private static int OPTION_WIDTH = 200;
	private static int OPTION_OFFSET = 7;
	
	public MainMenu(Stratego game) {
		Gdx.input.setInputProcessor(this);
		this.game = game;
		start  = new Texture("menu/MenuStartGameBlack.png");
		rules  = new Texture("menu/MenuRulesBlack.png");
		help  = new Texture("menu/MenuHelpBlack.png");
		settings  = new Texture("menu/MenuSettingsBlack.png");
		background  = new Texture("menu/StartingScreen.png");
		batch = new SpriteBatch();
		sr = new ShapeRenderer();
		optionHover = false;
		selectedOption = -1;
		optionColor = Color.RED;
	}
	
	@Override
	public void show() {

	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		checkSelectedOption();

		batch.begin();
		//Write options
		batch.draw(background, 0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		batch.draw(start, (Gdx.graphics.getWidth()/2)-(OPTION_WIDTH/2), ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*0)-(OPTION_OFFSET*0), OPTION_WIDTH, OPTION_HEIGHT);
		batch.draw(rules, (Gdx.graphics.getWidth()/2)-((OPTION_WIDTH)/2), ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*1)-(OPTION_OFFSET*1), OPTION_WIDTH, OPTION_HEIGHT);
		batch.draw(help, (Gdx.graphics.getWidth()/2)-((OPTION_WIDTH)/2), ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*2)-(OPTION_OFFSET*2), OPTION_WIDTH, OPTION_HEIGHT);
		batch.draw(settings, (Gdx.graphics.getWidth()/2)-(OPTION_WIDTH/2), ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*3)-(OPTION_OFFSET*3), OPTION_WIDTH, OPTION_HEIGHT);

		batch.end();
		sr.begin(ShapeType.Filled);
		//Draw Rectangles for options
		sr.setColor(optionColor);
		if(optionHover) {
			int x1 = (Gdx.graphics.getWidth()/2)-(OPTION_WIDTH/2)-20;
			int y1 = ((Gdx.graphics.getHeight()/2)-15)-(OPTION_HEIGHT*selectedOption)-(OPTION_OFFSET*selectedOption)-5;

			int x2 = (Gdx.graphics.getWidth()/2)-(OPTION_WIDTH/2)-20;
			int y2 = ((Gdx.graphics.getHeight()/2)-15)-(OPTION_HEIGHT*selectedOption)-(OPTION_OFFSET*selectedOption)+5;

			int x3 = (Gdx.graphics.getWidth()/2)-(OPTION_WIDTH/2)-10;
			int y3 = ((Gdx.graphics.getHeight()/2)-15)-(OPTION_HEIGHT*selectedOption)-(OPTION_OFFSET*selectedOption);

			sr.triangle(x1,y1,x2,y2,x3,y3);
			sr.circle((Gdx.graphics.getWidth()/2)-(OPTION_WIDTH/2)-20, ((Gdx.graphics.getHeight()/2)-15)-(OPTION_HEIGHT*selectedOption)-(OPTION_OFFSET*selectedOption), 5);
		}
		sr.end();
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

	public void checkSelectedOption() {
		switch(selectedOption) {
			case -1: //None
				start  = new Texture("menu/MenuStartGameBlack.png");
				rules  = new Texture("menu/MenuRulesBlack.png");
				help  = new Texture("menu/MenuHelpBlack.png");
				settings  = new Texture("menu/MenuSettingsBlack.png");
				break;
			case 0: //Start
				start  = new Texture("menu/MenuStartGameRed.png");
				rules  = new Texture("menu/MenuRulesBlack.png");
				help  = new Texture("menu/MenuHelpBlack.png");
				settings  = new Texture("menu/MenuSettingsBlack.png");
				break;
			case 1: //Rules
				start  = new Texture("menu/MenuStartGameBlack.png");
				rules  = new Texture("menu/MenuRulesRed.png");
				help  = new Texture("menu/MenuHelpBlack.png");
				settings  = new Texture("menu/MenuSettingsBlack.png");
				break;
			case 2: //Help
				start  = new Texture("menu/MenuStartGameBlack.png");
				rules  = new Texture("menu/MenuRulesBlack.png");
				help  = new Texture("menu/MenuHelpRed.png");
				settings  = new Texture("menu/MenuSettingsBlack.png");
				break;
			case 3: //Settings
				start  = new Texture("menu/MenuStartGameBlack.png");
				rules  = new Texture("menu/MenuRulesBlack.png");
				help  = new Texture("menu/MenuHelpBlack.png");
				settings  = new Texture("menu/MenuSettingsRed.png");
				break;
			default: //None
				start  = new Texture("menu/MenuStartGameBlack.png");
				rules  = new Texture("menu/MenuRulesBlack.png");
				help  = new Texture("menu/MenuHelpBlack.png");
				settings  = new Texture("menu/MenuSettingsBlack.png");
				break;
		}
	}
	
	//Input
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		optionColor = Color.BLACK;
		return true;
	}
	
	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		optionColor = Color.RED;
		int newScreenY = (Gdx.graphics.getHeight()) - screenY;
		if(screenX > 330 && screenX < 630) {
			if(newScreenY > ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*0)-(OPTION_OFFSET*0)
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*0)-(OPTION_OFFSET*0) + OPTION_HEIGHT) {
				//Option 0 selected
				game.setScreen(new GameScreen(game));
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*1)-(OPTION_OFFSET*1)
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*1)-(OPTION_OFFSET*1) + OPTION_HEIGHT) {
				//Option 1 selected
				
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*2)-(OPTION_OFFSET*2)
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*2)-(OPTION_OFFSET*2) + OPTION_HEIGHT) {
				//Option 2 selected
				
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*3)-(OPTION_OFFSET*3)
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*3)-(OPTION_OFFSET*3) + OPTION_HEIGHT) {
				//Option 3 selected
				
			}
		}
		return true;
	}
	
	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		int newScreenY = (Gdx.graphics.getHeight()) - screenY;
		if(screenX > 330 && screenX < 630) {
			if(newScreenY > ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*0)-(OPTION_OFFSET*0)
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*0)-(OPTION_OFFSET*0) + OPTION_HEIGHT) {
				selectedOption = 0;
				optionHover = true;
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*1)-(OPTION_OFFSET*1)
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*1)-(OPTION_OFFSET*1) + OPTION_HEIGHT) {
				selectedOption = 1;
				optionHover = true;
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*2)-(OPTION_OFFSET*2)
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*2)-(OPTION_OFFSET*2) + OPTION_HEIGHT) {
				selectedOption = 2;
				optionHover = true;
			}
			else if(newScreenY > ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*3)-(OPTION_OFFSET*3)
					&& newScreenY < ((Gdx.graphics.getHeight()/2)-40)-(OPTION_HEIGHT*3)-(OPTION_OFFSET*3) + OPTION_HEIGHT) {
				selectedOption = 3;
				optionHover = true;
			}
			else {
				selectedOption = -1;
				optionHover = false;
			}
		}
		else {
			selectedOption = -1;
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
