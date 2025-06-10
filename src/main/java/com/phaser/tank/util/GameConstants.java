package com.phaser.tank.util;

import java.util.List;

public class GameConstants {
    public static final int TILE_SIZE = 32;
    private static final int BULLET_SIZE = 32;           // or whatever your actual size is
    private static final int TANK_SIZE = 64;

    public static final List<String> BONUS_TYPES = List.of(
            "helmet", "boat", "gun", "grenade", "star", "shovel", "clock", "tank"
    );
}
