package com.chessanalyzer.ui;

import com.chessanalyzer.board.EvaluationFormatter;
import com.chessanalyzer.board.PositionView;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** "ENGINE EVALUATION" card: the evaluation of the displayed position, the engine's best move and depth. */
public final class EvaluationPanel extends VBox {

    private final Label value = new Label();
    private final Label description = new Label();
    private final Label bestMove = new Label();
    private final Label depth = new Label();

    public EvaluationPanel() {
        getStyleClass().addAll("card", "eval-panel");
        setSpacing(4);
        setPadding(new Insets(12, 14, 12, 14));

        Label title = new Label("ENGINE EVALUATION");
        title.getStyleClass().add("panel-title");
        value.getStyleClass().add("eval-value");
        description.getStyleClass().add("muted");
        description.setWrapText(true);
        description.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(2);
        ColumnConstraints c0 = new ColumnConstraints();
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);
        grid.add(keyLabel("Best move"), 0, 0);
        grid.add(bestMove, 1, 0);
        grid.add(keyLabel("Depth"), 0, 1);
        grid.add(depth, 1, 1);
        bestMove.getStyleClass().add("value-strong");
        depth.getStyleClass().add("value-strong");

        getChildren().addAll(title, value, description, grid);
    }

    public void show(PositionView v) {
        value.setText(EvaluationFormatter.headline(v.evaluation()));
        value.getStyleClass().removeAll("eval-white", "eval-black", "eval-equal");
        if (v.evaluation() != null) {
            double share = EvaluationFormatter.whiteShare(v.evaluation());
            value.getStyleClass().add(share > 0.53 ? "eval-white" : share < 0.47 ? "eval-black" : "eval-equal");
        }
        description.setText(EvaluationFormatter.describe(v.evaluation()));
        if (v.bestMoveSan() != null) {
            bestMove.setText(v.bestMoveSan());
        } else {
            bestMove.setText(v.isFinal() ? "\u2013 (game over)" : "\u2013");
        }
        depth.setText(v.depth() > 0 ? String.valueOf(v.depth()) : "\u2013");
    }

    private static Label keyLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        return l;
    }
}
