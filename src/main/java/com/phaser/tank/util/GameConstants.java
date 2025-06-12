package com.phaser.tank.util;

import java.util.List;

public class GameConstants {

    public static final int BASE_X = 12;
    public static final int BASE_Y = 25;

    public static final int BULLET_SIZE = 1;  // In tile units
    public static final int TANK_SIZE = 2;

    public static final List<String> BONUS_TYPES = List.of(
            "helmet", "boat", "gun", "grenade", "star", "shovel", "clock", "tank"
    );
}
