package com.phaser.tank.manager;

import com.phaser.tank.Room;
import com.phaser.tank.info.EnemyInfo;
import com.phaser.tank.info.EnemyInfo.Direction;
import com.phaser.tank.util.MovementValidator;

import java.util.*;
import java.util.concurrent.*;

public class EnemyManager {
    private final Room room;
    private final Map<String, EnemyInfo> enemies = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static final int MAX_ENEMIES = 2;
    private boolean movementScheduled = false;

    private static final int TILE_SIZE = 32;
    private static final List<int[]> SPAWN_POINTS = List.of(
            new int[]{1, 0},
            new int[]{12, 0},
            new int[]{24, 0}
    );

    public EnemyManager(Room room) {
        this.room = room;
    }

    public void startSpawning() {
        scheduler.scheduleAtFixedRate(this::trySpawnEnemy, 3, 10, TimeUnit.SECONDS);
    }

    private void trySpawnEnemy() {
        if (enemies.size() >= MAX_ENEMIES) return;

        String enemyId = UUID.randomUUID().toString();

        int[] spawnTile = SPAWN_POINTS.get(new Random().nextInt(SPAWN_POINTS.size()));
        double spawnX = spawnTile[0] * TILE_SIZE;
        double spawnY = spawnTile[1] * TILE_SIZE;

        EnemyInfo enemy = new EnemyInfo(enemyId, spawnX, spawnY, 180);
        enemies.put(enemyId, enemy);

        room.broadcast(Map.of(
                "type", "enemy_spawn",
                "enemyId", enemyId,
                "x", spawnX,
                "y", spawnY,
                "angle", 180
        ));

        // Schedule enemy movement only after the first enemy is spawned
        if (!movementScheduled) {
            scheduler.scheduleAtFixedRate(this::moveEnemies, 1, 1, TimeUnit.SECONDS);
            movementScheduled = true;
        }
    }

    private void moveEnemies() {
        List<Map<String, Object>> updates = new ArrayList<>();
        List<String> levelMap = room.getLevelMap();
        Random random = new Random();

        for (EnemyInfo enemy : enemies.values()) {
            // FIRST MOVE: assign a random direction regardless of current
            if (!enemy.hasMoved()) {
                List<Direction> possibleDirections = new ArrayList<>(List.of(Direction.DOWN, Direction.LEFT, Direction.RIGHT));
                Direction randomDir = possibleDirections.get(new Random().nextInt(possibleDirections.size()));
                enemy.setDirection(randomDir);
                enemy.setAngleFromDirection();
                enemy.setHasMoved(true);

                double nextX = enemy.getX();
                double nextY = enemy.getY();

                switch (randomDir) {
                    case DOWN -> nextY += TILE_SIZE;
                    case LEFT -> nextX -= TILE_SIZE;
                    case RIGHT -> nextX += TILE_SIZE;
                }

                if (MovementValidator.canMove(nextX, nextY, levelMap)) {
                    enemy.setX(nextX);
                    enemy.setY(nextY);
                }

                updates.add(Map.of(
                        "enemyId", enemy.getId(),
                        "x", enemy.getX(),
                        "y", enemy.getY(),
                        "angle", enemy.getAngle()
                ));

                continue; // Skip to next enemy
            }
            // Subsequent moves — your existing logic
            double nextX = enemy.getX();
            double nextY = enemy.getY();

            switch (enemy.getDirection()) {
                case UP -> nextY -= TILE_SIZE;
                case DOWN -> nextY += TILE_SIZE;
                case LEFT -> nextX -= TILE_SIZE;
                case RIGHT -> nextX += TILE_SIZE;
            }

            boolean canMove = MovementValidator.canMove(nextX, nextY, levelMap);
            System.out.println("can Move is " + canMove + " nx " + nextX + " ny " + nextY);
            if (canMove) {
                enemy.setX(nextX);
                enemy.setY(nextY);
                enemy.setAngleFromDirection();
                updates.add(Map.of(
                        "enemyId", enemy.getId(),
                        "x", enemy.getX(),
                        "y", enemy.getY(),
                        "angle", enemy.getAngle()
                ));
            } else {
                Direction oldDir = enemy.getDirection();
                List<Direction> directions = new ArrayList<>(List.of(Direction.values()));
                Collections.shuffle(directions); // Shuffle directions randomly

                boolean moved = false;
                for (Direction dir : directions) {
                    if (dir == oldDir) continue;

                    double tryX = enemy.getX();
                    double tryY = enemy.getY();

                    switch (dir) {
                        case UP -> tryY -= TILE_SIZE;
                        case DOWN -> tryY += TILE_SIZE;
                        case LEFT -> tryX -= TILE_SIZE;
                        case RIGHT -> tryX += TILE_SIZE;
                    }

                    if (MovementValidator.canMove(tryX, tryY, levelMap)) {
                        enemy.setDirection(dir);
                        enemy.setAngleFromDirection();
                        moved = true;
                        break;
                    }
                }

                if (!moved) {
                    // No alternative direction found, keep current direction's angle
                    enemy.setAngleFromDirection();
                }
                // Will attempt to move next tick
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
        EnemyInfo enemy = enemies.get(id);
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

    public Map<String, EnemyInfo> getEnemies() {
        return enemies;
    }
}
