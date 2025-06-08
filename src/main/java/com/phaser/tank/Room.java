package com.phaser.tank;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.phaser.tank.util.MovementValidator;

import java.util.*;

public class Room {

    private final String roomId;
    private final PlayerManager playerManager = new PlayerManager();
    private List<String> levelMap;

    private final BulletManager bulletManager;
    private final BonusManager bonusManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public Room(String roomId) {
        this.roomId = roomId;
        this.bulletManager = new BulletManager(this);
        this.bonusManager = new BonusManager(this);
    }

    public void addPlayer(PlayerInfo player) {
        playerManager.addPlayer(player);

        if (playerManager.getPlayerCount() == 2) {
            bonusManager.spawnBonus();
        }
    }

    public void removePlayer(WebSocketSession session) {
        playerManager.removePlayer(session);
    }

    public int playerCount() {
        return playerManager.getPlayerCount();
    }

    public List<PlayerInfo> getPlayers() {
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
        if (levelMap == null || y < 0 || y >= levelMap.size()) return;

        String row = levelMap.get(y);
        if (x < 0 || x >= row.length()) return;

        char[] chars = row.toCharArray();
        chars[x] = newChar;
        levelMap.set(y, new String(chars));
    }

    public char getTile(int x, int y) {
        if (levelMap == null || y < 0 || y >= levelMap.size()) return '?';
        String row = levelMap.get(y);
        if (x < 0 || x >= row.length()) return '?';
        return row.charAt(x);
    }

    public void addBullet(String bulletId, double x, double y, int angle) {
        bulletManager.addBullet(bulletId, x, y, angle);
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

    public void handlePlayerMove(WebSocketSession session, double newX, double newY, int newAngle) {
        PlayerInfo player = playerManager.getPlayerBySession(session);
        if (player == null) return;

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
            for (PlayerInfo player : playerManager.getPlayers()) {
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
