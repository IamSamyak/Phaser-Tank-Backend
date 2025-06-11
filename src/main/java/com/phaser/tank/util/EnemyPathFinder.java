package com.phaser.tank.util;

import java.util.*;

public class EnemyPathFinder {

    public static Queue<int[]> findShortestPath(int startRow, int startCol, int targetRow, int targetCol, List<String> levelMap) {
        int rows = levelMap.size();
        int cols = levelMap.get(0).length();
        boolean[][] visited = new boolean[rows][cols];
        int[][][] parent = new int[rows][cols][2];

        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int curRow = current[0], curCol = current[1];

            if (curRow == targetRow && curCol == targetCol) break;

            for (int[] dir : directions) {
                int newRow = curRow + dir[0];
                int newCol = curCol + dir[1];
                if (visited[newRow][newCol]) continue;
                if (!MovementValidator.canMove(newRow, newCol, levelMap)) continue;

                visited[newRow][newCol] = true;
                parent[newRow][newCol] = new int[]{curRow, curCol};
                queue.add(new int[]{newRow, newCol});
            }
        }

        LinkedList<int[]> path = new LinkedList<>();
        int[] curr = {targetRow, targetCol};
        if (!visited[targetRow][targetCol]) return path;

        while (!(curr[0] == startRow && curr[1] == startCol)) {
            path.addFirst(curr);
            curr = parent[curr[0]][curr[1]];
        }
        path.poll(); // remove current tile (start position)
        return path;
    }
}
