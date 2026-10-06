package com.chessanalyzer.ui;

import com.chessanalyzer.board.ClassificationStyle;
import com.chessanalyzer.board.EvaluationFormatter;
import com.chessanalyzer.board.PositionView;
import com.chessanalyzer.model.ChessGame;
import com.chessanalyzer.model.MoveAnalysis;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Detail card for the selected move: classification, played move vs. engine move, evaluation before
 * and after, centipawn loss, depth and the stored explanation. At the end of the game it also shows the
 * game-over summary. Everything comes from the {@link MoveAnalysis} stored in the analysis.
 */
public final class MoveInfoPanel extends VBox {

    private static final String[] CLASS_STYLES = java.util.Arrays.stream(ClassificationStyle.values())
            .map(ClassificationStyle::cssClass).toArray(String[]::new);

    private final ChessGame game;

    public MoveInfoPanel(ChessGame game) {
        this.game = game;
        getStyleClass().addAll("card", "move-info");
        setSpacing(8);
        setPadding(new Insets(12, 14, 12, 14));
    }

    public void show(PositionView v) {
        getChildren().clear();
        getStyleClass().removeAll(CLASS_STYLES);

        MoveAnalysis m = v.playedMove();
        if (v.isFinal() && v.plyCount() > 0) {
            getChildren().add(gameOverBanner(v));
        }

        if (m == null) {
            getChildren().add(title("INITIAL POSITION"));
            Label hint = new Label(v.plyCount() == 0 ? "This game has no analysed moves."
                    : "Use \u25B6 or the \u2192 key to step through the game.");
            hint.getStyleClass().add("muted");
            wrap(hint);
            getChildren().add(hint);
            return;
        }

        ClassificationStyle style = ClassificationStyle.of(m.getClassification());
        getStyleClass().add(style.cssClass());

        getChildren().add(title("MOVE " + m.moveLabel()));
        Label headline = new Label(style.icon() + " " + (style.isProblem() && m.getClassification() == com.chessanalyzer.model.MoveClassification.BLUNDER
                ? style.label().toUpperCase() : style.label()));
        headline.getStyleClass().addAll("class-headline", style.cssClass());
        getChildren().add(headline);

        boolean differsFromEngine = m.getBestMoveSan() != null && m.getBestMoveUci() != null
                && !m.getBestMoveUci().equalsIgnoreCase(m.getUci());

        if (style.isProblem() && differsFromEngine) {
            getChildren().add(yourMoveVsEngine(v, m, style));
        } else {
            getChildren().add(row("Played move", m.getSan() + style.annotation()));
            if (differsFromEngine) {
                getChildren().add(row("Engine preferred", m.getBestMoveSan()));
            } else if (m.getBestMoveUci() != null) {
                Label best = new Label("Matches the engine's first choice");
                best.getStyleClass().add("muted");
                getChildren().add(best);
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(3);
        ColumnConstraints c0 = new ColumnConstraints();
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);
        addGridRow(grid, 0, "Evaluation before", EvaluationFormatter.compact(m.getEvalBefore()));
        addGridRow(grid, 1, "Evaluation after", EvaluationFormatter.compact(m.getEvalAfter()));
        addGridRow(grid, 2, "Centipawn loss", String.valueOf(Math.round(m.getEvalLossCentipawns())));
        addGridRow(grid, 3, "Engine depth", m.getEngineDepth() > 0 ? String.valueOf(m.getEngineDepth()) : "\u2013");
        getChildren().add(grid);

        if (m.getPrincipalVariationSan() != null && !m.getPrincipalVariationSan().isBlank() && differsFromEngine) {
            Label pvTitle = new Label("Best continuation");
            pvTitle.getStyleClass().add("muted");
            Label pv = new Label(m.getPrincipalVariationSan());
            wrap(pv);
            pv.getStyleClass().add("pv-text");
            getChildren().addAll(pvTitle, pv);
        }
        if (m.getComment() != null && !m.getComment().isBlank()) {
            Label comment = new Label(m.getComment());
            wrap(comment);
            comment.getStyleClass().add("comment-text");
            getChildren().add(comment);
        }
    }

    private Node gameOverBanner(PositionView v) {
        VBox box = new VBox(2);
        box.getStyleClass().add("game-over");
        Label over = new Label("GAME OVER");
        over.getStyleClass().add("game-over-title");
        Label result = new Label("White " + prettyResult(game != null ? game.getResult() : "*") + " Black");
        result.getStyleClass().add("game-over-result");
        String finalText = v.checkmate() ? "Checkmate \u2013 final evaluation " + EvaluationFormatter.compact(v.evaluation())
                : "Final evaluation: " + EvaluationFormatter.compact(v.evaluation());
        Label eval = new Label(finalText);
        eval.getStyleClass().add("muted");
        box.getChildren().addAll(over, result, eval);
        return box;
    }

    /** Side-by-side "YOUR MOVE" / "ENGINE" boxes for inaccuracies, mistakes and blunders. */
    private Node yourMoveVsEngine(PositionView v, MoveAnalysis m, ClassificationStyle style) {
        HBox box = new HBox(10);
        VBox yours = moveBox("YOUR MOVE", m.getMoveNumber() + (m.isWhiteToMove() ? "." : "...") + m.getSan() + style.annotation(),
                style.cssClass());
        VBox engine = moveBox("ENGINE", v.alternativeLabel() + "!", "cls-best");
        HBox.setHgrow(yours, Priority.ALWAYS);
        HBox.setHgrow(engine, Priority.ALWAYS);
        box.getChildren().addAll(yours, engine);
        return box;
    }

    private static VBox moveBox(String caption, String move, String cssClass) {
        VBox b = new VBox(2);
        b.getStyleClass().addAll("move-box", cssClass);
        b.setPadding(new Insets(6, 10, 6, 10));
        Label c = new Label(caption);
        c.getStyleClass().add("move-box-caption");
        Label t = new Label(move);
        t.getStyleClass().add("move-box-move");
        b.getChildren().addAll(c, t);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    /** Makes a label wrap onto as many lines as it needs instead of truncating with an ellipsis. */
    private static void wrap(Label label) {
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
    }

    private static Label title(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("panel-title");
        return l;
    }

    private static Node row(String key, String value) {
        HBox h = new HBox(12);
        h.setAlignment(Pos.BASELINE_LEFT);
        Label k = new Label(key);
        k.getStyleClass().add("muted");
        Label v = new Label(value);
        v.getStyleClass().add("value-strong");
        h.getChildren().addAll(k, v);
        return h;
    }

    private static void addGridRow(GridPane grid, int row, String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("muted");
        Label v = new Label(value);
        v.getStyleClass().add("value-strong");
        grid.add(k, 0, row);
        grid.add(v, 1, row);
    }

    static String prettyResult(String result) {
        if (result == null) return "*";
        return switch (result) {
            case "1-0" -> "1 - 0";
            case "0-1" -> "0 - 1";
            case "1/2-1/2" -> "\u00BD - \u00BD";
            default -> "*";
        };
    }
}
