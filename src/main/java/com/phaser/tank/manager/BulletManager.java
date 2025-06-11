package com.phaser.tank.manager;

import com.phaser.tank.model.*;
import com.phaser.tank.util.Collisions;
import com.phaser.tank.util.EnemyMovementHelper;
import com.phaser.tank.util.MovementValidator;
import com.phaser.tank.util.TileHelper;

import java.util.*;
import java.util.concurrent.*;

import static com.phaser.tank.util.Collisions.isBulletHittingTank;

public class BulletManager {

    private final Room room;
    private final Map<String, Bullet> activeBullets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> bulletUpdateTask;
    private final Object lock = new Object();

    public BulletManager(Room room) {
        this.room = room;
    }

    public void addBullet(int x, int y, Direction direction, BulletOrigin origin) {
        Bullet bullet = new Bullet(x, y, direction, origin);
        activeBullets.put(bullet.id, bullet);

        synchronized (lock) {
            if (bulletUpdateTask == null || bulletUpdateTask.isCancelled() || bulletUpdateTask.isDone()) {
                bulletUpdateTask = scheduler.scheduleAtFixedRate(this::updateBullets, 0, 50, TimeUnit.MILLISECONDS);
            }
        }
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

        Map<String, Bullet> bulletsToDestroy = Collisions.detectBulletCollisions(activeBullets.values());

        if (!bulletsToDestroy.isEmpty()) {
            Bullet firstBullet = bulletsToDestroy.values().iterator().next();
            explosions.add(Map.of("x", firstBullet.x, "y", firstBullet.y));
        }

        for (Iterator<Map.Entry<String, Bullet>> it = activeBullets.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Bullet> entry = it.next();
            Bullet bullet = entry.getValue();

//            bullet.move();
            boolean hit = bulletsToDestroy.containsKey(bullet.id);

            for (Player player : room.getPlayers()) {
                if (bullet.origin == BulletOrigin.ENEMY && isBulletHittingTank(bullet.x, bullet.y, player.getX(), player.getY())) {
                    hit = true;
                    player.setHealth(player.getHealth() - 1);

                    if (player.getHealth() <= 0) {
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

                    explosions.add(Map.of("x", bullet.x, "y", bullet.y));
                    break;
                }
            }

            for (Enemy enemy : room.getEnemies()) {
                if (bullet.origin == BulletOrigin.PLAYER && isBulletHittingTank(bullet.x, bullet.y, enemy.getX(), enemy.getY())) {
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

                    explosions.add(Map.of("x", bullet.x, "y", bullet.y));
                    break;
                }
            }

            List<int[]> impactTiles = Collisions.getImpactTiles(bullet.x, bullet.y, bullet.direction);
            for (int[] tilePos : impactTiles) {
                int row = tilePos[0];
                int col = tilePos[1];

                char tileChar = room.getTile(col, row);
                if (TileHelper.canDestoryBullet(tileChar)) {
                    hit = true;
                    if (TileHelper.tileMapping(tileChar).equals("brick")) {
                        room.updateTile(col, row, '.');
                        tileUpdates.add(Map.of("x", col, "y", row, "tile", "."));
                        int[] next = EnemyMovementHelper.getNextPosition(bullet.x, bullet.y, bullet.direction);
                        int explosionX = next[0];
                        int explosionY = next[1];
                        if (MovementValidator.isWithinMapBounds(explosionX, explosionY)) {
                            explosions.add(Map.of("x", explosionX, "y", explosionY));
                        }

                    }
                }
            }

            if (Collisions.isBulletCollidingWithBase(bullet.x, bullet.y)) {
                hit = true;
                room.broadcast(Map.of("type", "base_destroyed"));
            }

            if (hit || MovementValidator.isOutOfBounds(bullet.x, bullet.y)) {
                bullet.destroyed = true;
            }

            if (bullet.destroyed) {
                it.remove();
                destroyedBullets.add(bullet.id);
            } else {
                bullet.move();
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

        // Stop scheduler if no bullets remain
        if (activeBullets.isEmpty()) {
            synchronized (lock) {
                if (bulletUpdateTask != null && !bulletUpdateTask.isCancelled()) {
                    bulletUpdateTask.cancel(false);
                    bulletUpdateTask = null;
                }
            }
        }
    }
}
