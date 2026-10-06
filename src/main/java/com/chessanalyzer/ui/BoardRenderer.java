package com.chessanalyzer.ui;

import com.chessanalyzer.board.BoardArrow;
import com.chessanalyzer.board.BoardScene;
import com.chessanalyzer.board.ClassificationStyle;
import com.chessanalyzer.board.FenPosition;
import com.chessanalyzer.board.Squares;
import com.chessanalyzer.model.MoveClassification;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * Draws a {@link BoardScene} onto a graphics context. Layers, bottom to top:
 * squares, last-move / check highlights, coordinates, pieces, arrows, classification badge.
 * Pure drawing - it holds no game state.
 */
public final class BoardRenderer {

    private static final String[] FONT_CANDIDATES = {
            "Segoe UI Symbol", "Apple Symbols", "DejaVu Sans", "Noto Sans Symbols 2", "FreeSerif", "Arial Unicode MS"};

    private static final String GLYPHS = "\u265A\u265B\u265C\u265D\u265E\u265F"; // K Q R B N P (solid forms)

    private static final Color WHITE_PIECE = Color.web("#fdfdfd");
    private static final Color WHITE_OUTLINE = Color.web("#2b2b2b");
    private static final Color BLACK_PIECE = Color.web("#1d1d1d");
    private static final Color BLACK_OUTLINE = Color.web("#000000");

    private static String pieceFamily;

    private BoardRenderer() {
    }

    /** First installed family that contains chess symbols; falls back to the platform default. */
    static synchronized String pieceFamily() {
        if (pieceFamily == null) {
            List<String> installed = Font.getFamilies();
            pieceFamily = Font.getDefault().getFamily();
            for (String candidate : FONT_CANDIDATES) {
                if (installed.contains(candidate)) {
                    pieceFamily = candidate;
                    break;
                }
            }
        }
        return pieceFamily;
    }

    public static void render(GraphicsContext g, double size, BoardScene scene, boolean flipped,
                              BoardTheme theme, boolean coordinates) {
        BoardGeometry geo = new BoardGeometry(size, flipped);
        FenPosition position = FenPosition.parse(scene.fen());
        double s = geo.squareSize();

        g.clearRect(0, 0, size, size);
        drawSquares(g, geo, theme);
        drawHighlights(g, geo, scene, theme);
        if (coordinates) {
            drawCoordinates(g, geo, theme);
        }
        drawPieces(g, geo, position);
        for (BoardArrow arrow : scene.arrows()) {
            MoveArrowRenderer.draw(g, geo, arrow);
        }
        drawBadge(g, geo, scene, s);
    }

    private static void drawSquares(GraphicsContext g, BoardGeometry geo, BoardTheme theme) {
        double s = geo.squareSize();
        for (int sq = 0; sq < 64; sq++) {
            boolean light = (Squares.file(sq) + Squares.rank(sq)) % 2 == 1; // a1 is dark
            g.setFill(light ? theme.light() : theme.dark());
            g.fillRect(geo.x(sq), geo.y(sq), s, s);
        }
    }

    private static void drawHighlights(GraphicsContext g, BoardGeometry geo, BoardScene scene, BoardTheme theme) {
        double s = geo.squareSize();
        if (scene.lastFrom() != Squares.NONE && scene.lastTo() != Squares.NONE) {
            g.setFill(theme.lastMove());
            g.fillRect(geo.x(scene.lastFrom()), geo.y(scene.lastFrom()), s, s);
            Color destination = theme.lastMove();
            ClassificationStyle style = scene.lastClassification() != null
                    ? ClassificationStyle.of(scene.lastClassification()) : null;
            if (style != null && style.isProblem()) {
                destination = Color.web(style.color(), 0.60); // problem moves tint the destination
            }
            g.setFill(destination);
            g.fillRect(geo.x(scene.lastTo()), geo.y(scene.lastTo()), s, s);
        }
        if (scene.checkSquare() != Squares.NONE) {
            double cx = geo.centerX(scene.checkSquare());
            double cy = geo.centerY(scene.checkSquare());
            g.setFill(new RadialGradient(0, 0, cx, cy, s * 0.62, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#ff2a2a", 0.95)), new Stop(0.55, Color.web("#e62929", 0.55)),
                    new Stop(1, Color.web("#e62929", 0.0))));
            g.fillRect(geo.x(scene.checkSquare()), geo.y(scene.checkSquare()), s, s);
        }
    }

