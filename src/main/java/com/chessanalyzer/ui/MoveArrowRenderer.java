package com.chessanalyzer.ui;

import com.chessanalyzer.board.BoardArrow;
import com.chessanalyzer.board.Squares;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Draws {@link BoardArrow}s. Reusable and independent of any controller: give it a graphics context
 * and a {@link BoardGeometry}. Knight moves are drawn L-shaped (as analysis boards usually do) so the
 * arrow never passes over the pieces between the squares; every other move is a straight arrow.
 * Arrows are semi-transparent and start outside the origin piece so they do not hide pieces.
 */
public final class MoveArrowRenderer {

    private static final double SHAFT_WIDTH = 0.13;      // fraction of a square
    private static final double HEAD_LENGTH = 0.40;
    private static final double HEAD_HALF_WIDTH = 0.28;
    private static final double START_INSET = 0.22;      // gap between square centre and arrow start
    private static final double OPACITY = 0.68;

    private MoveArrowRenderer() {
    }

    public static Color colorFor(BoardArrow.Kind kind) {
        return switch (kind) {
            case BEST_ALTERNATIVE -> Color.web("#81b64c");
            case SUGGESTION -> Color.web("#3d7dfc");
            case PLAYED -> Color.web("#e0533d");
            case ALTERNATIVE_LINE -> Color.web("#9aa4b2");
        };
    }

    public static void draw(GraphicsContext g, BoardGeometry geo, BoardArrow arrow) {
        double s = geo.squareSize();
        double x1 = geo.centerX(arrow.from());
        double y1 = geo.centerY(arrow.from());
        double x2 = geo.centerX(arrow.to());
        double y2 = geo.centerY(arrow.to());

        double[] corner = knightCorner(geo, arrow);
        double headFromX = corner != null ? corner[0] : x1;
        double headFromY = corner != null ? corner[1] : y1;

        // Direction of the final segment decides where the head points.
        double dx = x2 - headFromX;
        double dy = y2 - headFromY;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return;
        double ux = dx / len;
        double uy = dy / len;

        double headLen = HEAD_LENGTH * s;
        double tipX = x2 - ux * 0.08 * s;           // stop slightly inside the target square
        double tipY = y2 - uy * 0.08 * s;
        double baseX = tipX - ux * headLen;
        double baseY = tipY - uy * headLen;

        // Start slightly away from the origin square centre so the origin piece stays visible.
        double sx = x1, sy = y1;
        double fx, fy;
        if (corner == null) {
            double slen = Math.hypot(x2 - x1, y2 - y1);
            sx = x1 + (x2 - x1) / slen * START_INSET * s;
            sy = y1 + (y2 - y1) / slen * START_INSET * s;
        } else {
            double slen = Math.hypot(corner[0] - x1, corner[1] - y1);
            sx = x1 + (corner[0] - x1) / slen * START_INSET * s;
            sy = y1 + (corner[1] - y1) / slen * START_INSET * s;
        }

        Color color = colorFor(arrow.kind());
        g.save();
        g.setGlobalAlpha(OPACITY);
        g.setStroke(color);
        g.setFill(color);
        g.setLineWidth(SHAFT_WIDTH * s);
        g.setLineCap(StrokeLineCap.BUTT);
        g.setLineJoin(StrokeLineJoin.ROUND);

        g.beginPath();
        g.moveTo(sx, sy);
        if (corner != null) {
            g.lineTo(corner[0], corner[1]);
        }
        g.lineTo(baseX + ux * 1.0, baseY + uy * 1.0);
        g.stroke();

        double px = -uy;
        double py = ux;
        double hw = HEAD_HALF_WIDTH * s;
        g.fillPolygon(new double[]{tipX, baseX + px * hw, baseX - px * hw},
                new double[]{tipY, baseY + py * hw, baseY - py * hw}, 3);
        g.restore();
    }

    /** For a knight move, the pixel position of the L's corner (long leg first); null for other moves. */
    private static double[] knightCorner(BoardGeometry geo, BoardArrow arrow) {
        int df = Squares.file(arrow.to()) - Squares.file(arrow.from());
        int dr = Squares.rank(arrow.to()) - Squares.rank(arrow.from());
        boolean knight = (Math.abs(df) == 1 && Math.abs(dr) == 2) || (Math.abs(df) == 2 && Math.abs(dr) == 1);
        if (!knight) return null;
        int cornerFile = Math.abs(df) == 2 ? Squares.file(arrow.to()) : Squares.file(arrow.from());
        int cornerRank = Math.abs(df) == 2 ? Squares.rank(arrow.from()) : Squares.rank(arrow.to());
        int corner = Squares.of(cornerFile, cornerRank);
        return new double[]{geo.centerX(corner), geo.centerY(corner)};
    }
}
