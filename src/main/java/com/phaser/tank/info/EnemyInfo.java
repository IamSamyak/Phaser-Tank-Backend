package com.phaser.tank.info;

import java.util.*;

public class EnemyInfo {
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private final String id;
    private double x;
    private double y;
    private int angle;
    private int health = 1;

    private Direction direction;
    private boolean hasMoved = false;

    // NEW: Special enemy support
    private boolean isSpecial = false;
    private Queue<int[]> path = new LinkedList<>();

    public EnemyInfo(String id, double x, double y, int angle) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.direction = Direction.values()[new Random().nextInt(4)]; // Random start direction
        setAngleFromDirection();
    }

    // Basic getters/setters
    public String getId() { return id; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
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
