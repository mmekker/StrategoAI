package com.game.stratego.core.stratego;

import com.game.stratego.core.ai.NeuralNetAI;
import com.game.stratego.core.screens.GameScreen;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

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
		computerPlayer = new NeuralNetAI(false, "NeuralNetwork19.zip");
	}


	public void update() {
		if (state.equals("make")) {
			if (board.isTrayEmpty(board.getComputerTray())
					&& board.isTrayEmpty(board.getPlayerTray())) {
				state = "play";
			}
		} else if (state.equals("play")) {
			if (currentTurn == 1) { //Computer turn
				Piece[][] temp = Board.cloneBoard(this.getBoard());
				Move m = computerPlayer.getMove(temp, 1, true, false);
				if (m == null) {
					this.getGameBoard().setGameFinished(true);
					this.getGameBoard().setWinner(0);
				} else {
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
						game.setMessage("Error with AI Player) Move.");
					}
				}
			}*/
			if (board.isGameFinished()) {
				state = "end";
			}
		}
	}

	public static void main(String args[]) {
		for(int x = 0; x < 1; x++) {
			System.out.println("Go: " + x);
			go();
		}
	}

	public static void go() {
		System.out.println("Load NeuralNetwork");
		NeuralNetAI Ai1 = null;
		Ai1 = new NeuralNetAI(true, "NeuralNetwork.zip"); //Save to this one
		MultiLayerNetwork network = Ai1.getNetwork();
		//Get data
		ArrayList<DataSet> data = new ArrayList<DataSet>();

		int numOfGames = 700;
		DataSetIterator iter = new ListDataSetIterator(data);

		System.out.println("Playing " + numOfGames + " games.");
		for(int x = 0; x < numOfGames; x++) {
			System.out.println("Game " + x + ":");
			ArrayList<DataSet> temp = playGame(false);
			if(temp != null && !temp.isEmpty())
				data.addAll(temp);
			else {
				x--;
				continue;
			}
		}


		iter = new ListDataSetIterator(data, 5);
		//train
		System.out.println("Training over " + data.size() + " DataSets...");
		while(iter.hasNext()) {
			DataSet next = iter.next();
			network.fit(next);
			INDArray predict2 = network.output(next.getFeatureMatrix());
			System.out.println("outputs: " + predict2.toString());
		}

		//eval
		iter.reset();
		System.out.println("Evaluating...");
		Evaluation eval = new Evaluation();
		while (iter.hasNext()) {
			DataSet next = iter.next();
			INDArray predict2 = network.output(next.getFeatureMatrix());
			eval.eval(next.getLabels(), predict2);
		}

		System.out.println(eval.stats());
		//Save network
		System.out.println("Saving network.");
		try {
			Ai1.saveNet();
		} catch(IOException e) {
			e.printStackTrace();
		}
		data = new ArrayList<DataSet>();
		//Save network
        System.out.println("Saving network.");
		try {
			Ai1.saveNet();
		} catch(IOException e) {
			e.printStackTrace();
		}
		System.out.println("Network Saved. ");
	}
	public static int lastWin = 0;
	public static ArrayList<DataSet> playGame(boolean debug) {
		boolean DEBUG = debug;
		boolean randomMoves = false;
		//Create initial board
		Board board = new Board();
		board.createComputerSetup();
		board.createPlayerSetup();
		//Create empty datasets
		float dataset1Label = 0;
		ArrayList<INDArray> dataset1 = new ArrayList<INDArray>();
		float dataset2Label = 0;
		ArrayList<INDArray> dataset2 = new ArrayList<INDArray>();
		if(DEBUG) System.out.println("     Create Ai1");
		NeuralNetAI MoveAi = new NeuralNetAI(false, "NeuralNetwork19.zip"); //This is what it battles against
		int turnNum = 0;
		int fullTurnNum = 0;
		System.out.println("     Start game.");
		while(board.isGameFinished() != true) {
			if(fullTurnNum > 100) {
				System.out.println("Too many turns.");
				return null;
			}
			if(turnNum == 1) { //Ai
				if(DEBUG) System.out.println("     Ai's turn");
				Move m = MoveAi.getMove(Board.cloneBoard(board.getBoard()), 1, false, randomMoves);
				if(m == null) {
					/*turnNum = 0;
					INDArray b = NeuralNetAI.getINDArray(board.getBoard());
					dataset2.add(b);
					break;*/
					System.out.println("Ai has no more moves.");
					return null;
				}
				int x1 = m.source.x;
				int y1 = m.source.y;
				int x2 = m.destination.x;
				int y2 = m.destination.y;
				board.movePiece(x1, y1, x2, y2);
				INDArray b = NeuralNetAI.getINDArray(board.getBoard());
				dataset1.add(b);
				if(flagTaken(board, 0)) { //if team 0's flag was taken
					turnNum = 1;
					break;
				}
				turnNum = 0;
			}
			else if(turnNum == 0) { //Ai Player
				if (DEBUG) System.out.println("     Ai Player's turn");
				Move m = MoveAi.getMove(Board.cloneBoard(board.getBoard()), 0, false, true);
				if (m == null) {
					/*turnNum = 1;
					INDArray b = NeuralNetAI.getINDArrayFlipped(board.getBoard());
					dataset1.add(b);
					break;*/
					System.out.println("Random has no more moves.");
					return null;
				}
				int x1 = m.source.x;
				int y1 = m.source.y;
				int x2 = m.destination.x;
				int y2 = m.destination.y;
				board.movePiece(x1, y1, x2, y2);
				INDArray b = NeuralNetAI.getINDArrayFlipped(board.getBoard());
				dataset2.add(b);
				if(flagTaken(board, 1)) { //if team 1's flag was taken
					turnNum = 0;
					break;
				}
				turnNum = 1;
			}
			fullTurnNum++;
		}
		System.out.println("     Game over. Ai"+(turnNum+1)+" won.");
		if(turnNum == lastWin) {
			return null;
		}
		else {
			lastWin = turnNum;
		}
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

		//iterate through dataset arrays and set labels

		if(DEBUG) System.out.println("     Create d1");
		ArrayList<DataSet> d1 = new ArrayList<DataSet>();
		float[] f1 = new float[2];
		f1[0] = dataset1Label;
		f1[1] = dataset2Label;
		INDArray label1 = Nd4j.create(f1, new int[]{2});
		for(INDArray data1:dataset1) {
			d1.add(new DataSet(data1,label1));
		}

		if(DEBUG) System.out.println("     Create d2");
		ArrayList<DataSet> d2 = new ArrayList<DataSet>();
		float[] f2 = new float[2];
		f2[0] = dataset2Label;
		f2[1] = dataset1Label;
		INDArray label2 = Nd4j.create(f2, new int[]{2});
		for(INDArray data2:dataset2) {
			d2.add(new DataSet(data2,label2));
		}
		if(DEBUG) System.out.println("     Combine + Print d1");
		//Combine
		d1.addAll(d2);
		if(DEBUG) {
			for(DataSet d : d1) {
				System.out.println(d.toString());
			}
		}
		System.out.println("Number of Turns: " + fullTurnNum);
		//System.out.println("x: " + x);
		//System.out.println("y: " + y);
		long seed = System.nanoTime();
		Collections.shuffle(d1, new Random(seed));
		return d1;
	}

	private static boolean flagTaken(Board board, int teamNum) {
		Piece[][] b = board.getBoard();
		for(int x = 0; x < 10; x++) {
			for(int y = 0; y < 10; y++) {
				if(b[x][y] != null && b[x][y].getTeamNumber() == teamNum && b[x][y].getRank() == 'F') return false;
			}
		}
		return true;
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
