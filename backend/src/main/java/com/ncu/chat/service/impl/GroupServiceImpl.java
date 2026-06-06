package com.ncu.chat.service.impl;

import com.ncu.chat.common.BusinessException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncu.chat.common.PageResult;
import com.ncu.chat.mapper.*;
import com.ncu.chat.model.dto.CreateGroupDTO;
import com.ncu.chat.model.dto.GroupMessageSendDTO;
import com.ncu.chat.model.dto.UpdateGroupDTO;
import com.ncu.chat.model.entity.*;
import com.ncu.chat.model.vo.*;
import com.ncu.chat.service.GroupService;
import com.ncu.chat.service.AiBotService;
import com.ncu.chat.websocket.WebSocketGroupController;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    private final ChatGroupMapper chatGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final UserMapper userMapper;
    private final AiBotMapper aiBotMapper;
    private final WebSocketGroupController webSocketGroupController;
    private final SimpMessagingTemplate messagingTemplate;
    private final AiBotService aiBotService;

    // 手动构造函数，添加 @Lazy 解决循环依赖
    public GroupServiceImpl(ChatGroupMapper chatGroupMapper,
                            GroupMemberMapper groupMemberMapper,
                            GroupMessageMapper groupMessageMapper,
                            UserMapper userMapper,
                            AiBotMapper aiBotMapper,
                            @Lazy WebSocketGroupController webSocketGroupController,
                            SimpMessagingTemplate messagingTemplate,
                            @Lazy AiBotService aiBotService) {
        this.chatGroupMapper = chatGroupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.groupMessageMapper = groupMessageMapper;
        this.userMapper = userMapper;
        this.aiBotMapper = aiBotMapper;
        this.webSocketGroupController = webSocketGroupController;
        this.messagingTemplate = messagingTemplate;
        this.aiBotService = aiBotService;
    }

    // ==================== 群管理 ====================

    @Override
    @Transactional
    public GroupVO createGroup(Long userId, CreateGroupDTO dto) {
        ChatGroup group = new ChatGroup();
        group.setName(dto.getName());
        group.setAvatar(dto.getAvatar());
        group.setAnnouncement(dto.getAnnouncement());
        group.setOwnerId(userId);
        group.setMemberCount(1);
        group.setMaxMembers(500);
        group.setCreatedBy(userId);
        chatGroupMapper.insert(group);

        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(userId);
        ownerMember.setRole(2);
        ownerMember.setJoinTime(LocalDateTime.now());
        ownerMember.setLastReadTime(LocalDateTime.now());
        groupMemberMapper.insert(ownerMember);

        if (dto.getMemberIds() != null && !dto.getMemberIds().isEmpty()) {
            for (Long memberId : dto.getMemberIds()) {
                if (!memberId.equals(userId)) {
                    addMember(group.getId(), memberId, 0);
                    chatGroupMapper.incrementMemberCount(group.getId());
                }
            }

            // WebSocket 通知被邀请的成员
            for (Long memberId : dto.getMemberIds()) {
                if (!memberId.equals(userId)) {
                    Map<String, Object> wsEvent = new HashMap<>();
                    wsEvent.put("type", "MEMBER_INVITED");
                    wsEvent.put("groupId", group.getId());
                    wsEvent.put("groupName", group.getName());
                    wsEvent.put("userId", memberId);
                    messagingTemplate.convertAndSendToUser(String.valueOf(memberId), "/queue/group_events", wsEvent);
                }
            }
        }

        return convertToGroupVO(group, userId);
    }

    @Override
    public GroupVO getGroupInfo(Long userId, Long groupId) {
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getDeleted() == 1) {
            throw new BusinessException("群聊不存在");
        }
        GroupMember member = getMember(groupId, userId);
        if (member == null) {
            throw new BusinessException("你不是群成员");
        }
        return convertToGroupVO(group, userId);
    }

    @Override
    @Transactional
    public GroupVO updateGroupInfo(Long userId, Long groupId, UpdateGroupDTO dto) {
        GroupMember member = getMember(groupId, userId);
        if (member == null || member.getRole() < 1) {
            throw new BusinessException("只有群主或管理员可以修改群信息");
        }
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("群聊不存在");
        }
        if (dto.getName() != null) group.setName(dto.getName());
        if (dto.getAvatar() != null) group.setAvatar(dto.getAvatar());
        if (dto.getAnnouncement() != null) group.setAnnouncement(dto.getAnnouncement());
        chatGroupMapper.updateById(group);
        return convertToGroupVO(group, userId);
    }

    @Override
    @Transactional
    public void disbandGroup(Long userId, Long groupId) {
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null || !group.getOwnerId().equals(userId)) {
            throw new BusinessException("只有群主可以解散群聊");
        }

        // 先查出所有成员 ID（软删除前查，否则查不到）
        List<Long> memberIds = groupMemberMapper.getUserIdsByGroupId(groupId);

        // 使用 LambdaUpdateWrapper 强制更新 deleted = 1
        LambdaUpdateWrapper<ChatGroup> groupWrapper = new LambdaUpdateWrapper<>();
        groupWrapper.eq(ChatGroup::getId, groupId)
                .set(ChatGroup::getDeleted, 1);
        chatGroupMapper.update(null, groupWrapper);

        // 使用 LambdaUpdateWrapper 强制更新所有群成员的 deleted = 1
        LambdaUpdateWrapper<GroupMember> memberWrapper = new LambdaUpdateWrapper<>();
        memberWrapper.eq(GroupMember::getGroupId, groupId)
                .set(GroupMember::getDeleted, 1);
        groupMemberMapper.update(null, memberWrapper);

        // WebSocket 通知所有成员群已解散
        Map<String, Object> event = new HashMap<>();
        event.put("type", "GROUP_DISBAND");
        event.put("groupId", groupId);
        event.put("groupName", group.getName());
        for (Long memberId : memberIds) {
            if (memberId.equals(userId)) continue; // 不通知操作者自己
            messagingTemplate.convertAndSendToUser(String.valueOf(memberId), "/queue/group_events", event);
        }
    }

    @Override
    public List<GroupConversationVO> getMyGroups(Long userId) {
        List<Long> groupIds = groupMemberMapper.getGroupIdsByUserId(userId);
        if (groupIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询群信息
        List<ChatGroup> groups = chatGroupMapper.selectBatchIds(groupIds);
        Map<Long, ChatGroup> groupMap = groups.stream()
                .filter(g -> g.getDeleted() == 0)
                .collect(Collectors.toMap(ChatGroup::getId, g -> g));

        // 过滤有效群 ID
        List<Long> validGroupIds = new ArrayList<>(groupMap.keySet());

        // 批量查询当前用户在这些群的成员信息（1 次查询替代 N 次）
        LambdaQueryWrapper<GroupMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.in(GroupMember::getGroupId, validGroupIds)
                .eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getDeleted, 0);
        List<GroupMember> members = groupMemberMapper.selectList(memberWrapper);
        Set<Long> memberGroupIds = members.stream()
                .map(GroupMember::getGroupId)
                .collect(Collectors.toSet());

        // 批量获取所有群的最后一条消息（1 次查询替代 N 次）
        Map<Long, GroupMessage> lastMsgMap = new HashMap<>();
        if (!validGroupIds.isEmpty()) {
            List<GroupMessage> lastMessages = groupMessageMapper.getLastMessagesByGroupIds(validGroupIds);
            for (GroupMessage msg : lastMessages) {
                lastMsgMap.put(msg.getGroupId(), msg);
            }
        }

        List<GroupConversationVO> result = new ArrayList<>();
        for (Long groupId : validGroupIds) {
            if (!memberGroupIds.contains(groupId)) continue;

            ChatGroup group = groupMap.get(groupId);
            GroupMessage lastMsg = lastMsgMap.get(groupId);

            GroupConversationVO vo = new GroupConversationVO();
            vo.setGroupId(groupId);
            vo.setGroupName(group.getName());
            vo.setGroupAvatar(group.getAvatar());
            vo.setAnnouncement(group.getAnnouncement());
            if (lastMsg != null) {
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
            }
            result.add(vo);
        }
        return result;
    }

    // ==================== 成员管理 ====================

    @Override
    public List<GroupMemberVO> getGroupMembers(Long userId, Long groupId) {
        if (getMember(groupId, userId) == null) {
            throw new BusinessException("你不是群成员");
        }
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId).eq(GroupMember::getDeleted, 0);
        List<GroupMember> members = groupMemberMapper.selectList(wrapper);

        List<Long> userIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toList());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        }

        List<GroupMemberVO> result = new ArrayList<>();
        for (GroupMember m : members) {
            User u = userMap.get(m.getUserId());
            if (u == null) continue;
            GroupMemberVO vo = new GroupMemberVO();
            vo.setUserId(m.getUserId());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            vo.setRole(m.getRole());
            vo.setJoinTime(m.getJoinTime());
            result.add(vo);
        }
        result.sort((a, b) -> Integer.compare(b.getRole(), a.getRole()));
        return result;
    }

    @Override
    @Transactional
    public void inviteMembers(Long userId, Long groupId, List<Long> memberIds) {
        GroupMember current = getMember(groupId, userId);
        if (current == null || current.getRole() < 1) {
            throw new BusinessException("无权限");
        }
        ChatGroup group = chatGroupMapper.selectById(groupId);
        // 取出当前所有成员 ID（新成员加入前）
        List<Long> existingMemberIds = groupMemberMapper.getUserIdsByGroupId(groupId);

        for (Long targetId : memberIds) {
            if (getMember(groupId, targetId) != null) continue;
            addMember(groupId, targetId, 0);
            chatGroupMapper.incrementMemberCount(groupId);

            // 查询新成员信息
            User newUser = userMapper.selectById(targetId);

            // WebSocket 通知被邀请者
            Map<String, Object> invitedEvent = new HashMap<>();
            invitedEvent.put("type", "MEMBER_INVITED");
            invitedEvent.put("groupId", groupId);
            invitedEvent.put("groupName", group != null ? group.getName() : "");
            invitedEvent.put("userId", targetId);
            messagingTemplate.convertAndSendToUser(String.valueOf(targetId), "/queue/group_events", invitedEvent);

            // 广播给所有已有成员：有人加入了
            Map<String, Object> joinedEvent = new HashMap<>();
            joinedEvent.put("type", "MEMBER_JOINED");
            joinedEvent.put("groupId", groupId);
            joinedEvent.put("userId", targetId);
            joinedEvent.put("nickname", newUser != null ? newUser.getNickname() : "");
            joinedEvent.put("avatar", newUser != null ? newUser.getAvatar() : "");
            joinedEvent.put("memberCount", chatGroupMapper.selectById(groupId).getMemberCount());
            log.info("[WS] 广播 MEMBER_JOINED groupId={}, userId={}, nickname={}, 成员数={}", groupId, targetId, newUser != null ? newUser.getNickname() : "", existingMemberIds.size());
            for (Long memberId : existingMemberIds) {
                log.info("[WS] 发送 MEMBER_JOINED 给 memberId={}", memberId);
                messagingTemplate.convertAndSendToUser(String.valueOf(memberId), "/queue/group_events", joinedEvent);
            }
            // 加入后更新已有成员列表，使下一次循环能通知到新成员
            existingMemberIds.add(targetId);
        }
    }

    @Override
    @Transactional
    public void removeMember(Long userId, Long groupId, Long targetUserId) {
        GroupMember current = getMember(groupId, userId);
        GroupMember target = getMember(groupId, targetUserId);
        if (current == null) throw new BusinessException("你不是群成员");
        if (target == null) throw new BusinessException("目标用户不在群中");
        if (current.getRole() == 2) {
            if (targetUserId.equals(userId)) throw new BusinessException("群主不能踢自己");
        } else if (current.getRole() == 1) {
            if (target.getRole() >= 1) throw new BusinessException("管理员不能踢群主或其他管理员");
        } else {
            throw new BusinessException("无权限");
        }

        // 使用 LambdaUpdateWrapper 软删除成员
        LambdaUpdateWrapper<GroupMember> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, targetUserId)
                .set(GroupMember::getDeleted, 1);
        groupMemberMapper.update(null, wrapper);
        chatGroupMapper.decrementMemberCount(groupId);

        // WebSocket 通知被移除的成员
        ChatGroup group = chatGroupMapper.selectById(groupId);
        Map<String, Object> removedEvent = new HashMap<>();
        removedEvent.put("type", "MEMBER_REMOVED");
        removedEvent.put("groupId", groupId);
        removedEvent.put("groupName", group != null ? group.getName() : "");
        removedEvent.put("userId", targetUserId);
        messagingTemplate.convertAndSendToUser(String.valueOf(targetUserId), "/queue/group_events", removedEvent);

        // 广播给所有剩余成员：有人被移除了
        List<Long> remainingIds = groupMemberMapper.getUserIdsByGroupId(groupId);
        Map<String, Object> changeEvent = new HashMap<>();
        changeEvent.put("type", "MEMBER_LEFT");
        changeEvent.put("groupId", groupId);
        changeEvent.put("userId", targetUserId);
        changeEvent.put("memberCount", group != null ? group.getMemberCount() : 0);
        log.info("[WS] 广播 MEMBER_LEFT groupId={}, userId={}, 剩余成员={}", groupId, targetUserId, remainingIds);
        for (Long memberId : remainingIds) {
            log.info("[WS] 发送 MEMBER_LEFT 给 memberId={}", memberId);
            messagingTemplate.convertAndSendToUser(String.valueOf(memberId), "/queue/group_events", changeEvent);
        }
    }

    @Override
    @Transactional
    public void setAdmin(Long userId, Long groupId, Long targetUserId) {
        GroupMember current = getMember(groupId, userId);
        GroupMember target = getMember(groupId, targetUserId);
        if (current == null || current.getRole() != 2) {
            throw new BusinessException("只有群主可以设置管理员");
        }
        if (target.getRole() == 2) throw new BusinessException("不能设置群主为管理员");
        target.setRole(target.getRole() == 1 ? 0 : 1);
        groupMemberMapper.updateById(target);
    }

    @Override
    @Transactional
    public void transferOwner(Long userId, Long groupId, Long targetUserId) {
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null || !group.getOwnerId().equals(userId)) {
            throw new BusinessException("只有群主可以转让群聊");
        }
        GroupMember newOwner = getMember(groupId, targetUserId);
        if (newOwner == null) throw new BusinessException("目标用户不在群中");
        GroupMember oldOwner = getMember(groupId, userId);
        oldOwner.setRole(0);
        newOwner.setRole(2);
        groupMemberMapper.updateById(oldOwner);
        groupMemberMapper.updateById(newOwner);
        group.setOwnerId(targetUserId);
        chatGroupMapper.updateById(group);
    }

    @Override
    public void setDoNotDisturb(Long userId, Long groupId, Boolean enabled) {
        GroupMember member = getMember(groupId, userId);
        if (member == null) throw new BusinessException("你不是群成员");
        member.setDoNotDisturb(enabled ? 1 : 0);
        groupMemberMapper.updateById(member);
    }

    // ==================== 消息 ====================

    @Override
    @Transactional
    public GroupMessageVO sendMessage(Long userId, GroupMessageSendDTO dto) {
        GroupMember member = getMember(dto.getGroupId(), userId);
        if (member == null) throw new BusinessException("你不是群成员");

        System.out.println("[DEBUG] 发送群消息：groupId=" + dto.getGroupId() + ", content=" + dto.getContent() + ", messageType=" + dto.getMessageType() + ", fileUrl=" + dto.getFileUrl());

        GroupMessage msg = new GroupMessage();
        msg.setGroupId(dto.getGroupId());
        msg.setSenderId(userId);
        msg.setContent(dto.getContent());
        msg.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : 0);
        msg.setFileUrl(dto.getFileUrl());
        msg.setIsRecall(0);
        groupMessageMapper.insert(msg);

        System.out.println("[DEBUG] 消息已保存：id=" + msg.getId() + ", fileUrl=" + msg.getFileUrl());

        User sender = userMapper.selectById(userId);
        GroupMessageVO vo = new GroupMessageVO();
        vo.setId(msg.getId());
        vo.setGroupId(msg.getGroupId());
        vo.setSenderId(userId);
        vo.setSenderNickname(sender != null ? sender.getNickname() : "");
        vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        vo.setContent(msg.getContent());
        vo.setMessageType(msg.getMessageType());
        vo.setFileUrl(msg.getFileUrl());
        vo.setCreateTime(msg.getCreateTime());
        vo.setIsSelf(true);

        webSocketGroupController.sendGroupMessage(vo);

        // 异步检查机器人触发
        try {
            aiBotService.checkAndTriggerBots(dto.getGroupId(), userId, dto.getContent());
        } catch (Exception e) {
            System.err.println("[Bot] 触发检查失败: " + e.getMessage());
        }

        return vo;
    }

    @Override
    public PageResult<GroupMessageVO> getGroupMessages(Long userId, Long groupId, Integer pageNum, Integer pageSize) {
        if (getMember(groupId, userId) == null) {
            throw new BusinessException("你不是群成员");
        }
        Page<GroupMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMessage::getGroupId, groupId).orderByDesc(GroupMessage::getCreateTime);
        Page<GroupMessage> result = groupMessageMapper.selectPage(page, wrapper);

        List<GroupMessageVO> voList = new ArrayList<>();
        for (GroupMessage msg : result.getRecords()) {
            GroupMessageVO vo = new GroupMessageVO();
            vo.setId(msg.getId());
            vo.setGroupId(msg.getGroupId());
            vo.setSenderId(msg.getSenderId());
            vo.setBotId(msg.getBotId());
            vo.setContent(msg.getContent());
            vo.setMessageType(msg.getMessageType());
            vo.setFileUrl(msg.getFileUrl());
            vo.setCreateTime(msg.getCreateTime());
            vo.setIsSelf(msg.getSenderId().equals(userId));
            // 填充发送者信息：优先使用机器人信息
            if (msg.getBotId() != null) {
                AiBot bot = aiBotMapper.selectById(msg.getBotId());
                vo.setBotName(bot != null ? bot.getName() : "AI 机器人");
                vo.setBotAvatar(bot != null ? bot.getAvatar() : null);
                vo.setSenderNickname(bot != null ? bot.getName() : "AI 机器人");
                vo.setSenderAvatar(bot != null ? bot.getAvatar() : null);
            } else {
                User sender = userMapper.selectById(msg.getSenderId());
                vo.setSenderNickname(sender != null ? sender.getNickname() : "未知用户");
                vo.setSenderAvatar(sender != null ? sender.getAvatar() : null);
            }
            voList.add(vo);
        }
        Collections.reverse(voList);
        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public void markRead(Long userId, Long groupId) {
        groupMemberMapper.updateLastReadTime(groupId, userId);
    }

    @Override
    public List<GroupMessageVO> getUnreadGroupMessages(Long userId) {
        return new ArrayList<>();
    }

    // ==================== 私有方法 ====================

    private GroupMember getMember(Long groupId, Long userId) {
        LambdaQueryWrapper<GroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .eq(GroupMember::getDeleted, 0);
        return groupMemberMapper.selectOne(wrapper);
    }

    private void addMember(Long groupId, Long userId, Integer role) {
        // 先物理删除已软删除的旧记录，避免唯一键冲突
        groupMemberMapper.deleteSoftDeleted(groupId, userId);

        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        member.setJoinTime(LocalDateTime.now());
        member.setLastReadTime(LocalDateTime.now());
        member.setDoNotDisturb(0);
        groupMemberMapper.insert(member);
    }

    private GroupVO convertToGroupVO(ChatGroup group, Long currentUserId) {
        GroupMember member = getMember(group.getId(), currentUserId);
        GroupVO vo = new GroupVO();
        BeanUtils.copyProperties(group, vo);
        vo.setMyRole(member != null ? member.getRole() : null);
        vo.setIsDoNotDisturb(member != null && member.getDoNotDisturb() == 1);
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