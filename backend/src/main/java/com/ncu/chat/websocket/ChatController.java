package com.ncu.chat.websocket;

import com.ncu.chat.mapper.FriendshipMapper;
import com.ncu.chat.mapper.GroupMemberMapper;
import com.ncu.chat.mapper.PrivateMessageMapper;
import com.ncu.chat.mapper.UserMapper;
import com.ncu.chat.model.entity.Friendship;
import com.ncu.chat.model.entity.GroupMember;
import com.ncu.chat.model.entity.PrivateMessage;
import com.ncu.chat.model.entity.User;
import com.ncu.chat.model.vo.PrivateMessageVO;
import com.ncu.chat.model.vo.VoiceMessageVO;
import com.ncu.chat.service.VoiceMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PrivateMessageMapper privateMessageMapper;
    private final UserMapper userMapper;
    private final FriendshipMapper friendshipMapper;
    private final VoiceMessageService voiceMessageService;
    private final GroupMemberMapper groupMemberMapper;

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

    // ==================== 语音消息处理 ====================

    /**
     * 语音消息发送
     * 客户端发送到: /app/voice.send
     */
    @MessageMapping("/voice.send")
    public void sendVoiceMessage(VoicePayload payload, Principal principal) {
        Long senderId = Long.valueOf(principal.getName());

        // 持久化语音消息到 voice_message 表
        VoiceMessageVO vo = voiceMessageService.saveVoiceMessage(
                senderId, payload.getTo(), payload.getGroupId(),
                payload.getFileUrl(), payload.getDuration());

        // 同时写入 private_message 表，确保历史记录可查询
        PrivateMessage pm = new PrivateMessage();
        pm.setSenderId(senderId);
        pm.setReceiverId(payload.getTo());
        pm.setContent(String.valueOf(payload.getDuration()));
        pm.setMessageType(3);
        pm.setFileUrl(payload.getFileUrl());
        pm.setStatus(0);
        privateMessageMapper.insert(pm);

        User sender = userMapper.selectById(senderId);

        if (payload.getGroupId() != null) {
            // 群聊语音消息：推送给所有群成员
            List<GroupMember> members = groupMemberMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GroupMember>()
                            .eq(GroupMember::getGroupId, payload.getGroupId()));
            for (GroupMember member : members) {
                if (!member.getUserId().equals(senderId)) {
                    messagingTemplate.convertAndSendToUser(
                            String.valueOf(member.getUserId()), "/queue/voice_messages", vo);
                }
            }
        } else if (payload.getTo() != null) {
            // 私聊语音消息
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(payload.getTo()), "/queue/voice_messages", vo);
            // 推送给发送者自己（多端同步）
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(senderId), "/queue/voice_messages", vo);
        }

        log.info("语音消息已发送: from={} to={} group={}", senderId, payload.getTo(), payload.getGroupId());
    }

    // ==================== WebRTC 通话信令 ====================

    /**
     * 发送 SDP Offer（主叫 → 被叫）
     */
    @MessageMapping("/call.offer")
    public void handleCallOffer(CallSignal payload, Principal principal) {
        Long callerId = Long.valueOf(principal.getName());
        User caller = userMapper.selectById(callerId);

        Map<String, Object> signal = new HashMap<>();
        signal.put("type", "CALL_OFFER");
        signal.put("callerId", callerId);
        signal.put("callerName", caller != null ? caller.getNickname() : "未知用户");
        signal.put("callerAvatar", caller != null ? caller.getAvatar() : null);
        signal.put("sdp", payload.getSdp());

        messagingTemplate.convertAndSendToUser(
                String.valueOf(payload.getCalleeId()), "/queue/call", signal);
        log.info("通话 Offer: caller={} callee={}", callerId, payload.getCalleeId());
    }

    /**
     * 发送 SDP Answer（被叫 → 主叫）
     */
    @MessageMapping("/call.answer")
    public void handleCallAnswer(CallSignal payload, Principal principal) {
        Long answererId = Long.valueOf(principal.getName());

        Map<String, Object> signal = new HashMap<>();
        signal.put("type", "CALL_ANSWER");
        signal.put("answererId", answererId);
        signal.put("sdp", payload.getSdp());

        messagingTemplate.convertAndSendToUser(
                String.valueOf(payload.getCalleeId()), "/queue/call", signal);
        log.info("通话 Answer: answerer={} to={}", answererId, payload.getCalleeId());
    }

    /**
     * 转发 ICE Candidate
     */
    @MessageMapping("/call.ice")
    public void handleCallICE(CallSignal payload, Principal principal) {
        Long senderId = Long.valueOf(principal.getName());

        Map<String, Object> signal = new HashMap<>();
        signal.put("type", "CALL_ICE");
        signal.put("from", senderId);
        signal.put("candidate", payload.getCandidate());

        messagingTemplate.convertAndSendToUser(
                String.valueOf(payload.getCalleeId()), "/queue/call", signal);
    }

    /**
     * 拒绝通话
     */
    @MessageMapping("/call.reject")
    public void handleCallReject(CallSignal payload, Principal principal) {
        Long rejecterId = Long.valueOf(principal.getName());
        Long callerId = payload.getCalleeId(); // 拒绝时 calleeId 是主叫方

        Map<String, Object> signal = new HashMap<>();
        signal.put("type", "CALL_REJECT");
        signal.put("rejecterId", rejecterId);
        signal.put("reason", payload.getReason() != null ? payload.getReason() : "decline");

        messagingTemplate.convertAndSendToUser(
                String.valueOf(callerId), "/queue/call", signal);

        // 保存通话记录：以拨打方为 senderId，消息出现在拨打方一侧
        saveCallRecord(callerId, rejecterId, "对方已拒绝");

        log.info("通话拒绝: rejecter={} caller={}", rejecterId, callerId);
    }

    /**
     * 挂断通话
     */
    @MessageMapping("/call.hangup")
    public void handleCallHangup(CallSignal payload, Principal principal) {
        Long hanguperId = Long.valueOf(principal.getName());
        Long partnerId = payload.getCalleeId();
        int durationSec = payload.getDuration() != null ? payload.getDuration() : 0;
        String durationText = String.format("%02d:%02d", durationSec / 60, durationSec % 60);

        // 转发挂断信号
        Map<String, Object> signal = new HashMap<>();
        signal.put("type", "CALL_HANGUP");
        signal.put("hanguperId", hanguperId);
        signal.put("duration", durationSec);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(partnerId), "/queue/call", signal);

        // 保存通话记录消息到双方的消息列表（messageType=4）
        String callContent = "通话时长 " + durationText;
        saveCallRecord(hanguperId, partnerId, callContent);

        log.info("通话挂断: from={} to={} duration={}", hanguperId, partnerId, durationText);
    }

    private void saveCallRecord(Long userId1, Long userId2, String content) {
        PrivateMessage pm = new PrivateMessage();
        pm.setSenderId(userId1);
        pm.setReceiverId(userId2);
        pm.setContent(content);
        pm.setMessageType(4);  // 4=通话记录
        pm.setStatus(1);
        privateMessageMapper.insert(pm);

        // 推送给双方
        PrivateMessageVO vo = buildMessageVO(pm);
        messagingTemplate.convertAndSendToUser(String.valueOf(userId1), "/queue/messages", vo);
        messagingTemplate.convertAndSendToUser(String.valueOf(userId2), "/queue/messages", vo);
    }

    private PrivateMessageVO buildMessageVO(PrivateMessage pm) {
        PrivateMessageVO vo = new PrivateMessageVO();
        vo.setId(pm.getId());
        vo.setSenderId(pm.getSenderId());
        vo.setReceiverId(pm.getReceiverId());
        vo.setContent(pm.getContent());
        vo.setMessageType(pm.getMessageType());
        vo.setStatus(pm.getStatus());
        vo.setCreateTime(pm.getCreateTime() != null ? pm.getCreateTime() : LocalDateTime.now());
        User sender = userMapper.selectById(pm.getSenderId());
        vo.setSenderNickname(sender != null ? sender.getNickname() : "未知");
        vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        return vo;
    }

    /**
     * 取消通话（主叫取消呼叫）
     */
    @MessageMapping("/call.cancel")
    public void handleCallCancel(CallSignal payload, Principal principal) {
        Long callerId = Long.valueOf(principal.getName());
        Long calleeId = payload.getCalleeId();

        Map<String, Object> signal = new HashMap<>();
        signal.put("type", "CALL_CANCEL");
        signal.put("callerId", callerId);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(calleeId), "/queue/call", signal);

        // 保存通话记录：caller 取消，双方在聊天界面可见
        saveCallRecord(callerId, calleeId, "已取消");

        log.info("通话取消: caller={} callee={}", callerId, calleeId);
    }

    // ==================== 内部消息体 ====================

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

    @lombok.Data
    public static class VoicePayload {
        private Long to;
        private Long groupId;
        private String fileUrl;
        private Integer duration;
    }

    @lombok.Data
    public static class CallSignal {
        private Long calleeId;
        private String sdp;
        private String candidate;
        private String reason;
        private Integer duration;
    }
}
