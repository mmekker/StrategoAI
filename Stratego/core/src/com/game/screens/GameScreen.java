package com.game.screens;

import java.awt.Point;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.game.stratego.Board;
import com.game.stratego.Match;
import com.game.stratego.Stratego;

public class GameScreen implements Screen, InputProcessor {
	Stratego game;
	ShapeRenderer sr;
	BitmapFont font;
	SpriteBatch batch;
	
	private Match match;
	private Point selected;
	private String message;
	
	public GameScreen(Stratego game) {
		Gdx.input.setInputProcessor(this);
		this.game = game;
		sr = new ShapeRenderer();
		font = new BitmapFont();
		batch = new SpriteBatch();
		match = new Match();
		selected = null;
		message = "";
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		match.update();
		
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
		for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
			for(int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
				//Water
				if((x == 2 && y == 4)
						|| (x == 2 && y == 5)
						|| (x == 3 && y == 4)
						|| (x == 3 && y == 5)
						|| (x == 6 && y == 4)
						|| (x == 6 && y == 5)
						|| (x == 7 && y == 4)
						|| (x == 7 && y == 5)) {
					sr.setColor(Color.CYAN);
				}
				else if(match.getBoard()[x][y] == null) {
					continue;
				}
				else if(match.getBoard()[x][y].getTeamNumber() == 0){
					sr.setColor(Color.BLUE);
				}
				else if(match.getBoard()[x][y].getTeamNumber() == 1){
					sr.setColor(Color.RED);
				}
				else {
					sr.setColor(Color.BLACK);
				}
				sr.rect(((Gdx.graphics.getWidth()/2)-250)+(x+1)+(49*x), ((Gdx.graphics.getHeight()/2)-200)+(y+1)+(49*(y-1)), 49, 49);
				if(selected != null && selected.x == x && selected.y == y) {
					sr.setColor(Color.YELLOW);
					sr.rect(((Gdx.graphics.getWidth()/2)-250)+(x+1)+(49*x) + 15, ((Gdx.graphics.getHeight()/2)-200)+(y+1)+(49*(y-1))+15, 20, 20);
				}
			}
		}
		//Draw trays
		sr.setColor(Color.WHITE);
		sr.rect(20, 75, 165, 488);
		sr.setColor(Color.BLACK);
		for(int x = 0; x < 2; x++) {
			for(int y = 6; y > 0; y--) {
				sr.rect((21)+(x+1)+(80*x), (75)+(y+1)+(80*(y-1)), 80, 80);
			}
		}
		sr.setColor(Color.WHITE);
		sr.rect(775, 75, 165, 488);
		sr.setColor(Color.BLACK);
		for(int x = 0; x < 2; x++) {
			for(int y = 6; y > 0; y--) {
				sr.rect((776)+(x+1)+(80*x), (75)+(y+1)+(80*(y-1)), 80, 80);
			}
		}
		
		//Draw message box
		sr.setColor(Color.WHITE);
		sr.rect((Gdx.graphics.getWidth()/2)-250, 15, 501, 30);
		
		sr.end();
		
		batch.begin();
		GlyphLayout layout = new GlyphLayout(); //dont do this every frame! Store it as member
		layout.setText(font, message);
		font.setColor(Color.BLACK);
		font.draw(batch, message, ((Gdx.graphics.getWidth()/2)-(layout.width/2)), 37);
		
		for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
			for(int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
				if(match.getBoard()[x][y] != null) {
					if(match.getBoard()[x][y].isRevealed()) {
						font.setColor(Color.LIME);
					}
					else font.setColor(Color.WHITE);
					font.draw(batch, Character.toString(match.getBoard()[x][y].getRank()), ((Gdx.graphics.getWidth()/2)-250)+(x+1)+(49*x)+20, ((Gdx.graphics.getHeight()/2)-200)+(y+1)+(49*(y-1))+30);
				}
			}
		}
		
		batch.end();
	}
	
	public void setMessage(String message) {
		if(message.length() > 60) {
			message = message.substring(0, 60);
		}
		this.message = message;
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
	public boolean keyUp(int keycode) {
		if(match.getGameBoard().movePiece(0, 0, 0, 1)) {
			setMessage("Move (0,0) to (0,1)");
		}
		else setMessage("Illegal Move");
		return false;
	}
	@Override
	public boolean keyTyped(char character) {return false;}
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {return false;}
	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		int newY = Gdx.graphics.getHeight() - screenY;
		//Menu Button
		if(screenX > 0 && screenX < 40
				&& screenY > 0 && screenY < 25) {
			game.setScreen(new MainMenu(game));
			return true;
		}
		//Inside board
		else if(screenX > (Gdx.graphics.getWidth()/2)-250 && screenX < (Gdx.graphics.getWidth()/2)+250
				&& newY > (Gdx.graphics.getHeight()/2)-250 && newY < (Gdx.graphics.getHeight()/2)+250 ) {
			int inBoardX = screenX - ((Gdx.graphics.getWidth()/2)-250);
			int inBoardY = newY - ((Gdx.graphics.getHeight()/2)-250);
			int row = inBoardX / 50;
			int col = inBoardY / 50;
			if(selected == null) {
				if(        (row == 2 && col == 4) //Check if either point is in the water
						|| (row == 2 && col == 5)
						|| (row == 3 && col == 4)
						|| (row == 3 && col == 5)
						|| (row == 6 && col == 4)
						|| (row == 6 && col == 5)
						|| (row == 7 && col == 4)
						|| (row == 7 && col == 5)) {
					selected = null;
					return false;
				}
				else {
					selected = new Point(row, col);
					return true;
				}

			}
			else {
				if(match.getGameBoard().movePiece(selected.x, selected.y, row, col)) {
					setMessage("Move (" + selected.x + "," + selected.y + ") to (" + row + "," + col + ")");
					selected = null;
					return true;
				}
				else {
					setMessage("Illegal Move");
					selected = null;
					return false;
				}
			}
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
