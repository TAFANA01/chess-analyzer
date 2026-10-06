package com.chessanalyzer.ui;

import com.chessanalyzer.board.Squares;

/** Maps squares to pixel positions for a square board of {@code size} pixels, optionally flipped (Black at the bottom). */
public final class BoardGeometry {

    private final double size;
    private final double square;
    private final boolean flipped;

    public BoardGeometry(double size, boolean flipped) {
        this.size = size;
        this.square = size / 8.0;
        this.flipped = flipped;
    }

    public double size() {
        return size;
    }

    public double squareSize() {
        return square;
    }

    public boolean flipped() {
        return flipped;
    }

    /** Column on screen (0 = left) of the square. */
    public int column(int sq) {
        return flipped ? 7 - Squares.file(sq) : Squares.file(sq);
    }

    /** Row on screen (0 = top) of the square. */
    public int row(int sq) {
        return flipped ? Squares.rank(sq) : 7 - Squares.rank(sq);
    }

    public double x(int sq) {
        return column(sq) * square;
    }

    public double y(int sq) {
        return row(sq) * square;
    }

    public double centerX(int sq) {
        return x(sq) + square / 2;
    }

    public double centerY(int sq) {
        return y(sq) + square / 2;
    }

    /** Square under a pixel, or {@link Squares#NONE} when outside the board. */
    public int squareAt(double px, double py) {
        if (px < 0 || py < 0 || px >= size || py >= size) return Squares.NONE;
        int col = (int) (px / square);
        int row = (int) (py / square);
        int file = flipped ? 7 - col : col;
        int rank = flipped ? row : 7 - row;
        return Squares.of(file, rank);
    }
}
