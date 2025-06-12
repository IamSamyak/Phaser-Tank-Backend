package com.phaser.tank.manager;

import com.phaser.tank.model.Player;
import com.phaser.tank.model.Room;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerManager {

    private final List<Player> players = new CopyOnWriteArrayList<>();
    private final Room room;

    public PlayerManager(Room room) {
        this.room = room;
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void removePlayer(WebSocketSession session) {
        players.removeIf(p -> p.getSession().equals(session));
    }

    public void removePlayerById(int playerId) {
        players.removeIf(p -> p.getPlayerId() == playerId);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getPlayerBySession(WebSocketSession session) {
        return players.stream()
                .filter(p -> p.getSession().equals(session))
                .findFirst()
                .orElse(null);
    }

    public Player getPlayerById(int id) {
        return players.stream()
                .filter(p -> p.getPlayerId() == id)
                .findFirst()
                .orElse(null);
    }

    public void damagePlayer(int id, int amount) {
        Player player = getPlayerById(id);
        if (player == null) return;

        player.damage(amount);

        if (player.isDestroyed()) {
            removePlayerById(id);
            room.broadcast(Map.of(
                    "type", "player_destroy",
                    "playerId", player.getPlayerId(),
                    "x", player.getX(),
                    "y", player.getY()
            ));
        }
    }
}
