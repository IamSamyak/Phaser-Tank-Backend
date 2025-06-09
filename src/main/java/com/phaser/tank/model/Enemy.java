package com.phaser.tank.model;

import java.util.*;

public class Enemy {
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private final String id;
    private int x;
    private int y;
    private int angle;
    private int health = 1;

    private Direction direction;
    private boolean hasMoved = false;

    // NEW: Special enemy support
    private boolean isSpecial = false;
    private Queue<int[]> path = new LinkedList<>();

    public Enemy(String id, int x, int y, int angle) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.direction = Direction.values()[new Random().nextInt(4)]; // Random start direction
        setAngleFromDirection();
    }

    // Basic getters/setters
    public String getId() { return id; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getAngle() { return angle; }
    public void setAngle(int angle) { this.angle = angle; }
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

    public void setAngleFromDirection() {
        switch (direction) {
            case UP -> angle = 0;
            case RIGHT -> angle = 90;
            case DOWN -> angle = 180;
            case LEFT -> angle = 270;
        }
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
}
