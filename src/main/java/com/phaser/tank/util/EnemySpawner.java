package com.phaser.tank.util;

import com.phaser.tank.model.Enemy;

import java.util.List;
import java.util.Random;

public class EnemySpawner {
    private static final int TILE_SIZE = 32;
    private static final List<int[]> SPAWN_POINTS = List.of(
            new int[]{12, 0},
            new int[]{24, 0}
    );

    public static int[] getRandomSpawnTile() {
        return SPAWN_POINTS.get(new Random().nextInt(SPAWN_POINTS.size()));
    }

    public static Enemy spawnEnemy(String id) {
        int[] spawnTile = getRandomSpawnTile();
        int spawnX = spawnTile[0] * TILE_SIZE;
        int spawnY = spawnTile[1] * TILE_SIZE;

        return new Enemy(id, spawnX, spawnY, 180);
    }
}
