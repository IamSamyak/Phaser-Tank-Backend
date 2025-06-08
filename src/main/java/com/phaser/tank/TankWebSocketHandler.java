package com.phaser.tank;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;

public class TankWebSocketHandler extends TextWebSocketHandler {

    private static final RoomManager roomManager = new RoomManager();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String uri = session.getUri().toString();
        String roomId;

        if (uri.contains("/ws/create")) {
            roomId = roomManager.createRoom(session);

            Room room = roomManager.getRoom(roomId);
            List<String> levelMap = room.getLevelMap();

            session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                    "type", "start",
                    "playerNumber", 1,
                    "roomId", roomId,
                    "x", 10,
                    "y", 25,
                    "levelMap", levelMap
            ))));
        } else if (uri.contains("/ws/join/")) {
            roomId = uri.substring(uri.lastIndexOf('/') + 1);
            boolean success = roomManager.joinRoom(roomId, session);

            if (success) {
                Room room = roomManager.getRoom(roomId);
                List<String> levelMap = room.getLevelMap();

                session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                        "type", "start",
                        "playerNumber", 2,
                        "roomId", roomId,
                        "x", 16,
                        "y", 25,
                        "levelMap", levelMap
                ))));

                // Notify player 1 about player 2 joining
                PlayerInfo p1 = roomManager.getPlayer(roomId, 1);
                if (p1 != null) {
                    p1.getSession().sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                            "type", "spawn_other",
                            "x", 16,
                            "y", 25,
                            "playerNumber", 2
                    ))));
                }

                // Notify player 2 about player 1 position
                PlayerInfo p2 = roomManager.getPlayer(roomId, 2);
                if (p2 != null) {
                    p2.getSession().sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                            "type", "spawn_other",
                            "x", 10,
                            "y", 25,
                            "playerNumber", 1
                    ))));
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

            PlayerInfo player = roomManager.getPlayerBySession(session);
            if (player != null) {
                msgMap.put("playerNumber", player.getPlayerNumber());
            }

            String type = (String) msgMap.get("type");

            if ("player_move".equals(type)) {
                handlePlayerMove(roomId, player, msgMap);
            } else if ("fire_bullet".equals(type)) {
                String bulletId = (String) msgMap.get("bulletId");
                double x = ((Number) msgMap.get("x")).doubleValue();
                double y = ((Number) msgMap.get("y")).doubleValue();
                int angle = ((Number) msgMap.get("angle")).intValue();

                Room room = roomManager.getRoom(roomId);
                if (room != null) {
                    room.addBullet(bulletId, x, y, angle);
                }
            }

            // Broadcast to other players
            String broadcastMsg = mapper.writeValueAsString(msgMap);
            roomManager.broadcast(roomId, new TextMessage(broadcastMsg), session);
        }
    }

    private void handlePlayerMove(String roomId, PlayerInfo player, Map<String, Object> msgMap) {
        if (player == null) return;

        double x = ((Number) msgMap.get("x")).doubleValue();
        double y = ((Number) msgMap.get("y")).doubleValue();
        int direction = ((Number) msgMap.get("direction")).intValue();

        Room room = roomManager.getRoom(roomId);
        if (room != null) {
            // Correct method call with session, not player number
            room.handlePlayerMove(player.getSession(), x, y, direction);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        roomManager.removeSession(session);
    }
}
