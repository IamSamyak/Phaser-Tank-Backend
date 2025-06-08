package com.phaser.tank.util;

public class TileHelper {

    public static final int TILE_SIZE = 32;

    public static String tileMapping(char tileChar) {
        return switch (tileChar) {
            case '.' -> "empty";
            case '#' -> "brick";
            case '@' -> "stone";
            case '%' -> "bush";
            case '~' -> "water";
            case '-' -> "ice";
            default -> "unknown";
        };
    }

    public static boolean isWalkable(char tileChar) {
        String type = tileMapping(tileChar);
        return "empty".equals(type) || "bush".equals(type);
    }
}
