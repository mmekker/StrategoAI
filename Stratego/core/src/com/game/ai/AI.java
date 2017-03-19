package com.game.ai;

import com.game.stratego.Board;
import com.game.stratego.Move;
import com.game.stratego.Piece;

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
}
