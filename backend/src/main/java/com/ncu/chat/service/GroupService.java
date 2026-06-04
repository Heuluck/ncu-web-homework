package com.ncu.chat.service;

import com.ncu.chat.common.PageResult;
import com.ncu.chat.model.dto.CreateGroupDTO;
import com.ncu.chat.model.dto.GroupMessageSendDTO;
import com.ncu.chat.model.dto.UpdateGroupDTO;
import com.ncu.chat.model.vo.*;
import java.util.List;

public interface GroupService {
    GroupVO createGroup(Long userId, CreateGroupDTO dto);
    GroupVO getGroupInfo(Long userId, Long groupId);
    GroupVO updateGroupInfo(Long userId, Long groupId, UpdateGroupDTO dto);
    void disbandGroup(Long userId, Long groupId);
    List<GroupConversationVO> getMyGroups(Long userId);

    List<GroupMemberVO> getGroupMembers(Long userId, Long groupId);
    void inviteMembers(Long userId, Long groupId, List<Long> memberIds);
    void removeMember(Long userId, Long groupId, Long targetUserId);
    void setAdmin(Long userId, Long groupId, Long targetUserId);
    void transferOwner(Long userId, Long groupId, Long targetUserId);
    void setDoNotDisturb(Long userId, Long groupId, Boolean enabled);

    GroupMessageVO sendMessage(Long userId, GroupMessageSendDTO dto);
    PageResult<GroupMessageVO> getGroupMessages(Long userId, Long groupId, Integer pageNum, Integer pageSize);
    void markRead(Long userId, Long groupId);
    List<GroupMessageVO> getUnreadGroupMessages(Long userId);
}