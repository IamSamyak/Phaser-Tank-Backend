package com.phaser.tank.util;

import com.phaser.tank.model.Bullet;

import java.util.*;

import static com.phaser.tank.util.GameConstants.TILE_SIZE;

public class Collisions {


    private static final int BULLET_SIZE = 32;           // or whatever your actual size is
    private static final int TANK_SIZE = 64;

    /**
     * Detects bullet-bullet collisions based on overlapping positions.
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

    public static boolean isBulletCollidingWithBase(int bulletX, int bulletY) {
        int baseX = 12 * TILE_SIZE;
        int baseY = 25 * TILE_SIZE;

        return isBulletHittingWithTank(bulletX, bulletY, baseX, baseY);
    }

    public static boolean isBulletHittingWithTank(int bulletX, int bulletY, int tankBottomRightX, int tankBottomRightY) {

        // Adjust tank boundaries based on bottom-right coordinates
        int tankBottomLeftX = tankBottomRightX - 64;
        int tankTopLeftX = tankBottomLeftX;
        int tankTopLeftY = tankBottomRightY - 64;
        int tankTopRightX = tankBottomRightX;

        // Check if bullet is inside tank
        boolean insideTank = bulletX >= tankTopLeftX && bulletX < tankTopRightX
                && bulletY >= tankTopLeftY && bulletY < tankBottomRightY;

        // Check if bullet is hitting any of the tank corners
        boolean hittingCorner = (bulletX == tankTopLeftX && bulletY == tankTopLeftY) ||      // Top-left
                (bulletX == tankTopRightX && bulletY == tankTopLeftY) ||  // Top-right
                (bulletX == tankBottomLeftX && bulletY == tankBottomRightY) || // Bottom-left
                (bulletX == tankBottomRightX && bulletY == tankBottomRightY); // Bottom-right

        return insideTank || hittingCorner; // True if inside or hitting corner
    }

    public static boolean isBulletCollidingWithTank(int bulletX, int bulletY,
                                                    int tankX, int tankY) {

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


    public static List<int[]> getImpactTiles(int x, int y, int dx, int dy) {
        int tileX = x / TILE_SIZE;
        int tileY = y / TILE_SIZE;

        List<int[]> impactTiles = new ArrayList<>();

        if (dy != 0) {
            int colLeft = (x - TILE_SIZE / 2) / TILE_SIZE;
            int colRight = (x + TILE_SIZE / 2 - 1) / TILE_SIZE;
            int row = (y + dy) / TILE_SIZE;
            impactTiles.add(new int[]{row, colLeft});
            impactTiles.add(new int[]{row, colRight});
        } else if (dx != 0) {
            int rowTop = (y - TILE_SIZE / 2) / TILE_SIZE;
            int rowBottom = (y + TILE_SIZE / 2 - 1) / TILE_SIZE;
            int col = (x + dx) / TILE_SIZE;
            impactTiles.add(new int[]{rowTop, col});
            impactTiles.add(new int[]{rowBottom, col});
        } else {
            impactTiles.add(new int[]{tileY, tileX});
        }

        return impactTiles;
    }
}
