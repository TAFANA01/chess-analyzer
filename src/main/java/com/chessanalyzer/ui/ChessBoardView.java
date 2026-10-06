package com.chessanalyzer.ui;

import com.chessanalyzer.board.BoardScene;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Region;

/**
 * The visible chessboard: a square canvas that fills as much of its parent as it can, centred.
 * It is read-only by design - it has no mouse handlers and exposes no way to move pieces - so the
 * analysed game cannot be modified from the board. It draws whatever {@link BoardScene} it is given.
 */
public final class ChessBoardView extends Region {

    private static final double MIN_SIZE = 280;

    private final Canvas canvas = new Canvas(MIN_SIZE, MIN_SIZE);
    private final ObjectProperty<BoardScene> scene = new SimpleObjectProperty<>();
    private final ObjectProperty<BoardTheme> theme = new SimpleObjectProperty<>(BoardTheme.GREEN);
    private final BooleanProperty flipped = new SimpleBooleanProperty(false);
    private final BooleanProperty showCoordinates = new SimpleBooleanProperty(true);

    public ChessBoardView() {
        getStyleClass().add("chess-board");
        getChildren().add(canvas);
        setMinSize(MIN_SIZE, MIN_SIZE);
        scene.addListener((o, a, b) -> redraw());
        theme.addListener((o, a, b) -> redraw());
        flipped.addListener((o, a, b) -> redraw());
        showCoordinates.addListener((o, a, b) -> redraw());
    }

    public ObjectProperty<BoardScene> boardSceneProperty() {
        return scene;
    }

    public ObjectProperty<BoardTheme> themeProperty() {
        return theme;
    }

    public BooleanProperty flippedProperty() {
        return flipped;
    }

    public BooleanProperty showCoordinatesProperty() {
        return showCoordinates;
    }

    public Canvas canvas() {
        return canvas;
    }

    @Override
    protected double computePrefWidth(double height) {
        return 640;
    }

    @Override
    protected double computePrefHeight(double width) {
        return width > 0 ? width : 640;
    }

    @Override
    protected void layoutChildren() {
        double side = Math.max(MIN_SIZE, Math.floor(Math.min(getWidth(), getHeight())));
        // Whole-pixel square sizes keep the grid crisp.
        side = Math.floor(side / 8) * 8;
        canvas.setWidth(side);
        canvas.setHeight(side);
        canvas.relocate(Math.round((getWidth() - side) / 2), Math.round((getHeight() - side) / 2));
        redraw();
    }

    private void redraw() {
        BoardScene current = scene.get();
        if (current == null) return;
        BoardRenderer.render(canvas.getGraphicsContext2D(), canvas.getWidth(), current,
                flipped.get(), theme.get(), showCoordinates.get());
    }
}
