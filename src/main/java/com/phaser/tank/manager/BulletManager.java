package com.phaser.tank.manager;

import com.phaser.tank.model.*;
import com.phaser.tank.util.*;

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

        // Queue bullet creation
        room.queueBulletEvent(Map.of(
                "action", "create",
                "bulletId", bullet.id,
                "x", x,
                "y", y,
                "direction", direction,
                "origin", origin.name().toLowerCase()
        ));

        synchronized (lock) {
            if (bulletUpdateTask == null || bulletUpdateTask.isCancelled() || bulletUpdateTask.isDone()) {
                bulletUpdateTask = scheduler.scheduleAtFixedRate(this::updateBullets, 0, 50, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void removeBullet(String bulletId) {
        activeBullets.remove(bulletId);
        room.queueBulletEvent(Map.of(
                "action", "destroy",
                "bulletId", bulletId
        ));
    }

    private void updateBullets() {
        Map<String, Bullet> bulletsToDestroy = Collisions.detectBulletCollisions(activeBullets.values());

        if (!bulletsToDestroy.isEmpty()) {
            Bullet firstBullet = bulletsToDestroy.values().iterator().next();
            room.queueExplosion(Map.of("x", firstBullet.x, "y", firstBullet.y));
        }

        for (Iterator<Map.Entry<String, Bullet>> it = activeBullets.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Bullet> entry = it.next();
            Bullet bullet = entry.getValue();
            boolean hit = bulletsToDestroy.containsKey(bullet.id);

            for (Player player : room.getPlayers()) {
                if (bullet.origin == BulletOrigin.ENEMY && isBulletHittingTank(bullet.x, bullet.y, player.getX(), player.getY())) {
                    hit = true;
                    player.setHealth(player.getHealth() - 1);

                    if (player.getHealth() <= 0) {
                        room.queueEnemyEvent(Map.of(
                                "type", "player_destroyed",
                                "playerId", player.getPlayerId()
                        ));
                    } else {
                        room.queueEnemyEvent(Map.of(
                                "type", "player_hit",
                                "playerId", player.getPlayerId(),
                                "health", player.getHealth()
                        ));
                    }

                    room.queueExplosion(Map.of("x", bullet.x, "y", bullet.y));
                    break;
                }
            }

            for (Enemy enemy : room.getEnemies()) {
                if (bullet.origin == BulletOrigin.PLAYER && isBulletHittingTank(bullet.x, bullet.y, enemy.getX(), enemy.getY())) {
                    hit = true;
                    room.damageEnemy(enemy);
                    room.queueExplosion(Map.of("x", bullet.x, "y", bullet.y));
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
                        room.queueTileUpdate(Map.of("x", col, "y", row, "tile", "."));

                        int[] next = EnemyMovementHelper.getNextPosition(bullet.x, bullet.y, bullet.direction);
                        int explosionX = next[0];
                        int explosionY = next[1];
                        if (MovementValidator.isWithinMapBounds(explosionX, explosionY)) {
                            room.queueExplosion(Map.of("x", explosionX, "y", explosionY));
                        }
                    }
                }
            }

            if (Collisions.isBulletCollidingWithBase(bullet.x, bullet.y)) {
                hit = true;
                room.broadcast(Map.of(
                        "type", "base_destroyed",
                        "x", GameConstants.BASE_X,
                        "y",GameConstants.BASE_Y
                ));
            }

            if (hit || MovementValidator.isOutOfBounds(bullet.x, bullet.y)) {
                bullet.destroyed = true;
            }

            if (bullet.destroyed) {
                it.remove();
                room.queueBulletEvent(Map.of(
                        "action", "destroy",
                        "bulletId", bullet.id
                ));
            } else {
                bullet.move();
                room.queueBulletEvent(Map.of(
                        "action", "move",
                        "bulletId", bullet.id,
                        "x", bullet.x,
                        "y", bullet.y,
                        "direction", bullet.direction
                ));
            }
        }

        // Stop scheduler if all bullets are cleared
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
