package com.chessanalyzer.ui;

import com.chessanalyzer.board.ClassificationStyle;
import com.chessanalyzer.board.EvaluationFormatter;
import com.chessanalyzer.board.GameNavigator;
import com.chessanalyzer.model.Evaluation;
import com.chessanalyzer.model.MoveAnalysis;
import com.chessanalyzer.model.MoveClassification;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Evaluation graph of the whole game, built purely from the stored per-move evaluations (no engine call).
 * X = position (ply 0 = start), Y = White's winning share, so large swings stay readable and mates reach the
 * edges. Inaccuracies, mistakes and blunders are marked. Clicking or dragging jumps the navigator to that
 * position; the current position is drawn as a vertical line.
 */
public final class EvaluationGraphView extends Region {

    private static final double PAD = 8;

    private final Canvas canvas = new Canvas(400, 90);
    private final GameNavigator nav;
    private final double[] shares; // index = ply

    public EvaluationGraphView(GameNavigator nav) {
        this.nav = nav;
        getStyleClass().add("eval-graph");
        getChildren().add(canvas);
        setMinHeight(80);
        setPrefHeight(90);
        this.shares = computeShares(nav);

        nav.plyProperty().addListener((o, a, b) -> redraw());
        addEventHandler(MouseEvent.MOUSE_PRESSED, this::seek);
        addEventHandler(MouseEvent.MOUSE_DRAGGED, this::seek);
    }

    /** White's share (0..1) for each position 0..N, from the stored evaluations. */
    static double[] computeShares(GameNavigator nav) {
        int n = nav.plyCount();
        double[] s = new double[n + 1];
        for (int ply = 0; ply <= n; ply++) {
            Evaluation e;
            if (ply == 0) {
                e = n > 0 ? nav.moves().get(0).getEvalBefore() : null;
            } else {
                e = nav.moves().get(ply - 1).getEvalAfter();
            }
            s[ply] = EvaluationFormatter.whiteShare(e);
        }
        return s;
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        redraw();
    }

    private double xFor(int ply) {
        int n = Math.max(1, nav.plyCount());
        return PAD + (canvas.getWidth() - 2 * PAD) * ply / n;
    }

    private double yFor(double share) {
        double h = canvas.getHeight();
        return PAD + (h - 2 * PAD) * (1 - share);
    }

    private void seek(MouseEvent e) {
        int n = nav.plyCount();
        if (n == 0) return;
        double frac = (e.getX() - PAD) / Math.max(1, canvas.getWidth() - 2 * PAD);
        nav.goTo((int) Math.round(Math.max(0, Math.min(1, frac)) * n));
    }

    private void redraw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.setFill(Color.web("#262b35"));
        g.fillRect(0, 0, w, h);

        double mid = yFor(0.5);
        // White advantage region above the midline in light, Black advantage below in dark.
        g.setFill(Color.web("#e8e8e8", 0.12));
        g.fillRect(PAD, PAD, w - 2 * PAD, mid - PAD);
        g.setFill(Color.web("#000000", 0.18));
        g.fillRect(PAD, mid, w - 2 * PAD, h - PAD - mid);

        int n = nav.plyCount();
        if (n >= 1) {
            // Filled area between the evaluation line and the midline.
            double[] xs = new double[n + 3];
            double[] ys = new double[n + 3];
            for (int p = 0; p <= n; p++) {
                xs[p] = xFor(p);
                ys[p] = yFor(shares[p]);
            }
            xs[n + 1] = xFor(n);
            ys[n + 1] = mid;
            xs[n + 2] = xFor(0);
            ys[n + 2] = mid;
            g.setFill(Color.web("#a9b8d6", 0.35));
            g.fillPolygon(xs, ys, n + 3);

            g.setStroke(Color.web("#dfe6f5"));
            g.setLineWidth(1.6);
            g.beginPath();
            g.moveTo(xs[0], ys[0]);
            for (int p = 1; p <= n; p++) g.lineTo(xs[p], ys[p]);
            g.stroke();
        }
        g.setStroke(Color.web("#808a9b", 0.7));
        g.setLineWidth(1);
        g.strokeLine(PAD, Math.round(mid) + 0.5, w - PAD, Math.round(mid) + 0.5);

        // Markers for the moves that matter.
        for (int i = 0; i < n; i++) {
            MoveAnalysis m = nav.moves().get(i);
            MoveClassification c = m.getClassification();
            if (c == MoveClassification.INACCURACY || c == MoveClassification.MISTAKE || c == MoveClassification.BLUNDER) {
                double r = c == MoveClassification.INACCURACY ? 2.6 : 3.8;
                g.setFill(Color.web(ClassificationStyle.of(c).color()));
                g.fillOval(xFor(i + 1) - r, yFor(shares[i + 1]) - r, 2 * r, 2 * r);
            }
        }

        // Current position.
        double cx = xFor(nav.ply());
        g.setStroke(Color.web("#3d7dfc"));
        g.setLineWidth(2);
        g.strokeLine(cx, PAD - 3, cx, h - PAD + 3);
        g.setFill(Color.WHITE);
        g.setStroke(Color.web("#3d7dfc"));
        g.fillOval(cx - 4, yFor(shares[nav.ply()]) - 4, 8, 8);
        g.strokeOval(cx - 4, yFor(shares[nav.ply()]) - 4, 8, 8);
    }
}
