package com.phaser.tank.model;

import java.util.*;

public class Enemy {

    private final String id;
    private int x;
    private int y;
    private Direction direction;
    private int health = 1;
    private int moveCount = 0;

    private boolean hasMoved = false;

    // NEW: Special enemy support
    private boolean isSpecial = false;
    private Queue<int[]> path = new LinkedList<>();

    public Enemy(String id, int x, int y, Direction direction) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

    // Basic getters/setters
    public String getId() { return id; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public boolean isDestroyed() { return health <= 0; }

    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }

    public boolean hasMoved() { return hasMoved; }
    public void setHasMoved(boolean hasMoved) { this.hasMoved = hasMoved; }

    public void damage(int amount) {
        this.health -= amount;
    }

    // === SPECIAL ENEMY SUPPORT ===
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
    public void incrementMoveCount() {
        moveCount++;
    }

    public int getMoveCount() {
        return moveCount;
    }
}
