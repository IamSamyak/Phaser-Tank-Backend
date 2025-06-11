package com.phaser.tank.model;

import java.util.*;

public class Enemy {

    private final String id;
    private int x;
    private int y;
    private Direction direction;
    private int health = 1;
    private boolean hasMoved = false;

    // Special enemy support
    private boolean isSpecial = false;
    private Queue<int[]> path = new LinkedList<>();

    // Random firing behavior
    private static final Random random = new Random();

    public Enemy(String id, int x, int y, Direction direction) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

    // === Basic Getters/Setters ===

    public String getId() {
        return id;
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

    public boolean isDestroyed() {
        return health <= 0;
    }

    public void damage(int amount) {
        this.health -= amount;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    // === Special Enemy Support ===

    public boolean isSpecial() {
        return isSpecial;
    }

    public void setSpecial(boolean special) {
        this.isSpecial = special;
    }

    public Queue<int[]> getPath() {
        return path;
    }

    public void setPath(Queue<int[]> path) {
        this.path = path;
    }

    // === Randomized Fire Check ===

    public boolean shouldFire() {
        double chance = isSpecial ? 0.3 : 0.2; // 30% for special, 20% for normal
        return random.nextDouble() < chance;
    }

    @Override
    public String toString() {
        return "Enemy{" +
                "id='" + id + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", direction=" + direction +
                ", health=" + health +
                ", hasMoved=" + hasMoved +
                ", isSpecial=" + isSpecial +
                ", path=" + path +
                '}';
    }
}
