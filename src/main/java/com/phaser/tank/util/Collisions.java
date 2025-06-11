package com.phaser.tank.util;

import com.phaser.tank.model.Bullet;
import com.phaser.tank.model.Direction;

import java.util.*;

public class Collisions {

    private static final int BULLET_SIZE = 1;  // In tile units now
    private static final int TANK_SIZE = 2;    // 2x2 tile area

    /**
     * Detects bullet-bullet collisions based on overlapping tile positions.
     *
     * @param bullets A collection of all active bullets
     * @return A map of bullet IDs to the bullets that collided
     */
    public static Map<String, Bullet> detectBulletCollisions(Collection<Bullet> bullets) {
        Map<String, Bullet> bulletsToDestroy = new HashMap<>();

        List<Bullet> bulletList = new ArrayList<>(bullets);
        int n = bulletList.size();

        for (int i = 0; i < n; i++) {
            Bullet a = bulletList.get(i);
            for (int j = i + 1; j < n; j++) {
                Bullet b = bulletList.get(j);
                if (a.x == b.x && a.y == b.y) {
                    bulletsToDestroy.put(a.id, a);
                    bulletsToDestroy.put(b.id, b);
                }
            }
        }

        return bulletsToDestroy;
    }

    /**
     * Check if a bullet is colliding with the base at tile (12, 25).
     */
    public static boolean isBulletCollidingWithBase(int bulletX, int bulletY) {
        int baseX = 12;
        int baseY = 25;
        return isBulletHittingTank(bulletX, bulletY, baseX, baseY);
    }

    /**
     * Check if bullet (tileX, tileY) is hitting a tank occupying 2x2 tiles ending at (bottomRightX, bottomRightY).
     */
    public static boolean isBulletHittingTank(int bulletX, int bulletY, int bottomRightX, int bottomRightY) {
        int topLeftX = bottomRightX - (TANK_SIZE - 1);
        int topLeftY = bottomRightY - (TANK_SIZE - 1);

        return bulletX >= topLeftX && bulletX <= bottomRightX &&
                bulletY >= topLeftY && bulletY <= bottomRightY;
    }

    /**
     * Check if bullet is colliding with a tank centered at (tankX, tankY) in tile units.
     * Assumes both tank and bullet are centered.
     */
    public static boolean isBulletCollidingWithTank(int bulletX, int bulletY, int tankX, int tankY) {
        int bulletHalf = BULLET_SIZE / 2;
        int tankHalf = TANK_SIZE / 2;

        int bulletLeft = bulletX - bulletHalf;
        int bulletRight = bulletX + bulletHalf;
        int bulletTop = bulletY - bulletHalf;
        int bulletBottom = bulletY + bulletHalf;

        int tankLeft = tankX - tankHalf;
        int tankRight = tankX + tankHalf;
        int tankTop = tankY - tankHalf;
        int tankBottom = tankY + tankHalf;

        return bulletRight >= tankLeft &&
                bulletLeft <= tankRight &&
                bulletBottom >= tankTop &&
                bulletTop <= tankBottom;
    }

    /**
     * Given current tile (x, y) and direction (dx, dy), returns impacted tiles.
     * Assumes bullet is 1x1 tile and moves by (dx, dy).
     */
    public static List<int[]> getImpactTiles(int x, int y, Direction direction) {
        List<int[]> impactTiles = new ArrayList<>();
        int[][] deltas;
        switch (direction) {
            case UP -> deltas = new int[][]{{-1, -1}, {-1, 0}};       // top-left, top-center, top-right
            case DOWN -> deltas = new int[][]{{1, -1}, {1, 0}};        // bottom-left, bottom-center, bottom-right
            case LEFT -> deltas = new int[][]{{-1, -1}, {0, -1}};     // top-left, middle-left, bottom-left
            case RIGHT -> deltas = new int[][]{{-1, 1}, {0, 1}};       // top-right, middle-right, bottom-right
            default -> deltas = new int[][]{{0, 0}};
        }

        for (int[] delta : deltas) {
            int row = y + delta[0];
            int col = x + delta[1];
            if (MovementValidator.isWithinMapBounds(row, col)) {
                impactTiles.add(new int[]{row, col});
            }
        }

        return impactTiles;
    }

}
