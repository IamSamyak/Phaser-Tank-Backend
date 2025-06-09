package com.phaser.tank.manager;

import com.phaser.tank.model.Player;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerManager {

    private final List<Player> players = new CopyOnWriteArrayList<>();

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void removePlayer(WebSocketSession session) {
        players.removeIf(p -> p.getSession().equals(session));
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
}
