package com.game.stratego.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.game.stratego.core.Stratego;
import com.game.stratego.core.stratego.Board;
import com.game.stratego.core.stratego.Match;
import com.game.stratego.core.stratego.TrayPiece;

import java.awt.*;

//import com.badlogic.gdx.graphics.g2d.GlyphLayout;

public class GameScreen implements Screen, InputProcessor {
	Stratego game;
	ShapeRenderer sr;
	BitmapFont font;
	SpriteBatch batch;

	//Load All textures
	Texture background = new Texture("GameScreenWithGrid.png");
	Texture blueBack = new Texture("piece/BluePieceBack.png");
	Texture blue1 = new Texture("piece/BluePieceFront1.png");
	Texture blue2 = new Texture("piece/BluePieceFront2.png");
	Texture blue3 = new Texture("piece/BluePieceFront3.png");
	Texture blue4 = new Texture("piece/BluePieceFront4.png");
	Texture blue5 = new Texture("piece/BluePieceFront5.png");
	Texture blue6 = new Texture("piece/BluePieceFront6.png");
	Texture blue7 = new Texture("piece/BluePieceFront7.png");
	Texture blue8 = new Texture("piece/BluePieceFront8.png");
	Texture blue9 = new Texture("piece/BluePieceFront9.png");
	Texture blueB = new Texture("piece/BluePieceFrontB.png");
	Texture blueF = new Texture("piece/BluePieceFrontF.png");
	Texture blueS = new Texture("piece/BluePieceFrontS.png");
	Texture redBack = new Texture("piece/RedPieceBack.png");
	Texture red1 = new Texture("piece/RedPieceFront1.png");
	Texture red2 = new Texture("piece/RedPieceFront2.png");
	Texture red3 = new Texture("piece/RedPieceFront3.png");
	Texture red4 = new Texture("piece/RedPieceFront4.png");
	Texture red5 = new Texture("piece/RedPieceFront5.png");
	Texture red6 = new Texture("piece/RedPieceFront6.png");
	Texture red7 = new Texture("piece/RedPieceFront7.png");
	Texture red8 = new Texture("piece/RedPieceFront8.png");
	Texture red9 = new Texture("piece/RedPieceFront9.png");
	Texture redB = new Texture("piece/RedPieceFrontB.png");
	Texture redF = new Texture("piece/RedPieceFrontF.png");
	Texture redS = new Texture("piece/RedPieceFrontS.png");

	private Match match;
	private Point selected;
	private String message;		//TODO: Add who took who into the messages
	
