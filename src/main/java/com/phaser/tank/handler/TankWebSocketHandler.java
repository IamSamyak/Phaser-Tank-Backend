package com.phaser.tank.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phaser.tank.model.Player;
import com.phaser.tank.model.Room;
import com.phaser.tank.manager.RoomManager;
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
                Player p1 = roomManager.getPlayer(roomId, 1);
                if (p1 != null) {
                    p1.getSession().sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                            "type", "spawn_other",
                            "x", 16,
                            "y", 25,
                            "playerNumber", 2
                    ))));
                }

                // Notify player 2 about player 1 position
                Player p2 = roomManager.getPlayer(roomId, 2);
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

            Player player = roomManager.getPlayerBySession(session);
            if (player != null) {
                msgMap.put("playerNumber", player.getPlayerNumber());
            }

            String type = (String) msgMap.get("type");

            if ("player_move".equals(type)) {
                handlePlayerMove(roomId, player, msgMap);
                return;
            } else if ("fire_bullet".equals(type)) {
                String bulletId = (String) msgMap.get("bulletId");
                int x = ((Number) msgMap.get("x")).intValue();
                int y = ((Number) msgMap.get("y")).intValue();
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

    private void handlePlayerMove(String roomId, Player player, Map<String, Object> msgMap) {
        if (player == null) return;

        int x = ((Number) msgMap.get("x")).intValue();
        int y = ((Number) msgMap.get("y")).intValue();
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
