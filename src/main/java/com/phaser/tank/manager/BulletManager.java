package com.phaser.tank.manager;

import com.phaser.tank.model.*;
import com.phaser.tank.util.Collisions;
import com.phaser.tank.util.TileHelper;

import java.util.*;
import java.util.concurrent.*;

import static com.phaser.tank.util.Collisions.isBulletCollidingWithTank;
import static com.phaser.tank.util.Collisions.isBulletHittingWithTank;
import static com.phaser.tank.util.GameConstants.TILE_SIZE;

public class BulletManager {

    private final Room room;
    private final Map<String, Bullet> activeBullets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public BulletManager(Room room) {
        this.room = room;
        scheduler.scheduleAtFixedRate(this::updateBullets, 50, 50, TimeUnit.MILLISECONDS);
    }

    public void addBullet(String bulletId, int x, int y, Direction  direction, BulletOrigin origin) {

        // Adjust starting position based on angle
//        switch (angle) {
//            case 0:   // Facing Up
//                y -= TILE_SIZE;
//                break;
//            case 90:  // Facing Right
//                x += TILE_SIZE;
//                break;
//            case 180: // Facing Down
//                y += TILE_SIZE;
//                break;
//            case 270: // Facing Left
//                x -= TILE_SIZE;
//                break;
//        }

        Bullet bullet = new Bullet(bulletId, x, y, direction, origin);
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

        // Bullet collision detection at their exact meeting point
        Map<String, Bullet> bulletsToDestroy = Collisions.detectBulletCollisions(activeBullets.values());


        // Trigger explosion only at the precise meeting point
        if (!bulletsToDestroy.isEmpty()) {
            Bullet firstBullet = bulletsToDestroy.values().iterator().next();
            explosions.add(Map.of(
                    "x", firstBullet.x,
                    "y", firstBullet.y
            ));
        }

        for (Iterator<Map.Entry<String, Bullet>> it = activeBullets.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Bullet> entry = it.next();
            Bullet bullet = entry.getValue();

            bullet.move();

            boolean hit = bulletsToDestroy.containsKey(bullet.id);

            for (Player player : room.getPlayers()) {
                if (bullet.origin == BulletOrigin.ENEMY && isBulletHittingWithTank(bullet.x, bullet.y, player.getX(), player.getY())) {
                    hit = true;
                    player.setHealth(player.getHealth() - 1);

                    if (player.getHealth() <= 0) {
                        // optional: mark player as destroyed, or respawn logic
                        room.broadcast(Map.of(
                                "type", "player_destroyed",
                                "playerNumber", player.getPlayerNumber()
                        ));
                    } else {
                        room.broadcast(Map.of(
                                "type", "player_hit",
                                "playerNumber", player.getPlayerNumber(),
                                "health", player.getHealth()
                        ));
                    }

                    explosions.add(Map.of(
                            "x", bullet.x,
                            "y", bullet.y
                    ));

                    break; // stop checking after first hit
                }
            }

            for (Enemy enemy : room.getEnemies()) {
                if (bullet.origin == BulletOrigin.PLAYER && isBulletHittingWithTank(bullet.x, bullet.y, enemy.getX(), enemy.getY())) {
                    hit = true;
                    enemy.setHealth(enemy.getHealth() - 1);

                    if (enemy.getHealth() <= 0) {
                        room.removeEnemy(enemy);
                        room.broadcast(Map.of(
                                "type", "enemy_destroyed",
                                "enemyId", enemy.getId()
                        ));
                    } else {
                        room.broadcast(Map.of(
                                "type", "enemy_hit",
                                "enemyId", enemy.getId(),
                                "health", enemy.getHealth()
                        ));
                    }

                    explosions.add(Map.of(
                            "x", bullet.x,
                            "y", bullet.y
                    ));

                    break;
                }
            }

            List<int[]> impactTiles = Collisions.getImpactTiles(bullet.x, bullet.y, bullet.dx, bullet.dy);
            for (int[] tilePos : impactTiles) {
                int row = tilePos[0];
                int col = tilePos[1];
                if (!room.isWithinMapBounds(row, col)) continue;

                char tileChar = room.getTile(col, row);
                if (TileHelper.canDestoryBullet(tileChar)) {
                    hit = true;
                    if (TileHelper.tileMapping(tileChar).equals("brick")) {
                        room.updateTile(col, row, '.');
                        tileUpdates.add(Map.of("x", col, "y", row, "tile", ".")); // brick to empty
                        explosions.add(Map.of("x", bullet.x, "y", bullet.y));
                    }
                }
            }

            if (Collisions.isBulletCollidingWithBase(bullet.x, bullet.y)) {
                hit = true;
                room.broadcast(Map.of(
                        "type", "base_destroyed"
                ));
            }

            if (hit) {
                bullet.destroyed = true;
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
                        "y", bullet.y,
                        "direction", bullet.direction
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
