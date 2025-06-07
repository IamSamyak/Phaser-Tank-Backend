package com.phaser.tank;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.*;

public class Room {

    private final String roomId;
    private final List<PlayerInfo> players = new CopyOnWriteArrayList<>();
    private List<String> levelMap;

    // Bullet class to store bullet state
    private static class Bullet {
        String id;
        double x;
        double y;
        int angle; // 0, 90, 180, 270 degrees
        double speed = 5; // tiles per tick (adjust as needed)
        boolean destroyed = false;

        // direction vector
        double dx;
        double dy;

        Bullet(String id, double x, double y, int angle) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.angle = angle;
            switch (angle) {
                case 0 -> { dx = 0; dy = -1; }
                case 90 -> { dx = 1; dy = 0; }
                case 180 -> { dx = 0; dy = 1; }
                case 270 -> { dx = -1; dy = 0; }
                default -> { dx = 0; dy = -1; }
            }
        }

        // Move bullet position by speed in direction
        void move() {
            x += dx * speed;
            y += dy * speed;
        }
    }

    private final Map<String, Bullet> activeBullets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final ObjectMapper mapper = new ObjectMapper();

    public Room(String roomId) {
        this.roomId = roomId;

        // Start bullet movement task at 20 ticks per second (50ms)
        scheduler.scheduleAtFixedRate(this::updateBullets, 50, 50, TimeUnit.MILLISECONDS);
    }

    public void addPlayer(PlayerInfo player) {
        players.add(player);
    }

    public void removePlayer(WebSocketSession session) {
        players.removeIf(p -> p.getSession().equals(session));
    }

    public int playerCount() {
        return players.size();
    }

    public List<PlayerInfo> getPlayers() {
        return players;
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

    // ********* BULLET HANDLING *********

    public void addBullet(String bulletId, double x, double y, int angle) {
        Bullet bullet = new Bullet(bulletId, x, y, angle);
        activeBullets.put(bulletId, bullet);
    }

    public void removeBullet(String bulletId) {
        activeBullets.remove(bulletId);
        broadcast(Map.of(
                "type", "bullet_destroy",
                "bulletId", bulletId
        ));
    }

    // Periodic update to move bullets and broadcast position
    private void updateBullets() {
        for (Iterator<Map.Entry<String, Bullet>> it = activeBullets.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Bullet> entry = it.next();
            Bullet bullet = entry.getValue();

            bullet.move();

            // Check collisions or out of bounds here (simple example):
            if (isCollision(bullet.x, bullet.y) || isOutOfBounds(bullet.x, bullet.y)) {
                bullet.destroyed = true;
            }

            if (bullet.destroyed) {
                it.remove();
                broadcast(Map.of(
                        "type", "bullet_destroy",
                        "bulletId", bullet.id
                ));
            } else {
                broadcast(Map.of(
                        "type", "bullet_move",
                        "bulletId", bullet.id,
                        "x", bullet.x,
                        "y", bullet.y
                ));
            }
        }
    }

    // Simple collision check (can be enhanced)
    private boolean isCollision(double x, double y) {
        int tileX = (int) Math.floor(x);
        int tileY = (int) Math.floor(y);

        char tile = getTile(tileX, tileY);
        return tile == '#' || tile == '@'; // example: brick or stone blocks bullet
    }

    // Simple boundary check
    private boolean isOutOfBounds(double x, double y) {
        if (levelMap == null) return true;
        return x < 0 || y < 0 || y >= levelMap.size() || x >= levelMap.get(0).length();
    }

    // Send message to all players except optional excludeSession (nullable)
    private void broadcast(Map<String, Object> msg) {
        try {
            String json = mapper.writeValueAsString(msg);
            for (PlayerInfo player : players) {
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
