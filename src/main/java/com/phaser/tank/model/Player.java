package com.phaser.tank.model;

import org.springframework.web.socket.WebSocketSession;

import java.util.Random;

public class Player {
    private static final Random RANDOM = new Random();

    private final WebSocketSession session;
    private final int playerId;

    private int x;
    private int y;
    private boolean active = true;

    private Direction direction = Direction.UP;

    // Bonus-related attributes
    private int health = 1;          // Default starting health
    private int bulletCount = 0;     // Number of bullets fired or active
    private int maxBullets = 1;      // Max allowed bullets on screen

    public Player(WebSocketSession session, int playerId) {
        this.session = session;
        this.playerId = playerId;

        assignInitialState(playerId);
    }

    private static final Object[][] STATIC_SPAWN_DATA = {
            {10, 25, Direction.UP},    // Player 1
            {16, 25, Direction.UP},    // Player 2
            {10, 1, Direction.DOWN},   // Player 3
            {16, 1, Direction.DOWN}    // Player 4
    };

    private void assignInitialState(int playerId) {
        if (playerId >= 1 && playerId <= STATIC_SPAWN_DATA.length) {
            Object[] data = STATIC_SPAWN_DATA[playerId - 1];
            this.x = (int) data[0];
            this.y = (int) data[1];
            this.direction = (Direction) data[2];
        } else {
            this.x = 25 + RANDOM.nextInt(5);
            this.y = 25 + RANDOM.nextInt(5);
            this.direction = Direction.UP;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
