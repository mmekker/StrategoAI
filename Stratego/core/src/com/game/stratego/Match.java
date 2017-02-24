package com.game.stratego;

public class Match {
	private Board board;
	
	private int currentTurn; //0 = Player; 1 = Computer
	private String state;
	
	public Match() {
		board = new Board();
		
		currentTurn = 0;
		state = "make";
	}
	
	
	public void update() {
		if(state.equals("make")) {
			/*for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
				for(int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
					if(!Board.isWater(x,y)) {
						int t = (int)(Math.random()*9)+1;
						String rank = Integer.toString(t);
						getBoard()[x][y] = new Piece(rank.charAt(0), (int)(Math.random()*2));
					}
				}
			}*/
			getBoard()[0][0] = new Piece('9', 0);
			getBoard()[9][0] = new Piece('7', 0);
			getBoard()[5][0] = new Piece('5', 1);
			getBoard()[9][9] = new Piece('4', 1);

			state = "play";
		}
		else if(state.equals("play")) {
			if(currentTurn == 1) { //Computer turn
				//getComputerMove()
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
