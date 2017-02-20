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
			for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
				for(int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
					if(!((x == 2 && y == 4)
							|| (x == 2 && y == 5)
							|| (x == 3 && y == 4)
							|| (x == 3 && y == 5)
							|| (x == 6 && y == 4)
							|| (x == 6 && y == 5)
							|| (x == 7 && y == 4)
							|| (x == 7 && y == 5))) {
						int t = (int)(Math.random()*9)+1;
						String rank = Integer.toString(t);
						getBoard()[x][y] = new Piece(rank.charAt(0), (int)(Math.random()*2));
					}
				}
			}
			state = "play";
		}
		else if(state.equals("play")) {
			
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
