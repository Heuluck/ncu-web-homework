package com.ncu.chat.websocket;

import com.ncu.chat.mapper.FriendshipMapper;
import com.ncu.chat.mapper.PrivateMessageMapper;
import com.ncu.chat.mapper.UserMapper;
import com.ncu.chat.model.entity.Friendship;
import com.ncu.chat.model.entity.PrivateMessage;
import com.ncu.chat.model.entity.User;
import com.ncu.chat.model.vo.PrivateMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PrivateMessageMapper privateMessageMapper;
    private final UserMapper userMapper;
    private final FriendshipMapper friendshipMapper;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          PrivateMessageMapper privateMessageMapper,
                          UserMapper userMapper,
                          FriendshipMapper friendshipMapper) {
        this.messagingTemplate = messagingTemplate;
        this.privateMessageMapper = privateMessageMapper;
        this.userMapper = userMapper;
        this.friendshipMapper = friendshipMapper;
    }

    /**
     * 处理客户端发送的私聊消息
     * 客户端发送到: /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void sendPrivateMessage(ChatMessage message, Principal principal) {
        // 从 WebSocket session 中获取发送者 userId
        Long senderId = Long.valueOf(principal.getName());

        // 检查好友关系及拉黑状态
        Friendship friendship = friendshipMapper.findByUserPair(senderId, message.getTo());
        if (friendship == null || friendship.getStatus() != 1) {
            log.warn("消息发送被拒绝: sender={} receiver={} status={}", senderId, message.getTo(),
                    friendship != null ? friendship.getStatus() : "null");
            return;
        }
        boolean blocked;
        if (friendship.getRequesterId().equals(senderId)) {
            blocked = (friendship.getRequesterBlocked() != null && friendship.getRequesterBlocked() == 1)
                   || (friendship.getReceiverBlocked() != null && friendship.getReceiverBlocked() == 1);
        } else {
            blocked = (friendship.getReceiverBlocked() != null && friendship.getReceiverBlocked() == 1)
                   || (friendship.getRequesterBlocked() != null && friendship.getRequesterBlocked() == 1);
        }
        if (blocked) {
            log.warn("消息被拉黑拦截: sender={} receiver={}", senderId, message.getTo());
            return;
        }

        // 持久化消息
        PrivateMessage pm = new PrivateMessage();
        pm.setSenderId(senderId);
        pm.setReceiverId(message.getTo());
        pm.setContent(message.getContent());
        pm.setMessageType(message.getMessageType() != null ? message.getMessageType() : 0);
        pm.setFileUrl(message.getFileUrl());
        pm.setStatus(0);
        privateMessageMapper.insert(pm);

        // 查询发送者信息
        User sender = userMapper.selectById(senderId);

        // 构造推送消息
        PrivateMessageVO vo = new PrivateMessageVO();
        vo.setId(pm.getId());
        vo.setSenderId(senderId);
        vo.setReceiverId(message.getTo());
        vo.setContent(message.getContent());
        vo.setMessageType(pm.getMessageType());
        vo.setFileUrl(pm.getFileUrl());
        vo.setStatus(0);
        vo.setCreateTime(LocalDateTime.now());
        vo.setSenderNickname(sender != null ? sender.getNickname() : "未知用户");
        vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);

        // 推送给接收者
        messagingTemplate.convertAndSendToUser(
                String.valueOf(message.getTo()),
                "/queue/messages",
                vo
        );

        // 也推送给发送者自己（多端同步）
        messagingTemplate.convertAndSendToUser(
                String.valueOf(senderId),
                "/queue/messages",
                vo
        );

        log.info("私聊消息已发送: from={} to={}", senderId, message.getTo());
    }

    /**
     * 标记已读通知
     * 当用户打开会话时，通知对方消息已读
     */
    @MessageMapping("/chat.read")
    public void markRead(ReadNotification notification, Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        Long friendId = notification.getFriendId();

        // 更新 DB
        privateMessageMapper.markAsRead(userId, friendId);

        // 通知对方消息已读（让对方更新 UI 上的已读标记）
        Map<String, Object> readReceipt = new HashMap<>();
        readReceipt.put("type", "READ_RECEIPT");
        readReceipt.put("from", userId);
        readReceipt.put("friendId", userId); // 对方视角：谁读了我的消息
        readReceipt.put("timestamp", LocalDateTime.now().toString());

        messagingTemplate.convertAndSendToUser(
                String.valueOf(friendId),
                "/queue/messages",
                readReceipt
        );

        log.info("消息已读通知: userId={} friendId={}", userId, friendId);
    }

    /**
     * 内部消息体
     */
    @lombok.Data
    public static class ChatMessage {
        private Long to;
        private String content;
        private Integer messageType;
        private String fileUrl;
    }

    @lombok.Data
    public static class ReadNotification {
        private Long friendId;
    }
}
