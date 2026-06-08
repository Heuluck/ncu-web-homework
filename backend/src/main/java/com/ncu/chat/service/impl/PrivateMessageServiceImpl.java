package com.ncu.chat.service.impl;

import com.ncu.chat.common.BusinessException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.chat.common.PageResult;
import com.ncu.chat.mapper.FriendshipMapper;
import com.ncu.chat.mapper.PrivateMessageMapper;
import com.ncu.chat.mapper.UserMapper;
import com.ncu.chat.model.dto.PrivateMessageSendDTO;
import com.ncu.chat.model.entity.Friendship;
import com.ncu.chat.model.entity.PrivateMessage;
import com.ncu.chat.model.entity.User;
import com.ncu.chat.model.vo.ConversationVO;
import com.ncu.chat.model.vo.PrivateMessageVO;
import com.ncu.chat.service.PrivateMessageService;
import com.ncu.chat.service.SensitiveWordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PrivateMessageServiceImpl implements PrivateMessageService {

    private final PrivateMessageMapper privateMessageMapper;
    private final UserMapper userMapper;
    private final FriendshipMapper friendshipMapper;
    private final SensitiveWordService sensitiveWordService;

    public PrivateMessageServiceImpl(PrivateMessageMapper privateMessageMapper,
                                     UserMapper userMapper,
                                     FriendshipMapper friendshipMapper,
                                     SensitiveWordService sensitiveWordService) {
        this.privateMessageMapper = privateMessageMapper;
        this.userMapper = userMapper;
        this.sensitiveWordService = sensitiveWordService;
        this.friendshipMapper = friendshipMapper;
    }

    @Override
    @Transactional
    public PrivateMessageVO sendMessage(Long senderId, PrivateMessageSendDTO dto) {
        // 检查好友关系及拉黑状态
        Friendship friendship = friendshipMapper.findByUserPair(senderId, dto.getReceiverId());
        if (friendship == null || friendship.getStatus() != 1) {
            throw new BusinessException("你们还不是好友，无法发送消息");
        }
        // 双向拉黑检查
        boolean blocked;
        if (friendship.getRequesterId().equals(senderId)) {
            blocked = (friendship.getRequesterBlocked() != null && friendship.getRequesterBlocked() == 1)
                   || (friendship.getReceiverBlocked() != null && friendship.getReceiverBlocked() == 1);
        } else {
            blocked = (friendship.getReceiverBlocked() != null && friendship.getReceiverBlocked() == 1)
                   || (friendship.getRequesterBlocked() != null && friendship.getRequesterBlocked() == 1);
        }
        if (blocked) {
            throw new BusinessException("由于拉黑限制，无法发送消息");
        }

        // 敏感词检测（仅对文本消息）
        if (dto.getMessageType() == null || dto.getMessageType() == 0) {
            Map<String, Object> swResult = sensitiveWordService.checkSensitiveWords(dto.getContent());
            if (swResult.containsKey("hasSensitive") && (Boolean) swResult.get("hasSensitive")) {
                throw new BusinessException("消息包含敏感词，已被拦截");
            }
        }

        // 持久化消息
        PrivateMessage pm = new PrivateMessage();
        pm.setSenderId(senderId);
        pm.setReceiverId(dto.getReceiverId());
        pm.setContent(dto.getContent());
        pm.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : 0);
        pm.setFileUrl(dto.getFileUrl());
        pm.setStatus(0);
        privateMessageMapper.insert(pm);

        // 查询发送者信息
        User sender = userMapper.selectById(senderId);

        return convertToVO(pm, sender);
    }

    @Override
    public PageResult<PrivateMessageVO> getHistory(Long userId, Long friendId, Integer pageNum, Integer pageSize) {
        Page<PrivateMessage> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(PrivateMessage::getSenderId, userId).eq(PrivateMessage::getReceiverId, friendId))
                .or(w2 -> w2.eq(PrivateMessage::getSenderId, friendId).eq(PrivateMessage::getReceiverId, userId))
        );
        wrapper.orderByDesc(PrivateMessage::getCreateTime);

        Page<PrivateMessage> result = privateMessageMapper.selectPage(page, wrapper);

        // 批量查询相关用户信息
        Set<Long> userIds = result.getRecords().stream()
                .map(PrivateMessage::getSenderId)
                .collect(Collectors.toSet());
        userIds.add(userId);
        userIds.add(friendId);

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            users.forEach(u -> userMap.put(u.getId(), u));
        }

        List<PrivateMessageVO> voList = result.getRecords().stream()
                .map(pm -> convertToVO(pm, userMap.get(pm.getSenderId())))
                .collect(Collectors.toList());

        // 反转为时间正序（前端展示用）
        Collections.reverse(voList);

        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public List<PrivateMessageVO> getUnreadMessages(Long userId) {
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrivateMessage::getReceiverId, userId)
                .eq(PrivateMessage::getStatus, 0)
                .orderByDesc(PrivateMessage::getCreateTime);

        List<PrivateMessage> messages = privateMessageMapper.selectList(wrapper);

        Set<Long> senderIds = messages.stream()
                .map(PrivateMessage::getSenderId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(senderIds);
            users.forEach(u -> userMap.put(u.getId(), u));
        }

        return messages.stream()
                .map(pm -> convertToVO(pm, userMap.get(pm.getSenderId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long friendId) {
        privateMessageMapper.markAsRead(userId, friendId);
    }

    @Override
    public List<ConversationVO> getRecentConversations(Long userId) {
        // 批量获取每个会话的最后一条消息（1 次查询替代 N 次查询）
        List<PrivateMessage> lastMessages = privateMessageMapper.getLastMessagesPerConversation(userId);
        if (lastMessages.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有好友 ID
        Set<Long> friendIds = new HashSet<>();
        for (PrivateMessage pm : lastMessages) {
            if (pm.getSenderId().equals(userId)) {
                friendIds.add(pm.getReceiverId());
            } else {
                friendIds.add(pm.getSenderId());
            }
        }

        // 批量查询用户信息（1 次查询）
        Map<Long, User> userMap = new HashMap<>();
        if (!friendIds.isEmpty()) {
            userMapper.selectBatchIds(friendIds).forEach(u -> userMap.put(u.getId(), u));
        }

        // 批量查询未读数（1 次查询）
        Map<Long, Long> unreadMap = new HashMap<>();
        List<Map<String, Object>> unreadRows = privateMessageMapper.getUnreadCountsBySender(userId);
        for (Map<String, Object> row : unreadRows) {
            Long senderId = ((Number) row.get("senderId")).longValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            unreadMap.put(senderId, cnt);
        }

        List<ConversationVO> conversations = new ArrayList<>();
        for (PrivateMessage lastMsg : lastMessages) {
            Long friendId = lastMsg.getSenderId().equals(userId)
                    ? lastMsg.getReceiverId() : lastMsg.getSenderId();

            User friend = userMap.get(friendId);

            ConversationVO vo = new ConversationVO();
            vo.setFriendId(friendId);
            vo.setNickname(friend != null ? friend.getNickname() : "未知用户");
            vo.setAvatar(friend != null ? friend.getAvatar() : null);
            vo.setOnlineStatus(friend != null ? friend.getStatus() : 0);

            // emoji 表情消息：显示表情符号而非 [图片]
            if (lastMsg.getMessageType() != null && lastMsg.getMessageType() == 1
                    && lastMsg.getFileUrl() != null && lastMsg.getFileUrl().length() <= 10) {
                vo.setLastMessage(lastMsg.getFileUrl());
                vo.setLastMessageType("");
            } else {
                vo.setLastMessage(lastMsg.getContent());
                vo.setLastMessageType(getMessageTypeText(lastMsg.getMessageType()));
            }

            vo.setLastTime(lastMsg.getCreateTime());
            vo.setUnreadCount(unreadMap.getOrDefault(friendId, 0L).intValue());

            conversations.add(vo);
        }

        return conversations;
    }

    // --- 辅助方法 ---

    private PrivateMessageVO convertToVO(PrivateMessage pm, User sender) {
        PrivateMessageVO vo = new PrivateMessageVO();
        vo.setId(pm.getId());
        vo.setSenderId(pm.getSenderId());
        vo.setReceiverId(pm.getReceiverId());
        vo.setContent(pm.getContent());
        vo.setMessageType(pm.getMessageType());
        vo.setStatus(pm.getStatus());
        vo.setFileUrl(pm.getFileUrl());
        vo.setCreateTime(pm.getCreateTime());
        vo.setSenderNickname(sender != null ? sender.getNickname() : "未知用户");
        vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        return vo;
    }

    private String getMessageTypeText(Integer messageType) {
        if (messageType == null) return "";
        switch (messageType) {
            case 1: return "[图片]";
            case 2: return "[文件]";
            case 3: return "[语音]";
            case 4: return "[语音通话]";
            default: return "";
        }
    }
}
