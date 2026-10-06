package com.chessanalyzer.ui;

import com.chessanalyzer.board.ClassificationStyle;
import com.chessanalyzer.board.GameNavigator;
import com.chessanalyzer.board.MoveRows;
import com.chessanalyzer.board.MoveRows.Row;
import com.chessanalyzer.model.MoveAnalysis;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Two-column move list ("1. e4   e5"). Each move shows its classification marker, the move that is
 * currently displayed on the board is highlighted, and clicking any move jumps the navigator to it.
 * Row 0 is the initial position.
 */
public final class MoveListView extends ListView<Row> {

    private final GameNavigator nav;

    public MoveListView(GameNavigator nav) {
        this.nav = nav;
        getStyleClass().add("move-list");
        setSelectionModel(null); // the navigator owns "current"; the list only displays it and forwards clicks
        setFocusTraversable(false);
        setCellFactory(lv -> new RowCell());
        getItems().setAll(MoveRows.build(nav.moves()));
        nav.plyProperty().addListener((o, a, b) -> {
            refresh();
            scrollToCurrent();
        });
    }

    private void scrollToCurrent() {
        int row = MoveRows.rowIndexForPly(getItems(), nav.ply());
        VirtualFlow<?> flow = (VirtualFlow<?>) lookup(".virtual-flow");
        if (flow != null && flow.getFirstVisibleCell() != null && flow.getLastVisibleCell() != null) {
            int first = flow.getFirstVisibleCell().getIndex();
            int lastIdx = flow.getLastVisibleCell().getIndex();
            if (row > first && row < lastIdx) {
                return; // already comfortably visible: do not jump around while stepping
            }
        }
        scrollTo(Math.max(0, row - 3));
    }

    private final class RowCell extends ListCell<Row> {
        @Override
        protected void updateItem(Row row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            setText(null);
            if (row.startRow()) {
                Label start = new Label("Initial position");
                start.getStyleClass().add("move-cell");
                if (nav.ply() == 0) start.getStyleClass().add("selected-move");
                start.setOnMouseClicked(e -> nav.goTo(0));
                HBox box = new HBox(start);
                box.setPadding(new Insets(1, 6, 1, 6));
                setGraphic(box);
                return;
            }
            Label number = new Label(row.moveNumber() + ".");
            number.getStyleClass().add("move-number");
            number.setMinWidth(34);

            HBox whiteCell = moveCell(row.white(), row.whitePly());
            HBox blackCell = moveCell(row.black(), row.blackPly());
            HBox.setHgrow(whiteCell, Priority.ALWAYS);
            HBox.setHgrow(blackCell, Priority.ALWAYS);
            whiteCell.setPrefWidth(120);
            blackCell.setPrefWidth(120);

            HBox line = new HBox(4, number, whiteCell, blackCell);
            line.setAlignment(Pos.CENTER_LEFT);
            line.setPadding(new Insets(0, 4, 0, 4));
            setGraphic(line);
        }

        private HBox moveCell(MoveAnalysis m, int ply) {
            HBox cell = new HBox(5);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.getStyleClass().add("move-cell");
            if (m == null) {
                cell.getChildren().add(new Label(ply < 0 && getItem() != null && getItem().white() == null ? "\u2026" : ""));
                return cell;
            }
            ClassificationStyle style = ClassificationStyle.of(m.getClassification());
            Label san = new Label(m.getSan());
            san.getStyleClass().add("move-san");
            Label marker = new Label(style.icon());
            marker.getStyleClass().addAll("move-marker", style.cssClass());
            cell.getChildren().addAll(san, marker);
            cell.getStyleClass().add(style.cssClass());
            if (ply == nav.ply()) {
                cell.getStyleClass().add("selected-move");
            }
            cell.setOnMouseClicked(e -> nav.goTo(ply));
            return cell;
        }
    }
}
