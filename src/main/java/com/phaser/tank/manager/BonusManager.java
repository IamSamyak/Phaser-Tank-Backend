package com.phaser.tank.manager;

import com.phaser.tank.model.Room;
import com.phaser.tank.model.Player;
import com.phaser.tank.model.Bonus;
import com.phaser.tank.util.GameConstants;

import java.util.*;
import java.util.concurrent.*;

public class BonusManager {

    private final Room room;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Random random = new Random();

    private final Map<String, Bonus> activeBonuses = new ConcurrentHashMap<>();

    public BonusManager(Room room) {
        this.room = room;
    }

    public void spawnBonus() {
        int x = random.nextInt(26); // 0 to 25
        int y = random.nextInt(26); // 0 to 25

        String bonusType = GameConstants.BONUS_TYPES.get(random.nextInt(GameConstants.BONUS_TYPES.size()));

        String bonusId = UUID.randomUUID().toString();
        Bonus bonus = new Bonus(bonusId, x, y, bonusType);
        activeBonuses.put(bonusId, bonus);

        // Queue spawn event instead of broadcasting directly
        room.queueBonusEvent(Map.of(
                "event", "spawn",
                "bonusId", bonus.getId(),
                "x", bonus.getX(),
                "y", bonus.getY(),
                "bonusType", bonus.getType()
        ));

        scheduler.schedule(() -> {
            activeBonuses.remove(bonusId);

            // Queue remove event
            room.queueBonusEvent(Map.of(
                    "event", "remove",
                    "bonusId", bonusId
            ));

            scheduleNextBonus();
        }, 5, TimeUnit.SECONDS);
    }

    public void checkBonusCollision(Player player) {
        int px = (int) player.getX(); // assume already tile coordinates
        int py = (int) player.getY();

        for (Iterator<Map.Entry<String, Bonus>> it = activeBonuses.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Bonus> entry = it.next();
            Bonus bonus = entry.getValue();

            int bx = (int) bonus.getX();
            int by = (int) bonus.getY();

            if (px == bx && py == by) {
                it.remove();

                // Queue collect event
                room.queueBonusEvent(Map.of(
                        "event", "collected",
                        "playerId", player.getPlayerId(),
                        "bonusId", bonus.getId(),
                        "bonusType", bonus.getType()
                ));

                applyBonusEffect(player, bonus.getType());
            }
        }
    }

    private void applyBonusEffect(Player player, String type) {
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