	public GameScreen(Stratego game) {
		Gdx.input.setInputProcessor(this);
		this.game = game;
		sr = new ShapeRenderer();
		font = new BitmapFont();
		batch = new SpriteBatch();
		match = new Match(this);
		selected = null;
		message = "";
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		if(Gdx.input.isKeyPressed(Input.Keys.S)
				&& match.getState().equals("make")) {
			match.getGameBoard().createPlayerSetup();
		}

		if(match.getGameBoard().isGameFinished()) {
			if(match.getGameBoard().getWinner() == 0) {
				setMessage("You win! Congratulations!");
			}
			else setMessage("Oh no. You lost.");
		}
		batch.begin();
		batch.draw(background,0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		batch.end();

		sr.begin(ShapeType.Filled);

		//Draw top left menu button
		sr.setColor(Color.LIGHT_GRAY);
		sr.rect(0, (Gdx.graphics.getHeight()-27), 40, 27);
		sr.setColor(Color.GRAY);
		sr.rect(10, (Gdx.graphics.getHeight()-10), 20, 4);
		sr.rect(10, (Gdx.graphics.getHeight()-15), 20, 4);
		sr.rect(10, (Gdx.graphics.getHeight()-20), 20, 4);

		/*//Draw Turn Indicator
		if(match.getCurrentTurn() == 0) sr.setColor(Color.BLUE);
		else sr.setColor(Color.RED);
		sr.rect((Gdx.graphics.getWidth()/2)-20, (Gdx.graphics.getHeight()-50), 40, 40);*/

		drawBoardShapes();

		//Draw trays
		drawTrayShapes();
		
		//Draw message box
		sr.setColor(Color.WHITE);
		sr.rect((Gdx.graphics.getWidth()/2)-250, 15, 501, 30);
		
		sr.end();

		batch.begin();
		drawTrayTextures();
		font.setColor(Color.BLACK);
		font.draw(batch, message, ((Gdx.graphics.getWidth()/2)-(250)), 37);

		//Write Board text
		drawBoardTextures();

		//Write Tray text
		writeTrayText();
		
		batch.end();
		match.update();
	}

	/************Start Draw Methods***************/
	public void drawBoardShapes() {
		for (int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
			for (int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
				if (match.getState().equals("play") && selected != null
						&& selected.x == x && selected.y == y) {
					sr.setColor(Color.YELLOW);
					sr.rect(((Gdx.graphics.getWidth() / 2) - 250) + (x + 1) + (49 * x), ((Gdx.graphics.getHeight() / 2) - 200) + (y + 1) + (49 * (y - 1)), 50, 50);
				}
			}
		}
	}

	public void drawTrayShapes() {
		for(int x = 0; x < 2; x++) {
			for (int y = 0; y < 6; y++) {
				if (match.getState().equals("make") && selected != null
						&& selected.x == x && selected.y == y) {
					sr.setColor(Color.YELLOW);
					sr.rect((23)+(x+1)+(75*x), (80)+(y+1)+(75*y), 75, 75);
				}
			}
		}
	}

	public void drawTrayTextures() {
		//Player tray
		TrayPiece[] playerTray = match.getGameBoard().getPlayerTray();
		TrayPiece[] computerTray = match.getGameBoard().getComputerTray();
		for(int x = 0; x < 2; x++) {
			for(int y = 0; y < 6; y++) {
				//Get Piece index
				int i = (y+x)+(5*x);
				Texture current = getPieceTexture(playerTray[i].getRank(), 0);
				batch.draw(current,(23)+(x+1)+(75*x), (80)+(y+1)+(75*y), 75, 75);
			}
		}
		//Computer Tray
		for(int x = 0; x < 2; x++) {
			for(int y = 0; y < 6; y++) {
				//Get Piece index
				int i = (y+x)+(5*x);
				Texture current = getPieceTexture(computerTray[i].getRank(), 1);
				batch.draw(current,(785)+(x+1)+(75*x), (80)+(y+1)+(75*y), 75, 75);
			}
		}
	}

	public Texture getPieceTexture(char rank, int teamNum) {
		switch(rank) {
			case '1':
				if(teamNum == 0) return blue1;
				else return red1;
			case '2':
				if(teamNum == 0) return blue2;
				else return red2;
			case '3':
				if(teamNum == 0) return blue3;
				else return red3;
			case '4':
				if(teamNum == 0) return blue4;
				else return red4;
			case '5':
				if(teamNum == 0) return blue5;
				else return red5;
			case '6':
				if(teamNum == 0) return blue6;
				else return red6;
			case '7':
				if(teamNum == 0) return blue7;
				else return red7;
			case '8':
				if(teamNum == 0) return blue8;
				else return red8;
			case '9':
				if(teamNum == 0) return blue9;
				else return red9;
			case 'B':
				if(teamNum == 0) return blueB;
				else return redB;
			case 'F':
				if(teamNum == 0) return blueF;
				else return redF;
			case 'S':
				if(teamNum == 0) return blueS;
				else return redS;
			default:
				if(teamNum == 0) return blueBack;
				else return redBack;
		}
	}

	public void drawBoardTextures() {
		for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
			for(int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
				if(match.getBoard()[x][y] != null) {
					Texture current;
					if(match.getBoard()[x][y].getTeamNumber() == 0) {
						current = getPieceTexture(match.getBoard()[x][y].getRank(), 0);
						batch.draw(current,((Gdx.graphics.getWidth()/2)-250)+(x+1)+(49*x),((Gdx.graphics.getHeight()/2)-200)+(y+1)+(49*(y-1)),50,50);
						//font.draw(batch, Character.toString(match.getBoard()[x][y].getRank()), ((Gdx.graphics.getWidth()/2)-250)+(x+1)+(49*x)+20, ((Gdx.graphics.getHeight()/2)-200)+(y+1)+(49*(y-1))+30);
					}
					else if(match.getBoard()[x][y].getTeamNumber() == 1) {
						if(match.getBoard()[x][y].isRevealed()) {
							current = getPieceTexture(match.getBoard()[x][y].getRank(), 1);
						}else current = getPieceTexture(' ', 1);
						batch.draw(current,((Gdx.graphics.getWidth()/2)-250)+(x+1)+(49*x),((Gdx.graphics.getHeight()/2)-200)+(y+1)+(49*(y-1)),50,50);

						/*if(match.getBoard()[x][y].isRevealed()
								|| !match.getBoard()[x][y].isRevealed()) {
							font.setColor(Color.WHITE);
							font.draw(batch, Character.toString(match.getBoard()[x][y].getRank()), ((Gdx.graphics.getWidth()/2)-250)+(x+1)+(49*x)+20, ((Gdx.graphics.getHeight()/2)-200)+(y+1)+(49*(y-1))+30);
						}*/
					}
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
				//Draw piece remaining
				font.setColor(Color.YELLOW);
				font.draw(batch, Integer.toString(playerTray[i].getRemaining()), (23)+(x+1)+(75*x)+65,(80)+(y+1)+(75*y)+75);

			}
		}
		TrayPiece[] computerTray = match.getGameBoard().getComputerTray();
		for(int x = 0; x < 2; x++) {
			for(int y = 0; y < 6; y++) {
				//Get Piece index
				int i = (y+x)+(5*x);
				//Draw piece remaining
				font.setColor(Color.YELLOW);
				font.draw(batch, Integer.toString(computerTray[i].getRemaining()), (785)+(x+1)+(75*x)+65,(80)+(y+1)+(75*y)+75);
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
	public boolean keyUp(int keycode) {return false;}
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
