package com.phaser.tank;

import org.springframework.web.socket.WebSocketSession;

public class PlayerInfo {
    private final WebSocketSession session;
    private final int playerNumber;

    // Add position and angle
    private double x;
    private double y;
    private int angle;

    public PlayerInfo(WebSocketSession session, int playerNumber) {
        this.session = session;
        this.playerNumber = playerNumber;
        // Initialize position and angle to defaults
        this.x = 0;
        this.y = 0;
        this.angle = 0;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    // Position getters and setters
    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    // Angle getter and setter
    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    @Override
    public String toString() {
        return "PlayerInfo{" +
                "playerNumber=" + playerNumber +
                ", x=" + x +
                ", y=" + y +
                ", angle=" + angle +
                ", sessionId=" + (session != null ? session.getId() : "null") +
                '}';
    }
}
