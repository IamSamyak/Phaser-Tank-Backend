package com.phaser.tank;

import java.util.*;
import java.util.concurrent.*;

public class BulletManager {

    private final Room room;
    private final Map<String, Bullet> activeBullets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final int TILE_SIZE = 32;

    public BulletManager(Room room) {
        this.room = room;
        scheduler.scheduleAtFixedRate(this::updateBullets, 50, 50, TimeUnit.MILLISECONDS);
    }

    public void addBullet(String bulletId, double x, double y, int angle) {
        Bullet bullet = new Bullet(bulletId, x, y, angle);
        activeBullets.put(bulletId, bullet);
    }

    public void removeBullet(String bulletId) {
        activeBullets.remove(bulletId);
        room.broadcast(Map.of(
                "type", "bullet_destroy",
                "bulletId", bulletId
        ));
    }

    private void updateBullets() {
        for (Iterator<Map.Entry<String, Bullet>> it = activeBullets.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Bullet> entry = it.next();
            Bullet bullet = entry.getValue();

            bullet.move();

            List<int[]> impactTiles = getImpactTiles(bullet.x, bullet.y, bullet.dx, bullet.dy);
            boolean hit = false;

            for (int[] tilePos : impactTiles) {
                int row = tilePos[0];
                int col = tilePos[1];
                if (!room.isWithinMapBounds(row, col)) continue;

                char tile = room.getTile(col, row);
                if (tile == '#' || tile == '@') {
                    hit = true;
                    if (tile == '#') {
                        room.updateTile(col, row, '.');
                        room.broadcast(Map.of(
                                "type", "tile_update",
                                "x", col,
                                "y", row,
                                "tile", "."
                        ));
                    }
                }
            }

            if (hit) {
                bullet.destroyed = true;
                room.broadcast(Map.of(
                        "type", "explosion",
                        "x", bullet.x,
                        "y", bullet.y
                ));
            }

            if (room.isOutOfBounds(bullet.x, bullet.y)) {
                bullet.destroyed = true;
            }

            if (bullet.destroyed) {
                it.remove();
                room.broadcast(Map.of(
                        "type", "bullet_destroy",
                        "bulletId", bullet.id
                ));
            } else {
                room.broadcast(Map.of(
                        "type", "bullet_move",
                        "bulletId", bullet.id,
                        "x", bullet.x,
                        "y", bullet.y
                ));
            }
        }
    }

    private List<int[]> getImpactTiles(double x, double y, double dx, double dy) {
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
