package com.phaser.tank;

import com.phaser.tank.util.TileHelper;

import java.util.*;
import java.util.concurrent.*;

public class BonusManager {

    private final Room room;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Random random = new Random();

    private final List<String> bonusTypes = List.of(
            "helmet", "boat", "gun", "grenade", "star", "shovel", "clock", "tank"
    );

    // Inner class to represent a bonus
    private static class Bonus {
        String id;
        double x, y;
        String type;

        Bonus(String id, double x, double y, String type) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    // Store active bonuses
    private final Map<String, Bonus> activeBonuses = new ConcurrentHashMap<>();

    public BonusManager(Room room) {
        this.room = room;
    }

    public void spawnBonus() {
        List<int[]> walkableTiles = findWalkableTiles();

        if (walkableTiles.isEmpty()) {
            scheduleNextBonus();
            return;
        }

        int[] pos = walkableTiles.get(random.nextInt(walkableTiles.size()));
        int row = pos[0];
        int col = pos[1];
        String bonusType = bonusTypes.get(random.nextInt(bonusTypes.size()));

        double x = (col + 0.5) * TileHelper.TILE_SIZE;
        double y = (row + 0.5) * TileHelper.TILE_SIZE;

        String bonusId = UUID.randomUUID().toString();
        Bonus bonus = new Bonus(bonusId, x, y, bonusType);
        activeBonuses.put(bonusId, bonus);

        // Broadcast bonus spawn
        room.broadcast(Map.of(
                "type", "bonus_spawn",
                "bonusId", bonusId,
                "x", x,
                "y", y,
                "bonusType", bonusType
        ));

        // Remove after 5 seconds
        scheduler.schedule(() -> {
            activeBonuses.remove(bonusId);
            room.broadcast(Map.of(
                    "type", "bonus_remove",
                    "bonusId", bonusId
            ));
            scheduleNextBonus();
        }, 5, TimeUnit.SECONDS);
    }

    public void checkBonusCollision(PlayerInfo player) {
        double px = player.getX();
        double py = player.getY();

        for (Iterator<Map.Entry<String, Bonus>> it = activeBonuses.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Bonus> entry = it.next();
            Bonus bonus = entry.getValue();

            double dist = Math.hypot(px - bonus.x, py - bonus.y);
            if (dist < TileHelper.TILE_SIZE) {
                it.remove(); // Remove collected bonus

                room.broadcast(Map.of(
                        "type", "bonus_collected",
                        "playerNumber", player.getPlayerNumber(),
                        "bonusId", bonus.id,
                        "bonusType", bonus.type
                ));

                applyBonusEffect(player, bonus.type);
            }
        }
    }

    private void applyBonusEffect(PlayerInfo player, String type) {
        System.out.println("tupe "+type);
        switch (type) {
            case "helmet":
                player.setHealth(player.getHealth() + 1);
                break;
            case "gun":
                player.setMaxBullets(player.getMaxBullets() + 1);
                break;
            case "tank":
                player.setHealth(player.getHealth() + 1);
                player.setMaxBullets(player.getMaxBullets() + 1);
                break;
            // Extend other effects as needed
            default:
                System.out.println("Bonus applied: " + type);
        }
    }

    private List<int[]> findWalkableTiles() {
        List<int[]> walkables = new ArrayList<>();
        List<String> levelMap = room.getLevelMap();
        if (levelMap == null) return walkables;

        for (int row = 0; row < levelMap.size(); row++) {
            String line = levelMap.get(row);
            for (int col = 0; col < line.length(); col++) {
                char tile = line.charAt(col);
                if (TileHelper.isWalkable(tile)) {
                    walkables.add(new int[]{row, col});
                }
            }
        }
        return walkables;
    }

    private void scheduleNextBonus() {
        scheduler.schedule(this::spawnBonus, 6000, TimeUnit.MILLISECONDS);
    }
}
