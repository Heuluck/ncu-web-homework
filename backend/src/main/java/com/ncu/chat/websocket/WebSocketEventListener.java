package com.ncu.chat.websocket;

import com.ncu.chat.mapper.UserMapper;
import com.ncu.chat.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;
    private final UserMapper userMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnect(SessionConnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal user = headers.getUser();
        if (user == null) return;

        Long userId = Long.valueOf(user.getName());
        String sessionId = headers.getSessionId();

        sessionManager.addSession(userId, sessionId);

        // 物理上线 → 状态设为在线
        User u = new User();
        u.setId(userId);
        u.setStatus(1);
        userMapper.updateById(u);

        // 广播上线通知
        Map<String, Object> statusMsg = new HashMap<>();
        statusMsg.put("type", "STATUS_CHANGE");
        statusMsg.put("userId", userId);
        statusMsg.put("status", 1);
        statusMsg.put("timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend("/topic/status", statusMsg);

        log.info("用户上线: userId={}", userId);
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal user = headers.getUser();
        if (user == null) return;

        Long userId = Long.valueOf(user.getName());
        String sessionId = headers.getSessionId();

        sessionManager.removeSession(userId, sessionId);

        // 只有所有终端都断开才更新为离线
        if (!sessionManager.isOnline(userId)) {
            User u = new User();
            u.setId(userId);
            u.setStatus(0);
            userMapper.updateById(u);

            // 广播下线通知
            Map<String, Object> statusMsg = new HashMap<>();
            statusMsg.put("type", "STATUS_CHANGE");
            statusMsg.put("userId", userId);
            statusMsg.put("status", 0);
            statusMsg.put("timestamp", LocalDateTime.now().toString());
            messagingTemplate.convertAndSend("/topic/status", statusMsg);

            log.info("用户下线: userId={}", userId);
        }
    }
}