    private static void drawCoordinates(GraphicsContext g, BoardGeometry geo, BoardTheme theme) {
        double s = geo.squareSize();
        g.setFont(Font.font(pieceFamilyForText(), FontWeight.BOLD, Math.max(9, s * 0.17)));
        g.setTextBaseline(VPos.TOP);
        g.setTextAlign(TextAlignment.LEFT);
        for (int sq = 0; sq < 64; sq++) {
            boolean light = (Squares.file(sq) + Squares.rank(sq)) % 2 == 1;
            g.setFill(theme.coordinateColorOn(light));
            if (geo.column(sq) == 0) { // rank numbers down the left edge
                g.setTextAlign(TextAlignment.LEFT);
                g.setTextBaseline(VPos.TOP);
                g.fillText(String.valueOf(Squares.rank(sq) + 1), geo.x(sq) + s * 0.06, geo.y(sq) + s * 0.04);
            }
            if (geo.row(sq) == 7) { // file letters along the bottom edge
                g.setTextAlign(TextAlignment.RIGHT);
                g.setTextBaseline(VPos.BOTTOM);
                g.fillText(String.valueOf((char) ('a' + Squares.file(sq))), geo.x(sq) + s * 0.95, geo.y(sq) + s * 0.97);
            }
        }
    }

    private static String pieceFamilyForText() {
        return Font.getDefault().getFamily();
    }

    private static void drawPieces(GraphicsContext g, BoardGeometry geo, FenPosition position) {
        double s = geo.squareSize();
        Font font = Font.font(pieceFamily(), s * 0.80);
        g.setFont(font);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.BASELINE);
        g.setLineWidth(Math.max(1.0, s * 0.028));

        for (int sq = 0; sq < 64; sq++) {
            char piece = position.pieceAt(sq);
            if (piece == ' ') continue;
            boolean white = FenPosition.isWhite(piece);
            String glyph = String.valueOf(GLYPHS.charAt("KQRBNP".indexOf(Character.toUpperCase(piece))));

            Text probe = new Text(glyph);
            probe.setFont(font);
            Bounds b = probe.getLayoutBounds();
            double x = geo.centerX(sq) - b.getWidth() / 2;
            double y = geo.centerY(sq) - b.getHeight() / 2 - b.getMinY() + s * 0.01;

            g.setStroke(white ? WHITE_OUTLINE : BLACK_OUTLINE);
            g.setFill(white ? WHITE_PIECE : BLACK_PIECE);
            g.fillText(glyph, x, y);
            g.strokeText(glyph, x, y);
        }
    }

    /** Small round badge on the destination square for brilliant moves and for inaccuracies/mistakes/blunders. */
    private static void drawBadge(GraphicsContext g, BoardGeometry geo, BoardScene scene, double s) {
        if (scene.lastTo() == Squares.NONE || scene.lastClassification() == null) return;
        ClassificationStyle style = ClassificationStyle.of(scene.lastClassification());
        boolean worthShowing = style.isProblem() || scene.lastClassification() == MoveClassification.BRILLIANT;
        if (!worthShowing) return;
        double r = s * 0.20;
        double cx = geo.x(scene.lastTo()) + s - r * 0.95;
        double cy = geo.y(scene.lastTo()) + r * 0.95;
        // Keep the badge inside the board edge.
        g.setFill(Color.web("#000000", 0.30));
        g.fillOval(cx - r + 1, cy - r + 1.5, 2 * r, 2 * r);
        g.setFill(Color.web(style.color()));
        g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        g.setStroke(Color.WHITE);
        g.setLineWidth(Math.max(1, s * 0.015));
        g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);

        g.setFill(Color.WHITE);
        g.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.EXTRA_BOLD, r * 1.15));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(style.annotation(), cx, cy + r * 0.04);
        g.setTextAlign(TextAlignment.LEFT);
    }
}
