package com.chessanalyzer.ui;

import com.chessanalyzer.board.BoardArrow;
import com.chessanalyzer.board.BoardOptions;
import com.chessanalyzer.board.BoardScene;
import com.chessanalyzer.board.BoardScenePlanner;
import com.chessanalyzer.board.GameNavigator;
import com.chessanalyzer.board.PositionView;
import com.chessanalyzer.model.ChessGame;
import com.chessanalyzer.model.GameAnalysis;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The interactive analysis screen. It builds nothing but views: one {@link GameNavigator} is the single
 * source of truth, and the board, evaluation bar/panel, move information, move list, graph and
 * navigation controls all observe it. The analysis itself is never modified.
 *
 * <p>Keyboard: &larr; previous, &rarr; next, Home first, End last, Space play/pause.
 */
public final class AnalysisBoardView extends BorderPane {

    private final GameNavigator nav;
    private final ChessBoardView board = new ChessBoardView();
    private final EvaluationBarView evalBar = new EvaluationBarView();
    private final EvaluationPanel evalPanel = new EvaluationPanel();
    private final MoveInfoPanel infoPanel;
    private final MoveListView moveList;
    private final EvaluationGraphView graph;
    private final NavigationBar navBar;
    private final Label caption = new Label();

    private final ToggleButton showBest = new ToggleButton("Show best move");
    private final ToggleButton compare = new ToggleButton("Before move");
    private final ComboBox<BoardTheme> themeBox = new ComboBox<>();

    public AnalysisBoardView(GameAnalysis analysis, Runnable onBack) {
        this.nav = new GameNavigator(analysis);
        this.infoPanel = new MoveInfoPanel(analysis.getGame());
        this.moveList = new MoveListView(nav);
        this.graph = new EvaluationGraphView(nav);
        this.navBar = new NavigationBar(nav);

        getStyleClass().addAll("screen-root", "board-view");
        setTop(buildHeader(analysis.getGame(), onBack));
        setCenter(buildCenter());
        setRight(buildSidebar());

        showBest.setOnAction(e -> update());
        compare.setOnAction(e -> update());
        nav.positionProperty().addListener((o, a, b) -> update());

        addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
        sceneProperty().addListener((o, was, now) -> {
            if (now == null) dispose();
        });
        update();
        setFocusTraversable(true);
        javafx.application.Platform.runLater(this::requestFocus);
    }

    public GameNavigator navigator() {
        return nav;
    }

    /** Stops playback timers; called automatically when the view leaves the scene. */
    public void dispose() {
        navBar.dispose();
    }

    // ------------------------------------------------------------------ layout

    private HBox buildHeader(ChessGame game, Runnable onBack) {
        Button back = new Button("\u2190 Report");
        back.setFocusTraversable(false);
        back.setOnAction(e -> {
            dispose();
            if (onBack != null) onBack.run();
        });

        Label title = new Label(game.getWhite() + (game.getWhiteElo() != null ? " (" + game.getWhiteElo() + ")" : "")
                + "  vs  " + game.getBlack() + (game.getBlackElo() != null ? " (" + game.getBlackElo() + ")" : ""));
        title.getStyleClass().add("board-title");
        StringBuilder sub = new StringBuilder(MoveInfoPanel.prettyResult(game.getResult()));
        if (!game.getDate().isBlank() && !game.getDate().contains("?")) sub.append("  \u00B7  ").append(game.getDate());
        if (!game.getOpening().isBlank()) sub.append("  \u00B7  ").append(game.getOpening());
        else if (!game.getEco().isBlank()) sub.append("  \u00B7  ").append(game.getEco());
        Label subtitle = new Label(sub.toString());
        subtitle.getStyleClass().add("muted");
        VBox titles = new VBox(1, title, subtitle);

        showBest.setFocusTraversable(false);
        showBest.setTooltip(new Tooltip("Draw the engine's best move for the side to move"));
        compare.setFocusTraversable(false);
        compare.setTooltip(new Tooltip("Show the position before the selected inaccurate move, with your move and the engine's move"));

        Button flip = new Button("Flip board");
        flip.setFocusTraversable(false);
        flip.setOnAction(e -> {
            board.flippedProperty().set(!board.flippedProperty().get());
            evalBar.flippedProperty().set(board.flippedProperty().get());
        });

        themeBox.getItems().addAll(BoardTheme.all());
        themeBox.getSelectionModel().select(BoardTheme.GREEN);
        themeBox.setFocusTraversable(false);
        themeBox.valueProperty().addListener((o, a, b) -> board.themeProperty().set(b));

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(14, back, titles, spacer, showBest, compare, flip, themeBox);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.getStyleClass().add("report-toolbar");
        return header;
    }

