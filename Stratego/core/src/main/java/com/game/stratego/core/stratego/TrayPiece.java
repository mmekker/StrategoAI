package com.game.stratego.core.stratego;

public class TrayPiece {
	private char rank;
	private int remaining;
	
	public TrayPiece(char rank, int remaining) {
		this.rank = rank;
		this.remaining = remaining;
	}

	public Piece takePiece(int teamNumber) {
		if(remaining > 0) {
			remaining--;
			return (new Piece(rank, teamNumber));
		}
		return null;
	}

	public void returnPiece() {
		remaining++;
	}

	public char getRank() {
		return rank;
	}

	public void setRank(char rank) {
		this.rank = rank;
	}

	public int getRemaining() {
		return remaining;
	}

	public void setRemaining(int remaining) {
		this.remaining = remaining;
	}
	
}
