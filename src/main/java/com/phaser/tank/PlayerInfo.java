package com.phaser.tank;

import org.springframework.web.socket.WebSocketSession;

public class PlayerInfo {
    private final WebSocketSession session;
    private final int playerNumber;

    private double x;
    private double y;
    private int angle;

    // Bonus-related attributes
    private int health = 3;          // Default starting health
    private int bulletCount = 0;     // Number of bullets fired or active
    private int maxBullets = 1;      // Max allowed bullets on screen

    public PlayerInfo(WebSocketSession session, int playerNumber) {
        this.session = session;
        this.playerNumber = playerNumber;
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

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getBulletCount() {
        return bulletCount;
    }

    public void setBulletCount(int bulletCount) {
        this.bulletCount = bulletCount;
    }

    public int getMaxBullets() {
        return maxBullets;
    }

    public void setMaxBullets(int maxBullets) {
        this.maxBullets = maxBullets;
    }

    public void applyBonus(String bonusType) {
        switch (bonusType) {
            case "helmet":
            case "star":
                health++;
                break;
            case "grenade":
            case "gun":
                maxBullets++;
                break;
            case "tank":
                health += 2;
                maxBullets++;
                break;
            // Add more effects if needed
            default:
                break;
        }
    }

    @Override
    public String toString() {
        return "PlayerInfo{" +
                "playerNumber=" + playerNumber +
                ", x=" + x +
                ", y=" + y +
                ", angle=" + angle +
                ", health=" + health +
                ", bulletCount=" + bulletCount +
                ", maxBullets=" + maxBullets +
                ", sessionId=" + (session != null ? session.getId() : "null") +
                '}';
    }
}
