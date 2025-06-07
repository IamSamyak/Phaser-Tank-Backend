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

            Room room = roomManager.getRoom(roomId); // get room object
            List<String> levelMap = room.getLevelMap(); // get level map from room

            session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                    "type", "start",
                    "playerNumber", 1,
                    "roomId", roomId,
                    "x", 24,
                    "y", 9,
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
                        "x", 24,
                        "y", 15,
                        "levelMap", levelMap
                ))));

                // Notify player 1 about player 2 joining
                PlayerInfo p1 = roomManager.getPlayer(roomId, 1);
                if (p1 != null) {
                    p1.getSession().sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                            "type", "spawn_other",
                            "x", 24,
                            "y", 15,
                            "playerNumber", 2
                    ))));
                }

                // Notify player 2 about player 1 position
                PlayerInfo p2 = roomManager.getPlayer(roomId, 2);
                if (p2 != null) {
                    p2.getSession().sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
                            "type", "spawn_other",
                            "x", 24,
                            "y", 9,
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

            String broadcastMsg = mapper.writeValueAsString(msgMap);
            roomManager.broadcast(roomId, new TextMessage(broadcastMsg), session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        roomManager.removeSession(session);
    }
}
