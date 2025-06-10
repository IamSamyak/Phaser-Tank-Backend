package com.phaser.tank.model;

import org.springframework.web.socket.WebSocketSession;

public class Player {
    private final WebSocketSession session;
    private final int playerNumber;

    private int x;
    private int y;

    private Direction direction;

    // Bonus-related attributes
    private int health = 1;          // Default starting health
    private int bulletCount = 0;     // Number of bullets fired or active
    private int maxBullets = 1;      // Max allowed bullets on screen

    public Player(WebSocketSession session, int playerNumber) {
        this.session = session;
        this.playerNumber = playerNumber;
        this.x = 0;
        this.y = 0;
        this.direction = Direction.UP;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public int getX() {
        return x;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
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
                ", direction=" + direction +
                ", health=" + health +
                ", bulletCount=" + bulletCount +
                ", maxBullets=" + maxBullets +
                ", sessionId=" + (session != null ? session.getId() : "null") +
                '}';
    }
}
