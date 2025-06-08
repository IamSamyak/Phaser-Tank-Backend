package com.phaser.tank;

public class Bullet {
    public final String id;
    public double x, y;
    public int angle;
    public double dx, dy;
    public double speed = 1;
    public boolean destroyed = false;

    private int tickCount = 0;
    private static final int TILE_SIZE = 32;
    private static final int MAX_TICKS = 27;

    public Bullet(String id, double x, double y, int angle) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.angle = angle;
        switch (angle) {
            case 0 -> { dx = 0; dy = -TILE_SIZE; }
            case 90 -> { dx = TILE_SIZE; dy = 0; }
            case 180 -> { dx = 0; dy = TILE_SIZE; }
            case 270 -> { dx = -TILE_SIZE; dy = 0; }
            default -> { dx = 0; dy = -TILE_SIZE; }
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
