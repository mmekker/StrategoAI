package com.game.stratego.core.ai;

import com.game.stratego.core.stratego.Board;
import com.game.stratego.core.stratego.Move;
import com.game.stratego.core.stratego.Piece;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
/**
 * Created by user on 3/18/2017.
 */
public class NeuralNetAI {
    private Piece[][] board;
    private MultiLayerNetwork network;

    public NeuralNetAI(boolean isNewNetwork, String path) {
        if(isNewNetwork) {
            this.network = BoardClassifier.getModel();
        }
        else {
            try {
                this.network = NeuralNetAI.loadNet(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.board = null;
    }

    public Move getMove(Piece[][] nBoard, int teamNum, boolean showText, boolean randomMoves) {
        this.board = nBoard;
        if(showText) System.out.println("Finding all possible moves.");
        //Find all possible moves
        ArrayList<Move> possibleMoves = new ArrayList<Move>();
        for(int x1 = 0; x1 < Board.DEFAULT_BOARD_SIZE; x1++) {
            for(int y1 = 0; y1 < Board.DEFAULT_BOARD_SIZE; y1++) {
                for(int x2 = 0; x2 < Board.DEFAULT_BOARD_SIZE; x2++) {
                    for(int y2 = 0; y2 < Board.DEFAULT_BOARD_SIZE; y2++) {
                        if(checkMove(new Point(x1,y1), new Point(x2,y2), teamNum)) {
                            Move m = new Move(new Point(x1,y1), new Point(x2,y2));
                            if(showText) System.out.println(m.toString());
                            possibleMoves.add(m);
                        }
                    }
                }
            }
        }

        if(showText) System.out.println("Finding all possible boards.");
        //Use list of possible moves to make list of possible boards
        ArrayList<Piece[][]> possibleBoards = new ArrayList<Piece[][]>();
        Board temp = new Board();
        temp.setBoard(Board.cloneBoard(this.board));
        for(Move m : possibleMoves) {
            temp.movePiece(m.source.x, m.source.y, m.destination.x, m.destination.y);
            possibleBoards.add(temp.getBoard());
            temp = new Board();
            temp.setBoard(Board.cloneBoard(this.board));
        }

        if(showText) System.out.println("Finding the board with the highest score.");
        //Find the board with the highest score
        if(!possibleBoards.isEmpty()) {
            if(randomMoves) {
                int rnd = (int)(Math.random()*possibleBoards.size());
                return possibleMoves.get(rnd);
            }
            else {
                int highestScoreIndex = -1;
                double highscore = 0;
                for (int x = 0; x < possibleBoards.size(); x++) {
                    double score;
                    if(teamNum == 1) {
                        score = getScore(possibleBoards.get(x));
                    }
                    else {
                        score = getScoreFlipped(possibleBoards.get(x));
                    }
                    //System.out.println("Score " + x + ": " + score);
                    if (highestScoreIndex != -1) {
                        if (score > (highscore)) {
                            highestScoreIndex = x;
                            highscore = score;
                        }
                    } else {
                        highestScoreIndex = x;
                        highscore = score;
                    }
                }
                if (showText) System.out.println("Current board state: \n" + boardString(this.board));
                if (showText) System.out.println("Desired board state: \n" + boardString(possibleBoards.get(highestScoreIndex)));
                if (showText) System.out.println("Desired move: " + possibleMoves.get(highestScoreIndex));
                if (showText) System.out.println("Desired board's score: " + highscore);
                return possibleMoves.get(highestScoreIndex);
            }
        }
        return null;
    }
    public void saveNet() throws IOException {
        File locationToSave = new File("assets/net/NeuralNetwork.zip");
        boolean saveUpdater = true;
        ModelSerializer.writeModel(this.network, locationToSave, saveUpdater);
    }

    public static MultiLayerNetwork loadNet(String path) throws IOException {
        File locationToSave = new File("assets/net/"+path);
        return ModelSerializer.restoreMultiLayerNetwork(locationToSave);
    }

    public String boardString(Piece[][] b) {
        String str = "";
        for(int y = b.length-1; y >= 0; y--) {
            for(int x = 0; x < b.length; x++) {
                if(b[x][y] != null)
                    str += (b[x][y].getRank() + "|");
                else
                    str += "_|";
            }
            str += "\n";
        }
        return str;
    }

    public double getScore(Piece[][] board) {
        //Get the input
        INDArray input = getINDArray(board);
        //Reformat input
        //get output
        INDArray output = network.output(input,true);
        return output.getDouble(0);
    }

    public double getScoreFlipped(Piece[][] board) {
        //Get the input
        INDArray input = getINDArrayFlipped(board);
        //Reformat input
        //get output
        INDArray output = network.output(input,true);
        return output.getDouble(0);
    }

    public boolean checkMove(Point p1, Point p2, int teamNum) {
        int x1 = p1.x;
        int y1 = p1.y;
        int x2 = p2.x;
        int y2 = p2.y;

        if (x1 >= Board.DEFAULT_BOARD_SIZE || y1 >= Board.DEFAULT_BOARD_SIZE
                || x2 >= Board.DEFAULT_BOARD_SIZE || y2 >= Board.DEFAULT_BOARD_SIZE) { //move is off the board
            return false;
        } else if (isWater(x1, y1) || isWater(x2, y2)) {
            return false;
        } else if (board[x1][y1] == null
                || board[x1][y1].getRank() == 'B'
                || board[x1][y1].getRank() == 'F') { //Illegal moves
            return false;
        } else if (board[x1][y1].getTeamNumber() != teamNum){
            return false;
        } else if ((board[x1][y1].getRank() != '9') &&
                ((Math.abs(x1 - x2) > 1 || Math.abs(y1 - y2) > 1) //Move must be 1 space away (non-scout)
                        || (Math.abs(x1 - x2) == 1 && Math.abs(y1 - y2) == 1))) { //No diagonals
            return false;
        } else if ((board[x1][y1].getRank() == '9') &&
                ((Math.abs(x1 - x2) >= 1 && Math.abs(y1 - y2) >= 1)
                        || (Math.abs(x1 - x2) == 1 && Math.abs(y1 - y2) == 1))) { //No diagonals
            return false;
        } else if ((board[x1][y1].getRank() == '9') &&
                someoneInTheWay(x1, y1, x2, y2)) {
            return false;
        } else if (board[x2][y2] == null) { //if the spot is empty
            return true;
        } else if (board[x1][y1].getTeamNumber() == board[x2][y2].getTeamNumber()) { //if they're on the same team
            return false;
        } else {
            if (board[x2][y2].getRank() == 'F') { //Flag found
                return true;
            } else if (board[x2][y2].getRank() == 'B') { //Bomb found
                return true;
            } else if (board[x1][y1].getRank() == 'S') { //Spy is attacking
                if (board[x2][y2].getRank() == '1') { //if the spy is attacking a marshal
                    return true;
                } else {

                    return true;
                }
            } else {
                return true;
            }

        }
    }

    public static boolean isWater(int x, int y) {
        if ((x == 2 && y == 4) //Check if the point is in the water
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

        if (Math.abs(x1 - x2) == 0) { //Is vertical move
            if (y1 > y2) {
                for (int y = y2 + 1; y < y1; y++) {
                    if (board[x1][y] != null
                            || isWater(x1, y)) {
                        return true;
                    }
                }
            } else {
                for (int y = y1 + 1; y < y2; y++) {
                    if (board[x1][y] != null
                            || isWater(x1, y)) {
                        return true;
                    }
                }
            }
        } else { //Is horizontal move
            if (x1 > x2) {
                for (int x = x2 + 1; x < x1; x++) {
                    if (board[x][y1] != null
                            || isWater(x, y1)) {
                        return true;
                    }
                }
            } else {
                for (int x = x1 + 1; x < x2; x++) {
                    if (board[x][y1] != null
                            || isWater(x, y1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static INDArray getINDArray(Piece[][] board) {
        float[][] b1 = new float[Board.DEFAULT_BOARD_SIZE][Board.DEFAULT_BOARD_SIZE]; //Player's immoveable pieces

        for(int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
            for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
                if(board[x][y] == null) {continue;}
                if(board[x][y].getTeamNumber() == 1) {
                    char rank = board[x][y].getRank();
                    if(rank == 'F') {
                        b1[x][y] = 12;
                    }
                    else if (rank == 'B') {
                        b1[x][y] = 11;
                    }
                    else if (rank == 'S') {
                        b1[x][y] = 10;
                    }
                    else {
                        b1[x][y] = getFloat(rank);
                    }
                }
                else {
                    char rank = board[x][y].getRank();
                    if(rank == 'F') {
                        b1[x][y] = 12 + 20;
                    }
                    else if (rank == 'B') {
                        b1[x][y] = 11 + 20;
                    }
                    else if (rank == 'S') {
                        b1[x][y] = 10 + 20;
                    }
                    else {
                        b1[x][y] = getFloat(rank) + 20;
                    }
                }
            } //End for y
        } //End for x
        float min = 0;
        float max = 32;
        float[] f = new float[(Board.DEFAULT_BOARD_SIZE * Board.DEFAULT_BOARD_SIZE) * 1];
        int index = 0;
        for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
            for (int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
                if((b1[x][y]) != 0)
                    f[index] = ((b1[x][y])-min)/(max-min);
                else
                    f[index] = 0;
                index++;
            }
        }

        INDArray nd = Nd4j.create(f, new int[]{1,1,10,10});

        //nd = Transforms.normalizeZeroMeanAndUnitVariance(nd);
        //nd = Nd4j.toFlattened(nd);
        //nd = Nd4j.vstack(nd);

        return nd;
    }

    public static INDArray getINDArrayFlipped(Piece[][] board) {
        float[][] b1 = new float[Board.DEFAULT_BOARD_SIZE][Board.DEFAULT_BOARD_SIZE]; //Player's immoveable pieces

        for(int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
            for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
                int nx = 9-x;
                int ny = 9-y;

                if(board[nx][ny] == null) {continue;}
                if(board[nx][ny].getTeamNumber() == 1) {
                    char rank = board[nx][ny].getRank();
                    if(rank == 'F') {
                        b1[nx][ny] = 12 + 20;
                    }
                    else if (rank == 'B') {
                        b1[nx][ny] = 11 + 20;
                    }
                    else if (rank == 'S') {
                        b1[nx][ny] = 10 + 20;
                    }
                    else {
                        b1[nx][ny] = getFloat(rank) + 20;
                    }
                }
                else {
                    char rank = board[nx][ny].getRank();
                    if(rank == 'F') {
                        b1[nx][ny] = 12;
                    }
                    else if (rank == 'B') {
                        b1[nx][ny] = 11;
                    }
                    else if (rank == 'S') {
                        b1[nx][ny] = 10;
                    }
                    else {
                        b1[nx][ny] = getFloat(rank);
                    }
                }
            } //End for y
        } //End for x
        float min = 0;
        float max = 32;
        float[] f = new float[(Board.DEFAULT_BOARD_SIZE * Board.DEFAULT_BOARD_SIZE) * 1];
        int index = 0;
        for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
            for (int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
                if((b1[x][y]) != 0)
                    f[index] = ((b1[x][y])-min)/(max-min);
                else
                    f[index] = 0;
                index++;
            }
        }

        INDArray nd = Nd4j.create(f, new int[]{1,1,10,10});

        //nd = Transforms.normalizeZeroMeanAndUnitVariance(nd);
        //nd = Nd4j.toFlattened(nd);
        //nd = Nd4j.vstack(nd);

        return nd;
    }

    public static float getFloat(char rank) {
        float f = 0;
        switch(rank) {
            case '1':
                f = 1;
                break;
            case '2':
                f = 2;
                break;
            case '3':
                f = 3;
                break;
            case '4':
                f = 4;
                break;
            case '5':
                f = 5;
                break;
            case '6':
                f = 6;
                break;
            case '7':
                f = 7;
                break;
            case '8':
                f = 8;
                break;
            case '9':
                f = 9;
                break;
            default:
                f = 0;
        }
        return f;
    }

    public MultiLayerNetwork getNetwork() {
        return this.network;
    }
}
