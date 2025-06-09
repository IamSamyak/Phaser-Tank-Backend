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

    public static boolean isWalkable(char tileChar) {
        String type = tileMapping(tileChar);
        return "empty".equals(type) || "bush".equals(type);
    }

    // New: Find all walkable tiles in a level map
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

    // New: Convert tile coordinates to pixel center
    public static double[] tileToPixelCenter(int row, int col) {
        return new double[]{
                (col + 0.5) * TILE_SIZE,
                (row + 0.5) * TILE_SIZE
        };
    }

    public static List<int[]> getImpactTiles(double x, double y, double dx, double dy) {
        int tileX = (int) (x / TILE_SIZE);
        int tileY = (int) (y / TILE_SIZE);

        List<int[]> impactTiles = new ArrayList<>();

        if (dy != 0) {
            int colLeft = (int) ((x - TILE_SIZE / 2.0) / TILE_SIZE);
            int colRight = (int) ((x + TILE_SIZE / 2.0 - 1) / TILE_SIZE);
            int row = (int) ((y + dy) / TILE_SIZE);
            impactTiles.add(new int[]{row, colLeft});
            impactTiles.add(new int[]{row, colRight});
        } else if (dx != 0) {
            int rowTop = (int) ((y - TILE_SIZE / 2.0) / TILE_SIZE);
            int rowBottom = (int) ((y + TILE_SIZE / 2.0 - 1) / TILE_SIZE);
            int col = (int) ((x + dx) / TILE_SIZE);
            impactTiles.add(new int[]{rowTop, col});
            impactTiles.add(new int[]{rowBottom, col});
        } else {
            impactTiles.add(new int[]{tileY, tileX});
        }

        return impactTiles;
    }
}
