package com.chessanalyzer.ui;

import com.chessanalyzer.board.GameNavigator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Transport controls: first / previous / play-pause / next / last, a slider, the "Move n / N" counter and
 * a playback speed. It only calls the {@link GameNavigator}; it never keeps its own idea of the position.
 */
public final class NavigationBar extends VBox {

    private static final String[] SPEED_LABELS = {"0.5 s", "1 s", "2 s"};
    private static final int[] SPEED_MILLIS = {500, 1000, 2000};

    private final GameNavigator nav;
    private final Button first = new Button("\u23EE");
    private final Button previous = new Button("\u25C0");
    private final Button play = new Button("\u25B6 Play");
    private final Button next = new Button("\u25B6");
    private final Button last = new Button("\u23ED");
    private final Slider slider;
    private final Label counter = new Label();
    private final ComboBox<String> speed = new ComboBox<>();
    private final Timeline timeline = new Timeline();
    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private boolean updatingSlider;

    public NavigationBar(GameNavigator nav) {
        this.nav = nav;
        getStyleClass().add("nav-bar");
        setSpacing(6);
        setPadding(new Insets(8, 0, 0, 0));

        slider = new Slider(0, Math.max(1, nav.plyCount()), 0);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        slider.setBlockIncrement(1);
        slider.setDisable(nav.plyCount() == 0);
        slider.setFocusTraversable(false);

        for (Button b : new Button[]{first, previous, play, next, last}) {
            b.getStyleClass().add("nav-button");
            b.setFocusTraversable(false); // so Space/arrows reach the view's key handler, not a focused button
        }
        play.getStyleClass().add("play-button");
        first.setTooltip(new Tooltip("First position (Home)"));
        previous.setTooltip(new Tooltip("Previous move (\u2190)"));
        play.setTooltip(new Tooltip("Play / pause (Space)"));
        next.setTooltip(new Tooltip("Next move (\u2192)"));
        last.setTooltip(new Tooltip("Last position (End)"));

        first.setOnAction(e -> { pause(); nav.first(); });
        previous.setOnAction(e -> { pause(); nav.previous(); });
        next.setOnAction(e -> { pause(); nav.next(); });
        last.setOnAction(e -> { pause(); nav.last(); });
        play.setOnAction(e -> togglePlay());

        counter.getStyleClass().add("move-counter");
        counter.setMinWidth(110);
        counter.setAlignment(Pos.CENTER);

        speed.getItems().addAll(SPEED_LABELS);
        speed.getSelectionModel().select(1);
        speed.setFocusTraversable(false);
        speed.setTooltip(new Tooltip("Seconds per move during playback"));
        speed.valueProperty().addListener((o, a, b) -> {
            if (playing.get()) {
                startTimeline(); // apply the new speed immediately
            }
        });

        HBox buttons = new HBox(6, first, previous, play, next, last, counter, spacer(), new Label("Speed"), speed);
        buttons.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(slider, buttons);

        slider.valueProperty().addListener((o, a, b) -> {
            if (!updatingSlider) {
                pause();
                nav.goTo((int) Math.round(b.doubleValue()));
            }
        });
        nav.plyProperty().addListener((o, a, b) -> refresh());
        refresh();
    }

    private static HBox spacer() {
        HBox s = new HBox();
        HBox.setHgrow(s, Priority.ALWAYS);
        return s;
    }

    public BooleanProperty playingProperty() {
        return playing;
    }

    public void togglePlay() {
        if (playing.get()) {
            pause();
        } else {
            play();
        }
    }

    public void play() {
        if (nav.plyCount() == 0) return;
        if (nav.atEnd()) {
            nav.first(); // pressing Play at the end replays the game
        }
        playing.set(true);
        play.setText("\u23F8 Pause");
        startTimeline();
    }

    public void pause() {
        timeline.stop();
        if (playing.get()) {
            playing.set(false);
            play.setText("\u25B6 Play");
        }
    }

    /** Stops playback and releases the timer; call when the view is left. */
    public void dispose() {
        pause();
    }

    private void startTimeline() {
        timeline.stop();
        timeline.getKeyFrames().setAll(new KeyFrame(Duration.millis(currentSpeed()), e -> {
            nav.next();
            if (nav.atEnd()) {
                pause();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private int currentSpeed() {
        int idx = Math.max(0, speed.getSelectionModel().getSelectedIndex());
        return SPEED_MILLIS[idx];
    }

    private void refresh() {
        counter.setText("Move " + nav.ply() + " / " + nav.plyCount());
        updatingSlider = true;
        slider.setValue(nav.ply());
        updatingSlider = false;
        first.setDisable(nav.atStart());
        previous.setDisable(nav.atStart());
        next.setDisable(nav.atEnd());
        last.setDisable(nav.atEnd());
    }
}
