package com.ncu.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ncu.chat.mapper.GroupMessageMapper;
import com.ncu.chat.mapper.GroupMemberMapper;
import com.ncu.chat.mapper.PrivateMessageMapper;
import com.ncu.chat.mapper.UserMapper;
import com.ncu.chat.mapper.AiBotMapper;
import com.ncu.chat.model.entity.AiBot;
import com.ncu.chat.model.entity.GroupMessage;
import com.ncu.chat.model.entity.PrivateMessage;
import com.ncu.chat.model.entity.User;
import com.ncu.chat.model.vo.PrivateMessageVO;
import com.ncu.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息中心服务实现类
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final PrivateMessageMapper privateMessageMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final UserMapper userMapper;
    private final AiBotMapper aiBotMapper;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<PrivateMessageVO> searchPrivateMessages(Long userId, Long targetId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(PrivateMessage::getSenderId, userId).eq(PrivateMessage::getReceiverId, targetId))
                .or(w2 -> w2.eq(PrivateMessage::getSenderId, targetId).eq(PrivateMessage::getReceiverId, userId))
        );
        wrapper.like(PrivateMessage::getContent, keyword);
        wrapper.orderByAsc(PrivateMessage::getCreateTime);

        List<PrivateMessage> messages = privateMessageMapper.selectList(wrapper);
        return convertToVOList(messages);
    }

    @Override
    public List<PrivateMessageVO> filterPrivateMessages(Long userId, Long targetId, String startTime, String endTime) {
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(PrivateMessage::getSenderId, userId).eq(PrivateMessage::getReceiverId, targetId))
                .or(w2 -> w2.eq(PrivateMessage::getSenderId, targetId).eq(PrivateMessage::getReceiverId, userId))
        );

        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(PrivateMessage::getCreateTime, LocalDateTime.parse(startTime, DATETIME_FORMATTER));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(PrivateMessage::getCreateTime, LocalDateTime.parse(endTime, DATETIME_FORMATTER));
        }

        wrapper.orderByAsc(PrivateMessage::getCreateTime);
        List<PrivateMessage> messages = privateMessageMapper.selectList(wrapper);
        return convertToVOList(messages);
    }

    @Override
    public String exportPrivateMessagesAsTxt(Long userId, Long targetId) throws IOException {
        // 获取所有相关消息
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(PrivateMessage::getSenderId, userId).eq(PrivateMessage::getReceiverId, targetId))
                .or(w2 -> w2.eq(PrivateMessage::getSenderId, targetId).eq(PrivateMessage::getReceiverId, userId))
        );
        wrapper.orderByAsc(PrivateMessage::getCreateTime);

        List<PrivateMessage> messages = privateMessageMapper.selectList(wrapper);
        List<PrivateMessageVO> voList = convertToVOList(messages);

        StringBuilder sb = new StringBuilder();
        sb.append("聊天记录导出\n");
        sb.append("导出时间：").append(LocalDateTime.now().format(DATETIME_FORMATTER)).append("\n");
        sb.append("========================================\n\n");

        for (PrivateMessageVO vo : voList) {
            String senderName = vo.getSenderNickname();
            String time = vo.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String content = vo.getContent();

            sb.append("[").append(time).append("] ").append(senderName).append(": ").append(content).append("\n");
        }

        return sb.toString();
    }

    @Override
    public String exportPrivateMessagesAsCsv(Long userId, Long targetId) throws IOException {
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(PrivateMessage::getSenderId, userId).eq(PrivateMessage::getReceiverId, targetId))
                .or(w2 -> w2.eq(PrivateMessage::getSenderId, targetId).eq(PrivateMessage::getReceiverId, userId))
        );
        wrapper.orderByAsc(PrivateMessage::getCreateTime);

        List<PrivateMessage> messages = privateMessageMapper.selectList(wrapper);
        List<PrivateMessageVO> voList = convertToVOList(messages);

        StringBuilder sb = new StringBuilder();
        // CSV 表头
        sb.append("时间，发送者，内容，类型\n");

        for (PrivateMessageVO vo : voList) {
            String time = vo.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String sender = vo.getSenderNickname();
            String content = escapeCsv(vo.getContent());
            String type = getMessageTypeText(vo.getMessageType());

            sb.append(time).append(",").append(sender).append(",").append(content).append(",").append(type).append("\n");
        }

        return sb.toString();
    }

    // --- 群聊相关方法 ---

    @Override
    public List<PrivateMessageVO> searchGroupMessages(Long groupId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMessage::getGroupId, groupId);
        wrapper.like(GroupMessage::getContent, keyword);
        wrapper.orderByAsc(GroupMessage::getCreateTime);

        List<GroupMessage> messages = groupMessageMapper.selectList(wrapper);
        return convertGroupToVOList(messages);
    }

    @Override
    public List<PrivateMessageVO> filterGroupMessages(Long groupId, String startTime, String endTime) {
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMessage::getGroupId, groupId);

        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(GroupMessage::getCreateTime, LocalDateTime.parse(startTime, DATETIME_FORMATTER));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(GroupMessage::getCreateTime, LocalDateTime.parse(endTime, DATETIME_FORMATTER));
        }

        wrapper.orderByAsc(GroupMessage::getCreateTime);
        List<GroupMessage> messages = groupMessageMapper.selectList(wrapper);
        return convertGroupToVOList(messages);
    }

    @Override
    public String exportGroupMessagesAsTxt(Long groupId) throws IOException {
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMessage::getGroupId, groupId);
        wrapper.orderByAsc(GroupMessage::getCreateTime);

        List<GroupMessage> messages = groupMessageMapper.selectList(wrapper);
        List<PrivateMessageVO> voList = convertGroupToVOList(messages);

        StringBuilder sb = new StringBuilder();
        sb.append("群聊记录导出\n");
        sb.append("导出时间：").append(LocalDateTime.now().format(DATETIME_FORMATTER)).append("\n");
        sb.append("========================================\n\n");

        for (PrivateMessageVO vo : voList) {
            String senderName = vo.getSenderNickname();
            String time = vo.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String content = vo.getContent();

            sb.append("[").append(time).append("] ").append(senderName).append(": ").append(content).append("\n");
        }

        return sb.toString();
    }

    @Override
    public String exportGroupMessagesAsCsv(Long groupId) throws IOException {
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMessage::getGroupId, groupId);
        wrapper.orderByAsc(GroupMessage::getCreateTime);

        List<GroupMessage> messages = groupMessageMapper.selectList(wrapper);
        List<PrivateMessageVO> voList = convertGroupToVOList(messages);

        StringBuilder sb = new StringBuilder();
        sb.append("时间，发送者，内容，类型\n");

        for (PrivateMessageVO vo : voList) {
            String time = vo.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String sender = vo.getSenderNickname();
            String content = escapeCsv(vo.getContent());
            String type = getMessageTypeText(vo.getMessageType());

            sb.append(time).append(",").append(sender).append(",").append(content).append(",").append(type).append("\n");
        }

        return sb.toString();
    }

    // --- 辅助方法 ---

    private List<PrivateMessageVO> convertToVOList(List<PrivateMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询用户信息
        Set<Long> userIds = messages.stream()
                .map(PrivateMessage::getSenderId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            users.forEach(u -> userMap.put(u.getId(), u));
        }

        return messages.stream()
                .map(pm -> {
                    PrivateMessageVO vo = new PrivateMessageVO();
                    vo.setId(pm.getId());
                    vo.setSenderId(pm.getSenderId());
                    vo.setReceiverId(pm.getReceiverId());
                    vo.setContent(pm.getContent());
                    vo.setMessageType(pm.getMessageType());
                    vo.setStatus(pm.getStatus());
                    vo.setFileUrl(pm.getFileUrl());
                    vo.setCreateTime(pm.getCreateTime());
                    User sender = userMap.get(pm.getSenderId());
                    vo.setSenderNickname(sender != null ? sender.getNickname() : "未知用户");
                    vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private List<PrivateMessageVO> convertGroupToVOList(List<GroupMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询用户信息（仅非机器人消息）
        Set<Long> userIds = messages.stream()
                .filter(gm -> gm.getBotId() == null)
                .map(GroupMessage::getSenderId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            users.forEach(u -> userMap.put(u.getId(), u));
        }

        // 批量查询机器人信息
        Set<Long> botIds = messages.stream()
                .map(GroupMessage::getBotId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, AiBot> botMap = new HashMap<>();
        if (!botIds.isEmpty()) {
            List<AiBot> bots = aiBotMapper.selectBatchIds(botIds);
            bots.forEach(b -> botMap.put(b.getId(), b));
        }

        return messages.stream()
                .map(gm -> {
                    PrivateMessageVO vo = new PrivateMessageVO();
                    vo.setId(gm.getId());
                    vo.setSenderId(gm.getSenderId());
                    vo.setContent(gm.getContent());
                    vo.setMessageType(gm.getMessageType());
                    vo.setFileUrl(gm.getFileUrl());
                    vo.setCreateTime(gm.getCreateTime());

                    if (gm.getBotId() != null) {
                        AiBot bot = botMap.get(gm.getBotId());
                        vo.setBotId(gm.getBotId());
                        vo.setBotName(bot != null ? bot.getName() : "AI 机器人");
                        vo.setBotAvatar(bot != null ? bot.getAvatar() : null);
                        vo.setSenderNickname(bot != null ? bot.getName() : "AI 机器人");
                        vo.setSenderAvatar(bot != null ? bot.getAvatar() : null);
                    } else {
                        User sender = userMap.get(gm.getSenderId());
                        vo.setSenderNickname(sender != null ? sender.getNickname() : "未知用户");
                        vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private String getMessageTypeText(Integer messageType) {
        if (messageType == null) return "文字";
        switch (messageType) {
            case 1: return "图片";
            case 2: return "文件";
            case 3: return "语音";
            case 4: return "语音通话";
            default: return "文字";
        }
    }

    private String escapeCsv(String content) {
        if (content == null) return "";
        // CSV 转义：如果包含逗号、双引号或换行符，用双引号包裹，双引号转义为两个双引号
        if (content.contains(",") || content.contains("\"") || content.contains("\n")) {
            return "\"" + content.replace("\"", "\"\"") + "\"";
        }
        return content;
    }
}
