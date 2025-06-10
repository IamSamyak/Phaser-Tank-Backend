package com.phaser.tank.model;

public class Bullet {
    public final String id;
    public int x, y;
    public final Direction direction;
    public int dx, dy;
    public int speed = 1;
    public boolean destroyed = false;
    public final BulletOrigin origin; // Enum type for bullet origin

    private int tickCount = 0;
    private static final int TILE_SIZE = 32;
    private static final int MAX_TICKS = 27;

    public Bullet(String id, int x, int y, Direction direction, BulletOrigin origin) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.origin = origin;
        initializeDirection(direction);
    }

    private void initializeDirection(Direction direction) {
        switch (direction) {
            case UP -> { dx = 0; dy = -TILE_SIZE; }
            case RIGHT -> { dx = TILE_SIZE; dy = 0; }
            case DOWN -> { dx = 0; dy = TILE_SIZE; }
            case LEFT -> { dx = -TILE_SIZE; dy = 0; }
        }
    }

    public void move() {
        x += dx * speed;
        y += dy * speed;
        tickCount++;
        if (tickCount > MAX_TICKS) {
            destroyed = true;
        }
    }
}
