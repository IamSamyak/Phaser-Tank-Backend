package com.phaser.tank.util;

import java.util.List;

public class MovementValidator {

    private static final int MAP_ROWS = 26;
    private static final int MAP_COLS = 26;

    public static boolean canMove(int col, int row, List<String> levelMap) {
        // Check if all 4 tiles of the 2x2 tank are walkable, given bottom-right at (row, col)
        return isWalkable(row - 1, col - 1, levelMap) &&  // top-left
                isWalkable(row - 1, col, levelMap) &&      // top-right
                isWalkable(row, col - 1, levelMap) &&      // bottom-left
                isWalkable(row, col, levelMap);            // bottom-right
    }

    private static boolean isWalkable(int row, int col, List<String> levelMap) {
        if (!isWithinMapBounds(row, col)) return false;
        char tileChar = getTile(col, row, levelMap);
        return TileHelper.isWalkable(tileChar);
    }

    public static boolean isWithinMapBounds(int row, int col) {
        return row >= 0 && row < MAP_ROWS && col >= 0 && col < MAP_COLS;
    }

    public static boolean isOutOfBounds(int col, int row) {
        return row < 0 || row >= MAP_ROWS || col < 0 || col >= MAP_COLS;
    }

    private static char getTile(int col, int row, List<String> levelMap) {
        if (levelMap == null || !isWithinMapBounds(row, col)) return '?';
        return levelMap.get(row).charAt(col);
    }
}
