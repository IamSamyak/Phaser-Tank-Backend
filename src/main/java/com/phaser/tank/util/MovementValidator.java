package com.phaser.tank.util;

import com.phaser.tank.model.Enemy;
import com.phaser.tank.model.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public static boolean canOccupy(
            int x,
            int y,
            String selfEnemyId,
            Integer selfPlayerId,
            Map<String, Enemy> enemies,
            Map<Integer, Player> players,
            Set<String> reservedTiles
    ) {
        // Check enemy overlap (excluding self)
        for (Enemy enemy : enemies.values()) {
            if (selfEnemyId != null && selfEnemyId.equals(enemy.getId())) continue;
            if (rectanglesOverlap(x, y, enemy.getX(), enemy.getY())) return false;
        }

        // Check player overlap (excluding self)
        for (Map.Entry<Integer, Player> entry : players.entrySet()) {
            Integer playerId = entry.getKey();
            if (selfPlayerId != null && selfPlayerId.equals(playerId)) continue;

            Player player = entry.getValue();
            if (!player.isActive()) continue;

            if (rectanglesOverlap(x, y, player.getX(), player.getY())) return false;
        }

        // Check reserved tiles (2x2 tiles starting at (x, y))
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                String key = (x + dx) + "," + (y + dy);
                if (reservedTiles.contains(key)) return false;
            }
        }

        return true;
    }

    public static boolean rectanglesOverlap(int x1, int y1, int x2, int y2) {
        return !(x1 + 1 < x2 || x2 + 1 < x1 || y1 + 1 < y2 || y2 + 1 < y1);
    }
}
