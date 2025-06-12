package com.phaser.tank.model;

import org.springframework.web.socket.WebSocketSession;

import java.util.Random;

public class Player {
    private static final Random RANDOM = new Random();

    private final WebSocketSession session;
    private final int playerId;

    private int x;
    private int y;

    private Direction direction;

    // Bonus-related attributes
    private int health = 1;          // Default starting health
    private int bulletCount = 0;     // Number of bullets fired or active
    private int maxBullets = 1;      // Max allowed bullets on screen

    public Player(WebSocketSession session, int playerId) {
        this.session = session;
        this.playerId = playerId;

        assignInitialCoordinates(playerId);
        this.direction = Direction.UP;
    }

    private void assignInitialCoordinates(int playerId) {
        if (playerId == 1) {
            this.x = 10;
            this.y = 25;
        } else if (playerId == 2) {
            this.x = 16;
            this.y = 25;
        } else {
            this.x = 25 + RANDOM.nextInt(5); // 25–29
            this.y = 25 + RANDOM.nextInt(5); // 25–29
        }
    }

    public WebSocketSession getSession() {
        return session;
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getId() {
        return  playerId;
    }

    public int getX() {
        return x;
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

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
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

    public void damage(int amount) {
        this.health -= amount;
    }

    public boolean isDestroyed() {
        return this.health <= 0;
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
            default:
                break;
        }
    }

    @Override
    public String toString() {
        return "PlayerInfo{" +
                "playerId=" + playerId +
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
