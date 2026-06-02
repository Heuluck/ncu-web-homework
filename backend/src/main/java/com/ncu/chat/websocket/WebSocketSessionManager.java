package com.ncu.chat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器
 * 维护 userId → sessionId 集合的映射，支持同一用户多端在线
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    private final ConcurrentHashMap<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(Long userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.info("WebSocket 会话添加: userId={}, sessionId={}, 在线终端数={}", userId, sessionId, getSessionCount(userId));
    }

    /**
     * 移除会话
     */
    public void removeSession(Long userId, String sessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
            log.info("WebSocket 会话移除: userId={}, sessionId={}, 剩余终端数={}", userId, sessionId, getSessionCount(userId));
        }
    }

    /**
     * 判断用户是否在线
     */
    public boolean isOnline(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * 获取用户在线终端数
     */
    public int getSessionCount(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions == null ? 0 : sessions.size();
    }

    /**
     * 获取所有在线用户 ID
     */
    public Set<Long> getOnlineUsers() {
        return userSessions.keySet();
    }

    /**
     * 根据 sessionId 查找 userId
     */
    public Long getUserIdBySessionId(String sessionId) {
        for (var entry : userSessions.entrySet()) {
            if (entry.getValue().contains(sessionId)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
