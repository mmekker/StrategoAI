package com.game.stratego.core.ai;

import com.game.stratego.core.stratego.Board;
import com.game.stratego.core.stratego.Move;
import com.game.stratego.core.stratego.Piece;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by user on 3/18/2017.
 */
public class AI {
    private Piece[][] board;

    public AI(Piece[][] board) {
        this.board = board;
    }

    public Move getMove(Piece[][] nBoard) {
        this.board = nBoard;
        System.out.println("INDArray: " + getINDArray(board).toString());
        ArrayList<Move> possibleMoves = new ArrayList<Move>();
        for(int x1 = 0; x1 < Board.DEFAULT_BOARD_SIZE; x1++) {
            for(int y1 = 0; y1 < Board.DEFAULT_BOARD_SIZE; y1++) {
                for(int x2 = 0; x2 < Board.DEFAULT_BOARD_SIZE; x2++) {
                    for(int y2 = 0; y2 < Board.DEFAULT_BOARD_SIZE; y2++) {
                        if(checkMove(new Point(x1,y1), new Point(x2,y2))) {
                            Move m = new Move(new Point(x1,y1), new Point(x2,y2));
                            System.out.println(m.toString());
                            possibleMoves.add(m);
                        }
                    }
                }
            }
        }
        if(!possibleMoves.isEmpty()) {
            return possibleMoves.get(0);
        }
        return new Move(new Point(), new Point());
    }

    public boolean checkMove(Point p1, Point p2) {
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
        } else if (board[x1][y1].getTeamNumber() != 1){
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

    public INDArray getINDArray(Piece[][] board) {
        float[][] b1 = new float[Board.DEFAULT_BOARD_SIZE][Board.DEFAULT_BOARD_SIZE]; //Player's immoveable pieces
        float[][] b2 = new float[Board.DEFAULT_BOARD_SIZE][Board.DEFAULT_BOARD_SIZE]; //Player's movable pieces
        float[][] b3 = new float[Board.DEFAULT_BOARD_SIZE][Board.DEFAULT_BOARD_SIZE]; //Opponents's known movable pieces
        float[][] b4 = new float[Board.DEFAULT_BOARD_SIZE][Board.DEFAULT_BOARD_SIZE]; //Opponent's known bombs
        float[][] b5 = new float[Board.DEFAULT_BOARD_SIZE][Board.DEFAULT_BOARD_SIZE]; //Opponents unknown moved pieces
        float[][] b6 = new float[Board.DEFAULT_BOARD_SIZE][Board.DEFAULT_BOARD_SIZE]; //Opponents unknown unmoved pieces

        for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
            for(int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
                if(board[x][y] == null) {continue;}
                if(board[x][y].getTeamNumber() == 1) {
                    char rank = board[x][y].getRank();
                    if(rank == 'F') {
                        b1[x][y] = 10;
                    }
                    else if (rank == 'B') {
                        b1[x][y] = 11;
                    }
                    else if (rank == 'S') {
                        b2[x][y] = 12;
                    }
                    else {
                        b2[x][y] = (float)(Character.getNumericValue(rank));
                    }
                }
                else {
                    char rank = board[x][y].getRank();
                    if(rank == 'F') {
                        b6[x][y] = 10;
                    }
                    else if (rank == 'B') {
                        if(board[x][y].isRevealed()) {
                            b4[x][y] = 11;
                        }
                        else {
                            b6[x][y] = 11;
                        }
                    }
                    else if (rank == 'S') {
                        if(board[x][y].hasMoved()) {
                            b3[x][y] = 12;
                        }
                        else {
                            b5[x][y] = 12;
                        }
                    }
                    else {
                        if(board[x][y].hasMoved()) {
                            b3[x][y] = (float)(Character.getNumericValue(rank));
                        }
                        else {
                            b5[x][y] = (float)(Character.getNumericValue(rank));
                        }
                    }
                }
            } //End for y
        } //End for x
        float[] f = new float[(Board.DEFAULT_BOARD_SIZE * Board.DEFAULT_BOARD_SIZE) * 6];
        int index = 0;
        for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
            for (int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
                f[index] = b1[x][y];
                index++;
            }
        }
        for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
            for (int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
                f[index] = b2[x][y];
                index++;
            }
        }
        for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
            for (int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
                f[index] = b3[x][y];
                index++;
            }
        }
        for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
            for (int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
                f[index] = b4[x][y];
                index++;
            }
        }
        for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
            for (int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
                f[index] = b5[x][y];
                index++;
            }
        }
        for(int x = 0; x < Board.DEFAULT_BOARD_SIZE; x++) {
            for (int y = 0; y < Board.DEFAULT_BOARD_SIZE; y++) {
                f[index] = b6[x][y];
                index++;
            }
        }
        INDArray nd = Nd4j.create(f, new int[]{10,10,6});
        return nd;
    }
}
