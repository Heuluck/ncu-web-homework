package com.ncu.chat.service.impl;

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
import com.ncu.chat.websocket.WebSocketGroupController;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {

    private final ChatGroupMapper chatGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final UserMapper userMapper;
    private final WebSocketGroupController webSocketGroupController;

    // 手动构造函数，添加 @Lazy 解决循环依赖
    public GroupServiceImpl(ChatGroupMapper chatGroupMapper,
                            GroupMemberMapper groupMemberMapper,
                            GroupMessageMapper groupMessageMapper,
                            UserMapper userMapper,
                            @Lazy WebSocketGroupController webSocketGroupController) {
        this.chatGroupMapper = chatGroupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.groupMessageMapper = groupMessageMapper;
        this.userMapper = userMapper;
        this.webSocketGroupController = webSocketGroupController;
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
                }
            }
        }

        chatGroupMapper.incrementMemberCount(group.getId());
        return convertToGroupVO(group, userId);
    }

    @Override
    public GroupVO getGroupInfo(Long userId, Long groupId) {
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getDeleted() == 1) {
            throw new RuntimeException("群聊不存在");
        }
        GroupMember member = getMember(groupId, userId);
        if (member == null) {
            throw new RuntimeException("你不是群成员");
        }
        return convertToGroupVO(group, userId);
    }

    @Override
    @Transactional
    public GroupVO updateGroupInfo(Long userId, Long groupId, UpdateGroupDTO dto) {
        GroupMember member = getMember(groupId, userId);
        if (member == null || member.getRole() < 1) {
            throw new RuntimeException("只有群主或管理员可以修改群信息");
        }
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("群聊不存在");
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
            throw new RuntimeException("只有群主可以解散群聊");
        }

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
    }

    @Override
    public List<GroupConversationVO> getMyGroups(Long userId) {
        List<Long> groupIds = groupMemberMapper.getGroupIdsByUserId(userId);
        if (groupIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChatGroup> groups = chatGroupMapper.selectBatchIds(groupIds);
        Map<Long, ChatGroup> groupMap = groups.stream()
                .collect(Collectors.toMap(ChatGroup::getId, g -> g));

        List<GroupConversationVO> result = new ArrayList<>();
        for (Long groupId : groupIds) {
            ChatGroup group = groupMap.get(groupId);
            if (group == null || group.getDeleted() == 1) continue;

            GroupMember member = getMember(groupId, userId);
            if (member == null || member.getDeleted() == 1) continue;

            LambdaQueryWrapper<GroupMessage> lastMsgWrapper = new LambdaQueryWrapper<>();
            lastMsgWrapper.eq(GroupMessage::getGroupId, groupId)
                    .orderByDesc(GroupMessage::getCreateTime).last("LIMIT 1");
            GroupMessage lastMsg = groupMessageMapper.selectOne(lastMsgWrapper);

            GroupConversationVO vo = new GroupConversationVO();
            vo.setGroupId(groupId);
            vo.setGroupName(group.getName());
            vo.setGroupAvatar(group.getAvatar());
            vo.setAnnouncement(group.getAnnouncement());
            if (lastMsg != null) {
                vo.setLastMessage(lastMsg.getContent());
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
            throw new RuntimeException("你不是群成员");
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
            throw new RuntimeException("无权限");
        }
        for (Long targetId : memberIds) {
            if (getMember(groupId, targetId) != null) continue;
            addMember(groupId, targetId, 0);
            chatGroupMapper.incrementMemberCount(groupId);
        }
    }

    @Override
    @Transactional
    public void removeMember(Long userId, Long groupId, Long targetUserId) {
        GroupMember current = getMember(groupId, userId);
        GroupMember target = getMember(groupId, targetUserId);
        if (current == null) throw new RuntimeException("你不是群成员");
        if (target == null) throw new RuntimeException("目标用户不在群中");
        if (current.getRole() == 2) {
            if (targetUserId.equals(userId)) throw new RuntimeException("群主不能踢自己");
        } else if (current.getRole() == 1) {
            if (target.getRole() >= 1) throw new RuntimeException("管理员不能踢群主或其他管理员");
        } else {
            throw new RuntimeException("无权限");
        }

        // 使用 LambdaUpdateWrapper 软删除成员
        LambdaUpdateWrapper<GroupMember> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, targetUserId)
                .set(GroupMember::getDeleted, 1);
        groupMemberMapper.update(null, wrapper);
        chatGroupMapper.decrementMemberCount(groupId);
    }

    @Override
    @Transactional
    public void setAdmin(Long userId, Long groupId, Long targetUserId) {
        GroupMember current = getMember(groupId, userId);
        GroupMember target = getMember(groupId, targetUserId);
        if (current == null || current.getRole() != 2) {
            throw new RuntimeException("只有群主可以设置管理员");
        }
        if (target.getRole() == 2) throw new RuntimeException("不能设置群主为管理员");
        target.setRole(target.getRole() == 1 ? 0 : 1);
        groupMemberMapper.updateById(target);
    }

    @Override
    @Transactional
    public void transferOwner(Long userId, Long groupId, Long targetUserId) {
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null || !group.getOwnerId().equals(userId)) {
            throw new RuntimeException("只有群主可以转让群聊");
        }
        GroupMember newOwner = getMember(groupId, targetUserId);
        if (newOwner == null) throw new RuntimeException("目标用户不在群中");
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
        if (member == null) throw new RuntimeException("你不是群成员");
        member.setDoNotDisturb(enabled ? 1 : 0);
        groupMemberMapper.updateById(member);
    }

    // ==================== 消息 ====================

    @Override
    @Transactional
    public GroupMessageVO sendMessage(Long userId, GroupMessageSendDTO dto) {
        GroupMember member = getMember(dto.getGroupId(), userId);
        if (member == null) throw new RuntimeException("你不是群成员");

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
        vo.setContent(msg.getContent());
        vo.setMessageType(msg.getMessageType());
        vo.setFileUrl(msg.getFileUrl());
        vo.setCreateTime(msg.getCreateTime());
        vo.setIsSelf(true);

        webSocketGroupController.sendGroupMessage(vo);
        return vo;
    }

    @Override
    public PageResult<GroupMessageVO> getGroupMessages(Long userId, Long groupId, Integer pageNum, Integer pageSize) {
        if (getMember(groupId, userId) == null) {
            throw new RuntimeException("你不是群成员");
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
            vo.setContent(msg.getContent());
            vo.setMessageType(msg.getMessageType());
            vo.setCreateTime(msg.getCreateTime());
            vo.setIsSelf(msg.getSenderId().equals(userId));
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
}