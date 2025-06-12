package com.phaser.tank.manager;

import com.phaser.tank.model.BulletOrigin;
import com.phaser.tank.model.Direction;
import com.phaser.tank.model.Room;
import com.phaser.tank.model.Enemy;
import com.phaser.tank.util.EnemyMovementHelper;
import com.phaser.tank.util.EnemyPathFinder;
import com.phaser.tank.util.EnemySpawner;
import com.phaser.tank.util.MovementValidator;

import java.util.*;
import java.util.concurrent.*;

public class EnemyManager {
    private final Room room;
    private final Map<String, Enemy> enemies = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static final int MAX_ENEMIES = 3;
    private boolean movementScheduled = false;
    private final BulletManager bulletManager;
    private final Random random = new Random();

    public EnemyManager(Room room, BulletManager bulletManager) {
        this.room = room;
        this.bulletManager = bulletManager;
    }

    public void startSpawning() {
        System.out.println("[EnemyManager] Starting enemy spawn scheduler...");
        scheduler.scheduleAtFixedRate(this::trySpawnEnemy, 3, 10, TimeUnit.SECONDS);
    }

    private void trySpawnEnemy() {
        try {
            if (enemies.size() >= MAX_ENEMIES) return;

            String enemyId = UUID.randomUUID().toString();
            Enemy enemy = EnemySpawner.spawnEnemy(enemyId);
            enemies.put(enemyId, enemy);

            int spawnRow = enemy.getY();
            int spawnCol = enemy.getX();

            // Optional special path-following enemy
            /*
            if (enemies.size() == 1) {
                int targetRow = 24;
                int targetCol = 10;
                Queue<int[]> path = EnemyPathFinder.findShortestPath(spawnRow, spawnCol, targetRow, targetCol, room.getLevelMap());
                enemy.setPath(path);
                enemy.setSpecial(true);
            }
            */

            System.out.println("[EnemyManager] Spawned enemy " + enemyId + " at (" + enemy.getX() + "," + enemy.getY() + ")");

            room.queueEnemyEvent(Map.of(
                    "action", "spawn",
                    "enemyId", enemyId,
                    "x", enemy.getX(),
                    "y", enemy.getY(),
                    "direction", enemy.getDirection()
            ));

            if (!movementScheduled) {
                scheduler.scheduleAtFixedRate(this::moveEnemies, 1, 1, TimeUnit.SECONDS);
                movementScheduled = true;
                System.out.println("[EnemyManager] Started enemy movement scheduler.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void moveEnemies() {
        try {
            List<Enemy> enemiesToFire = new ArrayList<>();
            List<String> levelMap = room.getLevelMap();

            for (Enemy enemy : enemies.values()) {
                boolean moved = false;

                if (enemy.isSpecial()) {
                    Queue<int[]> path = enemy.getPath();
                    if (path != null && !path.isEmpty()) {
                        int[] nextTile = path.poll();
                        int nextCol = nextTile[1];
                        int nextRow = nextTile[0];

                        Direction dir = EnemyMovementHelper.getDirection(enemy.getX(), enemy.getY(), nextCol, nextRow);
                        enemy.setX(nextCol);
                        enemy.setY(nextRow);
                        enemy.setDirection(dir);
                        moved = true;
                    }
                } else if (!enemy.hasMoved()) {
                    List<Direction> directionsToCheck = List.of(Direction.DOWN, Direction.LEFT, Direction.RIGHT);
                    Direction chosenDir = EnemyMovementHelper.chooseRandomValidDirection(directionsToCheck, enemy.getX(), enemy.getY(), levelMap);

                    if (chosenDir != null) {
                        int[] next = EnemyMovementHelper.getNextPosition(enemy.getX(), enemy.getY(), chosenDir);
                        Direction actualDir = EnemyMovementHelper.getDirection(enemy.getX(), enemy.getY(), next[0], next[1]);

                        enemy.setDirection(actualDir);
                        enemy.setHasMoved(true);
                        enemy.setX(next[0]);
                        enemy.setY(next[1]);
                        moved = true;
                    } else {
                        Direction fallbackDir = directionsToCheck.get(random.nextInt(directionsToCheck.size()));
                        enemy.setDirection(fallbackDir);
                        enemy.setHasMoved(true);
                    }
                } else {
                    int[] next = EnemyMovementHelper.getNextPosition(enemy.getX(), enemy.getY(), enemy.getDirection());
                    boolean canMove = MovementValidator.canMove(next[0], next[1], levelMap);

                    if (canMove) {
                        enemy.setX(next[0]);
                        enemy.setY(next[1]);
                        moved = true;
                    } else {
                        List<Direction> directions = new ArrayList<>(List.of(Direction.values()));
                        Collections.shuffle(directions);

                        for (Direction dir : directions) {
                            if (dir == enemy.getDirection()) continue;
                            int[] tryPos = EnemyMovementHelper.getNextPosition(enemy.getX(), enemy.getY(), dir);
                            if (MovementValidator.canMove(tryPos[0], tryPos[1], levelMap)) {
                                enemy.setDirection(dir);
                                break;
                            }
                        }
                    }
                }

                if (moved) {
                    room.queueEnemyEvent(Map.of(
                            "action", "move",
                            "enemyId", enemy.getId(),
                            "x", enemy.getX(),
                            "y", enemy.getY(),
                            "direction", enemy.getDirection()
                    ));
                }

                if (enemy.shouldFire()) {
                    enemiesToFire.add(enemy);
                }
            }

            for (Enemy enemy : enemiesToFire) {
                bulletManager.addBullet(
                        enemy.getX(),
                        enemy.getY(),
                        enemy.getDirection(),
                        BulletOrigin.ENEMY
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void damageEnemy(String id, int amount) {
        Enemy enemy = enemies.get(id);
        if (enemy == null) return;

        enemy.damage(amount);
        if (enemy.isDestroyed()) {
            enemies.remove(id);
            room.queueEnemyEvent(Map.of(
                    "action", "destroy",
                    "enemyId", id
            ));
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    public Map<String, Enemy> getEnemies() {
        return enemies;
    }
}
