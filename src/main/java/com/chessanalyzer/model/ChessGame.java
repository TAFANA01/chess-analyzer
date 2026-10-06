package com.chessanalyzer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parsed and validated PGN game: headers + the ordered list of
 * moves in SAN, before any engine analysis is attached.
 */
public class ChessGame {

    private String white = "Unknown";
    private String black = "Unknown";
    private String result = "*";
    private String date = "";
    private String event = "";
    private String site = "";
    private String round = "";
    private String eco = "";
    private String opening = "";
    private String timeControl = "";
    private Integer whiteElo;
    private Integer blackElo;
    /** FEN of the starting position when the PGN uses [SetUp "1"]/[FEN]; null = standard start. */
    private String startFen;

    private String rawPgn;
    private final List<String> movesSan = new ArrayList<>();

    public int getMoveCount() {
        return movesSan.size();
    }

    public String getWhite() {
        return white;
    }

    public void setWhite(String white) {
        this.white = white;
    }

    public String getBlack() {
        return black;
    }

    public void setBlack(String black) {
        this.black = black;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public String getEco() {
        return eco;
    }

    public void setEco(String eco) {
        this.eco = eco;
    }

    public String getOpening() {
        return opening;
    }

    public void setOpening(String opening) {
        this.opening = opening;
    }

    public String getTimeControl() {
        return timeControl;
    }

    public void setTimeControl(String timeControl) {
        this.timeControl = timeControl;
    }

    public String getRawPgn() {
        return rawPgn;
    }

    public void setRawPgn(String rawPgn) {
        this.rawPgn = rawPgn;
    }

    public List<String> getMovesSan() {
        return movesSan;
    }

    public Integer getWhiteElo() {
        return whiteElo;
    }

    public void setWhiteElo(Integer whiteElo) {
        this.whiteElo = whiteElo;
    }

    public Integer getBlackElo() {
        return blackElo;
    }

    public void setBlackElo(Integer blackElo) {
        this.blackElo = blackElo;
    }

    public String getStartFen() {
        return startFen;
    }

    public void setStartFen(String startFen) {
        this.startFen = startFen;
    }
}
