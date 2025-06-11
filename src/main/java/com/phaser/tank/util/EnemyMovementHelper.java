package com.phaser.tank.util;

import com.phaser.tank.model.Direction;

import java.util.List;
import java.util.Random;

public class EnemyMovementHelper {

    public static Direction getDirection(int fromX, int fromY, int toX, int toY) {
        if (toX > fromX) return Direction.RIGHT;
        else if (toX < fromX) return Direction.LEFT;
        else if (toY > fromY) return Direction.DOWN;
        else if (toY < fromY) return Direction.UP;
        return Direction.DOWN;
    }

    public static int[] getNextPosition(int x, int y, Direction dir) {
        return switch (dir) {
            case UP -> new int[]{x, y - 1};
            case DOWN -> new int[]{x, y + 1};
            case LEFT -> new int[]{x - 1, y};
            case RIGHT -> new int[]{x + 1, y};
        };
    }

    public static Direction chooseRandomValidDirection(List<Direction> candidates, int currentX, int currentY, List<String> levelMap) {
        Random random = new Random();

        // Filter only valid directions based on walkability
        List<Direction> validDirections = candidates.stream()
                .filter(dir -> {
                    int[] next = getNextPosition(currentX, currentY, dir);
                    return MovementValidator.canMove(next[0], next[1], levelMap);
                }).toList();

        if (validDirections.isEmpty()) return null;
        return validDirections.get(random.nextInt(validDirections.size()));
    }
}
