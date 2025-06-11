package com.phaser.tank.model;

import java.util.UUID;

public class Bullet {
    public final String id;
    public int x, y;
    public final Direction direction;// Speed in pixels per tick
    public boolean destroyed = false;
    public final BulletOrigin origin;

    private int tickCount = 0;
    private static final int MAX_TICKS = 27;

    public Bullet(int x, int y, Direction direction, BulletOrigin origin) {
        this.id = UUID.randomUUID().toString();  // Generate bullet ID internally
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.origin = origin;
    }

    public void move() {
        switch (direction) {
            case UP -> y -= 1;
            case DOWN -> y += 1;
            case LEFT -> x -= 1;
            case RIGHT -> x += 1;
        }

        tickCount++;
        if (tickCount > MAX_TICKS) {
            destroyed = true;
        }
    }

    @Override
    public String toString() {
        return "Bullet{" +
                "id='" + id + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", direction=" + direction +
                ", destroyed=" + destroyed +
                ", origin=" + origin +
                ", tickCount=" + tickCount +
                '}';
    }
}
