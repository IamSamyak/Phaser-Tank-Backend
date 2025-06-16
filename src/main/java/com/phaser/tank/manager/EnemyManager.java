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

            // Prevent spawn overlap
            if (!MovementValidator.canOccupy(
                    enemy.getX(),
                    enemy.getY(),
                    enemyId,
                    null,
                    enemies,
                    room.getPlayerMap(),
                    Set.of()
            )) return;

            enemies.put(enemyId, enemy);

            // Optional: special path-following enemy
            /*
            if (enemies.size() == 1) {
                int targetRow = 24;
                int targetCol = 10;
                Queue<int[]> path = EnemyPathFinder.findShortestPath(spawnRow, spawnCol, targetRow, targetCol, room.getLevelMap());
                enemy.setPath(path);
                enemy.setSpecial(true);
            }
            */

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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void moveEnemies() {
        try {
            List<Enemy> enemiesToFire = new ArrayList<>();
            List<String> levelMap = room.getLevelMap();
            Set<String> reservedTiles = new HashSet<>();

            for (Enemy enemy : enemies.values()) {
                boolean moved = false;
                int currentX = enemy.getX();
                int currentY = enemy.getY();

                if (enemy.isSpecial()) {
                    Queue<int[]> path = enemy.getPath();
                    if (path != null && !path.isEmpty()) {
                        int[] nextTile = path.peek(); // look ahead
                        int nextCol = nextTile[1];
                        int nextRow = nextTile[0];

                        if (MovementValidator.canMove(nextCol, nextRow, levelMap) &&
                                MovementValidator.canOccupy(nextCol, nextRow, enemy.getId(), null, enemies, room.getPlayerMap(), reservedTiles)) {

                            path.poll(); // actually take step
                            Direction dir = EnemyMovementHelper.getDirection(currentX, currentY, nextCol, nextRow);
                            enemy.setX(nextCol);
                            enemy.setY(nextRow);
                            enemy.setDirection(dir);
                            moved = true;
                            reserveTiles(reservedTiles, nextCol, nextRow);
                        }
                    }
                } else if (!enemy.hasMoved()) {
                    List<Direction> directionsToCheck = List.of(Direction.DOWN, Direction.LEFT, Direction.RIGHT);
                    Direction chosenDir = EnemyMovementHelper.chooseRandomValidDirection(directionsToCheck, currentX, currentY, levelMap);

                    if (chosenDir != null) {
                        int[] next = EnemyMovementHelper.getNextPosition(currentX, currentY, chosenDir);

                        if (MovementValidator.canOccupy(next[0], next[1], enemy.getId(), null, enemies, room.getPlayerMap(), reservedTiles)) {
                            Direction actualDir = EnemyMovementHelper.getDirection(currentX, currentY, next[0], next[1]);

                            enemy.setDirection(actualDir);
                            enemy.setHasMoved(true);
                            enemy.setX(next[0]);
                            enemy.setY(next[1]);
                            moved = true;
                            reserveTiles(reservedTiles, next[0], next[1]);
                        }
                    }

                    if (!moved) {
                        // Couldn't move, just set fallback dir
                        Direction fallbackDir = directionsToCheck.get(random.nextInt(directionsToCheck.size()));
                        enemy.setDirection(fallbackDir);
                        enemy.setHasMoved(true);
                    }
                } else {
                    int[] next = EnemyMovementHelper.getNextPosition(currentX, currentY, enemy.getDirection());
                    boolean canMove = MovementValidator.canMove(next[0], next[1], levelMap) &&
                            MovementValidator.canOccupy(next[0], next[1], enemy.getId(), null, enemies, room.getPlayerMap(), reservedTiles);

                    if (canMove) {
                        enemy.setX(next[0]);
                        enemy.setY(next[1]);
                        moved = true;
                        reserveTiles(reservedTiles, next[0], next[1]);
                    } else {
                        List<Direction> directions = new ArrayList<>(List.of(Direction.values()));
                        Collections.shuffle(directions);

                        for (Direction dir : directions) {
                            if (dir == enemy.getDirection()) continue;

                            int[] tryPos = EnemyMovementHelper.getNextPosition(currentX, currentY, dir);
                            if (MovementValidator.canMove(tryPos[0], tryPos[1], levelMap) &&
                                    MovementValidator.canOccupy(tryPos[0], tryPos[1], enemy.getId(), null, enemies, room.getPlayerMap(), reservedTiles)) {
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

    private void reserveTiles(Set<String> reserved, int x, int y) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                reserved.add((x + dx) + "," + (y + dy));
            }
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
