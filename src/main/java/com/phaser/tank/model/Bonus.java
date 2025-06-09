package com.phaser.tank.model;

public class Bonus {
    private final String id;
    private final double x;
    private final double y;
    private final String type;

    public Bonus(String id, double x, double y, String type) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getType() {
        return type;
    }
}
