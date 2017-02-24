package com.game.screens;

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
import com.game.stratego.TrayPiece;

import java.awt.*;

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
		drawBoardShapes();

		//Draw trays
		drawTrayShapes();
		
		//Draw message box
		sr.setColor(Color.WHITE);
		sr.rect((Gdx.graphics.getWidth()/2)-250, 15, 501, 30);
		
		sr.end();
		
		batch.begin();
		GlyphLayout layout = new GlyphLayout(); //dont do this every frame! Store it as member
		layout.setText(font, message);
		font.setColor(Color.BLACK);
		font.draw(batch, message, ((Gdx.graphics.getWidth()/2)-(layout.width/2)), 37);

		//Write Board text
		writeBoardText();

		//Write Tray text
		writeTrayText();
		
		batch.end();
	}

	/************Start Draw Methods***************/
	public void drawBoardShapes() {
		sr.setColor(Color.WHITE);
		sr.rect((Gdx.graphics.getWidth() / 2) - 250, (Gdx.graphics.getHeight() / 2) - 250, 501, 502);
		sr.setColor(Color.BLACK);
		for (int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
			for (int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
				//Water
				if ((x == 2 && y == 4)
						|| (x == 2 && y == 5)
						|| (x == 3 && y == 4)
						|| (x == 3 && y == 5)
						|| (x == 6 && y == 4)
						|| (x == 6 && y == 5)
						|| (x == 7 && y == 4)
						|| (x == 7 && y == 5)) {
					sr.setColor(Color.CYAN);
				} else if (match.getBoard()[x][y] == null) {
					sr.setColor(Color.BLACK);
				} else if (match.getBoard()[x][y].getTeamNumber() == 0) {
					sr.setColor(Color.BLUE);
				} else if (match.getBoard()[x][y].getTeamNumber() == 1) {
					sr.setColor(Color.RED);
				} else {
					sr.setColor(Color.BLACK);
				}
				sr.rect(((Gdx.graphics.getWidth() / 2) - 250) + (x + 1) + (49 * x), ((Gdx.graphics.getHeight() / 2) - 200) + (y + 1) + (49 * (y - 1)), 49, 49);
				if (match.getState().equals("play") && selected != null
						&& selected.x == x && selected.y == y) {
					sr.setColor(Color.YELLOW);
					sr.rect(((Gdx.graphics.getWidth() / 2) - 250) + (x + 1) + (49 * x) + 15, ((Gdx.graphics.getHeight() / 2) - 200) + (y + 1) + (49 * (y - 1)) + 15, 20, 20);
				}
			}
		}
	}

	public void drawTrayShapes() {
		//Player tray
		sr.setColor(Color.WHITE);
		sr.rect(20, 75, 165, 488);
		sr.setColor(Color.BLUE);
		for(int x = 0; x < 2; x++) {
			for(int y = 0; y < 6; y++) {
				sr.setColor(Color.BLUE);
				sr.rect((21)+(x+1)+(80*x), (75)+(y+1)+(80*y), 80, 80);
				if (match.getState().equals("make") && selected != null
						&& selected.x == x && selected.y == y) {
					sr.setColor(Color.YELLOW);
					sr.rect((21)+(x+1)+(80*x)+30, (75)+(y+1)+(80*y)+30, 20, 20);
				}
			}
		}
		//Computer Tray
		sr.setColor(Color.WHITE);
		sr.rect(775, 75, 165, 488);
		sr.setColor(Color.RED);
		for(int x = 0; x < 2; x++) {
			for(int y = 0; y < 6; y++) {
				sr.rect((776)+(x+1)+(80*x), (75)+(y+1)+(80*y), 80, 80);
			}
		}
	}

	public void writeBoardText() {
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
	}

	public void writeTrayText() {
		TrayPiece[] playerTray = match.getGameBoard().getPlayerTray();
		for(int x = 0; x < 2; x++) {
			for(int y = 0; y < 6; y++) {
				//Get Piece index
				int i = (y+x)+(5*x);
				//Draw piece rank
				font.setColor(Color.WHITE);
				font.draw(batch, Character.toString(playerTray[i].getRank()), (21)+(x+1)+(80*x)+35,(75)+(y+1)+(80*y)+45);
				//Draw piece remaining
				font.setColor(Color.YELLOW);
				font.draw(batch, Integer.toString(playerTray[i].getRemaining()), (21)+(x+1)+(80*x)+65,(75)+(y+1)+(80*y)+75);

			}
		}
		TrayPiece[] computerTray = match.getGameBoard().getComputerTray();
		for(int x = 0; x < 2; x++) {
			for(int y = 0; y < 6; y++) {
				//Get Piece index
				int i = (y+x)+(5*x);
				//Draw piece rank
				font.setColor(Color.WHITE);
				font.draw(batch, Character.toString(computerTray[i].getRank()), (776)+(x+1)+(80*x)+35,(75)+(y+1)+(80*y)+45);
				//Draw piece remaining
				font.setColor(Color.YELLOW);
				font.draw(batch, Integer.toString(computerTray[i].getRemaining()), (776)+(x+1)+(80*x)+65,(75)+(y+1)+(80*y)+75);
			}
		}
	}
	/**************End Draw Methods***************/
	
	public void setMessage(String message) {
		if(message.length() > 60) {
			message = message.substring(0, 60);
		}
		this.message = message;
	}
	
	@Override
	public void show() {}
	@Override
	public void resize(int width, int height) {}
	@Override
	public void pause() {}
	@Override
	public void resume() {}
	@Override
	public void hide() {}
	@Override
	public void dispose() {}
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
			if(match.getState().equals("play")) {
				if (selected == null) {
					if (Board.isWater(row, col)
							|| match.getBoard()[row][col] == null
							|| match.getBoard()[row][col].getTeamNumber() != match.getCurrentTurn()) {
						selected = null;
						return false;
					} else {
						selected = new Point(row, col);
						return true;
					}

				} else {
					if (match.getGameBoard().movePiece(selected.x, selected.y, row, col)) {
						if (match.getCurrentTurn() == 0) match.setCurrentTurn(1);
						else match.setCurrentTurn(0);
						setMessage("Move (" + selected.x + "," + selected.y + ") to (" + row + "," + col + ")");
						selected = null;
						return true;
					} else {
						setMessage("Illegal Move");
						selected = null;
						return false;
					}
				}
			}
			else if(match.getState().equals("make")) {
				if(selected != null) {
					//Get Piece index
					int i = (selected.y+selected.x)+(5*selected.x);
					if(match.getGameBoard().getPlayerTray()[i].getRemaining() > 0) {
						if(match.getBoard()[row][col] == null
								&& col < (Board.DEFAULT_BOARD_SIZE/2)-1) {
							match.getBoard()[row][col] = match.getGameBoard().getPlayerTray()[i].takePiece(0);
							if(match.getGameBoard().isTrayEmpty(match.getGameBoard().getPlayerTray())) {
								selected = null;
							}
						}
					}
				}
			}
		}
		else if(screenX > 21 && screenX < 181
				&& newY > 75 && newY < 555 ) { //Inside the left tray
			int inBoardX = screenX - 21;
			int inBoardY = newY - 75;
			int row = inBoardX / 80;
			int col = inBoardY / 80;
			if(match.getState().equals("make")) {
				selected = new Point(row,col);
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
