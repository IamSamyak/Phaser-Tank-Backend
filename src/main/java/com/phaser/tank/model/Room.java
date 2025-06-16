package com.phaser.tank.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaser.tank.manager.BonusManager;
import com.phaser.tank.manager.BulletManager;
import com.phaser.tank.manager.EnemyManager;
import com.phaser.tank.manager.PlayerManager;
import com.phaser.tank.util.EnemyMovementHelper;
import com.phaser.tank.util.MovementValidator;
import com.phaser.tank.util.TileHelper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Room {

    private final String roomId;
    private List<String> levelMap;
    private boolean spawningStarted = false;

    private final PlayerManager playerManager;
    private final BulletManager bulletManager;
    private final BonusManager bonusManager;
    private final EnemyManager enemyManager;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService tickExecutor = Executors.newSingleThreadScheduledExecutor();

    // Queues for batching
    private final List<Map<String, Object>> bulletQueue = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> bonusQueue = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> tileQueue = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> explosionQueue = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> enemyQueue = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> playerQueue = Collections.synchronizedList(new ArrayList<>());

    public Room(String roomId) {
        this.roomId = roomId;
        this.playerManager = new PlayerManager(this);
        this.bulletManager = new BulletManager(this);
        this.bonusManager = new BonusManager(this);
        this.enemyManager = new EnemyManager(this, this.bulletManager);

        startGameTickLoop();
    }

    public void addPlayer(Player player) {
        playerManager.addPlayer(player);

        queuePlayerEvent(Map.of(
                "action", "spawn",
                "playerId", player.getPlayerId(),
                "x", player.getX(),
                "y", player.getY(),
                "direction", player.getDirection()
        ));

        if (playerManager.getPlayerCount() == 2 && !spawningStarted) {
            bonusManager.spawnBonus();
            enemyManager.startSpawning();
            spawningStarted = true;
        }
    }

    public void removePlayer(WebSocketSession session) {
        playerManager.removePlayer(session);
        if (playerManager.getPlayerCount() == 0) {
            enemyManager.shutdown();
        }
    }

    public int playerCount() {
        return playerManager.getPlayerCount();
    }

    public List<Player> getPlayers() {
        return playerManager.getPlayers();
    }

    public Map<Integer, Player> getPlayerMap() {
        return getPlayers().stream().collect(Collectors.toMap(Player::getId, p -> p));
    }

    public List<Enemy> getEnemies() {
        return new ArrayList<>(enemyManager.getEnemies().values());
    }

    public void damagePlayer(Player player) {
        playerManager.damagePlayer(player.getId(), 1);
    }

    public void damageEnemy(Enemy enemy) {
        enemyManager.damageEnemy(enemy.getId(), 1);
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

    public char getTile(int x, int y) {
        return TileHelper.getTile(x, y, levelMap);
    }

    public void updateTile(int x, int y, char newChar) {
        TileHelper.updateTile(x, y, newChar, levelMap);
    }

    public void addBullet(int x, int y, Direction direction, BulletOrigin origin) {
        bulletManager.addBullet(x, y, direction, origin);
    }

    public void removeBullet(String bulletId) {
        bulletManager.removeBullet(bulletId);
    }

    public void handlePlayerMove(WebSocketSession session, Direction direction) {
        Player player = playerManager.getPlayerBySession(session);
        if (player == null || player.getHealth() <= 0) return;

        int[] next = EnemyMovementHelper.getNextPosition(player.getX(), player.getY(), direction);
        int newX = next[0];
        int newY = next[1];
        player.setDirection(direction);
        // Ensure the tile is walkable
        if (!MovementValidator.canMove(newX, newY, levelMap)) return;

        // Prevent collision with enemies and other players
        boolean canOccupy = MovementValidator.canOccupy(
                newX, newY,
                null, // playerId is handled separately
                player.getPlayerId(),
                enemyManager.getEnemies(),
                getPlayerMap(),
                Set.of() // reservedTiles not used for player input
        );

        if (!canOccupy) return;

        // Apply movement
        player.setX(newX);
        player.setY(newY);

        bonusManager.checkBonusCollision(player);

        queuePlayerEvent(Map.of(
                "action", "move",
                "playerId", player.getPlayerId(),
                "x", player.getX(),
                "y", player.getY(),
                "direction", player.getDirection()
        ));
    }

    // ===== Queue Methods =====
    public void queueBulletEvent(Map<String, Object> bulletEvent) {
        bulletQueue.add(bulletEvent);
    }

    public void queueBonusEvent(Map<String, Object> bonusEvent) {
        bonusQueue.add(bonusEvent);
    }

    public void queueTileUpdate(Map<String, Object> tileUpdate) {
        tileQueue.add(tileUpdate);
    }

    public void queueExplosion(Map<String, Object> explosion) {
        explosionQueue.add(explosion);
    }

    public void queueEnemyEvent(Map<String, Object> event) {
        enemyQueue.add(event);
    }

    public void queuePlayerEvent(Map<String, Object> event) {
        playerQueue.add(event);
    }

    // ===== Tick Loop =====
    private void startGameTickLoop() {
        tickExecutor.scheduleAtFixedRate(this::flushGameTick, 0, 33, TimeUnit.MILLISECONDS); // ~30fps
    }

    private void flushGameTick() {
        Map<String, Object> tick = new HashMap<>();
        tick.put("type", "game_tick");

        flushQueue(tick, "bullets", bulletQueue);
        flushQueue(tick, "bonuses", bonusQueue);
        flushQueue(tick, "tiles", tileQueue);
        flushQueue(tick, "explosions", explosionQueue);
        flushQueue(tick, "enemyEvents", enemyQueue);
        flushQueue(tick, "playerEvents", playerQueue);

        if (tick.size() > 1) {
            broadcast(tick);
        }
    }

    private void flushQueue(Map<String, Object> tick, String key, List<Map<String, Object>> queue) {
        synchronized (queue) {
            if (!queue.isEmpty()) {
                tick.put(key, new ArrayList<>(queue));
                queue.clear();
            }
        }
    }

    // ===== WebSocket Broadcast =====
    public synchronized void broadcast(Map<String, Object> msg) {
        try {
            String json = mapper.writeValueAsString(msg);
            for (Player player : playerManager.getPlayers()) {
                WebSocketSession session = player.getSession();
                if (session.isOpen()) {
                    synchronized (session) { // ✅ Ensure only one message is sent at a time per session
                        session.sendMessage(new TextMessage(json));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
