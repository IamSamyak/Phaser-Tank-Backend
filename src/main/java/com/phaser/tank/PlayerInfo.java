package com.phaser.tank;

import org.springframework.web.socket.WebSocketSession;

public class PlayerInfo {
    private final WebSocketSession session;
    private final int playerNumber;

    public PlayerInfo(WebSocketSession session, int playerNumber) {
        this.session = session;
        this.playerNumber = playerNumber;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }
}
