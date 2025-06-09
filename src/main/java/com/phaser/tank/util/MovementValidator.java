package com.phaser.tank.util;

import java.util.List;

import static com.phaser.tank.util.GameConstants.TILE_SIZE;

public class MovementValidator {

    private static final int TANK_SIZE = TILE_SIZE;

    public static boolean canMove(int x, int y, List<String> levelMap) {
        int half = TANK_SIZE / 2;

        int topLeftRow = (int) Math.floor((double) (y - half) / TILE_SIZE);
        int topLeftCol = (int) Math.floor((double) (x - half) / TILE_SIZE);

        int topRightRow = topLeftRow;
        int topRightCol = (int) Math.floor((double) (x + half - 1) / TILE_SIZE);

        int bottomLeftRow = (int) Math.floor((double) (y + half - 1) / TILE_SIZE);
        int bottomLeftCol = topLeftCol;

        int bottomRightRow = bottomLeftRow;
        int bottomRightCol = topRightCol;

        return isWalkable(topLeftRow, topLeftCol, levelMap) &&
                isWalkable(topRightRow, topRightCol, levelMap) &&
                isWalkable(bottomLeftRow, bottomLeftCol, levelMap) &&
                isWalkable(bottomRightRow, bottomRightCol, levelMap);
    }

    private static boolean isWalkable(int row, int col, List<String> levelMap) {
        if (!isWithinMapBounds(row, col, levelMap)) return false;
        char tileChar = getTile(col, row, levelMap);
        return TileHelper.isWalkable(tileChar);
    }

    public static boolean isWithinMapBounds(int row, int col, List<String> levelMap) {
        return levelMap != null && row >= 0 && row < levelMap.size()
                && col >= 0 && col < levelMap.get(0).length();
    }

    public static boolean isOutOfBounds(double x, double y, List<String> levelMap) {
        if (levelMap == null) return true;
        return x < 0 || y < 0 || y >= levelMap.size() * TILE_SIZE || x >= levelMap.get(0).length() * TILE_SIZE;
    }

    private static char getTile(int x, int y, List<String> levelMap) {
        if (levelMap == null || y < 0 || y >= levelMap.size()) return '?';
        String row = levelMap.get(y);
        if (x < 0 || x >= row.length()) return '?';
        return row.charAt(x);
    }
}
