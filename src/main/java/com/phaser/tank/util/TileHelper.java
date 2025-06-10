package com.phaser.tank.util;

import java.util.ArrayList;
import java.util.List;

import static com.phaser.tank.util.GameConstants.TILE_SIZE;

public class TileHelper {

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

    public static boolean canDestoryBullet(char tileChar) {
        String type = tileMapping(tileChar);
        return "brick".equals(type) || "stone".equals(type) ||"ice".equals(type);
    }

    public static boolean isWalkable(char tileChar) {
        String type = tileMapping(tileChar);
        return "empty".equals(type) || "bush".equals(type);
    }

    public static List<int[]> findWalkableTiles(List<String> levelMap) {
        List<int[]> walkables = new ArrayList<>();
        if (levelMap == null) return walkables;

        for (int row = 0; row < levelMap.size(); row++) {
            String line = levelMap.get(row);
            for (int col = 0; col < line.length(); col++) {
                if (isWalkable(line.charAt(col))) {
                    walkables.add(new int[]{row, col});
                }
            }
        }
        return walkables;
    }

    public static double[] tileToPixelCenter(int row, int col) {
        return new double[]{
                (col + 0.5) * TILE_SIZE,
                (row + 0.5) * TILE_SIZE
        };
    }

    public static char getTile(int x, int y, List<String> levelMap) {
        if (levelMap == null || y < 0 || y >= levelMap.size()) return '?';
        String row = levelMap.get(y);
        if (x < 0 || x >= row.length()) return '?';
        return row.charAt(x);
    }

    public static void updateTile(int x, int y, char newChar, List<String> levelMap) {
        if (levelMap == null || y < 0 || y >= levelMap.size()) return;
        String row = levelMap.get(y);
        if (x < 0 || x >= row.length()) return;
        char[] chars = row.toCharArray();
        chars[x] = newChar;
        levelMap.set(y, new String(chars));
    }
}
