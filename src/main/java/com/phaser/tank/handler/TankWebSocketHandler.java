package com.phaser.tank.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaser.tank.model.BulletOrigin;
import com.phaser.tank.model.Direction;
import com.phaser.tank.model.Player;
import com.phaser.tank.model.Room;
import com.phaser.tank.manager.RoomManager;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;

public class TankWebSocketHandler extends TextWebSocketHandler {

    private static final RoomManager roomManager = new RoomManager();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String uri = session.getUri().toString();
        String roomId;

        if (uri.contains("/ws/create")) {
            String level = "1";
            if (uri.contains("?")) {
                String query = uri.substring(uri.indexOf("?") + 1);
                String[] params = query.split("&");
                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && pair[0].equals("level")) {
                        level = pair[1];
                        break;
                    }
                }
            }

            // Create room and get player
            roomId = roomManager.createRoom(session, level);
            Player player = roomManager.getPlayerBySession(session);
            Room room = roomManager.getRoom(roomId);

            // Prepare playerEvents array including the creator himself
            List<Map<String, Object>> playerEvents = new ArrayList<>();
            for (Player p : room.getPlayers()) {
                playerEvents.add(Map.of(
                        "action", "spawn",
                        "playerId", p.getPlayerId(),
                        "x", p.getX(),
                        "y", p.getY(),
                        "direction", p.getDirection()
                ));
            }

            // Send start message with playerEvents to creator
            session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                    "type", "start",
                    "playerId", player.getPlayerId(),
                    "roomId", roomId,
                    "levelMap", room.getLevelMap(),
                    "playerEvents", playerEvents
            ))));

        } else if (uri.contains("/ws/join/")) {
            roomId = uri.substring(uri.lastIndexOf('/') + 1);
            boolean success = roomManager.joinRoom(roomId, session);

            if (success) {
                Player newPlayer = roomManager.getPlayerBySession(session);
                Room room = roomManager.getRoom(roomId);

                List<Map<String, Object>> playerEvents = new ArrayList<>();

                // Build playerEvents for all players (including the new player)
                for (Player p : room.getPlayers()) {
                    playerEvents.add(Map.of(
                            "action", "spawn",
                            "playerId", p.getPlayerId(),
                            "x", p.getX(),
                            "y", p.getY(),
                            "direction", p.getDirection()
                    ));
                }

                // Send to new player
                session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                        "type", "start",
                        "playerId", newPlayer.getPlayerId(),
                        "roomId", roomId,
                        "levelMap", room.getLevelMap(),
                        "playerEvents", playerEvents
                ))));

                // Notify existing players about the new player
                for (Player other : room.getPlayers()) {
                    if (!other.getSession().equals(session)) {
                        other.getSession().sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                                "type", "spawn_new_player",
                                "playerId", newPlayer.getPlayerId(),
                                "x", newPlayer.getX(),
                                "y", newPlayer.getY(),
                                "direction", newPlayer.getDirection()
                        ))));
                    }
                }
            } else {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                        "type", "error",
                        "message", "Room full or not found"
                ))));
                session.close();
            }
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String roomId = roomManager.getRoomIdBySession(session);
        if (roomId != null) {
            Map<String, Object> msgMap = mapper.readValue(message.getPayload(), Map.class);

            Player player = roomManager.getPlayerBySession(session);
            if (player != null) {
                msgMap.put("playerId", player.getPlayerId());
            }

            String type = (String) msgMap.get("type");

            if ("player_move".equals(type)) {
                handlePlayerMove(roomId, player, msgMap);
                return;
            } else if ("fire_bullet".equals(type)) {
                Room room = roomManager.getRoom(roomId);
                if (room != null && player != null) {
                    room.addBullet(player.getX(), player.getY(), player.getDirection(), BulletOrigin.PLAYER);
                }
            }

            // Broadcast to others
            String broadcastMsg = mapper.writeValueAsString(msgMap);
            roomManager.broadcast(roomId, new TextMessage(broadcastMsg), session);
        }
    }

    private Direction getDirectionFromString(String directionStr) {
        try {
            return Direction.valueOf(directionStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid direction: " + directionStr);
        }
    }

    private void handlePlayerMove(String roomId, Player player, Map<String, Object> msgMap) {
        if (player == null) return;

        Direction direction = getDirectionFromString((String) msgMap.get("direction"));
        Room room = roomManager.getRoom(roomId);
        if (room != null) {
            room.handlePlayerMove(player.getSession(), direction);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        roomManager.removeSession(session);
    }
}
