package com.phaser.tank.manager;

import com.phaser.tank.model.Room;
import com.phaser.tank.model.Player;
import com.phaser.tank.model.Bonus;
import com.phaser.tank.util.TileHelper;
import com.phaser.tank.util.GameConstants;

import java.util.*;
import java.util.concurrent.*;

import static com.phaser.tank.util.GameConstants.TILE_SIZE;

public class BonusManager {

    private final Room room;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Random random = new Random();

    private final Map<String, Bonus> activeBonuses = new ConcurrentHashMap<>();

    public BonusManager(Room room) {
        this.room = room;
    }

    public void spawnBonus() {
        List<int[]> walkableTiles = TileHelper.findWalkableTiles(room.getLevelMap());

        if (walkableTiles.isEmpty()) {
            scheduleNextBonus();
            return;
        }

        int[] pos = walkableTiles.get(random.nextInt(walkableTiles.size()));
        int row = pos[0];
        int col = pos[1];
        String bonusType = GameConstants.BONUS_TYPES.get(random.nextInt(GameConstants.BONUS_TYPES.size()));

        double[] center = TileHelper.tileToPixelCenter(row, col);
        double x = center[0];
        double y = center[1];

        String bonusId = UUID.randomUUID().toString();
        Bonus bonus = new Bonus(bonusId, x, y, bonusType);
        activeBonuses.put(bonusId, bonus);

        room.broadcast(Map.of(
                "type", "bonus_spawn",
                "bonusId", bonus.getId(),
                "x", bonus.getX(),
                "y", bonus.getY(),
                "bonusType", bonus.getType()
        ));

        scheduler.schedule(() -> {
            activeBonuses.remove(bonusId);
            room.broadcast(Map.of(
                    "type", "bonus_remove",
                    "bonusId", bonusId
            ));
            scheduleNextBonus();
        }, 5, TimeUnit.SECONDS);
    }

    public void checkBonusCollision(Player player) {
        double px = player.getX();
        double py = player.getY();

        for (Iterator<Map.Entry<String, Bonus>> it = activeBonuses.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Bonus> entry = it.next();
            Bonus bonus = entry.getValue();

            double dist = Math.hypot(px - bonus.getX(), py - bonus.getY());
            if (dist < TILE_SIZE) {
                it.remove();

                room.broadcast(Map.of(
                        "type", "bonus_collected",
                        "playerNumber", player.getPlayerNumber(),
                        "bonusId", bonus.getId(),
                        "bonusType", bonus.getType()
                ));

                applyBonusEffect(player, bonus.getType());
            }
        }
    }

    private void applyBonusEffect(Player player, String type) {
        System.out.println("tupe " + type);
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
            default:
                System.out.println("Bonus applied: " + type);
        }
    }

    private void scheduleNextBonus() {
        scheduler.schedule(this::spawnBonus, 6000, TimeUnit.MILLISECONDS);
    }
}
