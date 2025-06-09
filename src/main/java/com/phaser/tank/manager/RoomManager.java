package com.phaser.tank.manager;

import com.phaser.tank.model.Room;
import com.phaser.tank.model.Player;
import org.springframework.web.socket.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;

public class RoomManager {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionToRoom = new ConcurrentHashMap<>();

    public String createRoom(WebSocketSession session) {
        String roomId = generateRoomId();
        Room room = new Room(roomId);
        room.addPlayer(new Player(session, 1));

        // Load default level into the room
        List<String> levelMap = loadLevelMap("levels/1.txt");
        room.setLevelMap(levelMap);

        rooms.put(roomId, room);
        sessionToRoom.put(session, roomId);
        return roomId;
    }

    public boolean joinRoom(String roomId, WebSocketSession session) {
        Room room = rooms.get(roomId);
        if (room != null && room.playerCount() < 2) {
            room.addPlayer(new Player(session, 2));
            sessionToRoom.put(session, roomId);
            return true;
        }
        return false;
    }

    public Player getPlayerBySession(WebSocketSession session) {
        String roomId = sessionToRoom.get(session);
        if (roomId == null) return null;

        Room room = rooms.get(roomId);
        if (room == null) return null;

        return room.getPlayers().stream()
                .filter(p -> p.getSession().equals(session))
                .findFirst()
                .orElse(null);
    }

    public Player getPlayer(String roomId, int playerNumber) {
        Room room = rooms.get(roomId);
        if (room == null) return null;
        return room.getPlayers().stream()
                .filter(p -> p.getPlayerNumber() == playerNumber)
                .findFirst()
                .orElse(null);
    }

    public String getRoomIdBySession(WebSocketSession session) {
        return sessionToRoom.get(session);
    }

    public void broadcast(String roomId, TextMessage message, WebSocketSession exclude) {
        Room room = rooms.get(roomId);
        if (room != null) {
            for (Player player : room.getPlayers()) {
                if (!player.getSession().equals(exclude)) {
                    try {
                        if (player.getSession().isOpen()) {
                            player.getSession().sendMessage(message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void notifyRoom(String roomId, WebSocketSession exclude, String infoMessage) {
        broadcast(roomId, new TextMessage("{\"type\":\"info\",\"message\":\"" + infoMessage + "\"}"), exclude);
    }

    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public void removeSession(WebSocketSession session) {
        String roomId = sessionToRoom.remove(session);
        if (roomId != null) {
            Room room = rooms.get(roomId);
            if (room != null) {
                room.removePlayer(session);
                if (room.playerCount() == 0) {
                    // Shutdown room resources like bullet scheduler if needed
                    rooms.remove(roomId);
                }
            }
        }
    }

    private String generateRoomId() {
        return String.valueOf(new Random().nextInt(9000) + 1000);
    }

    private List<String> loadLevelMap(String path) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path))))) {

            List<String> map = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                map.add(line.trim());
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(); // fallback to empty map
        }
    }
}
