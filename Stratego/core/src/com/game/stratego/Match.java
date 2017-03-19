package com.game.stratego;

import com.game.ai.AI;
import com.game.screens.GameScreen;

public class Match {
	private Board board;
	private GameScreen game;
	private AI computerPlayer;
	
	private int currentTurn; //0 = Player; 1 = Computer
	private String state;
	
	public Match(GameScreen game) {
		board = new Board();
		currentTurn = 0;
		state = "make";
		this.game = game;
		getGameBoard().createComputerSetup();
		//getGameBoard().createPlayerSetup();
		computerPlayer = new AI(this.getBoard().clone());
	}
	
	
	public void update() {
		if(state.equals("make")) {
			if(board.isTrayEmpty(board.getComputerTray())
					&& board.isTrayEmpty(board.getPlayerTray())) {
				state = "play";
			}
		}
		else if(state.equals("play")) {
			if(currentTurn == 1) { //Computer turn
				computerPlayer.getMove(this.getBoard().clone());
				int x1 = (int)(Math.random()*10);
				int y1 = (int)(Math.random()*10);
				int x2 = (int)(Math.random()*10);
				int y2 = (int)(Math.random()*10);
				while(getBoard()[x1][y1] != null
						&& getBoard()[x1][y1].getTeamNumber() == 0
						|| !getGameBoard().movePiece(x1, y1, x2, y2)) {
					x1 = (int)(Math.random()*10);
					y1 = (int)(Math.random()*10);
					x2 = (int)(Math.random()*10);
					y2 = (int)(Math.random()*10);
				}
				setCurrentTurn(0);
				game.setMessage("Move (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")");
			}
			if(board.isGameFinished()) {
				state = "end";
			}
		}
	}


	public Piece[][] getBoard() {
		return board.getBoard();
	}


	public void setBoard(Board board) {
		this.board = board;
	}
	
	public Board getGameBoard() {
		return board;
	}

	public int getCurrentTurn() {
		return currentTurn;
	}


	public void setCurrentTurn(int currentTurn) {
		this.currentTurn = currentTurn;
	}


	public String getState() {
		return state;
	}


	public void setState(String state) {
		this.state = state;
	}
	
}
