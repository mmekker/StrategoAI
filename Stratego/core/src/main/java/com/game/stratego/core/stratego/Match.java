package com.game.stratego.core.stratego;

import com.game.stratego.core.ai.AI;
import com.game.stratego.core.screens.GameScreen;

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
				Piece[][] temp = Board.cloneBoard(this.getBoard());
				Move m = computerPlayer.getMove(temp);
				int x1 = m.source.x;
				int y1 = m.source.y;
				int x2 = m.destination.x;
				int y2 = m.destination.y;

				if(getGameBoard().movePiece(x1, y1, x2, y2)) {
					setCurrentTurn(0);
					game.setMessage("Move (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")");
				}
				else {
					game.setMessage("Error with AI Move.");
				}
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
