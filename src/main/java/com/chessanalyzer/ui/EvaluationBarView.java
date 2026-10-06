package com.chessanalyzer.ui;

import com.chessanalyzer.board.EvaluationFormatter;
import com.chessanalyzer.model.Evaluation;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Vertical evaluation bar: White's share fills from the bottom (or from the top when the board is
 * flipped), Black's share is the dark part. The number sits at the end of the side that is ahead.
 * The value always comes from the {@link Evaluation} it is given.
 */
public final class EvaluationBarView extends Region {

    private static final Color WHITE_SIDE = Color.web("#f2f2f2");
    private static final Color BLACK_SIDE = Color.web("#403d39");

    private final Canvas canvas = new Canvas(30, 300);
    private final ObjectProperty<Evaluation> evaluation = new SimpleObjectProperty<>();
    private final BooleanProperty flipped = new SimpleBooleanProperty(false);

    public EvaluationBarView() {
        getStyleClass().add("eval-bar");
        getChildren().add(canvas);
        setMinWidth(30);
        setPrefWidth(30);
        setMaxWidth(30);
        evaluation.addListener((o, a, b) -> redraw());
        flipped.addListener((o, a, b) -> redraw());
    }

    public ObjectProperty<Evaluation> evaluationProperty() {
        return evaluation;
    }

    public BooleanProperty flippedProperty() {
        return flipped;
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        redraw();
    }

    private void redraw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext g = canvas.getGraphicsContext2D();
        Evaluation e = evaluation.get();
        double share = EvaluationFormatter.whiteShare(e);
        double whiteHeight = h * share;
        boolean whiteAtBottom = !flipped.get();

        g.clearRect(0, 0, w, h);
        g.setFill(BLACK_SIDE);
        g.fillRect(0, 0, w, h);
        g.setFill(WHITE_SIDE);
        if (whiteAtBottom) {
            g.fillRect(0, h - whiteHeight, w, whiteHeight);
        } else {
            g.fillRect(0, 0, w, whiteHeight);
        }

        g.setStroke(Color.web("#808080", 0.6)); // midline: equal position
        g.setLineWidth(1);
        g.strokeLine(0, Math.round(h / 2) + 0.5, w, Math.round(h / 2) + 0.5);

        String text = EvaluationFormatter.compact(e);
        boolean whiteAhead = share >= 0.5;
        // Label at the far end of the leading side.
        boolean atBottom = whiteAhead == whiteAtBottom;
        g.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, text.length() > 4 ? 9 : 11));
        g.setFill(whiteAhead ? Color.web("#333333") : Color.web("#eeeeee"));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(atBottom ? VPos.BOTTOM : VPos.TOP);
        g.fillText(text.replace("+", ""), w / 2, atBottom ? h - 4 : 4);
    }
}
