package com.game.stratego;

public class Board {
	private Piece[][] board;
	private TrayPiece[] playerTray;
	private TrayPiece[] computerTray;
	
	public static final int DEFAULT_BOARD_SIZE = 10;
	public static final int NUMBER_OF_PIECES = 12;
	
	public Board() {
		board = new Piece[DEFAULT_BOARD_SIZE][DEFAULT_BOARD_SIZE];
		playerTray = new TrayPiece[NUMBER_OF_PIECES];
		playerTray[0] = new TrayPiece('1', 1); //1 Marshal
		playerTray[1] = new TrayPiece('2', 1); //1 General
		playerTray[2] = new TrayPiece('3', 2); //2 Colonels
		playerTray[3] = new TrayPiece('4', 3); //3 Majors
		playerTray[4] = new TrayPiece('5', 4); //4 Captains
		playerTray[5] = new TrayPiece('6', 4); //4 Lieutenants
		playerTray[6] = new TrayPiece('7', 4); //4 Sergeants
		playerTray[7] = new TrayPiece('8', 5); //5 Miners
		playerTray[8] = new TrayPiece('9', 8); //8 Scouts
		playerTray[9] = new TrayPiece('S', 1); //1 Spy
		playerTray[10] = new TrayPiece('B', 6); //6 Bombs
		playerTray[11] = new TrayPiece('F', 1); //1 Flag
		computerTray = playerTray.clone();
	}
	
	public boolean validRank(char rank) {
		for(int x = 0; x < NUMBER_OF_PIECES; x++) {
			if(playerTray[x].getRank() == rank && playerTray[x].getRemaining() > 0) {
				return true;
			}
		}
		return false;
	}
	
	public boolean movePiece(int x1, int y1, int x2, int y2) {
		if(x1 >= DEFAULT_BOARD_SIZE || y1 >= DEFAULT_BOARD_SIZE
				|| x2 >= DEFAULT_BOARD_SIZE || y2 >= DEFAULT_BOARD_SIZE){ //move is off the board
			return false;
		}
		else if(   isWater(x1,y1) || isWater(x2,y2)) {
			return false;
		}
		else if(board[x1][y1] == null
				|| board[x1][y1].getRank() == 'B'
				|| board[x1][y1].getRank() == 'F') { //Illegal moves
			return false;
		}
		else if(   (board[x1][y1].getRank() != '9') &&
				   ((Math.abs(x1-x2) > 1 || Math.abs(y1-y2) > 1) //Move must be 1 space away (non-scout)
				|| (Math.abs(x1-x2) == 1 && Math.abs(y1-y2) == 1))) { //No diagonals
			return false;
		}
		else if(   (board[x1][y1].getRank() == '9') &&
				   ((Math.abs(x1-x2) >= 1 && Math.abs(y1-y2) >= 1)
				|| (Math.abs(x1-x2) == 1 && Math.abs(y1-y2) == 1))) { //No diagonals
			return false;
		}
		else if(   (board[x1][y1].getRank() == '9') &&
					someoneInTheWay(x1, y1, x2, y2)) {
			return false;
		}
		else if(board[x2][y2] == null) { //if the spot is empty
			board[x2][y2] = board[x1][y1];
			board[x2][y2].setIsRevealed(true);
			board[x1][y1] = null;
			return true;
		}
		else if(board[x1][y1].getTeamNumber() == board[x2][y2].getTeamNumber()) { //if they're on the same team
			return false;
		}
		else {
			if(board[x2][y2].getRank() == 'F') { //Flag found
				//Win
			}
			else if(board[x2][y2].getRank() == 'B') { //Bomb found
				if(board[x1][y1].getRank() == '8') { //Bomb defused by miner
					board[x2][y2] = board[x1][y1];
					board[x2][y2].setIsRevealed(true);
					board[x1][y1] = null;
					return true;
				}
				else { 
					board[x1][y1] = null; //Piece defeated by bomb
					return true;
				}
			}
			else if(board[x1][y1].getRank() == 'S') { //Spy is attacking
				if(board[x2][y2].getRank() == '1') { //if the spy is attacking a marshal
					board[x2][y2] = board[x1][y1];
					board[x2][y2].setIsRevealed(true);
					board[x1][y1] = null;
					return true;
				}
				else {
					board[x1][y1] = null; //Piece defeated
					return true;
				}
			}
			else {
				int rank1 = Character.getNumericValue(board[x1][y1].getRank());
				int rank2 = Character.getNumericValue(board[x2][y2].getRank());
				if(rank1 < rank2) { //Better piece lives
					board[x2][y2] = board[x1][y1];
					board[x2][y2].setIsRevealed(true);
					board[x1][y1] = null;
					return true;
				}
				else if(rank1 == rank2) {
					board[x1][y1] = null;
					board[x2][y2] = null;
					return true;
				}
				else {
					board[x1][y1] = null; //Piece defeated
					board[x2][y2].setIsRevealed(true);
					return true;
				}
			}
			
		}
		return false;
	}

	public static boolean isWater(int x, int y) {
		if(  	   (x == 2 && y == 4) //Check if the point is in the water
				|| (x == 2 && y == 5)
				|| (x == 3 && y == 4)
				|| (x == 3 && y == 5)
				|| (x == 6 && y == 4)
				|| (x == 6 && y == 5)
				|| (x == 7 && y == 4)
				|| (x == 7 && y == 5)) {
			return true;
		}
		return false;
	}
	
	private boolean someoneInTheWay(int x1, int y1, int x2, int y2) {

		if(Math.abs(x1-x2) == 0) { //Is vertical move
			if(y1 > y2) {
				for(int y = y2+2; y < y1; y++) {
					if(board[x1][y] != null
							|| isWater(x1,y)) {
						return true;
					}
				}
			}
			else {
				for(int y = y1+2; y < y2; y++) {
					if(board[x1][y] != null
							|| isWater(x1,y)) {
						return true;
					}
				}
			}
		}
		else { //Is horizontal move
			if(x1 > x2) {
				for(int x = x2+2; x < x1; x++) {
					if(board[x][y1] != null
							|| isWater(x,y1)) {
						return true;
					}
				}
			}
			else {
				for(int x = x1+2; x < x2; x++) {
					if(board[x][y1] != null
							|| isWater(x,y1)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public Piece[][] getBoard() {
		return board;
	}

	public void setBoard(Piece[][] board) {
		this.board = board;
	}

	public TrayPiece[] getPlayerTray() {
		return playerTray;
	}

	public void setPlayerTray(TrayPiece[] playerTray) {
		this.playerTray = playerTray;
	}

	public TrayPiece[] getComputerTray() {
		return computerTray;
	}

	public void setComputerTray(TrayPiece[] computerTray) {
		this.computerTray = computerTray;
	}
	
	
}
