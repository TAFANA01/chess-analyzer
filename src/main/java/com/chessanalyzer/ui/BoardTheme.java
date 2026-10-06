package com.chessanalyzer.ui;

import javafx.scene.paint.Color;

import java.util.List;

/** Colours of the squares and highlights. Piece and arrow colours are shared by all themes. */
public record BoardTheme(String name, Color light, Color dark, Color lastMove) {

    public static final BoardTheme GREEN = new BoardTheme("Green",
            Color.web("#eeeed2"), Color.web("#769656"), Color.web("#f6f669", 0.55));
    public static final BoardTheme BLUE = new BoardTheme("Blue",
            Color.web("#dee3e6"), Color.web("#8ca2ad"), Color.web("#f6f669", 0.50));
    public static final BoardTheme BROWN = new BoardTheme("Brown",
            Color.web("#f0d9b5"), Color.web("#b58863"), Color.web("#f6f669", 0.50));

    public static List<BoardTheme> all() {
        return List.of(GREEN, BLUE, BROWN);
    }

    /** Coordinate labels use the opposite square colour for contrast. */
    public Color coordinateColorOn(boolean lightSquare) {
        return lightSquare ? dark : light;
    }

    @Override
    public String toString() {
        return name;
    }
}
