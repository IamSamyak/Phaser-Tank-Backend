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
        int angle;
        double speed = 1;
        boolean destroyed = false;

        double dx;
        double dy;

        int tickCount = 0; // track number of moves
        static final int MAX_TICKS = 27; // limit to avoid infinite travel

        Bullet(String id, double x, double y, int angle) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.angle = angle;
            switch (angle) {
                case 0 -> { dx = 0; dy = -TILE_SIZE; }
                case 90 -> { dx = TILE_SIZE; dy = 0; }
                case 180 -> { dx = 0; dy = TILE_SIZE; }
                case 270 -> { dx = -TILE_SIZE; dy = 0; }
                default -> { dx = 0; dy = -TILE_SIZE; }
            }
        }

        void move() {
            x += dx * speed;
            y += dy * speed;
            tickCount++;
            if (tickCount > MAX_TICKS) {
                destroyed = true;
            }
        }
    }

    private final Map<String, Bullet> activeBullets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final ObjectMapper mapper = new ObjectMapper();

    private static final int TILE_SIZE = 32;

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

    // Check collision with blocking tiles for bullet
    private boolean isCollision(double x, double y) {
        int tileX = (int) (x / TILE_SIZE);
        int tileY = (int) (y / TILE_SIZE);

        char tile = getTile(tileX, tileY);
        return tile == '#' || tile == '@'; // example: brick or stone blocks bullet
    }

    // Boundary check
    private boolean isOutOfBounds(double x, double y) {
        if (levelMap == null) return true;
        return x < 0 || y < 0 || y >= levelMap.size() * TILE_SIZE || x >= levelMap.get(0).length() * TILE_SIZE;
    }

    // --- Movement logic moved to backend ---

    private static final int TANK_SIZE = TILE_SIZE; // Assuming tank occupies 1 tile square

    // Check if tank can move to x,y position (in pixels)
    public boolean canMove(double x, double y) {
        // Check all 4 corners of tank
        int half = TANK_SIZE / 2;

        // corners in tile coords
        int topLeftRow = (int) Math.floor((y - half) / TILE_SIZE);
        int topLeftCol = (int) Math.floor((x - half) / TILE_SIZE);

        int topRightRow = topLeftRow;
        int topRightCol = (int) Math.floor((x + half - 1) / TILE_SIZE);

        int bottomLeftRow = (int) Math.floor((y + half - 1) / TILE_SIZE);
        int bottomLeftCol = topLeftCol;

        int bottomRightRow = bottomLeftRow;
        int bottomRightCol = topRightCol;

        return isWalkable(topLeftRow, topLeftCol) &&
                isWalkable(topRightRow, topRightCol) &&
                isWalkable(bottomLeftRow, bottomLeftCol) &&
                isWalkable(bottomRightRow, bottomRightCol);
    }

    private boolean isWalkable(int row, int col) {
        if (!isWithinMapBounds(row, col)) return false;
        char tileChar = getTile(col, row);
        String type = tileMapping(tileChar);
        return "empty".equals(type) || "bush".equals(type);
    }

    private boolean isWithinMapBounds(int row, int col) {
        return levelMap != null && row >= 0 && row < levelMap.size()
                && col >= 0 && col < levelMap.get(0).length();
    }

    private String tileMapping(char tileChar) {
        return switch (tileChar) {
            case '.' -> "empty";
            case '#' -> "brick";
            case '@' -> "stone";
            case '%' -> "bush";
            case '~' -> "water";
            case '-' -> "ice";
            default -> "unknown";
        };
    }

    // Handle move command from client
    public void handlePlayerMove(WebSocketSession session, double newX, double newY, int newAngle) {
        // Find player
        PlayerInfo player = players.stream()
                .filter(p -> p.getSession().equals(session))
                .findFirst()
                .orElse(null);

        if (player == null) return;

        player.setAngle(newAngle);
        if (canMove(newX, newY)) {
            // Valid move: update position and angle
            player.setX(newX);
            player.setY(newY);
        }   // Broadcast to all players
        broadcast(Map.of(
                "type", "player_move",
                "playerNumber", player.getPlayerNumber(),
                "x", player.getX(),
                "y", player.getY(),
                "angle", player.getAngle()
        ));
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
