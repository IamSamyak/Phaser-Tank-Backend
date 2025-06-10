package com.phaser.tank.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaser.tank.manager.BonusManager;
import com.phaser.tank.manager.BulletManager;
import com.phaser.tank.manager.EnemyManager;
import com.phaser.tank.manager.PlayerManager;
import com.phaser.tank.util.TileHelper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.phaser.tank.util.MovementValidator;

import java.util.*;

public class Room {

    private final String roomId;
    private final PlayerManager playerManager = new PlayerManager();
    private List<String> levelMap;
    private boolean spawningStarted = false;

    private final BulletManager bulletManager;
    private final BonusManager bonusManager;
    private final EnemyManager enemyManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public Room(String roomId) {
        this.roomId = roomId;
        this.bulletManager = new BulletManager(this);
        this.bonusManager = new BonusManager(this);
        this.enemyManager = new EnemyManager(this, this.bulletManager);
    }

    public void addPlayer(Player player) {
        playerManager.addPlayer(player);

        if (playerManager.getPlayerCount() == 2) {
            if (!spawningStarted) {
                bonusManager.spawnBonus();
                enemyManager.startSpawning();
                spawningStarted = true;
            }
        }
    }

    public void removePlayer(WebSocketSession session) {
        playerManager.removePlayer(session);
        if (playerManager.getPlayerCount() == 0) {
            enemyManager.shutdown(); // stop enemy spawning
        }
    }

    public List<Enemy> getEnemies() {
        return new ArrayList<>(enemyManager.getEnemies().values());
    }

    public void removeEnemy(Enemy enemy) {
        enemyManager.getEnemies().remove(enemy.getId());
    }


    public int playerCount() {
        return playerManager.getPlayerCount();
    }

    public List<Player> getPlayers() {
        return playerManager.getPlayers();
    }

    public String getRoomId() {
        return roomId;
    }

    public void setLevelMap(List<String> levelMap) {
        this.levelMap = levelMap;
    }

    public List<String> getLevelMap() {
        return levelMap;
    }

    public void updateTile(int x, int y, char newChar) {
        TileHelper.updateTile(x, y, newChar, levelMap);
    }

    public char getTile(int x, int y) {
        return TileHelper.getTile(x, y, levelMap);
    }

    public void addBullet(String bulletId, int x, int y, int angle, BulletOrigin origin) {
        bulletManager.addBullet(bulletId, x, y, angle, origin);
    }

    public void removeBullet(String bulletId) {
        bulletManager.removeBullet(bulletId);
    }

    public boolean isOutOfBounds(double x, double y) {
        return MovementValidator.isOutOfBounds(x, y, levelMap);
    }

    public boolean isWithinMapBounds(int row, int col) {
        return MovementValidator.isWithinMapBounds(row, col, levelMap);
    }

    public void handlePlayerMove(WebSocketSession session, int newX, int newY, int newAngle) {
        Player player = playerManager.getPlayerBySession(session);
        if (player == null || player.getHealth() <= 0) return;

        player.setAngle(newAngle);
        if (MovementValidator.canMove(newX, newY, levelMap)) {
            player.setX(newX);
            player.setY(newY);
        }

        // Check for bonus collision after position update
        bonusManager.checkBonusCollision(player);

        broadcast(Map.of(
                "type", "player_move",
                "playerNumber", player.getPlayerNumber(),
                "x", player.getX(),
                "y", player.getY(),
                "angle", player.getAngle()
        ));
    }

    public void broadcast(Map<String, Object> msg) {
        try {
            String json = mapper.writeValueAsString(msg);
            for (Player player : playerManager.getPlayers()) {
                WebSocketSession session = player.getSession();
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
