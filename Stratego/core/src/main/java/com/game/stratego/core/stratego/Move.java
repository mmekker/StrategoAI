package com.game.stratego.core.stratego;

import java.awt.*;

/**
 * Created by user on 3/18/2017.
 */
public class Move {
    public Point source;
    public Point destination;

    public Move(Point s, Point d) {
        source = s;
        destination = d;
    }

    public String toString() {
        return "(" + source.x + "," + source.y + ") -> (" + destination.x + "," + destination.y + ")";
    }
}
