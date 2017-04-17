package com.game.stratego.core.stratego;

import com.game.stratego.core.ai.NeuralNetAI;
import com.game.stratego.core.screens.GameScreen;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.ArrayList;

public class Match {
	private Board board;
	private GameScreen game;
	private NeuralNetAI computerPlayer;
	
	private int currentTurn; //0 = Player; 1 = Computer
	private String state;
	
	public Match(GameScreen game) {
		board = new Board();
		currentTurn = 0;
		state = "make";
		this.game = game;
		getGameBoard().createComputerSetup();
		//getGameBoard().createPlayerSetup();
		computerPlayer = new NeuralNetAI(this.getBoard().clone());
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
				Move m = computerPlayer.getMove(temp, 1, true, false);
				if(m == null) {
					this.getGameBoard().setGameFinished(true);
					this.getGameBoard().setWinner(0);
				}
				else {
					int x1 = m.source.x;
					int y1 = m.source.y;
					int x2 = m.destination.x;
					int y2 = m.destination.y;

					if (getGameBoard().movePiece(x1, y1, x2, y2)) {
						setCurrentTurn(0);
						game.setMessage("Move (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")");
					} else {
						game.setMessage("Error with AI Move.");
					}
				}
			}
			/*else {
				Piece[][] temp = Board.cloneBoard(this.getBoard());
				Move m = computerPlayer.getMove(temp, 0, true, true);
				if(m == null) {
					this.getGameBoard().setGameFinished(true);
					this.getGameBoard().setWinner(1);
				}
				else {
					int x1 = m.source.x;
					int y1 = m.source.y;
					int x2 = m.destination.x;
					int y2 = m.destination.y;

					if (getGameBoard().movePiece(x1, y1, x2, y2)) {
						setCurrentTurn(1);
						game.setMessage("Move (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")");
					} else {
						game.setMessage("Error with AI Move.");
					}
				}
			}*/
			if(board.isGameFinished()) {
				state = "end";
			}
		}
	}

	public static void main(String[] args) {
		//Create initial board
		Board board = new Board();
		board.createComputerSetup();
		board.createPlayerSetup();
		//Create empty datasets
		int dataset1Label = 0;
		ArrayList<INDArray> dataset1 = new ArrayList<INDArray>();
		int dataset2Label = 0;
		ArrayList<INDArray> dataset2 = new ArrayList<INDArray>();
		System.out.println("Create Ai1");
		NeuralNetAI Ai1 = new NeuralNetAI(board.getBoard());
		int turnNum = 0;
		System.out.println("Start game.");
		while(board.isGameFinished() != true) {
			if(turnNum == 0) { //Ai1
				System.out.println("Ai1's turn");
				Move m = Ai1.getMove(Board.cloneBoard(board.getBoard()), 0, false, true);
				if(m == null) {
					turnNum = 1;
					break;
				}
				int x1 = m.source.x;
				int y1 = m.source.y;
				int x2 = m.destination.x;
				int y2 = m.destination.y;
				board.movePiece(x1, y1, x2, y2);
				INDArray b = NeuralNetAI.getINDArray(board.getBoard());
				dataset1.add(b);
				turnNum = 1;
			}
			if(turnNum == 1) { //Ai2
				System.out.println("Ai2's turn");
				Move m = Ai1.getMove(Board.cloneBoard(board.getBoard()), 1, false, true);
				if(m == null) {
					turnNum = 0;
					break;
				}
				int x1 = m.source.x;
				int y1 = m.source.y;
				int x2 = m.destination.x;
				int y2 = m.destination.y;
				board.movePiece(x1, y1, x2, y2);
				INDArray b = NeuralNetAI.getINDArray(board.getBoard());
				dataset2.add(b);
				turnNum = 0;
			}
		}
		System.out.println("Game over. Ai"+(turnNum+1)+" won.");

		//Label datasets with a 1 if that ai won
		//label with a 0 if they lost
		if(turnNum == 0) { //Ai1 won
			dataset1Label = 1;
			dataset2Label = 0;
		}
		else if(turnNum == 1) { //Ai1 won
			dataset1Label = 0;
			dataset2Label = 1;
		}
		//Put both datasets into a DataSet object
		//Load Neural Network
		//Train on above DataSet
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
