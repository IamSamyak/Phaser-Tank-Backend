package com.phaser.tank.manager;

import com.phaser.tank.model.Bullet;
import com.phaser.tank.model.Room;
import com.phaser.tank.util.TileHelper;

import java.util.*;
import java.util.concurrent.*;

public class BulletManager {

    private final Room room;
    private final Map<String, Bullet> activeBullets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public BulletManager(Room room) {
        this.room = room;
        scheduler.scheduleAtFixedRate(this::updateBullets, 50, 50, TimeUnit.MILLISECONDS);
    }

    public void addBullet(String bulletId, int x, int y, int angle) {
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
        List<Map<String, Object>> moveUpdates = new ArrayList<>();
        List<String> destroyedBullets = new ArrayList<>();
        List<Map<String, Object>> tileUpdates = new ArrayList<>();
        List<Map<String, Object>> explosions = new ArrayList<>();

        for (Iterator<Map.Entry<String, Bullet>> it = activeBullets.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Bullet> entry = it.next();
            Bullet bullet = entry.getValue();

            bullet.move();

            boolean hit = false;

            List<int[]> impactTiles = TileHelper.getImpactTiles(bullet.x, bullet.y, bullet.dx, bullet.dy);
            for (int[] tilePos : impactTiles) {
                int row = tilePos[0];
                int col = tilePos[1];
                if (!room.isWithinMapBounds(row, col)) continue;

                char tile = room.getTile(col, row);
                if (tile == '#' || tile == '@') {
                    hit = true;
                    if (tile == '#') {
                        room.updateTile(col, row, '.');
                        tileUpdates.add(Map.of(
                                "x", col,
                                "y", row,
                                "tile", "."
                        ));
                    }
                }
            }

            if (hit) {
                bullet.destroyed = true;
                explosions.add(Map.of(
                        "x", bullet.x,
                        "y", bullet.y
                ));
            }

            if (room.isOutOfBounds(bullet.x, bullet.y)) {
                bullet.destroyed = true;
            }

            if (bullet.destroyed) {
                it.remove();
                destroyedBullets.add(bullet.id);
            } else {
                moveUpdates.add(Map.of(
                        "bulletId", bullet.id,
                        "x", bullet.x,
                        "y", bullet.y
                ));
            }
        }

        if (!tileUpdates.isEmpty()) {
            room.broadcast(Map.of(
                    "type", "tile_update_batch",
                    "tiles", tileUpdates
            ));
        }

        if (!explosions.isEmpty()) {
            room.broadcast(Map.of(
                    "type", "explosion_batch",
                    "explosions", explosions
            ));
        }

        if (!destroyedBullets.isEmpty()) {
            room.broadcast(Map.of(
                    "type", "bullet_destroy_batch",
                    "bulletIds", destroyedBullets
            ));
        }

        if (!moveUpdates.isEmpty()) {
            room.broadcast(Map.of(
                    "type", "bullet_move_batch",
                    "bullets", moveUpdates
            ));
        }
    }
}
