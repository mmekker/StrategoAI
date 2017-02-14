package com.game.stratego;

public class Piece {
	private char rank;
	private boolean hasMoved;
	private int teamNumber;
	
	public Piece(char rank, int teamNumber) {
		this.rank = rank;
		hasMoved = false;
		this.teamNumber = teamNumber;
	}

	public char getRank() {
		return rank;
	}

	public void setRank(char rank) {
		this.rank = rank;
	}

	public boolean isHasMoved() {
		return hasMoved;
	}

	public void setHasMoved(boolean hasMoved) {
		this.hasMoved = hasMoved;
	}

	public int getTeamNumber() {
		return teamNumber;
	}

	public void setTeamNumber(int teamNumber) {
		this.teamNumber = teamNumber;
	}

}
