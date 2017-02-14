package com.game.stratego;

public class Board {
	private Piece[][] pieces;
	
	public static final int BOARD_SIZE = 10;
	
	public Board() {
		pieces = new Piece[BOARD_SIZE][BOARD_SIZE];
	}
	
	public boolean movePiece(int x1, int y1, int x2, int y2) {
		if(pieces[x1][y1] == null
				|| pieces[x1][y1].getRank() == 'B'
				|| pieces[x1][y1].getRank() == 'F') { //Illegal moves
			return false;
		}
		else if(pieces[x2][y2] == null) { //if the spot is empty
			pieces[x2][y2] = pieces[x1][y1];
			pieces[x2][y2].setHasMoved(true);
			pieces[x1][y1] = null;
			return true;
		}
		else if(pieces[x1][y1].getTeamNumber() == pieces[x2][y2].getTeamNumber()) { //if they're on the same team
			return false;
		}
		else {
			if(pieces[x2][y2].getRank() == 'F') { //Flag found
				//Win
			}
			else if(pieces[x2][y2].getRank() == 'B') { //Bomb found
				if(pieces[x1][y1].getRank() == '8') { //Bomb defused by miner
					pieces[x2][y2] = pieces[x1][y1];
					pieces[x2][y2].setHasMoved(true);
					pieces[x1][y1] = null;
					return true;
				}
				else { 
					pieces[x1][y1] = null; //Piece defeated by bomb
					return true;
				}
			}
			else if(pieces[x1][y1].getRank() == 'S') { //Spy is attacking
				if(pieces[x2][y2].getRank() == '1') { //if the spy is attacking a marshal
					pieces[x2][y2] = pieces[x1][y1];
					pieces[x2][y2].setHasMoved(true);
					pieces[x1][y1] = null;
					return true;
				}
				else {
					pieces[x1][y1] = null; //Piece defeated
					return true;
				}
			}
			else {
				int rank1 = Integer.getInteger(Character.toString(pieces[x1][y1].getRank()));
				int rank2 = Integer.getInteger(Character.toString(pieces[x2][y2].getRank()));
				if(rank1 < rank2) { //Better piece lives
					pieces[x2][y2] = pieces[x1][y1];
					pieces[x2][y2].setHasMoved(true);
					pieces[x1][y1] = null;
					return true;
				}
				else {
					pieces[x1][y1] = null; //Piece defeated
					return true;
				}
			}
			
		}
		return false;
	}
}
