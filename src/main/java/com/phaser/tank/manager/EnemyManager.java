package com.phaser.tank.manager;

import com.phaser.tank.model.Room;
import com.phaser.tank.model.Enemy;
import com.phaser.tank.model.Enemy.Direction;
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
    private static final int MAX_ENEMIES = 2;
    private boolean movementScheduled = false;

    private static final int TILE_SIZE = 32;

    public EnemyManager(Room room) {
        this.room = room;
    }

    public void startSpawning() {
        scheduler.scheduleAtFixedRate(this::trySpawnEnemy, 3, 10, TimeUnit.SECONDS);
    }

    private void trySpawnEnemy() {
        if (enemies.size() >= MAX_ENEMIES) return;

        String enemyId = UUID.randomUUID().toString();
        Enemy enemy = EnemySpawner.spawnEnemy(enemyId);
        enemies.put(enemyId, enemy);

        int spawnRow = enemy.getY() / TILE_SIZE;
        int spawnCol = enemy.getX() / TILE_SIZE;

        if (enemies.size() == 1) {
            int targetRow = 24;
            int targetCol = 10;

            Queue<int[]> path = EnemyPathFinder.findShortestPath(spawnRow, spawnCol, targetRow, targetCol, room.getLevelMap());
            enemy.setPath(path);
            enemy.setSpecial(true);
        }

        room.broadcast(Map.of(
                "type", "enemy_spawn",
                "enemyId", enemyId,
                "x", enemy.getX(),
                "y", enemy.getY(),
                "angle", enemy.getAngle()
        ));

        if (!movementScheduled) {
            scheduler.scheduleAtFixedRate(this::moveEnemies, 1, 1, TimeUnit.SECONDS);
            movementScheduled = true;
        }
    }

    private void moveEnemies() {
        List<Map<String, Object>> updates = new ArrayList<>();
        List<String> levelMap = room.getLevelMap();
        Random random = new Random();

        for (Enemy enemy : enemies.values()) {
            if (enemy.isSpecial()) {
                Queue<int[]> path = enemy.getPath();
                if (path != null && !path.isEmpty()) {
                    int[] nextTile = path.poll();
                    int nextX = nextTile[1] * TILE_SIZE;
                    int nextY = nextTile[0] * TILE_SIZE;

                    Direction dir = EnemyMovementHelper.getDirection(enemy.getX(), enemy.getY(), nextX, nextY);
                    enemy.setX(nextX);
                    enemy.setY(nextY);
                    enemy.setDirection(dir);
                    enemy.setAngleFromDirection();

                    updates.add(Map.of(
                            "enemyId", enemy.getId(),
                            "x", enemy.getX(),
                            "y", enemy.getY(),
                            "angle", enemy.getAngle()
                    ));
                }
                continue;
            }

            if (!enemy.hasMoved()) {
                List<Direction> directionsToCheck = List.of(Direction.DOWN, Direction.LEFT, Direction.RIGHT);

                Direction chosenDir = EnemyMovementHelper.chooseRandomValidDirection(directionsToCheck, enemy.getX(), enemy.getY(), levelMap);

                if (chosenDir != null) {
                    int[] next = EnemyMovementHelper.getNextPosition(enemy.getX(), enemy.getY(), chosenDir);

                    Direction actualDir = EnemyMovementHelper.getDirection(enemy.getX(), enemy.getY(), next[0], next[1]);

                    enemy.setDirection(actualDir);
                    enemy.setAngleFromDirection();
                    enemy.setHasMoved(true);
                    enemy.setX(next[0]);
                    enemy.setY(next[1]);
                } else {
                    // fallback: keep previous direction or random from directionsToCheck
                    Direction fallbackDir = directionsToCheck.get(random.nextInt(directionsToCheck.size()));
                    enemy.setDirection(fallbackDir);
                    enemy.setAngleFromDirection();
                    enemy.setHasMoved(true);
                }

                updates.add(Map.of(
                        "enemyId", enemy.getId(),
                        "x", enemy.getX(),
                        "y", enemy.getY(),
                        "angle", enemy.getAngle()
                ));
                continue;
            }

            int[] next = EnemyMovementHelper.getNextPosition(enemy.getX(), enemy.getY(), enemy.getDirection());
            boolean canMove = MovementValidator.canMove(next[0], next[1], levelMap);
            if (canMove) {
                enemy.setX(next[0]);
                enemy.setY(next[1]);
                enemy.setAngleFromDirection();

                updates.add(Map.of(
                        "enemyId", enemy.getId(),
                        "x", enemy.getX(),
                        "y", enemy.getY(),
                        "angle", enemy.getAngle()
                ));
            } else {
                List<Direction> directions = new ArrayList<>(List.of(Direction.values()));
                Collections.shuffle(directions);

                boolean moved = false;
                for (Direction dir : directions) {
                    if (dir == enemy.getDirection()) continue;
                    int[] tryPos = EnemyMovementHelper.getNextPosition(enemy.getX(), enemy.getY(), dir);
                    if (MovementValidator.canMove(tryPos[0], tryPos[1], levelMap)) {
                        enemy.setDirection(dir);
                        enemy.setAngleFromDirection();
                        moved = true;
                        break;
                    }
                }

                if (!moved) {
                    enemy.setAngleFromDirection();
                }
            }
        }

        if (!updates.isEmpty()) {
            room.broadcast(Map.of(
                    "type", "enemy_move_batch",
                    "enemies", updates
            ));
        }
    }

    public void damageEnemy(String id, int amount) {
        Enemy enemy = enemies.get(id);
        if (enemy == null) return;

        enemy.damage(amount);
        if (enemy.isDestroyed()) {
            enemies.remove(id);
            room.broadcast(Map.of(
                    "type", "enemy_destroyed",
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