    private VBox buildCenter() {
        HBox boardRow = new HBox(8, evalBar, board);
        boardRow.setAlignment(Pos.CENTER);
        // The board is square: its width follows its height (capped by the space available), so the
        // evaluation bar always sits right next to the squares instead of at the window edge.
        board.prefWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> Math.max(280, Math.min(board.getHeight(), boardRow.getWidth() - 48)),
                board.heightProperty(), boardRow.widthProperty()));
        board.maxWidthProperty().bind(board.prefWidthProperty());
        evalBar.prefHeightProperty().bind(board.heightProperty());
        evalBar.setMaxHeight(Double.MAX_VALUE);

        caption.getStyleClass().add("board-caption");
        caption.setWrapText(true);
        caption.setMinHeight(34);

        VBox center = new VBox(6, boardRow, caption, graph, navBar);
        VBox.setVgrow(boardRow, Priority.ALWAYS);
        center.setPadding(new Insets(12, 8, 12, 16));
        return center;
    }

    private VBox buildSidebar() {
        VBox.setVgrow(moveList, Priority.ALWAYS);
        VBox side = new VBox(10, evalPanel, infoPanel, moveList);
        side.setPrefWidth(390);
        side.setMinWidth(340);
        side.setMaxWidth(430);
        side.setPadding(new Insets(12, 16, 12, 8));
        return side;
    }

    // ------------------------------------------------------------------ synchronisation

    private BoardOptions options() {
        return new BoardOptions(showBest.isSelected(), compare.isSelected());
    }

    /** Pushes the navigator's current position into every view. The only place the views are updated. */
    private void update() {
        PositionView v = nav.position();
        BoardOptions o = options();
        BoardScene scene = BoardScenePlanner.plan(v, o);
        boolean comparing = BoardScenePlanner.isComparing(v, o);
        PositionView evaluated = comparing ? nav.viewAt(v.ply() - 1) : v;

        board.boardSceneProperty().set(scene);
        evalBar.evaluationProperty().set(evaluated.evaluation());
        evalPanel.show(evaluated);
        infoPanel.show(v);
        compare.setDisable(!BoardScenePlanner.isCompareApplicable(v));
        caption.setText(captionFor(v, scene, comparing));
    }

    private static String captionFor(PositionView v, BoardScene scene, boolean comparing) {
        if (comparing) {
            return "Position before " + v.playedMove().moveLabel() + ".  Red arrow: your move.  Green arrow: engine recommends "
                    + v.alternativeLabel() + ".";
        }
        for (BoardArrow a : scene.arrows()) {
            if (a.kind() == BoardArrow.Kind.BEST_ALTERNATIVE) {
                return "Green arrow: instead of " + v.playedMove().moveLabel() + ", the engine recommends "
                        + v.alternativeLabel() + ".";
            }
        }
        for (BoardArrow a : scene.arrows()) {
            if (a.kind() == BoardArrow.Kind.SUGGESTION) {
                return "Blue arrow: engine's best move here is " + v.bestMoveSan() + ".";
            }
        }
        return "";
    }

    private void onKey(KeyEvent e) {
        if (themeBox.isShowing()) return; // let an open drop-down use the keys
        KeyCode code = e.getCode();
        switch (code) {
            case LEFT -> { navBar.pause(); nav.previous(); }
            case RIGHT -> { navBar.pause(); nav.next(); }
            case HOME -> { navBar.pause(); nav.first(); }
            case END -> { navBar.pause(); nav.last(); }
            case SPACE -> navBar.togglePlay();
            default -> { return; }
        }
        e.consume();
    }
}
